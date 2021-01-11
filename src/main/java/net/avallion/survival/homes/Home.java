package net.avallion.survival.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public final class Home {

    public final String name, world;
    public final int x, y, z;
    public final float pitch, yaw;

    public Home(@NotNull String name, @NotNull String world, int x, int y, int z, float pitch, float yaw) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public Home(@NotNull String name, @NotNull Location location) {
        this(name, location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                location.getPitch(), location.getYaw());
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(world), x + .5, y, z + .5, yaw, pitch);
    }

    public void teleport(@NotNull Entity entity) {
        entity.teleport(getLocation());
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof Home && ((Home) obj).name.equals(name));
    }
}
