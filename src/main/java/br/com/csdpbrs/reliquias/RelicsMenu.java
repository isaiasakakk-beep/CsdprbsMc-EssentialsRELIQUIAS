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
import org.bukkit.scheduler.BukkitRunnable;
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
    private static final String SPIN_TITLE = ReliquiasPlugin.color("&8&lROLETA DE RELÍQUIAS");
    private final java.util.Set<java.util.UUID> spinning = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public RelicsMenu(ReliquiasPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        decorate(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(10, entry(Material.QUARTZ, "&f&lRARAS", "&7Relíquias de utilidade e sobrevivência", "&f◆ Raridade branca", "&8Clique para ver a coleção"));
        inv.setItem(12, entry(Material.AMETHYST_SHARD, "&5&lÉPICAS", "&7Relíquias de exploração e ocultação", "&5◆ Raridade roxa", "&8Clique para ver a coleção"));
        inv.setItem(14, entry(Material.REDSTONE_BLOCK, "&c&lMÍTICAS", "&7Relíquias de mobilidade e proteção", "&c◆ Raridade vermelha", "&8Clique para ver a coleção"));
        inv.setItem(16, entry(Material.BLAZE_POWDER, "&6&lLENDÁRIAS", "&7Relíquias especiais e raras", "&6◆ Raridade laranja", "&8Clique para ver a coleção"));
        inv.setItem(29, entry(Material.NETHER_STAR, "&e&lGUIA DE CHEFES", "&7Raro: Cherry Grove e Plains (5–8)", "&5Épico: cavernas (5–8)", "&cMítico: biomas do Nether (5–8)", "&6Lendário: dimensão do End (5–8)", "&7Todos garantem fragmentos raros.", "&7Há chance de fragmentos superiores."));
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
            lore.add(" "); lore.add("&7Categoria: &f" + relic.category()); lore.add("&7Poder: &f" + relic.power()); lore.add("&7Recarga: &f" + relic.cooldown());
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
        inv.setItem(13, entry(Material.CLOCK, "&d&lROULETA DE RELÍQUIAS",
            "&7Custo: &f8 fragmentos raros",
            "&7Animação de inventário em sequência.",
            "&7Chance por raridade: 60% / 25% / 11% / 4%.",
            "&eClique para começar!"));
        inv.setItem(22, entry(Material.BOOK, "&e&lCOMO FUNCIONA",
            "&7A roleta percorre os itens no inventário.",
            "&7Raras são mais comuns; lendárias são mais difíceis.",
            "&7Você conquista cada relíquia uma única vez.",
            "&7Os fragmentos só são consumidos após validar a troca."));
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
                "&7Categoria: &f" + relic.category(), "&7" + relic.power(), "&aConquistada"));
        }
        inv.setItem(49, entry(Material.ARROW, "&e&lVOLTAR", "&7Voltar ao menu principal"));
        inv.setItem(4, entry(Material.BOOK, "&b&lSUA COLEÇÃO", "&7Relíquias conquistadas: &f" + index + "&7/" + RelicCatalog.all().size()));
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
        RelicCatalog.Relic reward = chooseWeightedReward(missing);
        removeFragments(player, fragmentKey, cost);
        plugin.registerOwned(player, reward.id());
        plugin.giveOrDrop(player, plugin.createRelic(reward));
        player.sendMessage(ReliquiasPlugin.color("&6✦ &aVocê forjou: " + rarity.color + "&l" + reward.name() + "&a!"));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
        openSmith(player);
    }

    private void startSpin(Player player) {
        final int cost = plugin.getConfig().getInt("smith.spin-cost", 8);
        List<RelicCatalog.Relic> missing = RelicCatalog.all().stream()
            .filter(relic -> !plugin.owns(player, relic.id())).toList();
        if (missing.isEmpty()) {
            player.sendMessage(ReliquiasPlugin.color("&aVocê já conquistou todas as relíquias disponíveis!"));
            return;
        }
        int available = countFragments(player, "raro");
        if (available < cost) {
            player.sendMessage(ReliquiasPlugin.color("&cVocê precisa de &f" + cost
                + " fragmentos raros&c. Você possui &f" + available + "&c."));
            return;
        }

        // Sorteio ponderado por raridade; raridades esgotadas redistribuem seu peso.
        if (!spinning.add(player.getUniqueId())) {
            player.sendMessage(ReliquiasPlugin.color("&cSua roleta já está em andamento."));
            return;
        }
        RelicCatalog.Relic reward = chooseWeightedReward(missing);

        removeFragments(player, "raro", cost);
        Inventory spin = Bukkit.createInventory(null, 27, SPIN_TITLE);
        ItemStack pane = entry(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i=0;i<27;i++) if (i<9 || i>=18 || i%9==0 || i%9==8) spin.setItem(i,pane);
        player.openInventory(spin);

        final RelicCatalog.Relic finalReward = reward;
        new BukkitRunnable() {
            int step = 0;
            int slot = 10;
            @Override public void run() {
                if (!player.isOnline()) { spinning.remove(player.getUniqueId()); cancel(); return; }
                RelicCatalog.Relic preview = RelicCatalog.all().get(step % RelicCatalog.all().size());
                spin.setItem(slot, entry(preview.icon(), preview.rarity().color + "&l" + preview.name(),
                    "&7" + preview.category(), "&7A roleta está girando..."));
                spin.setItem(13, entry(Material.CLOCK, "&e&lSORTEANDO...", "&7Aguarde o resultado."));
                slot++;
                if (slot > 16) slot = 10;
                step++;
                if (step >= 32) {
                    cancel();
                    spinning.remove(player.getUniqueId());
                    spin.setItem(13, entry(finalReward.icon(), finalReward.rarity().color + "&l" + finalReward.name(),
                        "&7Categoria: &f" + finalReward.category(),
                        "&7" + finalReward.power(),
                        "&aRelíquia conquistada!"));
                    plugin.registerOwned(player, finalReward.id());
                    plugin.giveOrDrop(player, plugin.createRelic(finalReward));
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.15f);
                    player.sendMessage(ReliquiasPlugin.color("&6✦ &aA roleta parou em: "
                        + finalReward.rarity().color + "&l" + finalReward.name() + "&a!"));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openSmith(player), 50L);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private RelicCatalog.Relic chooseWeightedReward(List<RelicCatalog.Relic> candidates) {
        int totalWeight = 0;
        for (RelicCatalog.Relic relic : candidates) totalWeight += rarityWeight(relic.rarity());
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (RelicCatalog.Relic relic : candidates) {
            roll -= rarityWeight(relic.rarity());
            if (roll < 0) return relic;
        }
        return candidates.get(candidates.size() - 1);
    }

    private int rarityWeight(RelicCatalog.Rarity rarity) {
        return switch (rarity) {
            case RARO -> 60;
            case EPICO -> 25;
            case MITICO -> 11;
            case LENDARIO -> 4;
        };
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
        if (!title.equals(TITLE) && !title.startsWith(PREFIX) && !title.equals(SMITH_TITLE) && !title.equals(COLLECTION_TITLE) && !title.equals(SPIN_TITLE) && !plugin.missions().isMissionMenu(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null
            || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getSlot();
        if (title.equals(SPIN_TITLE)) return;
        if (plugin.missions().isMissionMenu(title)) { if (slot == 10 || slot == 12 || slot == 14 || slot == 16) plugin.missions().handleClick(player, slot); return; }
        if (title.equals(SMITH_TITLE)) {
            if (slot == 13) startSpin(player);
            return;
        }
        if (title.equals(COLLECTION_TITLE)) { if (slot == 49) open(player); return; }
        if (title.equals(TITLE)) {
            switch (slot) {
                case 10 -> openRarity(player, RelicCatalog.Rarity.RARO);
                case 12 -> openRarity(player, RelicCatalog.Rarity.EPICO);
                case 14 -> openRarity(player, RelicCatalog.Rarity.MITICO);
                case 16 -> openRarity(player, RelicCatalog.Rarity.LENDARIO);
                case 29 -> player.sendMessage(ReliquiasPlugin.color("&eOs chefes mantêm grupos de 5 a 8 nas áreas elegíveis: Raro em Cherry Grove/Plains, Épico em cavernas, Mítico no Nether e Lendário no End. Todos garantem fragmentos raros; alguns podem dar fragmentos superiores. O Guardião Raro revela coordenadas e pode ser alcançado pela Bússola do Guardião."));
                case 31 -> openCollection(player);
                case 33 -> openSmith(player);
                case 35 -> plugin.missions().open(player);
                default -> { }
            }
        } else if (slot == 49) open(player);
    }

    @EventHandler public void onMenuDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(TITLE) || title.startsWith(PREFIX) || title.equals(SMITH_TITLE) || title.equals(COLLECTION_TITLE) || title.equals(SPIN_TITLE) || plugin.missions().isMissionMenu(title))
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
