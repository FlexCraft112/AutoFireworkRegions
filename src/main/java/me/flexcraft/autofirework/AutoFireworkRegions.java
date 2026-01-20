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
                runShow();
            }
        }.runTaskTimer(this, 40L, interval * 20L);
    }

    private void runShow() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String name : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(name);
            if (zone == null) continue;

            World world = Bukkit.getWorld(zone.getString("world"));
            if (world == null) continue;

            // ❗ ЕСЛИ НИ ОДНОГО ИГРОКА В ЗОНЕ — НИЧЕГО НЕ ДЕЛАЕМ
            if (!isPlayerInside(world, zone)) continue;

            int points = Math.max(1,
                    getConfig().getInt("firework.locations-per-interval", 3));

            for (int i = 0; i < points; i++) {
                Location base = findGroundSafeLocation(world, zone);
                if (base == null) continue;

                int min = getConfig().getInt("firework.burst-min", 3);
                int max = getConfig().getInt("firework.burst-max", 8);
                int count = random.nextInt(max - min + 1) + min;

                for (int b = 0; b < count; b++) {
                    spawnFirework(base.clone().add(0, 0.5, 0));
                }
            }
        }
    }

    private boolean isPlayerInside(World world, ConfigurationSection zone) {
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

    // 🔥 ГЛАВНОЕ ИСПРАВЛЕНИЕ
    private Location findGroundSafeLocation(World world, ConfigurationSection zone) {
        int minX = zone.getInt("min.x");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxZ = zone.getInt("max.z");

        for (int attempt = 0; attempt < 80; attempt++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;

            int y = zone.getInt("max.y");

            // ⬇ ИДЁМ ВНИЗ ПОКА НЕ НАЙДЁМ ЗЕМЛЮ
            while (y > zone.getInt("min.y")) {
                Block b = world.getBlockAt(x, y, z);
                if (b.getType().isSolid()) break;
                y--;
            }

            if (y <= zone.getInt("min.y")) continue;

            // 🌤 ПРОВЕРКА НЕБА
            boolean clear = true;
            for (int i = 1; i <= 10; i++) {
                if (!world.getBlockAt(x, y + i, z).getType().isAir()) {
                    clear = false;
                    break;
                }
            }

            if (!clear) continue;

            return new Location(world, x + 0.5, y + 1.2, z + 0.5);
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
                FireworkEffect.Type.STAR
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
