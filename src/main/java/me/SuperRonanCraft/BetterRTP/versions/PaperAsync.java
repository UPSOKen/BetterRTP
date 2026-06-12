package me.SuperRonanCraft.BetterRTP.versions;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class PaperAsync {

    private static final Method WORLD_GET_CHUNK_LOCATION_GEN =
            findMethod(World.class, "getChunkAtAsync", Location.class, boolean.class);
    private static final Method WORLD_GET_CHUNK_XZ_GEN_URGENT =
            findMethod(World.class, "getChunkAtAsync", int.class, int.class, boolean.class, boolean.class);
    private static final Method WORLD_GET_CHUNK_XZ_GEN =
            findMethod(World.class, "getChunkAtAsync", int.class, int.class, boolean.class);
    private static final Method ENTITY_TELEPORT_ASYNC =
            findMethod(Entity.class, "teleportAsync", Location.class, PlayerTeleportEvent.TeleportCause.class);

    private static boolean warnedChunkFallback;
    private static boolean warnedTeleportFallback;

    private PaperAsync() {
    }

    public static CompletableFuture<Chunk> getChunkAtAsync(Location loc) {
        return getChunkAtAsync(loc, true);
    }

    @SuppressWarnings("unchecked")
    public static CompletableFuture<Chunk> getChunkAtAsync(Location loc, boolean gen) {
        if (loc == null || loc.getWorld() == null)
            return CompletableFuture.completedFuture(null);

        World world = loc.getWorld();
        try {
            if (WORLD_GET_CHUNK_LOCATION_GEN != null)
                return (CompletableFuture<Chunk>) WORLD_GET_CHUNK_LOCATION_GEN.invoke(world, loc, gen);

            int chunkX = loc.getBlockX() >> 4;
            int chunkZ = loc.getBlockZ() >> 4;
            if (WORLD_GET_CHUNK_XZ_GEN_URGENT != null)
                return (CompletableFuture<Chunk>) WORLD_GET_CHUNK_XZ_GEN_URGENT.invoke(world, chunkX, chunkZ, gen, false);
            if (WORLD_GET_CHUNK_XZ_GEN != null)
                return (CompletableFuture<Chunk>) WORLD_GET_CHUNK_XZ_GEN.invoke(world, chunkX, chunkZ, gen);
        } catch (IllegalAccessException | InvocationTargetException e) {
            warnChunkFallback(e);
        }

        warnChunkFallback(null);
        return PaperLib.getChunkAtAsync(loc, gen);
    }

    public static CompletableFuture<Boolean> teleportAsync(Entity entity, Location loc) {
        return teleportAsync(entity, loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @SuppressWarnings("unchecked")
    public static CompletableFuture<Boolean> teleportAsync(Entity entity, Location loc, PlayerTeleportEvent.TeleportCause cause) {
        if (entity == null || loc == null)
            return CompletableFuture.completedFuture(false);

        try {
            if (ENTITY_TELEPORT_ASYNC != null)
                return (CompletableFuture<Boolean>) ENTITY_TELEPORT_ASYNC.invoke(entity, loc, cause);
        } catch (IllegalAccessException | InvocationTargetException e) {
            warnTeleportFallback(e);
        }

        warnTeleportFallback(null);
        return PaperLib.teleportAsync(entity, loc, cause);
    }

    public static boolean hasNativeAsyncChunks() {
        return WORLD_GET_CHUNK_LOCATION_GEN != null || WORLD_GET_CHUNK_XZ_GEN_URGENT != null || WORLD_GET_CHUNK_XZ_GEN != null;
    }

    public static boolean hasNativeAsyncTeleport() {
        return ENTITY_TELEPORT_ASYNC != null;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static void warnChunkFallback(Throwable throwable) {
        if (warnedChunkFallback)
            return;
        warnedChunkFallback = true;
        logFallback("Paper async chunk API was not usable; falling back to PaperLib chunk loading.", throwable);
    }

    private static void warnTeleportFallback(Throwable throwable) {
        if (warnedTeleportFallback)
            return;
        warnedTeleportFallback = true;
        logFallback("Paper async teleport API was not usable; falling back to PaperLib teleporting.", throwable);
    }

    private static void logFallback(String message, Throwable throwable) {
        if (throwable == null)
            Bukkit.getLogger().warning("[BetterRTP] " + message);
        else
            Bukkit.getLogger().log(Level.WARNING, "[BetterRTP] " + message, throwable);
    }
}
