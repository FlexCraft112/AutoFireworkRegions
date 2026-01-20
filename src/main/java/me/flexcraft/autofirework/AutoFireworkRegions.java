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
                fireTick();
            }
        }.runTaskTimer(this, 0L, interval * 20L);
    }

    // --------------------------------------------------
    // MAIN LOGIC — ЧЁТКО ПО КОНФИГУ
    // --------------------------------------------------
    private void fireTick() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection z = zones.getConfigurationSection(zoneName);
            if (z == null) continue;

            World world = Bukkit.getWorld(z.getString("world"));
            if (world == null) continue;

            if (!playerInZone(world, z)) continue;

            int locations = getConfig().getInt("firework.locations-per-interval", 1);
            int min = getConfig().getInt("firework.burst-min", 5);
            int max = getConfig().getInt("firework.burst-max", 15);

            for (int i = 0; i < locations; i++) {
                Location base = skyLocation(world, z);
                int burst = min == max ? min : random.nextInt(max - min + 1) + min;

                for (int b = 0; b < burst; b++) {
                    spawnFirework(base.clone().add(
                            random.nextDouble() * 6 - 3,
                            0,
                            random.nextDouble() * 6 - 3
                    ));
                }
            }
        }
    }

    // --------------------------------------------------
    // CHECK PLAYER PRESENCE
    // --------------------------------------------------
    private boolean playerInZone(World world, ConfigurationSection z) {
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
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------
    // SAFE SKY SPAWN (KEY FIX)
    // --------------------------------------------------
    private Location skyLocation(World world, ConfigurationSection z) {
        int minX = z.getInt("min.x");
        int minZ = z.getInt("min.z");
        int maxX = z.getInt("max.x");
        int maxZ = z.getInt("max.z");

        int x = random.nextInt(maxX - minX + 1) + minX;
        int zed = random.nextInt(maxZ - minZ + 1) + minZ;

        int y = world.getHighestBlockYAt(x, zed) + 25 + random.nextInt(15);

        return new Location(world, x + 0.5, y, zed + 0.5);
    }

    // --------------------------------------------------
    // FIREWORK EFFECT (УЛЬТРА)
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
        if (colors.size() < 3) {
            colors = Arrays.asList(Color.RED, Color.AQUA, Color.LIME, Color.YELLOW);
        }

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
                .trail(true)
                .flicker(true)
                .build();

        meta.clearEffects();
        meta.addEffect(effect);
        meta.setPower(getConfig().getInt("firework.power", 3));
        fw.setFireworkMeta(meta);
    }
}
