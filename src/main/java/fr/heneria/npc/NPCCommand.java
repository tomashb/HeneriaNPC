package fr.heneria.npc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class NPCCommand implements CommandExecutor {

    private final HeneriaNPC plugin;

    public NPCCommand(HeneriaNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("henerianpc.admin")) {
            sender.sendMessage("§cYou do not have permission to execute this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.getNpcManager().reload();
            sender.sendMessage("§a[HeneriaNPC] NPCs reloaded successfully!");
            return true;
        }

        sender.sendMessage("§cUsage: /henerianpc reload");
        return true;
    }
}
