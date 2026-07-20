package me.lukiiy.mapix;

import me.lukiiy.mapling.Position;
import org.bukkit.Location;
import org.bukkit.World;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Utils {
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));

    public static Position toPos(Location loc) {
        return new Position(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public static Location toLoc(World world, Position pos) {
        return new Location(world, pos.getX(), pos.getY(), pos.getZ(), pos.getYaw(), pos.getPitch());
    }

    public static String toString(Location location) {
        if (location == null) return "Not set!";

        return String.format("%s %s %s : %s %s", DECIMAL_FORMATTER.format(location.x()), DECIMAL_FORMATTER.format(location.y()), DECIMAL_FORMATTER.format(location.z()), DECIMAL_FORMATTER.format(location.getYaw()), DECIMAL_FORMATTER.format(location.getPitch()));
    }
}
