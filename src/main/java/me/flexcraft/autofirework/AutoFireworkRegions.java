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

    // 🔥 МЯГКИЙ ЛИМИТ ФЕЙЕРВЕРКОВ НА ЗОНУ
    private static final int MAX_FIREWORKS_PER_ZONE = 120;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int interval = getConfig().getInt("interval-seconds", 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnFireworks();
                cleanupOverflow();
            }
        }.runTaskTimer(this, 0L, interval * 20L);
    }

    // --------------------------------------------------
    // СТАРАЯ ЛОГИКА СПАВНА (НЕ ТРОГАЕМ)
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

            for (int i = 0; i < locations; i++) {
                Location loc = randomLocationInZone(world, zone);

                int burst = random.nextInt(maxBurst - minBurst + 1) + minBurst;
                for (int b = 0; b < burst; b++) {
                    spawnFirework(loc);
                }
            }
        }
    }

    // --------------------------------------------------
    // МЯГКАЯ ЧИСТКА — БЕЗ УБИЙСТВА ШОУ
    // --------------------------------------------------
    private void cleanupOverflow() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            World world = Bukkit.getWorld(zone.getString("world"));
            if (world == null) continue;

            List<Firework> fireworks = new ArrayList<>();

            for (Firework fw : world.getEntitiesByClass(Firework.class)) {
                Location l = fw.getLocation();
                if (isInsideZone(l, zone)) {
                    fireworks.add(fw);
                }
            }

            if (fireworks.size() <= MAX_FIREWORKS_PER_ZONE) continue;

            int toRemove = fireworks.size() - MAX_FIREWORKS_PER_ZONE;
            for (int i = 0; i < toRemove; i++) {
                fireworks.get(i).remove(); // удаляем САМЫЕ СТАРЫЕ
            }
        }
    }

    private boolean isInsideZone(Location l, ConfigurationSection zone) {
        return l.getX() >= zone.getInt("min.x") && l.getX() <= zone.getInt("max.x")
            && l.getY() >= zone.getInt("min.y") && l.getY() <= zone.getInt("max.y")
            && l.getZ() >= zone.getInt("min.z") && l.getZ() <= zone.getInt("max.z");
    }

    // --------------------------------------------------
    // СЛУЧАЙНАЯ ТОЧКА ОТ ЗЕМЛИ
    // --------------------------------------------------
    private Location randomLocationInZone(World world, ConfigurationSection zone) {
        int x = random.nextInt(zone.getInt("max.x") - zone.getInt("min.x") + 1) + zone.getInt("min.x");
        int z = random.nextInt(zone.getInt("max.z") - zone.getInt("min.z") + 1) + zone.getInt("min.z");

        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y + 2, z + 0.5);
    }

    // --------------------------------------------------
    // ФЕЙЕРВЕРК (КРАСИВЫЙ)
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

        FireworkEffect.Type[] types = FireworkEffect.Type.values();

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
