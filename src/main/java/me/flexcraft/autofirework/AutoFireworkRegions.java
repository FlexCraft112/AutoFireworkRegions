package me.flexcraft.autofirework;

import org.bukkit.*;
import org.bukkit.block.Block;
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
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            Location loc = randomGroundLocation(zone);
            if (loc != null) {
                spawnFirework(loc);
            }
        }
    }

    /**
     * ВСЕГДА берём верхний твёрдый блок
     */
    private Location randomGroundLocation(ConfigurationSection zone) {
        World world = Bukkit.getWorld(zone.getString("world"));
        if (world == null) return null;

        int minX = zone.getInt("min.x");
        int maxX = zone.getInt("max.x");
        int minZ = zone.getInt("min.z");
        int maxZ = zone.getInt("max.z");

        int x = random.nextInt(maxX - minX + 1) + minX;
        int z = random.nextInt(maxZ - minZ + 1) + minZ;

        Block ground = world.getHighestBlockAt(x, z);

        // защита от воздуха и пустых чанков
        if (ground.getType() == Material.AIR) {
            return null;
        }

        // +1 чтобы фейерверк стоял НА земле
        return ground.getLocation().add(0.5, 1.0, 0.5);
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

        if (colors.isEmpty()) {
            colors.add(Color.WHITE);
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
