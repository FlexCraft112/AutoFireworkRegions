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
                spawnInWorld(world);
            }
        }, 40L, interval);
    }

    private void spawnInWorld(World world) {
        RegionManager rm = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(world));

        if (rm == null) return;

        for (String regionName : getConfig().getStringList("regions")) {
            ProtectedRegion region = rm.getRegion(regionName);
            if (region == null) continue;

            Location loc = randomLocationInRegion(world, region);
            if (loc != null) {
                spawnFirework(loc);
            }
        }
    }

    private Location randomLocationInRegion(World world, ProtectedRegion region) {
        int minX = region.getMinimumPoint().getBlockX();
        int minY = region.getMinimumPoint().getBlockY();
        int minZ = region.getMinimumPoint().getBlockZ();

        int maxX = region.getMaximumPoint().getBlockX();
        int maxY = region.getMaximumPoint().getBlockY();
        int maxZ = region.getMaximumPoint().getBlockZ();

        for (int i = 0; i < 15; i++) { // 15 попыток найти точку
            int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
            int y = ThreadLocalRandom.current().nextInt(minY, maxY + 1);
            int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);

            if (region.contains(x, y, z)) {
                Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                if (loc.getBlock().getType().isAir()) {
                    return loc;
                }
            }
        }
        return null;
    }

    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        List<Color> available = new ArrayList<>();
        for (String c : getConfig().getStringList("firework.colors")) {
            try {
                available.add(Color.fromRGB(Color.valueOf(c).asRGB()));
            } catch (Exception ignored) {}
        }

        Collections.shuffle(available);

        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.values()[ThreadLocalRandom.current().nextInt(FireworkEffect.Type.values().length)])
                .withColor(available.subList(0, Math.min(2, available.size())))
                .withFade(available.get(ThreadLocalRandom.current().nextInt(available.size())))
                .flicker(ThreadLocalRandom.current().nextBoolean())
                .trail(ThreadLocalRandom.current().nextBoolean())
                .build();

        meta.clearEffects();
        meta.addEffect(effect);
        meta.setPower(getConfig().getInt("firework.power", 2));
        fw.setFireworkMeta(meta);
    }
}
