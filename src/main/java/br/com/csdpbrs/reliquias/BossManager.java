package br.com.csdpbrs.reliquias;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Biome;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    private final Map<String, Integer> desiredPopulation = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nextSkill = new ConcurrentHashMap<>();
    private final Map<UUID, Location> bossHomes = new ConcurrentHashMap<>();
    private final Map<String, Integer> pendingRespawns = new ConcurrentHashMap<>();

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
        for (Player anchor : Bukkit.getOnlinePlayers()) {
            Location playerLocation = anchor.getLocation();
            if (!allowedBiome(playerLocation, "raro") && !allowedBiome(playerLocation, "epico")
                && !allowedBiome(playerLocation, "mitico") && !allowedBiome(playerLocation, "lendario")) continue;
            Biome biome = playerLocation.getBlock().getBiome();
            for (String rarity : List.of("raro", "epico", "mitico", "lendario")) {
                if (!allowedBiome(playerLocation, rarity)) continue;
                String path = "bosses." + rarity;
                if (!plugin.getConfig().getBoolean(path + ".enabled", true)) continue;
                String populationKey = populationKey(rarity, anchor.getWorld(), biome);
                int min = Math.max(1, plugin.getConfig().getInt(path + ".population-min", 5));
                int max = Math.max(min, plugin.getConfig().getInt(path + ".population-max", 8));
                int target = desiredPopulation.computeIfAbsent(populationKey,
                    key -> ThreadLocalRandom.current().nextInt(min, max + 1));
                int alive = countAlive(rarity, anchor.getWorld(), biome)
                    + pendingRespawns.getOrDefault(populationKey, 0);
                int attempts = 0;
                while (alive < target && attempts++ < target * 4) {
                    if (!spawnNow(rarity, anchor, biome)) break;
                    alive = countAlive(rarity, anchor.getWorld(), biome)
                        + pendingRespawns.getOrDefault(populationKey, 0);
                }
            }
        }
    }

    private String populationKey(String rarity, World world, Biome biome) {
        return rarity + ":" + world.getUID() + ":" + biome.name();
    }

    public boolean spawnNow(String rarity) {
        List<? extends Player> candidates = Bukkit.getOnlinePlayers().stream()
            .filter(player -> allowedBiome(player.getLocation(), rarity)).toList();
        if (candidates.isEmpty()) return false;
        Player anchor = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        return spawnNow(rarity, anchor, anchor.getLocation().getBlock().getBiome());
    }

    private boolean spawnNow(String rarity, Player anchor, Biome requiredBiome) {
        String path = "bosses." + rarity;
        if (!plugin.getConfig().contains(path)) return false;
        boolean firstInPack = countAlive(rarity, anchor.getWorld(), requiredBiome) == 0;
        World world = anchor.getWorld();
        int attempts = Math.max(1, plugin.getConfig().getInt("spawn.maximum-attempts", 48));
        for (int i = 0; i < attempts; i++) {
            Location loc = randomLocation(anchor, rarity);
            if (loc == null || loc.getBlock().getBiome() != requiredBiome || !validLocation(loc, rarity)) continue;
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
            boss.setRemoveWhenFarAway(false);
            saveBossHome(boss, loc);
            createBar(boss, rarity);
            if (firstInPack) {
                announce(boss, rarity);
                if (rarity.equals("raro")) {
                    for (Player player : Bukkit.getOnlinePlayers()) giveTrackerCompass(player);
                }
            }
            return true;
        }
        return false;
    }

    private Location randomLocation(Player anchor, String rarity) {
        var random = ThreadLocalRandom.current();
        World world = anchor.getWorld();
        var border = world.getWorldBorder();
        double minRadius = Math.max(25.0, plugin.getConfig().getDouble("spawn.player-radius-min", 28.0));
        double maxRadius = Math.max(minRadius + 1.0, plugin.getConfig().getDouble("spawn.player-radius-max", 48.0));
        double angle = random.nextDouble(0.0, Math.PI * 2.0);
        double radius = random.nextDouble(minRadius, maxRadius);
        int blockX = anchor.getLocation().getBlockX() + (int) Math.round(Math.cos(angle) * radius);
        int blockZ = anchor.getLocation().getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
        int anchorX = anchor.getLocation().getBlockX();
        int anchorZ = anchor.getLocation().getBlockZ();
        int surfaceY = world.getHighestBlockYAt(anchorX, anchorZ);
        boolean underground = anchor.getLocation().getBlockY() < surfaceY - 2;
        int y;
        if (world.getEnvironment() == World.Environment.NETHER || underground) {
            y = anchor.getLocation().getBlockY() + random.nextInt(-5, 6);
        } else {
            y = world.getHighestBlockYAt(blockX, blockZ) + 1;
        }
        y = Math.max(world.getMinHeight() + 2, Math.min(world.getMaxHeight() - 3, y));
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
        gear.setItemInMainHand(new ItemStack(Material.AIR));
        gear.setHelmetDropChance(0f); gear.setChestplateDropChance(0f);
        gear.setLeggingsDropChance(0f); gear.setBootsDropChance(0f);
        gear.setItemInMainHandDropChance(0f);
    }

    private boolean validLocation(Location loc, String rarity) {
        if (!allowedBiome(loc, rarity)) return false;
        if (!loc.getBlock().isPassable() || !loc.clone().add(0, 1, 0).getBlock().isPassable()) return false;
        if (loc.clone().subtract(0, 1, 0).getBlock().isPassable()) return false;
        if (loc.getWorld().getPlayers().stream().anyMatch(p -> p.getLocation().distanceSquared(loc) < Math.pow(plugin.getConfig().getInt("spawn.minimum-distance-between-players", 24), 2))) return false;
        return true;
    }

    private boolean allowedBiome(Location location, String rarity) {
        World.Environment environment = location.getWorld().getEnvironment();
        Biome biome = location.getBlock().getBiome();
        return switch (rarity) {
            case "raro" -> environment == World.Environment.NORMAL
                && (biome == Biome.CHERRY_GROVE || biome == Biome.PLAINS);
            case "epico" -> environment == World.Environment.NORMAL
                && (biome == Biome.DRIPSTONE_CAVES || biome == Biome.LUSH_CAVES || biome == Biome.DEEP_DARK);
            case "mitico" -> environment == World.Environment.NETHER;
            case "lendario" -> environment == World.Environment.THE_END;
            default -> false;
        };
    }

    private int countAlive(String rarity, World targetWorld, Biome targetBiome) {
        var key = new org.bukkit.NamespacedKey(plugin, "boss_rarity");
        int count = 0;
        for (Entity entity : targetWorld.getEntities()) {
            if (rarity.equals(entity.getPersistentDataContainer().get(key, PersistentDataType.STRING))
                && entity.getLocation().getBlock().getBiome() == targetBiome) count++;
        }
        return count;
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
                boss.setRemoveWhenFarAway(false);
                Location home = bossHomes.computeIfAbsent(boss.getUniqueId(), id -> loadBossHome(boss));
                if (home != null && (!boss.getWorld().equals(home.getWorld())
                    || boss.getLocation().getBlock().getBiome() != home.getBlock().getBiome())) {
                    boss.teleport(home);
                }
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
        LivingEntity dead = event.getEntity();
        String rarity = dead.getPersistentDataContainer().get(
            new org.bukkit.NamespacedKey(plugin, "boss_rarity"), PersistentDataType.STRING);
        UUID id = dead.getUniqueId();
        BossBar bar = bars.remove(id);
        if (bar != null) bar.removeAll();
        nextSkill.remove(id);
        bossHomes.remove(id);
        if (rarity == null) return;

        Location deathLocation = dead.getLocation().clone();
        String pendingKey = populationKey(rarity, deathLocation.getWorld(), deathLocation.getBlock().getBiome());
        pendingRespawns.merge(pendingKey, 1, Integer::sum);
        Bukkit.getScheduler().runTaskLater(plugin,
            () -> attemptReplacement(rarity, deathLocation, pendingKey, 0), 40L * 20L);
    }

    private void attemptReplacement(String rarity, Location origin, String pendingKey, int retry) {
        if (!plugin.isEnabled()) return;
        if (spawnReplacement(rarity, origin)) {
            pendingRespawns.compute(pendingKey, (key, value) -> value == null || value <= 1 ? null : value - 1);
            return;
        }
        if (retry < 30) {
            Bukkit.getScheduler().runTaskLater(plugin,
                () -> attemptReplacement(rarity, origin, pendingKey, retry + 1), 20L * 10L);
        } else {
            pendingRespawns.compute(pendingKey, (key, value) -> value == null || value <= 1 ? null : value - 1);
            plugin.getLogger().warning("Não foi possível reaparecer o chefe " + rarity + " no bioma original após várias tentativas.");
        }
    }

    private boolean spawnReplacement(String rarity, Location origin) {
        if (origin.getWorld() == null) return false;
        String path = "bosses." + rarity;
        World world = origin.getWorld();
        Biome originalBiome = origin.getBlock().getBiome();
        EntityType type;
        try {
            type = EntityType.valueOf(plugin.getConfig().getString(path + ".entity", "ZOMBIE").toUpperCase());
        } catch (IllegalArgumentException ex) {
            return false;
        }

        for (int attempt = 0; attempt < 80; attempt++) {
            int dx = ThreadLocalRandom.current().nextInt(-12, 13);
            int dz = ThreadLocalRandom.current().nextInt(-12, 13);
            int x = origin.getBlockX() + dx;
            int z = origin.getBlockZ() + dz;
            int y = (world.getEnvironment() == World.Environment.NETHER
                || world.getEnvironment() == World.Environment.NORMAL && origin.getBlockY() < world.getHighestBlockYAt(origin.getBlockX(), origin.getBlockZ()) - 2)
                ? origin.getBlockY() + ThreadLocalRandom.current().nextInt(-3, 4)
                : world.getHighestBlockYAt(x, z) + 1;
            y = Math.max(world.getMinHeight() + 2, Math.min(world.getMaxHeight() - 3, y));
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            if (loc.getBlock().getBiome() != originalBiome) continue;
            if (!allowedBiome(loc, rarity)) continue;
            if (!loc.getBlock().isPassable() || !loc.clone().add(0, 1, 0).getBlock().isPassable()
                || loc.clone().subtract(0, 1, 0).getBlock().isPassable()) continue;

            Entity entity = world.spawnEntity(loc, type);
            if (!(entity instanceof LivingEntity boss)) { entity.remove(); continue; }
            boss.setCustomName(ReliquiasPlugin.color(plugin.getConfig().getString(path + ".display-name", "&fChefe")));
            boss.setCustomNameVisible(true);
            double hp = plugin.getConfig().getDouble(path + ".max-health", 60.0);
            var maxHealth = boss.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) { maxHealth.setBaseValue(hp); boss.setHealth(hp); }
            var attack = boss.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attack != null) attack.setBaseValue(plugin.getConfig().getDouble(path + ".damage", 5.0));
            boss.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "boss_rarity"), PersistentDataType.STRING, rarity);
            styleBoss(boss, rarity);
            boss.setRemoveWhenFarAway(false);
            saveBossHome(boss, loc);
            createBar(boss, rarity);
            Bukkit.broadcastMessage(ReliquiasPlugin.color("&6&l[CHEFE] &e" + boss.getCustomName()
                + " &areapareceu no mesmo bioma após 40 segundos!"));
            return true;
        }
        return false;
    }

    private void saveBossHome(LivingEntity boss, Location home) {
        var data = boss.getPersistentDataContainer();
        data.set(new org.bukkit.NamespacedKey(plugin, "boss_home_world"), PersistentDataType.STRING, home.getWorld().getName());
        data.set(new org.bukkit.NamespacedKey(plugin, "boss_home_x"), PersistentDataType.DOUBLE, home.getX());
        data.set(new org.bukkit.NamespacedKey(plugin, "boss_home_y"), PersistentDataType.DOUBLE, home.getY());
        data.set(new org.bukkit.NamespacedKey(plugin, "boss_home_z"), PersistentDataType.DOUBLE, home.getZ());
        bossHomes.put(boss.getUniqueId(), home.clone());
    }

    private Location loadBossHome(LivingEntity boss) {
        var data = boss.getPersistentDataContainer();
        String worldName = data.get(new org.bukkit.NamespacedKey(plugin, "boss_home_world"), PersistentDataType.STRING);
        Double x = data.get(new org.bukkit.NamespacedKey(plugin, "boss_home_x"), PersistentDataType.DOUBLE);
        Double y = data.get(new org.bukkit.NamespacedKey(plugin, "boss_home_y"), PersistentDataType.DOUBLE);
        Double z = data.get(new org.bukkit.NamespacedKey(plugin, "boss_home_z"), PersistentDataType.DOUBLE);
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world != null && x != null && y != null && z != null) return new Location(world, x, y, z);
        Location current = boss.getLocation().clone();
        saveBossHome(boss, current);
        return current;
    }

    private void announce(LivingEntity boss, String rarity) {
        if (rarity.equals("raro")) {
            Location loc = boss.getLocation();
            String message = "&f&l[CHEFE RARO] &e" + boss.getCustomName()
                + " &asurgiu! Localização exata: &f" + boss.getWorld().getName()
                + " &7(X: &f" + loc.getBlockX() + "&7, Y: &f" + loc.getBlockY()
                + "&7, Z: &f" + loc.getBlockZ() + "&7). &eUse a bússola para dar TP até ele!";
            Bukkit.broadcastMessage(ReliquiasPlugin.color(message));
            return;
        }
        String message = "&5&l[CHEFE] &e" + boss.getCustomName()
            + " &asurgiu na dimensão &f" + dimensionName(boss.getWorld())
            + "&a! Localização não revelada.";
        Bukkit.broadcastMessage(ReliquiasPlugin.color(message));
    }

    private org.bukkit.NamespacedKey trackerCompassKey() {
        return new org.bukkit.NamespacedKey(plugin, "rare_boss_compass");
    }

    private void giveTrackerCompass(Player player) {
        org.bukkit.NamespacedKey key = trackerCompassKey();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;
        }
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName(ReliquiasPlugin.color("&f&lBÚSSOLA DO GUARDIÃO"));
        meta.setLore(List.of(
            ReliquiasPlugin.color("&7Clique com o botão direito para"),
            ReliquiasPlugin.color("&7teleportar até o Guardião Errante."),
            ReliquiasPlugin.color("&eDisponível enquanto ele estiver vivo.")
        ));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        plugin.giveOrDrop(player, compass);
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = event.getPlayer();
            for (World world : Bukkit.getWorlds()) {
                boolean rareAlive = world.getEntities().stream().anyMatch(entity ->
                    "raro".equals(entity.getPersistentDataContainer().get(
                        new org.bukkit.NamespacedKey(plugin, "boss_rarity"), PersistentDataType.STRING)));
                if (rareAlive) {
                    giveTrackerCompass(player);
                    return;
                }
            }
        });
    }

    @EventHandler
    public void onTrackerCompass(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()
            || !item.getItemMeta().getPersistentDataContainer().has(trackerCompassKey(), PersistentDataType.BYTE)) return;
        event.setCancelled(true);
        LivingEntity target = null;
        var bossKey = new org.bukkit.NamespacedKey(plugin, "boss_rarity");
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity living
                    && "raro".equals(entity.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING))) {
                    target = living;
                    break;
                }
            }
            if (target != null) break;
        }
        if (target == null || target.isDead()) {
            event.getPlayer().sendMessage(ReliquiasPlugin.color("&cO Guardião Errante não está vivo no momento."));
            return;
        }
        Location destination = findSafeTeleportLocation(target.getLocation());
        if (destination == null) {
            event.getPlayer().sendMessage(ReliquiasPlugin.color("&cNão foi possível encontrar um local seguro perto do Guardião."));
            return;
        }
        event.getPlayer().teleport(destination);
        event.getPlayer().sendMessage(ReliquiasPlugin.color("&aVocê foi teleportado até o Guardião Errante!"));
        event.getPlayer().playSound(destination, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        destination.getWorld().spawnParticle(Particle.PORTAL, destination, 35, 0.7, 1, 0.7, 0.05);
    }

    private Location findSafeTeleportLocation(Location bossLocation) {
        World world = bossLocation.getWorld();
        for (int radius = 3; radius <= 7; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    Location candidate = bossLocation.clone().add(dx + 0.5, 0, dz + 0.5);
                    if (candidate.getBlock().isPassable()
                        && candidate.clone().add(0, 1, 0).getBlock().isPassable()
                        && candidate.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                        candidate.setYaw(bossLocation.getYaw());
                        candidate.setPitch(bossLocation.getPitch());
                        return candidate;
                    }
                }
            }
        }
        return null;
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
