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
import org.bukkit.block.Block;
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
        var data = player.getPersistentDataContainer();
        var cooldownKey = new org.bukkit.NamespacedKey(plugin, "relic_cd_" + id);
        var usesKey = new org.bukkit.NamespacedKey(plugin, "relic_uses_" + id);
        long readyAt = data.getOrDefault(cooldownKey, PersistentDataType.LONG, 0L);
        if (now < readyAt) {
            long seconds = Math.max(1, (readyAt - now + 999) / 1000);
            player.sendMessage(ReliquiasPlugin.color("&cEssa relíquia recarrega em &f" + seconds + "s&c."));
            return;
        }
        if (readyAt > 0L) {
            data.remove(cooldownKey);
            data.set(usesKey, PersistentDataType.INTEGER, 0);
        }

        if (!activate(player, id)) return;
        int uses = data.getOrDefault(usesKey, PersistentDataType.INTEGER, 0) + 1;
        if (uses >= 5) {
            data.set(usesKey, PersistentDataType.INTEGER, 0);
            data.set(cooldownKey, PersistentDataType.LONG, now + 35_000L);
            player.sendMessage(ReliquiasPlugin.color("&eVocê usou esta relíquia 5 vezes! Recarga de &f35 segundos&e."));
        } else {
            data.set(usesKey, PersistentDataType.INTEGER, uses);
            player.sendMessage(ReliquiasPlugin.color("&7Usos: &f" + uses + "/5 &7— sem recarga."));
        }
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

        // A Relíquia da Imortalidade protege continuamente enquanto estiver em uma das mãos.
        if ("imortalidade".equals(main) || "imortalidade".equals(off)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 45, 3, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 45, 2, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 45, 3, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 45, 0, true, false, true));
        }
    }


    private void wardenBlast(Player player) {
        var world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        double range = 24.0;

        if (player.isSneaking()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 2, true, false, true));
            world.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 0.65f);
        }

        for (double distance = 0.5; distance <= range; distance += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            if (world.getBlockAt(point).getType().isSolid()) break;
            world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.SCULK_SOUL, point, 3, 0.12, 0.12, 0.12, 0.01);
        }

        var hit = world.rayTraceEntities(start, direction, range, 1.15,
            entity -> entity instanceof LivingEntity && entity != player);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.8f);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 0.7f);

        if (hit == null || !(hit.getHitEntity() instanceof LivingEntity target)) {
            player.sendMessage(ReliquiasPlugin.color("&8A rajada sônica atravessou o vazio."));
            return;
        }

        target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0, 1, 0), 1);
        target.getWorld().spawnParticle(Particle.SCULK_SOUL, target.getLocation().add(0, 1, 0), 35, 0.7, 0.9, 0.7, 0.08);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WARDEN_HURT, 1.0f, 0.8f);

        if (target instanceof Player victim) {
            if (hasFullArmor(victim, "IRON")) {
                victim.setHealth(0.0);
            } else if (hasFullArmor(victim, "DIAMOND")) {
                victim.setHealth(Math.min(victim.getHealth(), 2.0));
            } else {
                victim.setHealth(Math.max(0.0, victim.getHealth() - 16.0));
            }
        } else {
            target.damage(32.0, player);
        }
        player.sendMessage(ReliquiasPlugin.color("&5&lRAJADA SÔNICA! &7Alvo atingido."));
    }

    private boolean hasFullArmor(Player player, String material) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor.length != 4) return false;
        String prefix = material + "_";
        String[] expected = {prefix + "BOOTS", prefix + "LEGGINGS", prefix + "CHESTPLATE", prefix + "HELMET"};
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] == null || !armor[i].getType().name().equals(expected[i])) return false;
        }
        return true;
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
            case "warden" -> wardenBlast(p);
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

}
