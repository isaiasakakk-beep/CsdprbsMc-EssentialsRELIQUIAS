package br.com.csdpbrs.reliquias;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class RelicsMenu implements Listener {
    private final ReliquiasPlugin plugin;
    private static final String TITLE = ReliquiasPlugin.color("&8&lRELIQUIAS");

    public RelicsMenu(ReliquiasPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, border);
        inv.setItem(10, entry(Material.QUARTZ, "&f&lRARAS", "&7Cinco relíquias comuns da coleção", "&fFragmentos raros"));
        inv.setItem(12, entry(Material.AMETHYST_SHARD, "&5&lÉPICAS", "&7Cinco relíquias de poder elevado", "&5Fragmentos épicos"));
        inv.setItem(14, entry(Material.REDSTONE, "&c&lMÍTICAS", "&7Cinco relíquias de poder extraordinário", "&cFragmentos míticos"));
        inv.setItem(16, entry(Material.ORANGE_DYE, "&6&lLENDÁRIAS", "&7Cinco relíquias lendárias", "&6Fragmentos lendários"));
        inv.setItem(29, entry(Material.NETHER_STAR, "&e&lGUIA DE CHEFES", "&7Veja onde cada chefe aparece."));
        inv.setItem(31, entry(Material.BOOK, "&b&lMINHAS RELÍQUIAS", "&7Consulte sua coleção."));
        inv.setItem(33, entry(Material.EMERALD, "&a&lFORJADOR DE RELÍQUIAS", "&7Troque fragmentos por relíquias.", "&7NPC: Forjador de Relíquias"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClock(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(plugin.clockKey())) return;
        event.setCancelled(true);
        open(event.getPlayer());
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }

    private ItemStack entry(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ReliquiasPlugin.color(name));
        meta.setLore(List.of(lore).stream().map(ReliquiasPlugin::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane(Material material, String name) { return entry(material, name); }
}
