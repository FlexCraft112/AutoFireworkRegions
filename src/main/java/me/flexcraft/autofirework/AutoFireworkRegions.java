package me.flexcraft.autofirework;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
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
        getLogger().info("AutoFireworkRegions enabled");
    }

    private void startTask() {
        int interval = getConfig().getInt("interval-seconds", 10);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (String regionName : getConfig().getStringList("regions")) {
                spawnFireworkInRegion(regionName);
            }
        }, 20L, interval * 20L);
    }

    private void spawnFireworkInRegion(String regionName) {
        for (World world : Bukkit.getWorlds()) {

            RegionManager rm = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (rm == null) continue;

            ProtectedRegion region = rm.getRegion(regionName);
            if (region == null) continue;

            Location loc = randomLocationInRegion(world, region);
            if (loc == null) return;

            Firework fw = world.spawn(loc, Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();

            meta.setPower(getConfig().getInt("firework.power", 2));
            meta.addEffect(buildEffect());

            fw.setFireworkMeta(meta);
            return;
        }
    }

    private Location randomLocationInRegion(World world, ProtectedRegion region) {
        int minX = region.getMinimumPoint().getBlockX();
        int minY = region.getMinimumPoint().getBlockY();
        int minZ = region.getMinimumPoint().getBlockZ();

        int maxX = region.getMaximumPoint().getBlockX();
        int maxY = region.getMaximumPoint().getBlockY();
        int maxZ = region.getMaximumPoint().getBlockZ();

        for (int i = 0; i < 20; i++) {
            int x = rand(minX, maxX);
            int y = rand(minY, maxY);
            int z = rand(minZ, maxZ);

            Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);

            ApplicableRegionSet set = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .createQuery()
                    .getApplicableRegions(BukkitAdapter.adapt(loc));

            for (ProtectedRegion r : set) {
                if (r.getId().equalsIgnoreCase(region.getId())) {
                    return loc;
                }
            }
        }
        return null;
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
                .flicker(random.nextBoolean())
                .trail(random.nextBoolean())
                .build();
    }

    private Color parseColor(String name) {
        try {
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
        } catch (Exception e) {
            return null;
        }
    }

    private int rand(int min, int max) {
        if (max <= min) return min;
        return random.nextInt(max - min + 1) + min;
    }
}
