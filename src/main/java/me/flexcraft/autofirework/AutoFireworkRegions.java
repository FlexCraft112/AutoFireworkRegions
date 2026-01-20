package me.flexcraft.autofirework;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AutoFireworkRegions extends JavaPlugin {

    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int interval = getConfig().getInt("interval-seconds", 3);

        new BukkitRunnable() {
            @Override
            public void run() {
                runFireworkShow();
            }
        }.runTaskTimer(this, 40L, interval * 20L);
    }

    private void runFireworkShow() {
        ConfigurationSection zones = getConfig().getConfigurationSection("zones");
        if (zones == null) return;

        for (String zoneName : zones.getKeys(false)) {
            ConfigurationSection zone = zones.getConfigurationSection(zoneName);
            if (zone == null) continue;

            World world = Bukkit.getWorld(zone.getString("world"));
            if (world == null) continue;

            // ❌ НЕТ ИГРОКОВ В ЗОНЕ → НЕТ ШОУ
            if (!hasPlayersInZone(world, zone)) continue;

            int locations = getConfig().getInt("firework.locations-per-interval", 2);

            for (int i = 0; i < locations; i++) {
                Location loc = findSafeGroundLocation(world, zone);
                if (loc == null) continue;

                int min = getConfig().getInt("firework.burst-min", 3);
                int max = getConfig().getInt("firework.burst-max", 7);
                int count = min + random.nextInt(Math.max(1, max - min + 1));

                for (int f = 0; f < count; f++) {
                    spawnFirework(loc);
                }
            }
        }
    }

    // ---------------------------------------------------
    // ПРОВЕРКА: ЕСТЬ ЛИ ИГРОКИ В ЗОНЕ
    // ---------------------------------------------------
    private boolean hasPlayersInZone(World world, ConfigurationSection zone) {
        int minX = zone.getInt("min.x");
        int minY = zone.getInt("min.y");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxY = zone.getInt("max.y");
        int maxZ = zone.getInt("max.z");

        for (Player p : world.getPlayers()) {
            Location l = p.getLocation();
            if (l.getX() >= minX && l.getX() <= maxX
                    && l.getY() >= minY && l.getY() <= maxY
                    && l.getZ() >= minZ && l.getZ() <= maxZ) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------
    // ПОИСК БЕЗОПАСНОЙ ТОЧКИ НА ЗЕМЛЕ
    // ---------------------------------------------------
    private Location findSafeGroundLocation(World world, ConfigurationSection zone) {
        int minX = zone.getInt("min.x");
        int minZ = zone.getInt("min.z");
        int maxX = zone.getInt("max.x");
        int maxZ = zone.getInt("max.z");

        for (int tries = 0; tries < 15; tries++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;

            int y = world.getHighestBlockYAt(x, z);
            Location base = new Location(world, x + 0.5, y + 1, z + 0.5);

            // ❌ если над точкой есть блоки — пропускаем
            if (!world.getBlockAt(base).isEmpty()) continue;
            if (!world.getBlockAt(base.clone().add(0, 1, 0)).isEmpty()) continue;
            if (!world.getBlockAt(base.clone().add(0, 2, 0)).isEmpty()) continue;

            return base;
        }
        return null;
    }

    // ---------------------------------------------------
    // МОЩНЫЙ ФЕЙЕРВЕРК
    // ---------------------------------------------------
    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        List<Color> palette = new ArrayList<>();
        for (String s : getConfig().getStringList("firework.colors")) {
            try {
                palette.add((Color) Color.class.getField(s).get(null));
            } catch (Exception ignored) {}
        }
        if (palette.isEmpty()) palette.add(Color.WHITE);

        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        meta.clearEffects();

        int effects = 2 + random.nextInt(3); // 2–4 эффекта

        for (int i = 0; i < effects; i++) {
            Collections.shuffle(palette);

            Color main = palette.get(0);
            Color fade = palette.size() > 1 ? palette.get(1) : main;

            FireworkEffect effect = FireworkEffect.builder()
                    .with(types[random.nextInt(types.length)])
                    .withColor(main)
                    .withFade(fade)
                    .trail(true)
                    .flicker(random.nextBoolean())
                    .build();

            meta.addEffect(effect);
        }

        meta.setPower(getConfig().getInt("firework.power", 3));
        fw.setFireworkMeta(meta);
    }
}
