package me.flexcraft.autofirework;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class AutoFireworkRegions extends JavaPlugin {

    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startFireworkTask();
        getLogger().info("AutoFireworkRegions enabled");
    }

    private void startFireworkTask() {
        int interval = getConfig().getInt("interval-seconds", 10);

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnFireworks();
            }
        }.runTaskTimer(this, 20L, interval * 20L);
    }

    private void spawnFireworks() {
        WorldGuardPlugin wg = getWorldGuard();
        if (wg == null) return;

        List<String> regionNames = getConfig().getStringList("regions");
        if (regionNames.isEmpty()) return;

        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (manager == null) continue;

            for (String regionName : regionNames) {
                ProtectedRegion region = manager.getRegion(regionName);
                if (region == null) continue;

                Location loc = getRandomLocationInRegion(world, region);
                if (loc != null) {
                    spawnFirework(loc);
                }
            }
        }
    }

    private Location getRandomLocationInRegion(World world, ProtectedRegion region) {
        int minX = region.getMinimumPoint().getBlockX();
        int minY = region.getMinimumPoint().getBlockY();
        int minZ = region.getMinimumPoint().getBlockZ();

        int maxX = region.getMaximumPoint().getBlockX();
        int maxY = region.getMaximumPoint().getBlockY();
        int maxZ = region.getMaximumPoint().getBlockZ();

        for (int i = 0; i < 10; i++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            if (region.contains(
                    BukkitAdapter.asBlockVector(loc)
            )) {
                return loc;
            }
        }
        return null;
    }

    private void spawnFirework(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect.Type type = FireworkEffect.Type.values()
                [random.nextInt(FireworkEffect.Type.values().length)];

        List<String> colorsCfg = getConfig().getStringList("firework.colors");
        Color c1 = Color.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255));

        if (!colorsCfg.isEmpty()) {
            try {
                c1 = Color.valueOf(colorsCfg.get(random.nextInt(colorsCfg.size())));
            } catch (Exception ignored) {}
        }

        FireworkEffect effect = FireworkEffect.builder()
                .with(type)
                .withColor(c1)
                .flicker(true)
                .trail(true)
                .build();

        meta.addEffect(effect);
        meta.setPower(getConfig().getInt("firework.power", 2));
        firework.setFireworkMeta(meta);
    }

    private WorldGuardPlugin getWorldGuard() {
        return (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
    }
}
