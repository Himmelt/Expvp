package org.soraworld.expvp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Himmelt
 */
public final class Expvp extends JavaPlugin implements Listener {

    private double expRatio = 0.0D;
    private final HashSet<String> pvpPlayers = new HashSet<>();
    private final HashSet<String> cmdWorlds = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reload();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void reload() {
        reloadConfig();
        expRatio = getConfig().getDouble("expRatio", 0.0D);
        cmdWorlds.clear();
        cmdWorlds.addAll(getConfig().getStringList("cmdWorlds"));
        pvpPlayers.clear();
        pvpPlayers.addAll(getConfig().getStringList("pvpPlayers"));
    }

    private void save() {
        getConfig().set("expRatio", expRatio);
        getConfig().set("cmdWorlds", new ArrayList<>(cmdWorlds));
        getConfig().set("pvpPlayers", new ArrayList<>(pvpPlayers));
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if ("reload".equalsIgnoreCase(args[0])) {
                if (sender.hasPermission("expvp.admin")) {
                    reload();
                    sender.sendMessage("config reloaded.");
                    return true;
                } else {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
            }

            if (sender instanceof Player) {
                String world = ((Player) sender).getWorld().getName();
                if (cmdWorlds.contains(world)) {
                    if ("on".equalsIgnoreCase(args[0])) {
                        pvpPlayers.add(sender.getName());
                        sender.sendMessage("Your pvp is on.");
                    } else if ("off".equalsIgnoreCase(args[0])) {
                        pvpPlayers.remove(sender.getName());
                        sender.sendMessage("Your pvp is off.");
                    } else {
                        sender.sendMessage("Param must be [on] or [off].");
                        return true;
                    }
                    save();
                } else {
                    sender.sendMessage("You cant change your pvp in this world.");
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        if (pvpPlayers.contains(event.getPlayer().getName())) {
            event.setAmount((int) Math.round((1.0D + expRatio) * event.getAmount()));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity damagee = event.getEntity();
        if (damager instanceof Player && damagee instanceof Player) {
            Player p1 = (Player) damager;
            Player p2 = (Player) damagee;
            if (!pvpPlayers.contains(p1.getName()) || !pvpPlayers.contains(p2.getName())) {
                event.setDamage(0);
                event.setCancelled(true);
            }
        }
    }
}
