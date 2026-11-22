package br.com.csdpbrs.reliquias;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReliquiasPlugin extends JavaPlugin {
    private NamespacedKey clockKey;
    private RelicsMenu menu;
    private BossManager bossManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        clockKey = new NamespacedKey(this, "relics_clock");
        menu = new RelicsMenu(this);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getPluginManager().registerEvents(new RelicsListener(this), this);
        bossManager = new BossManager(this);
        bossManager.start();
        getLogger().info("CSDPBRS ESSENTIALS RELIQUIAS ativado.");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) bossManager.stop();
    }

    public NamespacedKey clockKey() { return clockKey; }
    public RelicsMenu menu() { return menu; }
    public BossManager bossManager() { return bossManager; }

    public org.bukkit.inventory.ItemStack createClock() {
        var item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.CLOCK);
        var meta = item.getItemMeta();
        meta.setDisplayName(color(getConfig().getString("items.clock-name", "&6&lRelógio das Relíquias")));
        var lore = getConfig().getStringList("items.clock-lore").stream().map(ReliquiasPlugin::color).toList();
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(clockKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("reliquias")) return false;
        if (!sender.hasPermission("reliquias.admin")) {
            sender.sendMessage(color("&cVocê não tem permissão."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(color("&6/reliquias relogio <jogador>"));
            sender.sendMessage(color("&6/reliquias reload"));
            sender.sendMessage(color("&6/reliquias boss <raridade>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage(color("&aConfiguração recarregada. Reinicie para aplicar os novos ciclos de chefes."));
            return true;
        }
        if (args[0].equalsIgnoreCase("relogio") && args.length >= 2) {
            var target = getServer().getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage(color("&cJogador não encontrado.")); return true; }
            target.getInventory().addItem(createClock());
            sender.sendMessage(color("&aRelógio entregue para " + target.getName() + "."));
            return true;
        }
        if (args[0].equalsIgnoreCase("boss") && args.length >= 2) {
            if (bossManager.spawnNow(args[1].toLowerCase())) sender.sendMessage(color("&aTentativa de invocação iniciada."));
            else sender.sendMessage(color("&cNão foi possível invocar esse chefe. Confira a raridade e a dimensão."));
            return true;
        }
        return true;
    }
}
