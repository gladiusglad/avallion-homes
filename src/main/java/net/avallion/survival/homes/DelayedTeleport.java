package net.avallion.survival.homes;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

public class DelayedTeleport implements Runnable {

    private static final double MOVE_THRESHOLD = 0.8;
    private final AvallionHomes plugin;
    private final Player player;
    private int taskId;
    private final long startX, startY, startZ, tpTime;
    private double health;
    private final boolean canMove;
    private final Consumer<Player> tpFunc;

    public DelayedTeleport(AvallionHomes plugin, Player player, int delay, Consumer<Player> tpFunc) {
        this.plugin = plugin;
        this.player = player;
        Location location = player.getLocation();
        health = player.getHealth();
        startX = Math.round(location.getX() * MOVE_THRESHOLD);
        startY = Math.round(location.getY() * MOVE_THRESHOLD);
        startZ = Math.round(location.getZ() * MOVE_THRESHOLD);
        canMove = player.hasPermission("avallionhomes.home.delay.move");
        this.tpFunc = tpFunc;
        this.tpTime = System.currentTimeMillis() + delay;

        taskId = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this, 10, 10).getTaskId();
    }

    @Override
    public void run() {
        if (player == null) {
            cancelTp(false);
            return;
        }

        Location location = player.getLocation();

        if (!canMove &&
                (Math.round(location.getX() * MOVE_THRESHOLD) != startX ||
                Math.round(location.getY() * MOVE_THRESHOLD) != startY ||
                Math.round(location.getZ() * MOVE_THRESHOLD) != startZ ||
                player.getHealth() < health)) {
            cancelTp(true);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                health = player.getHealth();
                if (System.currentTimeMillis() > tpTime) {
                    cancelTp(false);
                    plugin.msg.send(player, "Teleportation commencing...");

                    tpFunc.accept(player);
                }
            }
        }.runTask(plugin);
    }

    public void cancelTp(boolean messagePlayer) {
        if (taskId == -1) return;
        plugin.getServer().getScheduler().cancelTask(taskId);
        if (messagePlayer) {
            plugin.msg.error(player, "You moved; home teleportation cancelled.");
        }
        taskId = -1;
    }
}
