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
                runShow();
            }
        }.runTaskTimer(this, 20L, interval * 20L);
    }

    // --------------------------------------------------
    // MAIN LOGIC
    // --------------------------------------------------
    private void runShow() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            World world = Bukkit.getWorld(zone.getString("world"));
            if (world == null) continue;

            List<Player> players = playersInZone(world, zone);
            if (players.isEmpty()) continue; // нет игроков — нет шоу

            int baseLocations = getConfig().getInt("firework.locations-per-interval", 3);
            int locations = Math.min(baseLocations + players.size() * 2, 12);

            for (int i = 0; i < locations; i++) {
                Location base = findGround(world, zone);
                if (base == null) continue;

                int min = getConfig().getInt("firework.burst-min", 5);
                int max = getConfig().getInt("firework.burst-max", 15);
                int burst = min + random.nextInt(max - min + 1);

                for (int b = 0; b < burst; b++) {
                    Location launch = base.clone().add(
                            random.nextDouble() * 4 - 2,
                            0,
                            random.nextDouble() * 4 - 2
                    );

                    Bukkit.getScheduler().runTaskLater(
                            this,
                            () -> spawnSkyFirework(launch),
                            random.nextInt(8)
                    );
                }
            }
        }
    }

    // --------------------------------------------------
    // PLAYERS CHECK
    // --------------------------------------------------
    private List<Player> playersInZone(World world, ConfigurationSection z) {
        List<Player> list = new ArrayList<>();

        int minX = z.getInt("min.x");
        int minY = z.getInt("min.y");
        int minZ = z.getInt("min.z");
        int maxX = z.getInt("max.x");
        int maxY = z.getInt("max.y");
        int maxZ = z.getInt("max.z");

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
    // SAFE GROUND FINDER
    // --------------------------------------------------
    private Location findGround(World world, ConfigurationSection z) {
        int minX = z.getInt("min.x");
        int minZ = z.getInt("min.z");
        int maxX = z.getInt("max.x");
        int maxZ = z.getInt("max.z");

        for (int i = 0; i < 30; i++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int zed = random.nextInt(maxZ - minZ + 1) + minZ;

            int y = world.getHighestBlockYAt(x, zed);
            Location loc = new Location(world, x + 0.5, y + 1, zed + 0.5);

            if (world.getBlockAt(loc).isEmpty()
                    && world.getBlockAt(loc.clone().add(0, 1, 0)).isEmpty()
                    && world.getBlockAt(loc.clone().add(0, 2, 0)).isEmpty()) {
                return loc;
            }
        }
        return null;
    }

    // --------------------------------------------------
    // ULTRA SKY FIREWORK
    // --------------------------------------------------
    private void spawnSkyFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        List<Color> colors = new ArrayList<>();
        for (String s : getConfig().getStringList("firework.colors")) {
            try {
                colors.add((Color) Color.class.getField(s).get(null));
            } catch (Exception ignored) {}
        }
        if (colors.size() < 2) {
            colors.add(Color.RED);
            colors.add(Color.AQUA);
            colors.add(Color.YELLOW);
        }

        FireworkEffect.Type[] types = {
                FireworkEffect.Type.BALL_LARGE,
                FireworkEffect.Type.BURST,
                FireworkEffect.Type.STAR,
                FireworkEffect.Type.CREEPER
        };

        meta.clearEffects();

        int effectCount = 4 + random.nextInt(5); // 4–8 эффектов

        for (int i = 0; i < effectCount; i++) {
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
