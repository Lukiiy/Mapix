package me.lukiiy.mapix;

import me.lukiiy.mapling.ManagedWorld;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;

import java.util.List;

public record EditSession(ManagedWorld<World> world, List<TextDisplay> plips, BossBar bar) {}