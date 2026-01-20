package me.flexcraft.autofirework;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AutoFireworkRegions extends JavaPlugin {

    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int interval = getConfig().getInt("interval-seconds", 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnFireworks();
            }
        }.runTaskTimer(this, 20L, interval * 20L);
    }

    private void spawnFireworks() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        int locationsPerInterval = getConfig().getInt("firework.locations-per-interval", 3);
        int burstMin = getConfig().getInt("firework.burst-min", 5);
        int burstMax = getConfig().getInt("firework.burst-max", 15);

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            for (int i = 0; i < locationsPerInterval; i++) {
                Location baseLoc = findSafeGroundLocation(zone);
                if (baseLoc == null) continue;

                int burst = random.nextInt(burstMax - burstMin + 1) + burstMin;

                for (int j = 0; j < burst; j++) {
                    Location offset = baseLoc.clone().add(
                            random.nextDouble() * 2 - 1,
                            0,
                            random.nextDouble() * 2 - 1
                    );

                    spawnFirework(offset);
                }
            }
        }
    }

    private Location findSafeGroundLocation(ConfigurationSection zone) {
        World world = Bukkit.getWorld(zone.getString("world"));
        if (world == null) return null;

        int minX = zone.getInt("min.x");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxZ = zone.getInt("max.z");

        for (int i = 0; i < 25; i++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;

            Block ground = world.getHighestBlockAt(x, z);
            if (ground.getType().isAir()) continue;

            boolean clear = true;
            for (int y = 1; y <= 6; y++) {
                if (!ground.getRelative(0, y, 0).getType().isAir()) {
                    clear = false;
                    break;
                }
            }

            if (!clear) continue;

            return ground.getLocation().add(0.5, 1.0, 0.5);
        }

        return null;
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
        if (colors.isEmpty()) colors.add(Color.WHITE);

        FireworkEffect.Type[] types = {
                FireworkEffect.Type.BALL,
                FireworkEffect.Type.BALL_LARGE,
                FireworkEffect.Type.BURST,
                FireworkEffect.Type.STAR,
                FireworkEffect.Type.CREEPER
        };

        FireworkEffect effect = FireworkEffect.builder()
                .with(types[random.nextInt(types.length)])
                .withColor(colors)
                .withFade(colors)
                .flicker(true)
                .trail(true)
                .build();

        meta.clearEffects();
        meta.addEffect(effect);
        meta.setPower(getConfig().getInt("firework.power", 3));
        fw.setFireworkMeta(meta);
    }
}
