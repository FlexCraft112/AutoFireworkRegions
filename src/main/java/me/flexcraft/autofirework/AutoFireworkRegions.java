package me.flexcraft.autofirework;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AutoFireworkRegions extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startTask();
    }

    private void startTask() {
        int interval = getConfig().getInt("interval-seconds", 10) * 20;

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                handleWorld(world);
            }
        }, 40L, interval);
    }

    private void handleWorld(World world) {
        RegionManager rm = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(world));

        if (rm == null) return;

        for (String regionName : getConfig().getStringList("regions")) {
            ProtectedRegion region = rm.getRegion(regionName);
            if (region == null) continue;

            Location loc = randomLocationInRegion(world, region);
            if (loc != null) spawnFirework(loc);
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
            int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
            int y = ThreadLocalRandom.current().nextInt(minY, maxY + 1);
            int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);

            if (region.contains(x, y, z)) {
                Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                if (loc.getBlock().getType().isAir()) return loc;
            }
        }
        return null;
    }

    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        List<Color> colors = new ArrayList<>();
        for (String name : getConfig().getStringList("firework.colors")) {
            try {
                colors.add(DyeColor.valueOf(name).getColor());
            } catch (Exception ignored) {}
        }

        if (colors.isEmpty()) {
            colors.add(Color.RED);
        }

        Collections.shuffle(colors);

        int count = ThreadLocalRandom.current().nextInt(1, Math.min(3, colors.size()) + 1);
        List<Color> mainColors = colors.subList(0, count);
        Color fade = colors.get(ThreadLocalRandom.current().nextInt(colors.size()));

        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.values()[ThreadLocalRandom.current().nextInt(FireworkEffect.Type.values().length)])
                .withColor(mainColors)
                .withFade(fade)
                .trail(ThreadLocalRandom.current().nextBoolean())
                .flicker(ThreadLocalRandom.current().nextBoolean())
                .build();

        meta.clearEffects();
        meta.addEffect(effect);
        meta.setPower(getConfig().getInt("firework.power", 2));
        fw.setFireworkMeta(meta);
    }
}
