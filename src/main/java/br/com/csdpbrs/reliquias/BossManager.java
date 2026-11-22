package br.com.csdpbrs.reliquias;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class BossManager implements Listener {
    private final ReliquiasPlugin plugin;
    private int taskId = -1;
    private boolean nextAnnouncementIsDetailed = true;
    private final Map<String, Long> nextSpawn = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nextSkill = new ConcurrentHashMap<>();

    public BossManager(ReliquiasPlugin plugin) { this.plugin = plugin; }

    public void start() {
        long ticks = Math.max(20L, plugin.getConfig().getLong("spawn.check-interval-seconds", 60) * 20L);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::checkSpawns, ticks, ticks);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossBarsAndSkills, 20L, 20L);
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
    }

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
        World world = Bukkit.getWorld(plugin.getConfig().getString(path + ".world", "world"));
        if (world == null) return false;
        int attempts = Math.max(1, plugin.getConfig().getInt("spawn.maximum-attempts", 24));
        for (int i = 0; i < attempts; i++) {
            Location loc = randomLocation(world, rarity);
            if (loc == null || !validLocation(loc, rarity)) continue;
            EntityType type;
            try {
                type = EntityType.valueOf(plugin.getConfig().getString(path + ".entity", "ZOMBIE").toUpperCase());
            } catch (IllegalArgumentException ex) { return false; }

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
            createBar(boss, rarity);
            nextSpawn.put(rarity, System.currentTimeMillis() + plugin.getConfig().getLong(path + ".respawn-minutes", 30) * 60_000L);
            announce(boss, rarity);
            return true;
        }
        return false;
    }

    private Location randomLocation(World world, String rarity) {
        var random = ThreadLocalRandom.current();
        var border = world.getWorldBorder();
        double radius = Math.max(16, Math.min(500, border.getSize() / 2.0 - 8));
        double x = border.getCenter().getX() + random.nextDouble(-radius, radius);
        double z = border.getCenter().getZ() + random.nextDouble(-radius, radius);
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        boolean cave = plugin.getConfig().getBoolean("bosses." + rarity + ".cave-only", false);
        int y;
        if (cave) {
            int min = world.getMinHeight() + 8;
            int max = Math.min(world.getMaxHeight() - 8, world.getHighestBlockYAt(blockX, blockZ) - 4);
            if (max <= min) return null;
            y = random.nextInt(min, max + 1);
        } else {
            y = world.getHighestBlockYAt(blockX, blockZ) + 1;
        }
        Location location = new Location(world, blockX + 0.5, y, blockZ + 0.5);
        return border.isInside(location) ? location : null;
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
        if (!loc.getBlock().isPassable() || !loc.clone().add(0, 1, 0).getBlock().isPassable()) return false;
        if (loc.clone().subtract(0, 1, 0).getBlock().isPassable()) return false;
        if (loc.getWorld().getPlayers().stream().anyMatch(p -> p.getLocation().distanceSquared(loc) < Math.pow(plugin.getConfig().getInt("spawn.minimum-distance-between-players", 24), 2))) return false;
        if (plugin.getConfig().getBoolean("bosses." + rarity + ".cave-only", false)) {
            if (loc.getBlock().getLightLevel() > 7 || loc.getBlock().getLightFromSky() > 0) return false;
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

    private void createBar(LivingEntity boss, String rarity) {
        BarColor color = switch (rarity) {
            case "raro" -> BarColor.WHITE;
            case "epico" -> BarColor.PURPLE;
            case "mitico" -> BarColor.RED;
            default -> BarColor.YELLOW;
        };
        BossBar bar = Bukkit.createBossBar(boss.getCustomName(), color, BarStyle.SEGMENTED_10);
        bars.put(boss.getUniqueId(), bar);
    }

    private void updateBossBarsAndSkills() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity boss)) continue;
                String rarity = boss.getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey(plugin, "boss_rarity"), PersistentDataType.STRING);
                if (rarity == null) continue;
                BossBar bar = bars.computeIfAbsent(boss.getUniqueId(), id -> {
                    BarColor color = switch (rarity) {
                        case "raro" -> BarColor.WHITE;
                        case "epico" -> BarColor.PURPLE;
                        case "mitico" -> BarColor.RED;
                        default -> BarColor.YELLOW;
                    };
                    return Bukkit.createBossBar(boss.getCustomName(), color, BarStyle.SEGMENTED_10);
                });
                double max = boss.getAttribute(Attribute.MAX_HEALTH) == null ? 1.0 : boss.getAttribute(Attribute.MAX_HEALTH).getValue();
                bar.setProgress(Math.max(0.0, Math.min(1.0, boss.getHealth() / max)));
                for (Player viewer : world.getPlayers()) {
                    if (viewer.getLocation().distanceSquared(boss.getLocation()) <= 48 * 48) bar.addPlayer(viewer);
                    else bar.removePlayer(viewer);
                }
                if (boss.getHealth() > 0 && boss.getNearbyEntities(10, 6, 10).stream().filter(Player.class::isInstance).map(Player.class::cast).findAny().isPresent()) {
                    long now = System.currentTimeMillis();
                    if (now >= nextSkill.getOrDefault(boss.getUniqueId(), 0L)) {
                        useBossSkill(boss, rarity);
                        nextSkill.put(boss.getUniqueId(), now + 12_000L);
                    }
                }
            }
        }
    }

    private void useBossSkill(LivingEntity boss, String rarity) {
        Player target = boss.getNearbyEntities(10, 6, 10).stream()
            .filter(Player.class::isInstance).map(Player.class::cast)
            .min(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(boss.getLocation())))
            .orElse(null);
        if (target == null) return;
        switch (rarity) {
            case "raro" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 0));
                boss.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 18, 0.7, 0.5, 0.7, 0.02);
            }
            case "epico" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                boss.getWorld().spawnParticle(Particle.SOUL, target.getLocation(), 28, 1, 0.8, 1, 0.03);
            }
            case "mitico" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 1));
                boss.getWorld().spawnParticle(Particle.FLAME, target.getLocation(), 30, 1, 0.6, 1, 0.04);
            }
            default -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                boss.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 45, 1, 1, 1, 0.1);
            }
        }
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        BossBar bar = bars.remove(id);
        if (bar != null) bar.removeAll();
        nextSkill.remove(id);
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
