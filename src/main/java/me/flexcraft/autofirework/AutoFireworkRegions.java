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

        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                spawnFireworks();
            }
        }, 40L, interval * 20L);

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
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        meta.setPower(config.getInt("firework.power", 2));

        List<Color> colors = loadColors();
        Collections.shuffle(colors);

        int count = ThreadLocalRandom.current().nextInt(1, 4);

        FireworkEffect.Builder effect = FireworkEffect.builder()
                .flicker(true)
                .trail(true)
                .with(randomEffectType());

        for (int i = 0; i < count && i < colors.size(); i++) {
            effect.withColor(colors.get(i));
        }

        meta.addEffect(effect.build());
        fw.setFireworkMeta(meta);
    }

    private FireworkEffect.Type randomEffectType() {
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    private List<Color> loadColors() {

        List<Color> result = new ArrayList<Color>();
        List<String> cfg = config.getStringList("firework.colors");

        for (String s : cfg) {
            s = s.toUpperCase();

            if (s.equals("RED")) result.add(Color.fromRGB(255, 60, 60));
            else if (s.equals("BLUE")) result.add(Color.fromRGB(60, 60, 255));
            else if (s.equals("GREEN")) result.add(Color.fromRGB(60, 255, 60));
            else if (s.equals("AQUA")) result.add(Color.fromRGB(60, 255, 255));
            else if (s.equals("PURPLE")) result.add(Color.fromRGB(180, 60, 255));
            else if (s.equals("YELLOW")) result.add(Color.fromRGB(255, 255, 60));
            else if (s.equals("ORANGE")) result.add(Color.fromRGB(255, 140, 40));
            else if (s.equals("PINK")) result.add(Color.fromRGB(255, 120, 200));
        }

        if (result.isEmpty()) {
            result.add(Color.WHITE);
        }

        return result;
    }
}
