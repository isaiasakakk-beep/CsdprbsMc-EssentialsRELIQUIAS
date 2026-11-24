package br.com.csdpbrs.reliquias;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

public final class RelicsListener implements Listener {
    private final ReliquiasPlugin plugin;
    public RelicsListener(ReliquiasPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        String rarity = event.getEntity().getPersistentDataContainer().get(
            new NamespacedKey(plugin, "boss_rarity"), PersistentDataType.STRING);
        if (rarity == null) return;

        event.getDrops().clear();

        // Todos os chefes garantem fragmentos raros.
        int rareAmount = ThreadLocalRandom.current().nextInt(1, 3);
        event.getDrops().add(createFragment("raro", rareAmount));

        // Chefes de maior raridade podem conceder um fragmento extra correspondente.
        double extraChance = switch (rarity) {
            case "raro" -> 0.05;
            case "epico" -> 0.18;
            case "mitico" -> 0.24;
            case "lendario" -> 0.30;
            default -> 0.0;
        };
        if (ThreadLocalRandom.current().nextDouble() < extraChance) {
            event.getDrops().add(createFragment(rarity, 1));
        }
        event.setDroppedExp(Math.max(event.getDroppedExp(), 15));
    }

    private ItemStack createFragment(String rarity, int amount) {
        ItemStack fragment = new ItemStack(Material.AMETHYST_SHARD, amount);
        ItemMeta meta = fragment.getItemMeta();
        String color = switch (rarity) {
            case "raro" -> "&f";
            case "epico" -> "&5";
            case "mitico" -> "&c";
            default -> "&6";
        };
        meta.setDisplayName(ReliquiasPlugin.color(color + "&lFragmento " + rarity));
        meta.setLore(java.util.List.of(ReliquiasPlugin.color("&7Fragmento usado pelo Forjador.")));
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "fragment_rarity"), PersistentDataType.STRING, rarity);
        fragment.setItemMeta(meta);
        return fragment;
    }
}
