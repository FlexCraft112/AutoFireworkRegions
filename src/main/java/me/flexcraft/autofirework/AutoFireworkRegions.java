package me.flexcraft.autofirework;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AutoFireworkRegions extends JavaPlugin {

    private final Random random = new Random();

    // 🔥 МАКСИМУМ ОДНОВРЕМЕННЫХ ФЕЙЕРВЕРКОВ В ЗОНЕ
    private static final int MAX_ACTIVE_FIREWORKS = 120;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int interval = getConfig().getInt("interval-seconds", 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnFireworks();
            }
        }.runTaskTimer(this, 0L, interval * 20L);
    }

    // --------------------------------------------------
    // ОСНОВНОЙ SPAWN — КАК В СТАРОЙ ВЕРСИИ
    // --------------------------------------------------
    private void spawnFireworks() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        int locations = getConfig().getInt("firework.locations-per-interval", 1);
        int minBurst = getConfig().getInt("firework.burst-min", 5);
        int maxBurst = getConfig().getInt("firework.burst-max", 15);

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            World world = Bukkit.getWorld(zone.getString("world"));
            if (world == null) continue;

            // 🔥 СЧИТАЕМ АКТИВНЫЕ ФЕЙЕРВЕРКИ В ЗОНЕ
            int active = countFireworksInZone(world, zone);
            if (active >= MAX_ACTIVE_FIREWORKS) continue;

            for (int i = 0; i < locations; i++) {
                if (active >= MAX_ACTIVE_FIREWORKS) break;

                Location loc = randomLocationInZone(world, zone);
                int burst = random.nextInt(maxBurst - minBurst + 1) + minBurst;

                for (int b = 0; b < burst; b++) {
                    if (active >= MAX_ACTIVE_FIREWORKS) break;
                    spawnFirework(loc);
                    active++;
                }
            }
        }
    }

    private int countFireworksInZone(World world, ConfigurationSection zone) {
        int count = 0;
        for (Firework fw : world.getEntitiesByClass(Firework.class)) {
            Location l = fw.getLocation();
            if (isInsideZone(l, zone)) {
                count++;
            }
        }
        return count;
    }

    private boolean isInsideZone(Location l, ConfigurationSection zone) {
        return l.getX() >= zone.getInt("min.x") && l.getX() <= zone.getInt("max.x")
            && l.getY() >= zone.getInt("min.y") && l.getY() <= zone.getInt("max.y")
            && l.getZ() >= zone.getInt("min.z") && l.getZ() <= zone.getInt("max.z");
    }

    private Location randomLocationInZone(World world, ConfigurationSection zone) {
        int x = random.nextInt(zone.getInt("max.x") - zone.getInt("min.x") + 1) + zone.getInt("min.x");
        int z = random.nextInt(zone.getInt("max.z") - zone.getInt("min.z") + 1) + zone.getInt("min.z");
        int y = world.getHighestBlockYAt(x, z);

        return new Location(world, x + 0.5, y + 2, z + 0.5);
    }

    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        List<Color> colors = new ArrayList<>();
        for (String s : getConfig().getStringList("firework.colors")) {
            try {
                colors.add((Color) Color.class.getField(s).get(null));
            } catch (Exception ignored) {}
        }

        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)])
                .withColor(colors)
                .withFade(colors)
                .trail(true)
                .flicker(true)
                .build();

        meta.clearEffects();
        meta.addEffect(effect);
        meta.setPower(getConfig().getInt("firework.power", 3));
        fw.setFireworkMeta(meta);
    }
}
