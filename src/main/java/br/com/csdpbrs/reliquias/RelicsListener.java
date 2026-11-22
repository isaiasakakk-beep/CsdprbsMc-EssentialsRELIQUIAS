package br.com.csdpbrs.reliquias;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public final class RelicsListener implements Listener {
    private final ReliquiasPlugin plugin;
    public RelicsListener(ReliquiasPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        var pdc = event.getEntity().getPersistentDataContainer();
        String rarity = pdc.get(new org.bukkit.NamespacedKey(plugin, "boss_rarity"), PersistentDataType.STRING);
        if (rarity == null) return;
        event.getDrops().clear();
        String key = "bosses." + rarity + ".fragments-";
        int min = plugin.getConfig().getInt(key + "min", 2);
        int max = plugin.getConfig().getInt(key + "max", min);
        int amount = min + (max > min ? java.util.concurrent.ThreadLocalRandom.current().nextInt(max - min + 1) : 0);
        var fragment = new org.bukkit.inventory.ItemStack(org.bukkit.Material.AMETHYST_SHARD, amount);
        var meta = fragment.getItemMeta();
        String color = switch (rarity) { case "raro" -> "&f"; case "epico" -> "&5"; case "mitico" -> "&c"; default -> "&6"; };
        meta.setDisplayName(ReliquiasPlugin.color(color + "Fragmento " + rarity));
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "fragment_rarity"), PersistentDataType.STRING, rarity);
        fragment.setItemMeta(meta);
        event.getDrops().add(fragment);
        event.setDroppedExp(Math.max(event.getDroppedExp(), 20));
    }
}
