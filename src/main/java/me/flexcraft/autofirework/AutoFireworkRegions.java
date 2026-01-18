package me.flexcraft.autofirework;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoFireworkRegions extends JavaPlugin {

    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startFireworksTask();
        getLogger().info("AutoFireworkRegions enabled");
    }

    private void startFireworksTask() {
        FileConfiguration cfg = getConfig();

        int interval = cfg.getInt("interval-seconds", 10);
        List<String> regions = cfg.getStringList("regions");
        int power = cfg.getInt("firework.power", 2);
        List<String> colorNames = cfg.getStringList("firework.colors");

        List<Color> colors = new ArrayList<>();
        for (String name : colorNames) {
            try {
                colors.add(DyeColor.valueOf(name.toUpperCase()).getColor());
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Invalid color in config: " + name);
            }
        }

        if (colors.isEmpty()) {
            colors.add(Color.WHITE);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (String regionName : regions) {
                        spawnFireworkInRegion(world, regionName, colors, power);
                    }
                }
            }
        }.runTaskTimer(this, 20L, interval * 20L);
    }

    private void spawnFireworkInRegion(World world, String regionName, List<Color> colors, int power) {
        // ⚠ Пока заглушка — дальше можно подключить WorldGuard
        Location loc = world.getSpawnLocation().clone().add(
                random.nextInt(10) - 5,
                random.nextInt(5) + 2,
                random.nextInt(10) - 5
        );

        Firework fw = world.spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(colors.get(random.nextInt(colors.size())))
                .withFade(Color.WHITE)
                .flicker(true)
                .trail(true)
                .build();

        meta.addEffect(effect);
        meta.setPower(power);
        fw.setFireworkMeta(meta);
    }
}
