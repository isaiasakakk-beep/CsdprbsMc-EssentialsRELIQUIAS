package br.com.csdpbrs.reliquias;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class BossManager {
    private final ReliquiasPlugin plugin;
    private int taskId = -1;
    private boolean nextAnnouncementIsDetailed = true;
    private final Map<String, Long> nextSpawn = new java.util.concurrent.ConcurrentHashMap<>();

    public BossManager(ReliquiasPlugin plugin) { this.plugin = plugin; }

    public void start() {
        long ticks = Math.max(20L, plugin.getConfig().getLong("spawn.check-interval-seconds", 60) * 20L);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::checkSpawns, ticks, ticks);
    }

    public void stop() { if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId); }

    private void checkSpawns() {
        for (String rarity : List.of("raro", "epico", "mitico", "lendario")) {
            if (!plugin.getConfig().getBoolean("bosses." + rarity + ".enabled", true)) continue;
            long readyAt = nextSpawn.getOrDefault(rarity, 0L);
            if (System.currentTimeMillis() < readyAt || countAlive(rarity) > 0) continue;
            spawnNow(rarity);
        }
    }

    public boolean spawnNow(String rarity) {
        String path = "bosses." + rarity;
        if (!plugin.getConfig().contains(path)) return false;
        String worldName = plugin.getConfig().getString(path + ".world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return false;
        int attempts = plugin.getConfig().getInt("spawn.maximum-attempts", 24);
        for (int i = 0; i < attempts; i++) {
            int x = ThreadLocalRandom.current().nextInt(-500, 501);
            int z = ThreadLocalRandom.current().nextInt(-500, 501);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            if (!validLocation(loc, rarity)) continue;
            String entityName = plugin.getConfig().getString(path + ".entity", "ZOMBIE");
            EntityType type;
            try { type = EntityType.valueOf(entityName.toUpperCase()); } catch (IllegalArgumentException ex) { return false; }
            Entity entity = world.spawnEntity(loc, type);
            if (!(entity instanceof LivingEntity boss)) { entity.remove(); return false; }
            boss.setCustomName(ReliquiasPlugin.color(plugin.getConfig().getString(path + ".display-name", "&fChefe")));
            boss.setCustomNameVisible(true);
            double hp = plugin.getConfig().getDouble(path + ".max-health", 60.0);
            var maxHealth = boss.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) { maxHealth.setBaseValue(hp); boss.setHealth(hp); }
            var attack = boss.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attack != null) attack.setBaseValue(plugin.getConfig().getDouble(path + ".damage", 5.0));
            boss.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "boss_rarity"), PersistentDataType.STRING, rarity);
            styleBoss(boss, rarity);
            nextSpawn.put(rarity, System.currentTimeMillis() + plugin.getConfig().getLong(path + ".respawn-minutes", 30) * 60_000L);
            announce(boss, rarity);
            return true;
        }
        return false;
    }

    private void styleBoss(LivingEntity boss, String rarity) {
        boss.setGlowing(true);
        EntityEquipment gear = boss.getEquipment();
        if (gear == null) return;
        Material armor = switch (rarity) {
            case "raro" -> Material.CHAINMAIL_CHESTPLATE;
            case "epico" -> Material.IRON_CHESTPLATE;
            case "mitico" -> Material.DIAMOND_CHESTPLATE;
            default -> Material.NETHERITE_CHESTPLATE;
        };
        Material helmet = switch (rarity) {
            case "raro" -> Material.CHAINMAIL_HELMET;
            case "epico" -> Material.IRON_HELMET;
            case "mitico" -> Material.DIAMOND_HELMET;
            default -> Material.NETHERITE_HELMET;
        };
        gear.setHelmet(new ItemStack(helmet));
        gear.setChestplate(new ItemStack(armor));
        gear.setLeggings(new ItemStack(rarity.equals("lendario") ? Material.NETHERITE_LEGGINGS : rarity.equals("mitico") ? Material.DIAMOND_LEGGINGS : Material.IRON_LEGGINGS));
        gear.setBoots(new ItemStack(rarity.equals("lendario") ? Material.NETHERITE_BOOTS : rarity.equals("mitico") ? Material.DIAMOND_BOOTS : Material.IRON_BOOTS));
        Material weapon = switch (rarity) {
            case "raro" -> Material.IRON_AXE;
            case "epico" -> Material.DIAMOND_AXE;
            case "mitico" -> Material.DIAMOND_SWORD;
            default -> Material.NETHERITE_SWORD;
        };
        gear.setItemInMainHand(new ItemStack(weapon));
        gear.setHelmetDropChance(0f); gear.setChestplateDropChance(0f);
        gear.setLeggingsDropChance(0f); gear.setBootsDropChance(0f);
        gear.setItemInMainHandDropChance(0f);
    }

    private boolean validLocation(Location loc, String rarity) {
        if (loc.getBlock().getType() != Material.AIR || !loc.clone().add(0, 1, 0).getBlock().isPassable()) return false;
        if (loc.clone().subtract(0, 1, 0).getBlock().isPassable()) return false;
        if (loc.getWorld().getPlayers().stream().anyMatch(p -> p.getLocation().distanceSquared(loc) < Math.pow(plugin.getConfig().getInt("spawn.minimum-distance-between-players", 24), 2))) return false;
        if (plugin.getConfig().getBoolean("bosses." + rarity + ".cave-only", false)) {
            int light = loc.getBlock().getLightLevel();
            if (light > 7 || loc.getBlock().getLightFromSky() > 0) return false;
        }
        return true;
    }

    private int countAlive(String rarity) {
        var key = new org.bukkit.NamespacedKey(plugin, "boss_rarity");
        int count = 0;
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities()) {
            if (rarity.equals(entity.getPersistentDataContainer().get(key, PersistentDataType.STRING))) count++;
        }
        return count;
    }

    private void announce(LivingEntity boss, String rarity) {
        boolean detailed = nextAnnouncementIsDetailed;
        nextAnnouncementIsDetailed = !nextAnnouncementIsDetailed;
        String message = detailed
                ? "&6Um chefe " + rarity + " surgiu na dimensão " + dimensionName(boss.getWorld()) + "!"
                : "&6Um chefe surgiu! Explore o mundo para encontrá-lo.";
        Bukkit.broadcastMessage(ReliquiasPlugin.color(message));
    }

    private String dimensionName(World world) {
        return switch (world.getEnvironment()) {
            case NORMAL -> "Mundo Normal";
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> world.getName();
        };
    }
}
