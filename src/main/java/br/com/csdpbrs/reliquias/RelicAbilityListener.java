package br.com.csdpbrs.reliquias;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
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
import org.bukkit.inventory.ItemStack;

public final class RelicAbilityListener implements Listener {
    private final ReliquiasPlugin plugin;
    private final org.bukkit.NamespacedKey relicKey;

    public RelicAbilityListener(ReliquiasPlugin plugin) {
        this.plugin = plugin;
        this.relicKey = new org.bukkit.NamespacedKey(plugin, "relic_id");
    }

    @EventHandler
    public void onRelicUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        String id = relicId(item);
        if (id == null) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        RelicCatalog.Relic relic = RelicCatalog.all().stream()
            .filter(r -> r.id().equals(id)).findFirst().orElse(null);
        if (relic == null) return;

        if (java.util.List.of("fazendeiro","minerador","invisibilidade","nether").contains(id)) {
            player.sendMessage(ReliquiasPlugin.color("&eEsta relíquia tem poder passivo: deixe-a equipada ou na mão."));
            return;
        }

        long now = System.currentTimeMillis();
        var cooldownKey = new org.bukkit.NamespacedKey(plugin, "relic_cd_" + id);
        long readyAt = player.getPersistentDataContainer().getOrDefault(
            cooldownKey, PersistentDataType.LONG, 0L);
        if (now < readyAt) {
            long seconds = Math.max(1, (readyAt - now + 999) / 1000);
            player.sendMessage(ReliquiasPlugin.color("&cEssa relíquia recarrega em &f" + seconds + "s&c."));
            return;
        }

        if (!activate(player, id)) return;
        long cooldown = cooldownSeconds(relic.cooldown());
        player.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, now + cooldown * 1000L);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.25f);
        player.sendMessage(ReliquiasPlugin.color("&d✦ &f" + relic.name() + " &dativada!"));
    }

    public void applyPassiveEffects(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack item : armor) {
            String id = relicId(item);
            if ("invisibilidade".equals(id))
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 45, 0, true, false, true));
            if ("nether".equals(id))
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 45, 0, true, false, true));
            if ("fazendeiro".equals(id))
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 45, 0, true, false, true));
        }

        String main = relicId(player.getInventory().getItemInMainHand());
        String off = relicId(player.getInventory().getItemInOffHand());
        if ("minerador".equals(main) || "minerador".equals(off))
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 45, 1, true, false, true));
    }

    private String relicId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(relicKey, PersistentDataType.STRING);
    }

    private boolean activate(Player p, String id) {
        switch (id) {
            case "sorte" -> p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 20 * 30, 1));
            case "explorador" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 60, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 45, 0));
            }
            case "explorador_nether" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 60, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 45, 0));
            }
            case "explorador_2" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 75, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 60, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 0));
            }
            case "espiao" -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 12, 0));
            case "palhaco" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 18, 2));
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 45, 1.2, 1, 1.2, 0.08);
                p.getWorld().spawnParticle(Particle.NOTE, p.getLocation(), 30, 1, 1, 1, 1);
            }
            case "deus_vento" -> {
                Vector velocity = p.getLocation().getDirection().normalize().multiply(1.7);
                velocity.setY(Math.max(0.25, Math.min(0.8, velocity.getY() + 0.35)));
                p.setVelocity(velocity);
                p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 35, 0.6, 0.5, 0.6, 0.12);
            }
            case "imortalidade" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 18, 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 8, 1));
            }
            case "defesa" -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 12, 1));
            case "resistencia" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 18, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 18, 1));
            }
            case "dragao" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 45, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 20, 0));
            }
            case "warden" -> {
                for (Entity entity : p.getNearbyEntities(24, 16, 24)) {
                    if (entity instanceof LivingEntity target && target != p)
                        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 6, 0));
                }
                p.getWorld().spawnParticle(Particle.SCULK_SOUL, p.getLocation().add(0, 1, 0), 55, 2.5, 1.2, 2.5, 0.04);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.65f);
            }
            case "stomper" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 15, 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 15, 0));
            }
            case "bizonho" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 12, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 12, 1));
            }
            case "bizonho_2" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 18, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 20, 0));
            }
            case "deus" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 60, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 45, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 20, 2));
            }
            default -> { return false; }
        }
        return true;
    }

    private long cooldownSeconds(String value) {
        try { return Long.parseLong(value.split(" ")[0]); }
        catch (Exception ignored) { return 60L; }
    }
}
