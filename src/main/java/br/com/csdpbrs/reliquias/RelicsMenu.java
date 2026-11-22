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
import java.util.ArrayList;
import java.util.List;

public final class RelicsMenu implements Listener {
    private final ReliquiasPlugin plugin;
    private static final String TITLE = ReliquiasPlugin.color("&8&lRELIQUIAS | COLEÇÃO");
    private static final String PREFIX = ReliquiasPlugin.color("&8&lRELIQUIAS | ");
    public RelicsMenu(ReliquiasPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        decorate(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(10, entry(Material.QUARTZ, "&f&lRARAS", "&7Cinco relíquias de iniciação", "&f◆ Raridade branca", "&8Clique para ver a coleção"));
        inv.setItem(12, entry(Material.AMETHYST_SHARD, "&5&lÉPICAS", "&7Cinco relíquias de poder elevado", "&5◆ Raridade roxa", "&8Clique para ver a coleção"));
        inv.setItem(14, entry(Material.REDSTONE_BLOCK, "&c&lMÍTICAS", "&7Cinco relíquias de grande poder", "&c◆ Raridade vermelha", "&8Clique para ver a coleção"));
        inv.setItem(16, entry(Material.BLAZE_POWDER, "&6&lLENDÁRIAS", "&7Cinco relíquias excepcionais", "&6◆ Raridade laranja", "&8Clique para ver a coleção"));
        inv.setItem(29, entry(Material.NETHER_STAR, "&e&lGUIA DE CHEFES", "&7Raros: explorando o mundo normal", "&5Épicos: cavernas", "&cMíticos: Nether", "&6Lendários: End"));
        inv.setItem(31, entry(Material.BOOK, "&b&lMINHAS RELÍQUIAS", "&7Sua coleção será exibida aqui", "&8Sistema de posse em desenvolvimento"));
        inv.setItem(33, entry(Material.EMERALD, "&a&lFORJADOR DE RELÍQUIAS", "&7Troque fragmentos por relíquias", "&8NPC em desenvolvimento"));
        player.openInventory(inv);
    }

    private void openRarity(Player player, RelicCatalog.Rarity rarity) {
        Inventory inv = Bukkit.createInventory(null, 54, PREFIX + ReliquiasPlugin.color(rarity.color + rarity.display.toUpperCase()));
        decorate(inv, rarity.pane);
        List<RelicCatalog.Relic> relics = RelicCatalog.byRarity(rarity);
        int[] slots = {19, 21, 23, 25, 31};
        for (int i=0;i<relics.size();i++) {
            RelicCatalog.Relic relic = relics.get(i);
            List<String> lore = new ArrayList<>();
            lore.addAll(relic.lore());
            lore.add(" ");
            lore.add("&7Poder: &f" + relic.power());
            lore.add("&7Recarga: &f" + relic.cooldown());
            lore.add(" ");
            lore.add(rarity.color + "◆ " + rarity.display);
            lore.add("&8Disponível para todos os jogadores");
            inv.setItem(slots[i], entry(relic.icon(), rarity.color + "&l" + relic.name(), lore.toArray(String[]::new)));
        }
        inv.setItem(49, entry(Material.ARROW, "&e&lVOLTAR", "&7Voltar ao menu principal"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClock(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(plugin.clockKey())) return;
        event.setCancelled(true);
        open(event.getPlayer());
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(TITLE) && !title.startsWith(PREFIX)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getSlot();
        if (title.equals(TITLE)) {
            switch (slot) {
                case 10 -> openRarity(player, RelicCatalog.Rarity.RARO);
                case 12 -> openRarity(player, RelicCatalog.Rarity.EPICO);
                case 14 -> openRarity(player, RelicCatalog.Rarity.MITICO);
                case 16 -> openRarity(player, RelicCatalog.Rarity.LENDARIO);
                case 29 -> player.sendMessage(ReliquiasPlugin.color("&eGuia: &fRaros no mundo normal, épicos nas cavernas, míticos no Nether e lendários no End."));
                case 33 -> player.sendMessage(ReliquiasPlugin.color("&aO Forjador de Relíquias será implementado na próxima etapa."));
                default -> { }
            }
        } else if (slot == 49) open(player);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(TITLE) || title.startsWith(PREFIX)) event.setCancelled(true);
    }

    private void decorate(Inventory inv, Material border) {
        ItemStack pane = entry(border, " ");
        for (int i=0;i<54;i++) if (i<9 || i>=45 || i%9==0 || i%9==8) inv.setItem(i,pane);
        inv.setItem(4, entry(Material.AMETHYST_CLUSTER, "&d&lCÓDICE DAS RELÍQUIAS", "&7Descubra poderes esquecidos"));
    }
    private ItemStack entry(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ReliquiasPlugin.color(name));
        meta.setLore(List.of(lore).stream().map(ReliquiasPlugin::color).toList());
        item.setItemMeta(meta);
        return item;
    }
}
