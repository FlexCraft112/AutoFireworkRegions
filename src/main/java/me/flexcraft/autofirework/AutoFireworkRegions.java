package me.flexcraft.autofirework;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AutoFireworkRegions extends JavaPlugin {

    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int interval = Math.max(1, getConfig().getInt("interval-seconds", 2));

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnShow();
            }
        }.runTaskTimer(this, 40L, interval * 20L);
    }

    private void spawnShow() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            World world = Bukkit.getWorld(zone.getString("world"));
            if (world == null) continue;

            // ❗ КЛЮЧЕВОЕ: если в зоне нет игроков — НИЧЕГО НЕ ДЕЛАЕМ
            if (!hasPlayerInside(world, zone)) continue;

            int locations = Math.max(1,
                    getConfig().getInt("firework.locations-per-interval", 3));

            for (int i = 0; i < locations; i++) {
                Location loc = findGroundLocation(world, zone);
                if (loc == null) continue;

                int minBurst = getConfig().getInt("firework.burst-min", 3);
                int maxBurst = getConfig().getInt("firework.burst-max", 8);
                int burst = random.nextInt(maxBurst - minBurst + 1) + minBurst;

                for (int b = 0; b < burst; b++) {
                    spawnFirework(loc.clone().add(0, 0.2, 0));
                }
            }
        }
    }

    // 🔍 ПРОВЕРКА ИГРОКА В ЗОНЕ
    private boolean hasPlayerInside(World world, ConfigurationSection zone) {
        int minX = zone.getInt("min.x");
        int minY = zone.getInt("min.y");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxY = zone.getInt("max.y");
        int maxZ = zone.getInt("max.z");

        for (Player p : world.getPlayers()) {
            Location l = p.getLocation();
            if (l.getBlockX() >= minX && l.getBlockX() <= maxX
                    && l.getBlockY() >= minY && l.getBlockY() <= maxY
                    && l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ) {
                return true;
            }
        }
        return false;
    }

    // 🌍 ВСЕГДА С ЗЕМЛИ + ПРОВЕРКА НЕБА
    private Location findGroundLocation(World world, ConfigurationSection zone) {
        int minX = zone.getInt("min.x");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxZ = zone.getInt("max.z");

        for (int attempt = 0; attempt < 30; attempt++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;

            int y = world.getHighestBlockYAt(x, z);
            Block ground = world.getBlockAt(x, y - 1, z);

            if (!ground.getType().isSolid()) continue;

            // Проверка свободного неба
            boolean free = true;
            for (int i = 0; i < 7; i++) {
                if (!world.getBlockAt(x, y + i, z).getType().isAir()) {
                    free = false;
                    break;
                }
            }

            if (!free) continue;

            return new Location(world, x + 0.5, y, z + 0.5);
        }
        return null;
    }

    // 🎆 ФЕЙЕРВЕРК
    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        List<Color> colors = new ArrayList<>();
        for (String c : getConfig().getStringList("firework.colors")) {
            try {
                colors.add((Color) Color.class.getField(c).get(null));
            } catch (Exception ignored) {}
        }
        if (colors.isEmpty()) colors.add(Color.WHITE);

        FireworkEffect.Type[] types = {
                FireworkEffect.Type.BALL,
                FireworkEffect.Type.BALL_LARGE,
                FireworkEffect.Type.BURST,
                FireworkEffect.Type.STAR
        };

        FireworkEffect effect = FireworkEffect.builder()
                .with(types[random.nextInt(types.length)])
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
