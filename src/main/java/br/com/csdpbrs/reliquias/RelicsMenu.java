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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RelicsMenu implements Listener {
    private final ReliquiasPlugin plugin;
    private static final String TITLE = ReliquiasPlugin.color("&8&lRELIQUIAS | COLEÇÃO");
    private static final String PREFIX = ReliquiasPlugin.color("&8&lRELIQUIAS | ");
    private static final String SMITH_TITLE = ReliquiasPlugin.color("&8&lFORJADOR DE RELÍQUIAS");
    private static final String COLLECTION_TITLE = ReliquiasPlugin.color("&8&lMINHAS RELÍQUIAS");
    public RelicsMenu(ReliquiasPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        decorate(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(10, entry(Material.QUARTZ, "&f&lRARAS", "&7Cinco relíquias de iniciação", "&f◆ Raridade branca", "&8Clique para ver a coleção"));
        inv.setItem(12, entry(Material.AMETHYST_SHARD, "&5&lÉPICAS", "&7Cinco relíquias de poder elevado", "&5◆ Raridade roxa", "&8Clique para ver a coleção"));
        inv.setItem(14, entry(Material.REDSTONE_BLOCK, "&c&lMÍTICAS", "&7Cinco relíquias de grande poder", "&c◆ Raridade vermelha", "&8Clique para ver a coleção"));
        inv.setItem(16, entry(Material.BLAZE_POWDER, "&6&lLENDÁRIAS", "&7Cinco relíquias excepcionais", "&6◆ Raridade laranja", "&8Clique para ver a coleção"));
        inv.setItem(29, entry(Material.NETHER_STAR, "&e&lGUIA DE CHEFES", "&7Todos surgem perto de jogadores", "&7em qualquer mundo/dimensão.", "&fRaro: 2 fragmentos | 20 min", "&5Épico: 4 fragmentos", "&cMítico: 9 fragmentos", "&6Lendário: 12 fragmentos"));
        inv.setItem(31, entry(Material.BOOK, "&b&lMINHAS RELÍQUIAS", "&7Veja as relíquias que você já conquistou", "&8Clique para abrir sua coleção"));
        inv.setItem(33, entry(Material.EMERALD, "&a&lFORJADOR DE RELÍQUIAS", "&7Troque fragmentos por relíquias", "&8Clique para negociar"));
        inv.setItem(35, entry(Material.WRITABLE_BOOK, "&e&lMISSÕES", "&7Complete desafios difíceis", "&7e ganhe fragmentos de raridade."));
        player.openInventory(inv);
    }

    private void openRarity(Player player, RelicCatalog.Rarity rarity) {
        Inventory inv = Bukkit.createInventory(null, 54, PREFIX + ReliquiasPlugin.color(rarity.color + rarity.display.toUpperCase()));
        decorate(inv, rarity.pane);
        List<RelicCatalog.Relic> relics = RelicCatalog.byRarity(rarity);
        int[] slots = {19, 21, 23, 25, 31};
        for (int i=0;i<relics.size();i++) {
            RelicCatalog.Relic relic = relics.get(i);
            List<String> lore = new ArrayList<>(relic.lore());
            lore.add(" "); lore.add("&7Poder: &f" + relic.power()); lore.add("&7Recarga: &f" + relic.cooldown());
            lore.add(" "); lore.add(rarity.color + "◆ " + rarity.display);
            lore.add(plugin.owns(player, relic.id()) ? "&a✔ Você já possui esta relíquia." : "&c✘ Ainda não conquistada.");
            inv.setItem(slots[i], entry(relic.icon(), rarity.color + "&l" + relic.name(), lore.toArray(String[]::new)));
        }
        inv.setItem(49, entry(Material.ARROW, "&e&lVOLTAR", "&7Voltar ao menu principal"));
        player.openInventory(inv);
    }

    public void openSmith(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, SMITH_TITLE);
        ItemStack border = entry(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i=0;i<27;i++) if (i<9 || i>=18 || i%9==0 || i%9==8) inv.setItem(i,border);
        setTrade(inv, 10, RelicCatalog.Rarity.RARO, Material.QUARTZ, "&f&lFORJAR RELÍQUIA RARA");
        setTrade(inv, 12, RelicCatalog.Rarity.EPICO, Material.AMETHYST_SHARD, "&5&lFORJAR RELÍQUIA ÉPICA");
        setTrade(inv, 14, RelicCatalog.Rarity.MITICO, Material.REDSTONE_BLOCK, "&c&lFORJAR RELÍQUIA MÍTICA");
        setTrade(inv, 16, RelicCatalog.Rarity.LENDARIO, Material.BLAZE_POWDER, "&6&lFORJAR RELÍQUIA LENDÁRIA");
        inv.setItem(22, entry(Material.BOOK, "&e&lCOMO FUNCIONA", "&7Cada troca entrega uma relíquia aleatória", "&7da raridade escolhida que você ainda não possui.", "&7Fragmentos são consumidos somente após validar a troca."));
        player.openInventory(inv);
    }

    private void setTrade(Inventory inv, int slot, RelicCatalog.Rarity rarity, Material icon, String name) {
        int cost = plugin.getConfig().getInt("smith.cost." + rarity.key, 8);
        inv.setItem(slot, entry(icon, name, "&7Custo: " + rarity.color + cost + " fragmentos",
            "&7Você receberá uma relíquia desta raridade.", "&eClique para forjar!"));
    }

    private void openCollection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, COLLECTION_TITLE);
        decorate(inv, Material.BLACK_STAINED_GLASS_PANE);
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33};
        int index = 0;
        for (RelicCatalog.Relic relic : RelicCatalog.all()) {
            if (!plugin.owns(player, relic.id())) continue;
            inv.setItem(slots[index++], entry(relic.icon(), relic.rarity().color + "&l" + relic.name(),
                "&7" + relic.power(), "&aConquistada"));
        }
        inv.setItem(49, entry(Material.ARROW, "&e&lVOLTAR", "&7Voltar ao menu principal"));
        inv.setItem(4, entry(Material.BOOK, "&b&lSUA COLEÇÃO", "&7Relíquias conquistadas: &f" + index + "&7/20"));
        player.openInventory(inv);
    }

    private void forge(Player player, RelicCatalog.Rarity rarity) {
        String fragmentKey = rarity.key;
        int cost = plugin.getConfig().getInt("smith.cost." + fragmentKey, 8);
        List<RelicCatalog.Relic> missing = RelicCatalog.byRarity(rarity).stream()
            .filter(relic -> !plugin.owns(player, relic.id())).toList();
        if (missing.isEmpty()) {
            player.sendMessage(ReliquiasPlugin.color("&aVocê já conquistou todas as relíquias desta raridade!"));
            return;
        }
        int available = countFragments(player, fragmentKey);
        if (available < cost) {
            player.sendMessage(ReliquiasPlugin.color("&cVocê precisa de &f" + cost + " fragmentos " + fragmentKey
                + "&c. Você possui &f" + available + "&c."));
            return;
        }
        RelicCatalog.Relic reward = missing.get(ThreadLocalRandom.current().nextInt(missing.size()));
        removeFragments(player, fragmentKey, cost);
        plugin.registerOwned(player, reward.id());
        plugin.giveOrDrop(player, plugin.createRelic(reward));
        player.sendMessage(ReliquiasPlugin.color("&6✦ &aVocê forjou: " + rarity.color + "&l" + reward.name() + "&a!"));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
        openSmith(player);
    }

    private int countFragments(Player player, String rarity) {
        NamespacedKey key = new NamespacedKey(plugin, "fragment_rarity");
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            if (rarity.equals(item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING)))
                total += item.getAmount();
        }
        return total;
    }

    private void removeFragments(Player player, String rarity, int amount) {
        NamespacedKey key = new NamespacedKey(plugin, "fragment_rarity");
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i=0;i<contents.length && remaining>0;i++) {
            ItemStack item = contents[i];
            if (item == null || !item.hasItemMeta()) continue;
            if (!rarity.equals(item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING))) continue;
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            if (item.getAmount() <= 0) contents[i] = null;
            remaining -= take;
        }
        player.getInventory().setStorageContents(contents);
    }

    @EventHandler public void onClock(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(plugin.clockKey())) return;
        event.setCancelled(true); open(event.getPlayer());
    }

    @EventHandler public void onMenuClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(TITLE) && !title.startsWith(PREFIX) && !title.equals(SMITH_TITLE) && !title.equals(COLLECTION_TITLE) && !plugin.missions().isMissionMenu(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null
            || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getSlot();
        if (plugin.missions().isMissionMenu(title)) { if (slot == 10 || slot == 12 || slot == 14 || slot == 16) plugin.missions().handleClick(player, slot); return; }
        if (title.equals(SMITH_TITLE)) {
            switch (slot) {
                case 10 -> forge(player, RelicCatalog.Rarity.RARO);
                case 12 -> forge(player, RelicCatalog.Rarity.EPICO);
                case 14 -> forge(player, RelicCatalog.Rarity.MITICO);
                case 16 -> forge(player, RelicCatalog.Rarity.LENDARIO);
                default -> { }
            }
            return;
        }
        if (title.equals(COLLECTION_TITLE)) { if (slot == 49) open(player); return; }
        if (title.equals(TITLE)) {
            switch (slot) {
                case 10 -> openRarity(player, RelicCatalog.Rarity.RARO);
                case 12 -> openRarity(player, RelicCatalog.Rarity.EPICO);
                case 14 -> openRarity(player, RelicCatalog.Rarity.MITICO);
                case 16 -> openRarity(player, RelicCatalog.Rarity.LENDARIO);
                case 29 -> player.sendMessage(ReliquiasPlugin.color("&eTodos os chefes surgem perto de jogadores em qualquer dimensão. O Guardião Raro deixa coordenadas no chat e pode ser alcançado pela Bússola do Guardião."));
                case 31 -> openCollection(player);
                case 33 -> openSmith(player);
                case 35 -> plugin.missions().open(player);
                default -> { }
            }
        } else if (slot == 49) open(player);
    }

    @EventHandler public void onMenuDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(TITLE) || title.startsWith(PREFIX) || title.equals(SMITH_TITLE) || title.equals(COLLECTION_TITLE) || plugin.missions().isMissionMenu(title))
            event.setCancelled(true);
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
        item.setItemMeta(meta); return item;
    }
}
