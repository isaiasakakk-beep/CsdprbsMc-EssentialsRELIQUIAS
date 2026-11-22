package br.com.csdpbrs.reliquias;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class RelicSmithNpc implements Listener {
    private final ReliquiasPlugin plugin;
    private final NamespacedKey key;
    public RelicSmithNpc(ReliquiasPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "relic_smith_npc");
    }
    public void spawn(Player player) {
        Villager npc = (Villager) player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER);
        npc.setCustomName(ReliquiasPlugin.color("&6&lFORJADOR DE RELÍQUIAS"));
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setProfession(Villager.Profession.WEAPONSMITH);
        npc.setVillagerLevel(5);
        npc.setVillagerExperience(250);
        npc.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        npc.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
        npc.getEquipment().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
        npc.getEquipment().setItemInMainHand(new ItemStack(Material.BLAZE_ROD));
        npc.getEquipment().setHelmetDropChance(0f);
        npc.getEquipment().setChestplateDropChance(0f);
        npc.getEquipment().setItemInMainHandDropChance(0f);
        player.sendMessage(ReliquiasPlugin.color("&aForjador de Relíquias criado."));
    }
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;
        event.setCancelled(true);
        plugin.menu().openSmith(event.getPlayer());
    }
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) event.setCancelled(true);
    }
}
