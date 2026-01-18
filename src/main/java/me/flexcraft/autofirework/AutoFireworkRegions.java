package me.flexcraft.autofirework;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoFireworkRegions extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getLogger().info("[AutoFireworkRegions] Enabled");
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("[AutoFireworkRegions] Disabled");
    }
}
