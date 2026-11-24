package br.com.csdpbrs.reliquias;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ReliquiasPlugin extends JavaPlugin implements org.bukkit.event.Listener {
    private NamespacedKey clockKey, ownedKey;
    private RelicsMenu menu;
    private BossManager bossManager;
    private RelicSmithNpc smithNpc;
    private MissionManager missions;

    @Override public void onEnable() {
        saveDefaultConfig();
        clockKey = new NamespacedKey(this, "relics_clock");
        ownedKey = new NamespacedKey(this, "owned_relics");
        menu = new RelicsMenu(this);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getPluginManager().registerEvents(new RelicsListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);
        missions = new MissionManager(this);
        getServer().getPluginManager().registerEvents(missions, this);
        getServer().getPluginManager().registerEvents(new RelicAbilityListener(this), this);
        smithNpc = new RelicSmithNpc(this);
        getServer().getPluginManager().registerEvents(smithNpc, this);
        bossManager = new BossManager(this);
        getServer().getPluginManager().registerEvents(bossManager, this);
        bossManager.start();
        getLogger().info("CSDPBRS ESSENTIALS RELIQUIAS ativado.");
    }

    @org.bukkit.event.EventHandler
    public void onFirstJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (!player.getPersistentDataContainer().has(clockKey, PersistentDataType.BYTE)) {
            giveOrDrop(player, createClock());
            player.getPersistentDataContainer().set(clockKey, PersistentDataType.BYTE, (byte) 1);
            player.sendMessage(color("&6Você recebeu o Relógio das Relíquias!"));
        }
    }

    @org.bukkit.event.EventHandler
    public void onClockDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        event.getDrops().removeIf(item -> item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(clockKey, PersistentDataType.BYTE));
    }

    @org.bukkit.event.EventHandler
    public void onClockRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(this, () -> {
            var player = event.getPlayer();
            boolean hasClock = java.util.Arrays.stream(player.getInventory().getContents())
                .anyMatch(item -> item != null && item.hasItemMeta()
                    && item.getItemMeta().getPersistentDataContainer().has(clockKey, PersistentDataType.BYTE));
            if (!hasClock) giveOrDrop(player, createClock());
        });
    }

    @Override public void onDisable() { if (bossManager != null) bossManager.stop(); }
    public NamespacedKey clockKey() { return clockKey; }
    public NamespacedKey ownedKey() { return ownedKey; }
    public RelicsMenu menu() { return menu; }
    public BossManager bossManager() { return bossManager; }
    public RelicSmithNpc smithNpc() { return smithNpc; }
    public MissionManager missions() { return missions; }

    public Set<String> ownedRelics(org.bukkit.entity.Player player) {
        String raw = player.getPersistentDataContainer().get(ownedKey, PersistentDataType.STRING);
        Set<String> ids = new LinkedHashSet<>();
        if (raw != null && !raw.isBlank()) ids.addAll(Arrays.asList(raw.split(",")));
        return ids;
    }
    public boolean owns(org.bukkit.entity.Player player, String id) { return ownedRelics(player).contains(id); }
    public void registerOwned(org.bukkit.entity.Player player, String id) {
        Set<String> ids = ownedRelics(player); ids.add(id);
        player.getPersistentDataContainer().set(ownedKey, PersistentDataType.STRING, String.join(",", ids));
    }

    public void giveOrDrop(org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack item) {
        getServer().getScheduler().runTask(this, () -> {
            var leftovers = player.getInventory().addItem(item);
            leftovers.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        });
    }

    public org.bukkit.inventory.ItemStack createClock() {
        var item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.CLOCK);
        var meta = item.getItemMeta();
        meta.setDisplayName(color(getConfig().getString("items.clock-name", "&6&lRelógio das Relíquias")));
        meta.setLore(getConfig().getStringList("items.clock-lore").stream().map(ReliquiasPlugin::color).toList());
        meta.getPersistentDataContainer().set(clockKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta); return item;
    }
    public org.bukkit.inventory.ItemStack createRelic(RelicCatalog.Relic relic) {
        var item = new org.bukkit.inventory.ItemStack(relic.icon());
        var meta = item.getItemMeta();
        meta.setDisplayName(color(relic.rarity().color + "&l" + relic.name()));
        meta.setLore(java.util.List.of(color("&7Categoria: &f" + relic.category()), color("&7" + relic.power()), color("&7Recarga: &f" + relic.cooldown()),
            color("&eClique com o botão direito para ativar."), color("&8Relíquia: " + relic.id())));
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "relic_id"), PersistentDataType.STRING, relic.id());
        item.setItemMeta(meta); return item;
    }
    public static String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("reliquias")) return false;
        if (!sender.hasPermission("reliquias.admin")) { sender.sendMessage(color("&cVocê não tem permissão.")); return true; }
        if (args.length == 0) {
            sender.sendMessage(color("&6/reliquias relogio <jogador>"));
            sender.sendMessage(color("&6/reliquias give <jogador> <id>"));
            sender.sendMessage(color("&6/reliquias reload"));
            sender.sendMessage(color("&6/reliquias boss <raridade>"));
            sender.sendMessage(color("&6/reliquias npc")); return true;
        }
        if (args[0].equalsIgnoreCase("reload")) { reloadConfig(); sender.sendMessage(color("&aConfiguração recarregada.")); return true; }
        if (args[0].equalsIgnoreCase("relogio") && args.length >= 2) {
            var target = getServer().getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage(color("&cJogador não encontrado.")); return true; }
            giveOrDrop(target, createClock()); sender.sendMessage(color("&aRelógio entregue.")); return true;
        }
        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            var target = getServer().getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage(color("&cJogador não encontrado.")); return true; }
            var relic = RelicCatalog.all().stream().filter(r -> r.id().equalsIgnoreCase(args[2])).findFirst().orElse(null);
            if (relic == null) { sender.sendMessage(color("&cID inválido. IDs: " + String.join(", ", RelicCatalog.all().stream().map(RelicCatalog.Relic::id).toList()))); return true; }
            giveOrDrop(target, createRelic(relic)); registerOwned(target, relic.id());
            sender.sendMessage(color("&aRelíquia &f" + relic.name() + " &aentregue para " + target.getName() + ".")); return true;
        }
        if (args[0].equalsIgnoreCase("npc")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) { sender.sendMessage(color("&cUse esse comando dentro do jogo.")); return true; }
            smithNpc.spawn(player); return true;
        }
        if (args[0].equalsIgnoreCase("boss") && args.length >= 2) {
            if (bossManager.spawnNow(args[1].toLowerCase())) sender.sendMessage(color("&aTentativa de invocação iniciada."));
            else sender.sendMessage(color("&cNão foi possível invocar esse chefe. Confira a raridade e a dimensão."));
            return true;
        }
        return true;
    }
}