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

        int interval = getConfig().getInt("interval-seconds", 2);

        new BukkitRunnable() {
            @Override
            public void run() {
                runUltraShow();
            }
        }.runTaskTimer(this, 40L, interval * 20L);
    }

    // --------------------------------------------------
    // ULTRA SHOW CORE
    // --------------------------------------------------
    private void runUltraShow() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            World world = Bukkit.getWorld(zone.getString("world"));
            if (world == null) continue;

            List<Player> players = getPlayersInZone(world, zone);
            if (players.isEmpty()) continue;

            double tps = Bukkit.getTPS()[0];

            int baseLocations = getConfig().getInt("firework.locations-per-interval", 3);
            int locations = Math.min(6, baseLocations + players.size());

            if (tps < 18.0) locations = Math.max(1, locations / 2);

            for (int i = 0; i < locations; i++) {
                Location loc = findSafeGroundLocation(world, zone);
                if (loc == null) continue;

                int min = getConfig().getInt("firework.burst-min", 5);
                int max = getConfig().getInt("firework.burst-max", 15);
                int burst = min + random.nextInt(max - min + 1);

                if (tps < 18.0) burst = Math.max(3, burst / 2);

                for (int b = 0; b < burst; b++) {
                    Bukkit.getScheduler().runTaskLater(
                            this,
                            () -> spawnUltraFirework(loc),
                            random.nextInt(10)
                    );
                }
            }
        }
    }

    // --------------------------------------------------
    // PLAYERS IN ZONE
    // --------------------------------------------------
    private List<Player> getPlayersInZone(World world, ConfigurationSection zone) {
        List<Player> list = new ArrayList<>();

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
                list.add(p);
            }
        }
        return list;
    }

    // --------------------------------------------------
    // SAFE GROUND DETECTION
    // --------------------------------------------------
    private Location findSafeGroundLocation(World world, ConfigurationSection zone) {
        int minX = zone.getInt("min.x");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxZ = zone.getInt("max.z");

        for (int tries = 0; tries < 20; tries++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;

            int y = world.getHighestBlockYAt(x, z);
            Location base = new Location(world, x + 0.5, y + 1, z + 0.5);

            if (!world.getBlockAt(base).isEmpty()) continue;
            if (!world.getBlockAt(base.clone().add(0, 1, 0)).isEmpty()) continue;
            if (!world.getBlockAt(base.clone().add(0, 2, 0)).isEmpty()) continue;

            return base;
        }
        return null;
    }

    // --------------------------------------------------
    // ULTRA FIREWORK
    // --------------------------------------------------
    private void spawnUltraFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        List<Color> colors = new ArrayList<>();
        for (String s : getConfig().getStringList("firework.colors")) {
            try {
                colors.add((Color) Color.class.getField(s).get(null));
            } catch (Exception ignored) {}
        }
        if (colors.size() < 2) colors.add(Color.WHITE);

        FireworkEffect.Type[] types = {
                FireworkEffect.Type.BALL_LARGE,
                FireworkEffect.Type.BURST,
                FireworkEffect.Type.STAR,
                FireworkEffect.Type.CREEPER
        };

        meta.clearEffects();

        int effects = 3 + random.nextInt(4); // 3–6 эффектов

        for (int i = 0; i < effects; i++) {
            Collections.shuffle(colors);

            FireworkEffect effect = FireworkEffect.builder()
                    .with(types[random.nextInt(types.length)])
                    .withColor(colors.get(0), colors.get(1))
                    .withFade(colors.get(random.nextInt(colors.size())))
                    .trail(true)
                    .flicker(true)
                    .build();

            meta.addEffect(effect);
        }

        meta.setPower(getConfig().getInt("firework.power", 3));
        fw.setFireworkMeta(meta);
    }
}
