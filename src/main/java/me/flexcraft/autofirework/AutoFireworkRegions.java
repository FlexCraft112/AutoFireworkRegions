package me.flexcraft.autofirework;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AutoFireworkRegions extends JavaPlugin {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        int interval = config.getInt("interval-seconds", 10);

        Bukkit.getScheduler().runTaskTimer(this, this::spawnFireworks, 40L, interval * 20L);
        getLogger().info("AutoFireworkRegions enabled");
    }

    private void spawnFireworks() {
        List<String> regionNames = config.getStringList("regions");
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

                Location loc = randomLocationInRegion(world, region);
                spawnFirework(loc);
            }
        }
    }

    private Location randomLocationInRegion(World world, ProtectedRegion region) {

        int minX = region.getMinimumPoint().getBlockX();
        int minZ = region.getMinimumPoint().getBlockZ();
        int maxX = region.getMaximumPoint().getBlockX();
        int maxZ = region.getMaximumPoint().getBlockZ();

        int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
        int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);

        int y = world.getHighestBlockYAt(x, z) + 1;

        Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);

        // дополнительная проверка — строго внутри региона
        ApplicableRegionSet set = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(world))
                .getApplicableRegions(BukkitAdapter.asBlockVector(loc));

        if (!set.getRegions().contains(region)) {
            return randomLocationInRegion(world, region);
        }

        return loc;
    }

    private void spawnFirework(Location loc) {
        Firework firework = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        meta.setPower(config.getInt("firework.power", 2));

        List<Color> availableColors = loadColors();
        Collections.shuffle(availableColors);

        int colorCount = ThreadLocalRandom.current().nextInt(1, 4);

        FireworkEffect.Builder effect = FireworkEffect.builder()
                .flicker(true)
                .trail(true)
                .with(randomEffectType());

        for (int i = 0; i < Math.min(colorCount, availableColors.size()); i++) {
            effect.withColor(availableColors.get(i));
        }

        meta.addEffect(effect.build());
        firework.setFireworkMeta(meta);
    }

    private FireworkEffect.Type randomEffectType() {
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    private List<Color> loadColors() {
        List<Color> colors = new ArrayList<>();

        for (String s : config.getStringList("firework.colors")) {
            switch (s.toUpperCase()) {
                case "RED" -> colors.add(Color.fromRGB(255, 60, 60));
                case "BLUE" -> colors.add(Color.fromRGB(60, 60, 255));
                case "GREEN" -> colors.add(Color.fromRGB(60, 255, 60));
                case "AQUA" -> colors.add(Color.fromRGB(60, 255, 255));
                case "PURPLE" -> colors.add(Color.fromRGB(180, 60, 255));
                case "YELLOW" -> colors.add(Color.fromRGB(255, 255, 60));
                case "ORANGE" -> colors.add(Color.fromRGB(255, 140, 40));
                case "PINK" -> colors.add(Color.fromRGB(255, 120, 200));
            }
        }

        if (colors.isEmpty()) {
            colors.add(Color.WHITE);
        }

        return colors;
    }
}
