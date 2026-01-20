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

        // ОСНОВНОЙ ТИК ШОУ
        new BukkitRunnable() {
            @Override
            public void run() {
                runShow(false);
            }
        }.runTaskTimer(this, 20L, interval * 20L);

        // МИКРОВОЛНЫ (частые, слабые)
        new BukkitRunnable() {
            @Override
            public void run() {
                runShow(true);
            }
        }.runTaskTimer(this, 40L, 40L); // каждые 2 секунды
    }

    private void runShow(boolean micro) {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            World world = Bukkit.getWorld(zone.getString("world"));
            if (world == null) continue;

            // ✅ ЕСЛИ НЕТ ИГРОКОВ В ЗОНЕ — НИЧЕГО НЕ ДЕЛАЕМ
            if (!isAnyPlayerInZone(world, zone)) continue;

            int locations = micro
                    ? 1
                    : Math.max(1, getConfig().getInt("firework.locations-per-interval", 3));

            for (int i = 0; i < locations; i++) {
                Location loc = findSafeGroundLocation(zone, world);
                if (loc == null) continue;

                int minBurst = getConfig().getInt("firework.burst-min", 3);
                int maxBurst = getConfig().getInt("firework.burst-max", 8);

                if (micro) {
                    minBurst = 2;
                    maxBurst = 4;
                }

                int burst = random.nextInt(maxBurst - minBurst + 1) + minBurst;

                for (int b = 0; b < burst; b++) {
                    spawnFirework(loc.clone().add(0, 0.3, 0));
                }
            }
        }
    }

    // 🔍 ПРОВЕРКА: есть ли игрок в зоне
    private boolean isAnyPlayerInZone(World world, ConfigurationSection zone) {
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

    // 🌍 НАХОДИМ ЗЕМЛЮ + ПРОВЕРКА ЧИСТОГО НЕБА
    private Location findSafeGroundLocation(ConfigurationSection zone, World world) {
        int minX = zone.getInt("min.x");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxZ = zone.getInt("max.z");

        for (int attempts = 0; attempts < 20; attempts++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;

            int y = world.getHighestBlockYAt(x, z);
            Block ground = world.getBlockAt(x, y - 1, z);

            if (!ground.getType().isSolid()) continue;

            // проверяем 6 блоков вверх — должно быть пусто
            boolean clear = true;
            for (int i = 0; i < 6; i++) {
                if (!world.getBlockAt(x, y + i, z).getType().isAir()) {
                    clear = false;
                    break;
                }
            }

            if (!clear) continue;

            return new Location(world, x + 0.5, y, z + 0.5);
        }
        return null;
    }

    // 🎆 СОЗДАНИЕ ФЕЙЕРВЕРКА
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
                FireworkEffect.Type.STAR,
                FireworkEffect.Type.BURST
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
