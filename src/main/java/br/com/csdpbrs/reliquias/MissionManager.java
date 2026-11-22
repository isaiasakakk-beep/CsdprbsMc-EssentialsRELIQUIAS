package br.com.csdpbrs.reliquias;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class MissionManager implements Listener {
    private static final String TITLE = ReliquiasPlugin.color("&8&lMISSÕES DAS RELÍQUIAS");
    private final ReliquiasPlugin plugin;
    private final NamespacedKey kills, ores, bosses, claimed;

    public MissionManager(ReliquiasPlugin plugin) {
        this.plugin = plugin;
        kills = new NamespacedKey(plugin, "mission_mob_kills");
        ores = new NamespacedKey(plugin, "mission_ores_mined");
        bosses = new NamespacedKey(plugin, "mission_boss_kills");
        claimed = new NamespacedKey(plugin, "mission_claimed");
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        String rarity = event.getEntity().getPersistentDataContainer().get(
            new NamespacedKey(plugin, "boss_rarity"), PersistentDataType.STRING);
        if (rarity != null) {
            increment(killer, bosses, 1);
            return;
        }
        if (event.getEntity() instanceof Monster) increment(killer, kills, 1);
    }

    @EventHandler
    public void onOreBreak(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (type.name().endsWith("_ORE") || type == Material.ANCIENT_DEBRIS)
            increment(event.getPlayer(), ores, 1);
    }

    private void increment(Player player, NamespacedKey key, int amount) {
        int value = player.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
        player.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value + amount);
    }

    private int progress(Player player, NamespacedKey key) {
        return player.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    private boolean isClaimed(Player player, String id) {
        String raw = player.getPersistentDataContainer().get(claimed, PersistentDataType.STRING);
        return raw != null && List.of(raw.split(",")).contains(id);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, pane);
        mission(inv, 10, player, "cacador", Material.IRON_SWORD, "&f&lCAÇADOR DE CRIATURAS",
            progress(player, kills), 50, "Elimine 50 criaturas hostis.", "raro", 8, kills);
        mission(inv, 12, player, "minerador", Material.DIAMOND_PICKAXE, "&5&lMINERADOR DAS PROFUNDEZAS",
            progress(player, ores), 96, "Minere 96 minérios.", "epico", 8, ores);
        mission(inv, 14, player, "cacador_boss", Material.NETHER_STAR, "&c&lCAÇADOR DE CHEFES",
            progress(player, bosses), 3, "Derrote 3 chefes de relíquias.", "mitico", 8, bosses);
        mission(inv, 16, player, "soberano", Material.DRAGON_EGG, "&6&lSOBERANO DAS RELÍQUIAS",
            progress(player, bosses), 10, "Derrote 10 chefes de relíquias.", "lendario", 8, bosses);
        inv.setItem(22, item(Material.BARRIER, "&cFechar"));
        player.openInventory(inv);
    }

    private void mission(Inventory inv, int slot, Player player, String id, Material icon, String name,
                        int current, int target, String description, String rarity, int reward, NamespacedKey key) {
        boolean done = current >= target;
        boolean already = isClaimed(player, id);
        String status = already ? "&7Recompensa resgatada" : done ? "&aClique para resgatar!" : "&eEm andamento";
        inv.setItem(slot, item(icon, name, "&7" + description,
            "&7Progresso: &f" + Math.min(current, target) + "/" + target,
            "&7Recompensa: &f" + reward + " fragmentos " + rarity, status));
    }

    public void handleClick(Player player, int slot) {
        String id;
        NamespacedKey key;
        int target;
        String rarity;
        int reward = 8;
        if (slot == 10) { id = "cacador"; key = kills; target = 50; rarity = "raro"; }
        else if (slot == 12) { id = "minerador"; key = ores; target = 96; rarity = "epico"; }
        else if (slot == 14) { id = "cacador_boss"; key = bosses; target = 3; rarity = "mitico"; }
        else if (slot == 16) { id = "soberano"; key = bosses; target = 10; rarity = "lendario"; }
        else return;
        Player p = player;
        if (isClaimed(p, id)) { p.sendMessage(ReliquiasPlugin.color("&cVocê já resgatou essa missão.")); return; }
        int current = progress(p, key);
        if (current < target) {
            p.sendMessage(ReliquiasPlugin.color("&cMissão incompleta: &f" + current + "/" + target));
            return;
        }
        String raw = p.getPersistentDataContainer().get(claimed, PersistentDataType.STRING);
        p.getPersistentDataContainer().set(claimed, PersistentDataType.STRING,
            raw == null || raw.isBlank() ? id : raw + "," + id);
        ItemStack fragments = new ItemStack(Material.AMETHYST_SHARD, reward);
        ItemMeta meta = fragments.getItemMeta();
        meta.setDisplayName(ReliquiasPlugin.color((rarity.equals("raro") ? "&f" : rarity.equals("epico") ? "&5" : rarity.equals("mitico") ? "&c" : "&6") + "Fragmento " + rarity));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "fragment_rarity"), PersistentDataType.STRING, rarity);
        fragments.setItemMeta(meta);
        p.getInventory().addItem(fragments);
        p.sendMessage(ReliquiasPlugin.color("&aMissão concluída! Você recebeu &f" + reward + " fragmentos " + rarity + "&a."));
        open(p);
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ReliquiasPlugin.color(name));
        meta.setLore(java.util.Arrays.stream(lore).map(ReliquiasPlugin::color).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isMissionMenu(String title) { return TITLE.equals(title); }
}
