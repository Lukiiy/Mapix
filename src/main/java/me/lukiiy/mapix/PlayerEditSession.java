package me.lukiiy.mapix;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;

public class PlayerEditSession {
    public Location first;
    public Location second;

    public BlockDisplay selectionDisplay;
    public String selectedGroup;

    public boolean displayVisible = true;
    public BossBar editorBar;
}

