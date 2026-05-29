package de.julien.pluginhider;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PluginHider extends JavaPlugin implements Listener {

    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
            "pl", "plugins", "bukkit:pl", "bukkit:plugins"
    ));

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        tryRemoveCommands();
        getLogger().info("Enabled - /pl and /plugins are blocked.");
    }

    private void tryRemoveCommands() {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            getLogger().warning("Could not access CommandMap via reflection, using event-based blocking only.");
            return;
        }

        Map<String, Command> knownCommands = getKnownCommands(commandMap);
        if (knownCommands == null) {
            getLogger().warning("Could not access knownCommands, using event-based blocking only.");
            return;
        }

        for (String cmd : BLOCKED) {
            Command removed = knownCommands.remove(cmd);
            if (removed != null) {
                getLogger().info("Removed /" + cmd + " from CommandMap.");
            }
        }
    }

    private CommandMap getCommandMap() {
        Class<?> serverClass = Bukkit.getServer().getClass();
        while (serverClass != null) {
            try {
                Field field = serverClass.getDeclaredField("commandMap");
                field.setAccessible(true);
                return (CommandMap) field.get(Bukkit.getServer());
            } catch (NoSuchFieldException e) {
                serverClass = serverClass.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands(CommandMap commandMap) {
        Class<?> clazz = commandMap.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField("knownCommands");
                field.setAccessible(true);
                return (Map<String, Command>) field.get(commandMap);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String getLabel(String message) {
        String msg = message.trim();
        if (msg.isEmpty()) return "";
        int start = msg.charAt(0) == '/' ? 1 : 0;
        int end = msg.indexOf(' ');
        if (end == -1) end = msg.length();
        return msg.substring(start, end).toLowerCase();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (BLOCKED.contains(getLabel(event.getMessage()))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Unknown command. Type \"/help\" for help.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (BLOCKED.contains(getLabel(event.getCommand()))) {
            event.setCancelled(true);
            event.getSender().sendMessage(ChatColor.RED + "Unknown command. Type \"help\" for help.");
        }
    }
}
