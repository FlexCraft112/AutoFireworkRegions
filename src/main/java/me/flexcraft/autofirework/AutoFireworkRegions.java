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

        int interval = Math.max(1, getConfig().getInt("interval-seconds", 1));

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnFireworks();
            }
        }.runTaskTimer(this, 0L, interval * 20L);
    }

    // --------------------------------------------------
    // ОСНОВНАЯ ЛОГИКА (СТАРАЯ, РАБОЧАЯ)
    // --------------------------------------------------
    private void spawnFireworks() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            World world = Bukkit.getWorld(zone.getString("world"));
            if (world == null) continue;

            // ❗ КЛЮЧЕВОЙ ФИКС — ЕСЛИ НЕТ ИГРОКОВ, НЕ СПАВНИМ
            if (!hasPlayerInZone(world, zone)) {
                continue;
            }

            int locations = getConfig().getInt("firework.locations-per-interval", 1);
            int minBurst = getConfig().getInt("firework.burst-min", 5);
            int maxBurst = getConfig().getInt("firework.burst-max", 10);

            for (int i = 0; i < locations; i++) {
                Location loc = randomLocationInZone(world, zone);

                int burst = (minBurst == maxBurst)
                        ? minBurst
                        : random.nextInt(maxBurst - minBurst + 1) + minBurst;

                for (int f = 0; f < burst; f++) {
                    spawnFirework(loc);
                }
            }
        }
    }

    // --------------------------------------------------
    // ПРОВЕРКА ИГРОКОВ В ЗОНЕ (ПРОСТО И НАДЁЖНО)
    // --------------------------------------------------
    private boolean hasPlayerInZone(World world, ConfigurationSection zone) {
        int minX = zone.getInt("min.x");
        int minY = zone.getInt("min.y");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxY = zone.getInt("max.y");
        int maxZ = zone.getInt("max.z");

        for (Player p : world.getPlayers()) {
            Location l = p.getLocation();
            if (l.getX() >= minX && l.getX() <= maxX
                    && l.getY() >= minY && l.getY() <= maxY
                    && l.getZ() >= minZ && l.getZ() <= maxZ) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------
    // СЛУЧАЙНАЯ ТОЧКА В ЗОНЕ (КАК РАНЬШЕ)
    // --------------------------------------------------
    private Location randomLocationInZone(World world, ConfigurationSection zone) {
        int minX = zone.getInt("min.x");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxZ = zone.getInt("max.z");

        int x = random.nextInt(maxX - minX + 1) + minX;
        int z = random.nextInt(maxZ - minZ + 1) + minZ;

        int ground = world.getHighestBlockYAt(x, z);
        int y = ground + 2; // 🔥 как было, от земли

        return new Location(world, x + 0.5, y, z + 0.5);
    }

    // --------------------------------------------------
    // СПАВН ФЕЙЕРВЕРКА (БЕЗ ОЧЕРЕДЕЙ)
    // --------------------------------------------------
    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        List<Color> colors = new ArrayList<>();
        for (String s : getConfig().getStringList("firework.colors")) {
            try {
                colors.add((Color) Color.class.getField(s).get(null));
            } catch (Exception ignored) {}
        }

        if (colors.isEmpty()) {
            colors.add(Color.RED);
            colors.add(Color.AQUA);
            colors.add(Color.LIME);
        }

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
                .trail(true)
                .flicker(true)
                .build();

        meta.clearEffects();
        meta.addEffect(effect);
        meta.setPower(getConfig().getInt("firework.power", 3));
        fw.setFireworkMeta(meta);
    }
}
