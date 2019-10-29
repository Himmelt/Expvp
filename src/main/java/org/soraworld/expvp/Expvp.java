package org.soraworld.expvp;

import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Himmelt
 */
public final class Expvp extends JavaPlugin implements Listener {

    private final HashSet<String> pvps = new HashSet<>();
    private final HashMap<String, Float> ratios = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reload();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void reload() {
        reloadConfig();
        pvps.clear();
        pvps.addAll(getConfig().getStringList("pvpWorlds"));
        ratios.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("expRatios");
        if (section != null) {
            for (String world : section.getKeys(false)) {
                float ratio = Float.parseFloat(section.getString(world));
                ratios.put(world, ratio);
            }
        }
    }

    private void save() {
        getConfig().set("pvpWorlds", new ArrayList<>(pvps));
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            boolean enable;
            if ("on".equalsIgnoreCase(args[0])) {
                enable = true;
            } else if ("off".equalsIgnoreCase(args[0])) {
                enable = false;
            } else if ("reload".equalsIgnoreCase(args[0])) {
                reload();
                return true;
            } else {
                sender.sendMessage("must param [on] or [off].");
                return false;
            }
            String world;
            if (args.length == 1) {
                if (sender instanceof Player) {
                    world = ((Player) sender).getWorld().getName();
                } else if (sender instanceof BlockCommandSender) {
                    world = ((BlockCommandSender) sender).getBlock().getWorld().getName();
                } else if (sender instanceof CommandMinecart) {
                    world = ((CommandMinecart) sender).getWorld().getName();
                } else {
                    sender.sendMessage("sender must be player or commandblock or commandminecart.");
                    return false;
                }
            } else {
                world = args[1];
            }
            if (world != null && enable) {
                pvps.add(world);
                sender.sendMessage("pvp for world [" + world + "] is on.");
            } else {
                pvps.remove(world);
                sender.sendMessage("pvp for world [" + world + "] is off.");
            }
            save();
        } else {
            sender.sendMessage(command.getUsage());
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        String name = event.getPlayer().getWorld().getName();
        if (pvps.contains(name)) {
            int exp = event.getAmount();
            if (exp > 0) {
                event.setAmount(Math.round((1.0F + ratios.getOrDefault(name, 0.0F)) * exp));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity damagee = event.getEntity();
        String world = damagee.getWorld().getName();
        if (!pvps.contains(world) && damager instanceof Player && damagee instanceof Player) {
            event.setDamage(0);
            event.setCancelled(true);
        }
    }
}
