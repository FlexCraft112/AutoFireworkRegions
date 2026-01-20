package me.flexcraft.autofirework;

import org.bukkit.*;
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

        int interval = getConfig().getInt("interval-seconds", 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnFireworks();
            }
        }.runTaskTimer(this, 0L, interval * 20L);
    }

    private void spawnFireworks() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            World world = Bukkit.getWorld(zone.getString("world"));
            if (world == null) continue;

            // 🔴 КЛЮЧЕВОЕ: если в зоне нет игроков — НИЧЕГО НЕ ДЕЛАЕМ
            if (!hasPlayersInZone(world, zone)) {
                continue;
            }

            int locations = getConfig().getInt("firework.locations-per-interval", 3);
            int burstMin = getConfig().getInt("firework.burst-min", 5);
            int burstMax = getConfig().getInt("firework.burst-max", 15);

            for (int i = 0; i < locations; i++) {
                Location loc = randomLocationInZone(world, zone);
                if (loc == null) continue;

                int burst = random.nextInt(burstMax - burstMin + 1) + burstMin;

                for (int b = 0; b < burst; b++) {
                    spawnFirework(loc);
                }
            }
        }
    }

    // ✅ Проверка: есть ли ХОТЯ БЫ 1 игрок в зоне
    private boolean hasPlayersInZone(World world, ConfigurationSection zone) {
        int minX = Math.min(zone.getInt("min.x"), zone.getInt("max.x"));
        int minY = Math.min(zone.getInt("min.y"), zone.getInt("max.y"));
        int minZ = Math.min(zone.getInt("min.z"), zone.getInt("max.z"));

        int maxX = Math.max(zone.getInt("min.x"), zone.getInt("max.x"));
        int maxY = Math.max(zone.getInt("min.y"), zone.getInt("max.y"));
        int maxZ = Math.max(zone.getInt("min.z"), zone.getInt("max.z"));

        for (Player player : world.getPlayers()) {
            Location l = player.getLocation();
            if (
                    l.getX() >= minX && l.getX() <= maxX &&
                    l.getY() >= minY && l.getY() <= maxY &&
                    l.getZ() >= minZ && l.getZ() <= maxZ
            ) {
                return true;
            }
        }
        return false;
    }

    // 🔥 Старая логика — БЕЗ ЛИМИТОВ
    private Location randomLocationInZone(World world, ConfigurationSection zone) {
        int minX = Math.min(zone.getInt("min.x"), zone.getInt("max.x"));
        int minY = Math.min(zone.getInt("min.y"), zone.getInt("max.y"));
        int minZ = Math.min(zone.getInt("min.z"), zone.getInt("max.z"));

        int maxX = Math.max(zone.getInt("min.x"), zone.getInt("max.x"));
        int maxY = Math.max(zone.getInt("min.y"), zone.getInt("max.y"));
        int maxZ = Math.max(zone.getInt("min.z"), zone.getInt("max.z"));

        int x = random.nextInt(maxX - minX + 1) + minX;
        int z = random.nextInt(maxZ - minZ + 1) + minZ;

        int y = world.getHighestBlockYAt(x, z) + 1;

        return new Location(world, x + 0.5, y, z + 0.5);
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
