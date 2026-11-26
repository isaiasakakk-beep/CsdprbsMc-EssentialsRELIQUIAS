package br.com.csdpbrs.reliquias;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
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
        getConfig().addDefault("warden.owner-name", "Spiderzinpvp");
        getConfig().options().copyDefaults(true);
        saveConfig();
        menu = new RelicsMenu(this);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getPluginManager().registerEvents(new RelicsListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);
        missions = new MissionManager(this);
        getServer().getPluginManager().registerEvents(missions, this);
        RelicAbilityListener relicAbilities = new RelicAbilityListener(this);
        getServer().getPluginManager().registerEvents(relicAbilities, this);
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (var online : Bukkit.getOnlinePlayers()) relicAbilities.applyPassiveEffects(online);
        }, 20L, 20L);
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
    public NamespacedKey relicKey() { return new NamespacedKey(this, "relic_id"); }
    public String wardenOwnerName() { return getConfig().getString("warden.owner-name", "Spiderzinpvp"); }
    public boolean isWardenOwner(OfflinePlayer player) {
        String uuid = getConfig().getString("warden.owner-uuid", "");
        if (!uuid.isBlank() && player.getUniqueId().toString().equalsIgnoreCase(uuid)) return true;
        return player.getName() != null && player.getName().equalsIgnoreCase(wardenOwnerName());
    }
    public void setWardenOwner(Player player) {
        getConfig().set("warden.owner-uuid", player.getUniqueId().toString());
        getConfig().set("warden.owner-name", player.getName());
        saveConfig();
        registerOwned(player, "warden");
    }
    public void unregisterOwned(Player player, String id) {
        Set<String> ids = ownedRelics(player);
        ids.remove(id);
        player.getPersistentDataContainer().set(ownedKey, PersistentDataType.STRING, String.join(",", ids));
    }
    public void removeRelicItems(Player player, String id) {
        var inv = player.getInventory();
        ItemStack[] storage = inv.getStorageContents();
        for (int i=0;i<storage.length;i++) if (hasRelicId(storage[i], id)) storage[i] = null;
        inv.setStorageContents(storage);
        ItemStack[] armor = inv.getArmorContents();
        for (int i=0;i<armor.length;i++) if (hasRelicId(armor[i], id)) armor[i] = null;
        inv.setArmorContents(armor);
        if (hasRelicId(inv.getItemInOffHand(), id)) inv.setItemInOffHand(null);
        if (hasRelicId(player.getItemOnCursor(), id)) player.setItemOnCursor(null);
        unregisterOwned(player, id);
    }
    private boolean hasRelicId(ItemStack item, String id) {
        return item != null && item.hasItemMeta() && id.equals(item.getItemMeta().getPersistentDataContainer().get(relicKey(), PersistentDataType.STRING));
    }
    private void queueOfflineRemoval(OfflinePlayer player, String id) {
        String key = "pending-removals." + player.getUniqueId();
        java.util.List<String> ids = new java.util.ArrayList<>(getConfig().getStringList(key));
        if (!ids.contains(id)) ids.add(id);
        getConfig().set(key, ids);
    }
    private void applyPendingRemovals(Player player) {
        String key = "pending-removals." + player.getUniqueId();
        for (String id : getConfig().getStringList(key)) removeRelicItems(player, id);
        getConfig().set(key, null);
        saveConfig();
    }
    @org.bukkit.event.EventHandler
    public void onRelicPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equalsIgnoreCase(wardenOwnerName()) && getConfig().getString("warden.owner-uuid", "").isBlank()) {
            getConfig().set("warden.owner-uuid", player.getUniqueId().toString());
            saveConfig();
        }
        applyPendingRemovals(player);
        if (!isWardenOwner(player)) removeRelicItems(player, "warden");
    }
    @org.bukkit.event.EventHandler
    public void onWardenDrop(PlayerDropItemEvent event) {
        if (hasRelicId(event.getItemDrop().getItemStack(), "warden")) event.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void onWardenInventoryClick(InventoryClickEvent event) {
        if (hasRelicId(event.getCurrentItem(), "warden") || hasRelicId(event.getCursor(), "warden")) event.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void onWardenInventoryDrag(InventoryDragEvent event) {
        if (hasRelicId(event.getOldCursor(), "warden")) event.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void onWardenHopperMove(InventoryMoveItemEvent event) {
        if (hasRelicId(event.getItem(), "warden")) event.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void onWardenPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (hasRelicId(event.getItem().getItemStack(), "warden") && !isWardenOwner(player)) event.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void onWardenOwnerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline() || !isWardenOwner(player)) return;
            boolean hasWarden = java.util.Arrays.stream(player.getInventory().getContents()).anyMatch(item -> hasRelicId(item, "warden"))
                || java.util.Arrays.stream(player.getInventory().getArmorContents()).anyMatch(item -> hasRelicId(item, "warden"))
                || hasRelicId(player.getInventory().getItemInOffHand(), "warden");
            if (!hasWarden) {
                var warden = RelicCatalog.all().stream().filter(r -> r.id().equals("warden")).findFirst().orElseThrow();
                registerOwned(player, "warden");
                giveOrDrop(player, createRelic(warden));
            }
        }, 1L);
    }
    @org.bukkit.event.EventHandler
    public void onWardenOwnerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!isWardenOwner(victim)) return;
        event.getDrops().removeIf(item -> hasRelicId(item, "warden"));
        unregisterOwned(victim, "warden");
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            setWardenOwner(killer);
            Bukkit.getScheduler().runTask(this, () -> giveOrDrop(killer, createRelic(RelicCatalog.all().stream().filter(r -> r.id().equals("warden")).findFirst().orElseThrow())));
            Bukkit.broadcastMessage(color("&6&lRELIQUIA DO WARDEN &7agora pertence a &e" + killer.getName() + "&7!"));
        }
    }
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
        meta.getPersistentDataContainer().set(relicKey(), PersistentDataType.STRING, relic.id());
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
            sender.sendMessage(color("&6/reliquias npc"));
            sender.sendMessage(color("&6/reliquias remover <id|todas> <jogador|todos> [exceto <nick>]"));
            sender.sendMessage(color("&6/reliquias warden")); return true;
        }
        if (args[0].equalsIgnoreCase("reload")) { reloadConfig(); sender.sendMessage(color("&aConfiguração recarregada.")); return true; }
        if (args[0].equalsIgnoreCase("warden")) { sender.sendMessage(color("&6A Relíquia do Warden pertence a: &e" + wardenOwnerName())); return true; }
        if (args[0].equalsIgnoreCase("remover") && args.length >= 3) {
            String requested = args[1].equalsIgnoreCase("todas") ? "todas" : args[1].toLowerCase();
            if (!requested.equals("todas") && RelicCatalog.all().stream().noneMatch(r -> r.id().equals(requested))) { sender.sendMessage(color("&cID inválido. Use: " + String.join(", ", RelicCatalog.all().stream().map(RelicCatalog.Relic::id).toList()) + ", ou todas.")); return true; }
            String exclude = args.length >= 5 && args[3].equalsIgnoreCase("exceto") ? args[4] : "";
            java.util.List<String> ids = requested.equals("todas") ? RelicCatalog.all().stream().map(RelicCatalog.Relic::id).toList() : java.util.List.of(requested);
            java.util.List<OfflinePlayer> targets = new java.util.ArrayList<>();
            if (args[2].equalsIgnoreCase("todos")) targets.addAll(java.util.Arrays.asList(Bukkit.getOfflinePlayers()));
            else targets.add(Bukkit.getOfflinePlayer(args[2]));
            int onlineCount = 0, queuedCount = 0;
            for (OfflinePlayer target : targets) {
                if (target.getName() != null && target.getName().equalsIgnoreCase(exclude)) continue;
                Player online = target.getPlayer();
                for (String id : ids) {
                    if (online != null) removeRelicItems(online, id);
                    else queueOfflineRemoval(target, id);
                }
                if (online != null) onlineCount++; else queuedCount++;
            }
            saveConfig();
            sender.sendMessage(color("&aRemoção de &f" + requested + "&a processada. Online: &f" + onlineCount + "&a; offline com remoção pendente: &f" + queuedCount + "&a. Exceção: &f" + (exclude.isBlank() ? "nenhuma" : exclude)));
            return true;
        }
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
            if (relic.id().equals("warden") && !isWardenOwner(target)) { sender.sendMessage(color("&cO Warden é único e só pode ser transferido ao derrotar o proprietário atual: &f" + wardenOwnerName())); return true; }
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