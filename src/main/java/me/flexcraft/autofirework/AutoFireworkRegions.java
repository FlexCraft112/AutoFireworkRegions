package me.flexcraft.autofirework;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class AutoFireworkRegions extends JavaPlugin {

    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startTask();
        getLogger().info("AutoFireworkRegions enabled (COORDINATES MODE)");
    }

    private void startTask() {
        int interval = getConfig().getInt("interval-seconds", 10);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().isConfigurationSection("zones")) return;

            for (String zone : getConfig().getConfigurationSection("zones").getKeys(false)) {
                spawnFireworkInZone(zone);
            }
        }, 20L, interval * 20L);
    }

    private void spawnFireworkInZone(String zone) {
        String path = "zones." + zone;

        World world = Bukkit.getWorld(getConfig().getString(path + ".world"));
        if (world == null) return;

        int minX = getConfig().getInt(path + ".min.x");
        int minY = getConfig().getInt(path + ".min.y");
        int minZ = getConfig().getInt(path + ".min.z");

        int maxX = getConfig().getInt(path + ".max.x");
        int maxY = getConfig().getInt(path + ".max.y");
        int maxZ = getConfig().getInt(path + ".max.z");

        Location loc = new Location(
                world,
                rand(minX, maxX) + 0.5,
                rand(minY, maxY) + 0.5,
                rand(minZ, maxZ) + 0.5
        );

        Firework fw = world.spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        meta.setPower(getConfig().getInt("firework.power", 2));
        meta.addEffect(buildEffect());

        fw.setFireworkMeta(meta);
    }

    private FireworkEffect buildEffect() {
        List<String> cfgColors = getConfig().getStringList("firework.colors");
        List<Color> colors = new ArrayList<>();

        for (String s : cfgColors) {
            Color c = parseColor(s);
            if (c != null) colors.add(c);
        }

        if (colors.isEmpty()) {
            colors.add(Color.RED);
            colors.add(Color.BLUE);
        }

        Collections.shuffle(colors);

        return FireworkEffect.builder()
                .with(FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)])
                .withColor(colors)
                .withFade(Color.WHITE)
                .trail(random.nextBoolean())
                .flicker(random.nextBoolean())
                .build();
    }

    private Color parseColor(String name) {
        switch (name.toUpperCase()) {
            case "RED": return Color.RED;
            case "BLUE": return Color.BLUE;
            case "GREEN": return Color.GREEN;
            case "AQUA": return Color.AQUA;
            case "PURPLE": return Color.PURPLE;
            case "YELLOW": return Color.YELLOW;
            case "ORANGE": return Color.ORANGE;
            case "WHITE": return Color.WHITE;
            case "BLACK": return Color.BLACK;
            case "FUCHSIA": return Color.FUCHSIA;
            case "LIME": return Color.LIME;
            case "NAVY": return Color.NAVY;
            case "MAROON": return Color.MAROON;
            case "TEAL": return Color.TEAL;
            case "SILVER": return Color.SILVER;
            case "GRAY": return Color.GRAY;
            default: return null;
        }
    }

    private int rand(int min, int max) {
        if (max <= min) return min;
        return random.nextInt(max - min + 1) + min;
    }
}
