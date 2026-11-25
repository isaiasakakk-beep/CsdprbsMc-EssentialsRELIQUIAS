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

        // Drops reforçados: cada chefe garante fragmentos da própria raridade
        // e também entrega fragmentos raros para manter a progressão acessível.
        switch (rarity) {
            case "raro" -> event.getDrops().add(createFragment("raro", ThreadLocalRandom.current().nextInt(3, 6)));
            case "epico" -> {
                event.getDrops().add(createFragment("raro", ThreadLocalRandom.current().nextInt(2, 5)));
                event.getDrops().add(createFragment("epico", ThreadLocalRandom.current().nextInt(3, 6)));
            }
            case "mitico" -> {
                event.getDrops().add(createFragment("raro", ThreadLocalRandom.current().nextInt(3, 6)));
                event.getDrops().add(createFragment("mitico", ThreadLocalRandom.current().nextInt(3, 6)));
                if (ThreadLocalRandom.current().nextDouble() < 0.35)
                    event.getDrops().add(createFragment("epico", ThreadLocalRandom.current().nextInt(1, 3)));
            }
            case "lendario" -> {
                event.getDrops().add(createFragment("raro", ThreadLocalRandom.current().nextInt(3, 6)));
                event.getDrops().add(createFragment("lendario", ThreadLocalRandom.current().nextInt(3, 6)));
                event.getDrops().add(createFragment("mitico", ThreadLocalRandom.current().nextInt(1, 3)));
                if (ThreadLocalRandom.current().nextDouble() < 0.50)
                    event.getDrops().add(createFragment("epico", 1));
            }
            default -> event.getDrops().add(createFragment("raro", 3));
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
