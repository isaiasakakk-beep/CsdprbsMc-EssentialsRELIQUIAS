package br.com.csdpbrs.reliquias;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RelicAbilityListener implements Listener {
    private final ReliquiasPlugin plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public RelicAbilityListener(ReliquiasPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onRelicUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        var item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String id = item.getItemMeta().getPersistentDataContainer()
            .get(new org.bukkit.NamespacedKey(plugin, "relic_id"), PersistentDataType.STRING);
        if (id == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        RelicCatalog.Relic relic = RelicCatalog.all().stream().filter(r -> r.id().equals(id)).findFirst().orElse(null);
        if (relic == null) return;

        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(id, 0L);
        if (now < readyAt) {
            long seconds = Math.max(1, (readyAt - now + 999) / 1000);
            player.sendMessage(ReliquiasPlugin.color("&cEssa relíquia recarrega em &f" + seconds + "s&c."));
            return;
        }

        boolean used = activate(player, id);
        if (!used) {
            player.sendMessage(ReliquiasPlugin.color("&cNão há espaço seguro para ativar essa relíquia."));
            return;
        }
        long cooldown = cooldownSeconds(relic.cooldown());
        cooldowns.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).put(id, now + cooldown * 1000L);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.25f);
        player.sendMessage(ReliquiasPlugin.color("&d✦ &f" + relic.name() + " &dativada!"));
    }

    private boolean activate(Player p, String id) {
        switch (id) {
            case "pena_vento" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 8, 2));
                p.setVelocity(p.getVelocity().add(new Vector(0, 0.35, 0)));
            }
            case "coracao_vital" -> p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 6.0));
            case "olho_noturno" -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 45, 0));
            case "botas_coelho" -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 12, 1));
            case "cristal_aquatico" -> p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 35, 0));
            case "orbe_vazio" -> {
                Vector direction = p.getLocation().getDirection().normalize().multiply(1.6);
                direction.setY(Math.max(-0.1, Math.min(0.35, direction.getY())));
                p.setVelocity(direction);
            }
            case "coracao_glacial" -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 8, 1));
            case "nucleo_trovao" -> {
                p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation(), 45, 3, 1, 3, 0.08);
                for (Entity e : p.getNearbyEntities(4, 3, 4)) {
                    if (e instanceof LivingEntity target && target != p && !(target instanceof Player)) {
                        Vector push = target.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.15);
                        push.setY(0.35);
                        target.setVelocity(push);
                    }
                }
            }
            case "manto_sombras" -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 7, 0));
            case "ampulheta_tempo" -> {
                for (Entity e : p.getNearbyEntities(6, 4, 6)) {
                    if (e instanceof LivingEntity target && target != p && !(target instanceof Player))
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 6, 2));
                }
                p.getWorld().spawnParticle(Particle.WITCH, p.getLocation(), 35, 3, 1, 3, 0.02);
            }
            case "rubi_sangrento" -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 12, 1));
            case "coroa_fenix" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 20, 0));
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 4.0));
            }
            case "garra_abismo" -> {
                LivingEntity target = nearestMob(p, 10);
                if (target == null) return false;
                Vector pull = p.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.1);
                pull.setY(0.25);
                target.setVelocity(pull);
            }
            case "coracao_dragao" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 10, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 10, 0));
            }
            case "portal_espectral" -> {
                Location safe = findSafeForward(p, 5);
                if (safe == null) return false;
                p.teleport(safe);
                p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 50, 0.6, 1, 0.6, 0.2);
            }
            case "coracao_rei" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 12, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 8, 1));
            }
            case "espada_apocalipse" -> {
                p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(0, 1, 0), 1);
                for (Entity e : p.getNearbyEntities(3.5, 2.5, 3.5)) {
                    if (e instanceof LivingEntity target && target != p && !(target instanceof Player))
                        target.damage(5.0, p);
                }
            }
            case "olho_supremo" -> {
                for (Entity e : p.getNearbyEntities(16, 10, 16)) {
                    if (e instanceof LivingEntity target && target != p)
                        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 8, 0));
                }
            }
            case "relogio_destino" -> {
                for (PotionEffect effect : p.getActivePotionEffects()) {
                    if (effect.getType().equals(PotionEffectType.POISON)
                        || effect.getType().equals(PotionEffectType.WITHER)
                        || effect.getType().equals(PotionEffectType.SLOWNESS)
                        || effect.getType().equals(PotionEffectType.WEAKNESS)
                        || effect.getType().equals(PotionEffectType.BLINDNESS)
                        || effect.getType().equals(PotionEffectType.MINING_FATIGUE)) p.removePotionEffect(effect.getType());
                }
            }
            case "coracao_infinito" -> p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 15, 2));
            default -> { return false; }
        }
        return true;
    }

    private LivingEntity nearestMob(Player p, double range) {
        LivingEntity nearest = null;
        double best = range * range;
        for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity target && target != p && !(target instanceof Player)) {
                double distance = target.getLocation().distanceSquared(p.getLocation());
                if (distance < best) { best = distance; nearest = target; }
            }
        }
        return nearest;
    }

    private Location findSafeForward(Player p, int maxDistance) {
        Location origin = p.getLocation();
        Vector direction = origin.getDirection().setY(0).normalize();
        if (direction.lengthSquared() == 0) return null;
        for (int distance = maxDistance; distance >= 2; distance--) {
            Location candidate = origin.clone().add(direction.clone().multiply(distance));
            candidate.setYaw(origin.getYaw());
            candidate.setPitch(origin.getPitch());
            if (candidate.getBlock().isPassable()
                && candidate.clone().add(0, 1, 0).getBlock().isPassable()
                && candidate.clone().subtract(0, 1, 0).getBlock().getType().isSolid()
                && candidate.getWorld().getWorldBorder().isInside(candidate)) return candidate;
        }
        return null;
    }

    private long cooldownSeconds(String value) {
        try { return Long.parseLong(value.split(" ")[0]); }
        catch (Exception ignored) { return 60L; }
    }
}
