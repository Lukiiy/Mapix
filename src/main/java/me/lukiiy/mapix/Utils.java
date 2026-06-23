package me.lukiiy.mapix;

import me.lukiiy.mapling.Position;
import org.bukkit.Location;
import org.bukkit.World;

public class Utils {
    public static Position toPos(Location loc) {
        return new Position(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public static Location toLoc(World world, Position pos) {
        return new Location(world, pos.getX(), pos.getY(), pos.getZ(), pos.getYaw(), pos.getPitch());
    }
}
