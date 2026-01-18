package me.flexcraft.autofirework;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AutoFireworkRegions extends JavaPlugin {

    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startTask();
    }

    private void startTask() {
        FileConfiguration cfg = getConfig();
        int interval = cfg.getInt("interval-seconds", 10);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (String regionName : cfg.getStringList("regions")) {
                spawnInRegion(regionName);
            }
        }, 20L, interval * 20L);
    }

    private void spawnInRegion(String regionName) {
        for (World world : Bukkit.getWorlds()) {

            RegionManager manager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (manager == null) continue;

            ProtectedRegion region = manager.getRegion(regionName);
            if (region == null) continue;

            Location loc = randomLocationInRegion(world, region, manager);
            if (loc == null) continue;

            spawnFirework(loc);
        }
    }

    private Location randomLocationInRegion(World world, ProtectedRegion region, RegionManager manager) {

        int minX = region.getMinimumPoint().getBlockX();
        int minZ = region.getMinimumPoint().getBlockZ();
        int maxX = region.getMaximumPoint().getBlockX();
        int maxZ = region.getMaximumPoint().getBlockZ();

        for (int i = 0; i < 25; i++) { // максимум 25 попыток

            int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
            int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);
            int y = world.getHighestBlockYAt(x, z) + 1;

            BlockVector3 vec = BlockVector3.at(x, y, z);
            ApplicableRegionSet set = manager.getApplicableRegions(vec);

            if (set.getRegions().contains(region)) {
                return new Location(world, x + 0.5, y + 0.5, z + 0.5);
            }
        }

        return null;
    }

    private void spawnFirework(Location loc) {

        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        List<Color> colors = new ArrayList<>();
        for (String s : getConfig().getStringList("firework.colors")) {
            try {
                colors.add(Color.fromRGB(Color.valueOf(s).asRGB()));
            } catch (Exception ignored) {}
        }

        if (colors.isEmpty()) {
            colors.add(Color.RED);
            colors.add(Color.AQUA);
            colors.add(Color.PURPLE);
            colors.add(Color.BLUE);
        }

        FireworkEffect effect = FireworkEffect.builder()
                .flicker(true)
                .trail(true)
                .with(FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)])
                .withColor(colors)
                .withFade(colors)
                .build();

        meta.addEffect(effect);
        meta.setPower(getConfig().getInt("firework.power", 2));
        fw.setFireworkMeta(meta);
    }
}
