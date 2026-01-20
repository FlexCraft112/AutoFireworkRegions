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

    // храним активные фейерверки по зонам
    private final Map<String, List<Firework>> activeFireworks = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int interval = getConfig().getInt("interval-seconds", 2);

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnFireworksSafe();
            }
        }.runTaskTimer(this, 20L, interval * 20L);
    }

    private void spawnFireworksSafe() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        int maxPerZone = getConfig().getInt("max-fireworks-per-zone", 25);
        int lifeTime = getConfig().getInt("firework-lifetime-seconds", 6) * 20;

        for (String zoneName : zones.getKeys(false)) {

            activeFireworks.putIfAbsent(zoneName, new ArrayList<>());
            List<Firework> list = activeFireworks.get(zoneName);

            // чистим мёртвые
            list.removeIf(fw -> fw.isDead() || !fw.isValid());

            // если лимит превышен — НЕ СПАВНИМ
            if (list.size() >= maxPerZone) continue;

            int points = getConfig().getInt("firework.locations-per-interval", 1);

            for (int i = 0; i < points; i++) {
                if (list.size() >= maxPerZone) break;

                Location loc = randomLocationInZone(zones.getConfigurationSection(zoneName));
                if (loc == null) continue;

                int burstMin = getConfig().getInt("firework.burst-min", 1);
                int burstMax = getConfig().getInt("firework.burst-max", 2);
                int burst = random.nextInt(burstMax - burstMin + 1) + burstMin;

                for (int b = 0; b < burst && list.size() < maxPerZone; b++) {
                    Firework fw = spawnFirework(loc);
                    list.add(fw);

                    // авто-удаление
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (fw.isValid()) fw.remove();
                    }, lifeTime);
                }
            }
        }
    }

    private Location randomLocationInZone(ConfigurationSection zone) {
        if (zone == null) return null;

        World world = Bukkit.getWorld(zone.getString("world"));
        if (world == null) return null;

        int minX = zone.getInt("min.x");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxZ = zone.getInt("max.z");

        int x = random.nextInt(maxX - minX + 1) + minX;
        int z = random.nextInt(maxZ - minZ + 1) + minZ;

        int y = world.getHighestBlockYAt(x, z) + 1;

        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private Firework spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        List<Color> colors = new ArrayList<>();
        for (String s : getConfig().getStringList("firework.colors")) {
            try {
                colors.add((Color) Color.class.getField(s).get(null));
            } catch (Exception ignored) {}
        }

        if (colors.isEmpty()) colors.add(Color.WHITE);

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

        return fw;
    }
}
