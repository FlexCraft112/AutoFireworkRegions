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

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int interval = getConfig().getInt("interval-seconds", 10);

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnFireworks();
            }
        }.runTaskTimer(this, 20L, interval * 20L);
    }

    private void spawnFireworks() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String zoneName : zones.getKeys(false)) {
            Location loc = randomLocationInZone(zones.getConfigurationSection(zoneName));
            if (loc != null) {
                spawnFirework(loc);
            }
        }
    }

    private Location randomLocationInZone(ConfigurationSection zone) {
        if (zone == null) return null;

        World world = Bukkit.getWorld(zone.getString("world"));
        if (world == null) return null;

        int minX = zone.getInt("min.x");
        int minY = zone.getInt("min.y");
        int minZ = zone.getInt("min.z");

        int maxX = zone.getInt("max.x");
        int maxY = zone.getInt("max.y");
        int maxZ = zone.getInt("max.z");

        int x = random.nextInt(maxX - minX + 1) + minX;
        int y = random.nextInt(maxY - minY + 1) + minY;
        int z = random.nextInt(maxZ - minZ + 1) + minZ;

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
                .flicker(true)
                .trail(true)
                .build();

        meta.clearEffects();
        meta.addEffect(effect);
        meta.setPower(getConfig().getInt("firework.power", 4));
        fw.setFireworkMeta(meta);
    }
}
