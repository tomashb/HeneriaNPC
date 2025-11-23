package fr.heneria.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NPCCommand implements CommandExecutor, TabCompleter {

    private final HeneriaNPC plugin;
    private final MiniMessage miniMessage;
    private final String prefix = "<gradient:#ffaa00:#ffff55><b>HeneriaNPC</b></gradient> <dark_gray>» ";

    public NPCCommand(HeneriaNPC plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("heneria.admin")) {
            sender.sendMessage(miniMessage.deserialize("<red>You do not have permission to execute this command."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize("<red>This command can only be executed by a player."));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                plugin.getNpcManager().reload();
                player.sendMessage(miniMessage.deserialize(prefix + "<green>NPCs reloaded successfully!"));
                break;

            case "create":
                if (args.length < 3) {
                    player.sendMessage(miniMessage.deserialize(prefix + "<red>Usage: /hnpc create <id> <skin>"));
                    return true;
                }
                plugin.getNpcManager().createNPC(args[1], args[2], player.getLocation());
                player.sendMessage(miniMessage.deserialize(prefix + "<green>NPC <white>" + args[1] + " <green>created!"));
                break;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage(miniMessage.deserialize(prefix + "<red>Usage: /hnpc delete <id>"));
                    return true;
                }
                if (!plugin.getNpcManager().exists(args[1])) {
                    player.sendMessage(miniMessage.deserialize(prefix + "<red>NPC not found."));
                    return true;
                }
                plugin.getNpcManager().deleteNPC(args[1]);
                player.sendMessage(miniMessage.deserialize(prefix + "<green>NPC <white>" + args[1] + " <green>deleted!"));
                break;

            case "move":
                if (args.length < 2) {
                    player.sendMessage(miniMessage.deserialize(prefix + "<red>Usage: /hnpc move <id>"));
                    return true;
                }
                if (!plugin.getNpcManager().exists(args[1])) {
                    player.sendMessage(miniMessage.deserialize(prefix + "<red>NPC not found."));
                    return true;
                }
                plugin.getNpcManager().moveNPC(args[1], player.getLocation());
                player.sendMessage(miniMessage.deserialize(prefix + "<green>NPC <white>" + args[1] + " <green>moved to your location!"));
                break;

            case "equip":
                if (args.length < 3) {
                    player.sendMessage(miniMessage.deserialize(prefix + "<red>Usage: /hnpc equip <id> <slot>"));
                    return true;
                }
                if (!plugin.getNpcManager().exists(args[1])) {
                    player.sendMessage(miniMessage.deserialize(prefix + "<red>NPC not found."));
                    return true;
                }
                plugin.getNpcManager().updateEquipment(args[1], args[2], player.getInventory().getItemInMainHand());
                player.sendMessage(miniMessage.deserialize(prefix + "<green>Equipped item to <white>" + args[2] + "<green> on NPC <white>" + args[1] + "<green>!"));
                break;

            case "rename":
                if (args.length < 3) {
                    player.sendMessage(miniMessage.deserialize(prefix + "<red>Usage: /hnpc rename <id> <lines...>"));
                    return true;
                }
                if (!plugin.getNpcManager().exists(args[1])) {
                    player.sendMessage(miniMessage.deserialize(prefix + "<red>NPC not found."));
                    return true;
                }
                String fullText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                // Support newline via \n or similar if desired, for now just single string
                // But the API expects List<String>. Let's treat '|' as newline
                List<String> lines = Arrays.asList(fullText.split("\\|"));
                plugin.getNpcManager().renameNPC(args[1], lines);
                player.sendMessage(miniMessage.deserialize(prefix + "<green>Hologram updated for NPC <white>" + args[1] + "<green>!"));
                break;

            case "pose":
                if (args.length < 3) {
                     player.sendMessage(miniMessage.deserialize(prefix + "<red>Usage: /hnpc pose <id> <default|walking|pointing|look_down>"));
                     return true;
                }
                if (!plugin.getNpcManager().exists(args[1])) {
                    player.sendMessage(miniMessage.deserialize(prefix + "<red>NPC not found."));
                    return true;
                }
                plugin.getNpcManager().setPose(args[1], args[2]);
                player.sendMessage(miniMessage.deserialize(prefix + "<green>Pose set to <white>" + args[2] + "<green> for NPC <white>" + args[1] + "<green>!"));
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessage.deserialize("<gradient:#ffaa00:#ffff55>----------------[ Heneria NPC ]----------------</gradient>"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/hnpc create <id> <skin> <dark_gray>- <gray>Créer un NPC"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/hnpc delete <id> <dark_gray>- <gray>Supprimer un NPC"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/hnpc move <id> <dark_gray>- <gray>Déplacer un NPC ici"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/hnpc equip <id> <slot> <dark_gray>- <gray>Équiper l'item en main"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/hnpc rename <id> <text> <dark_gray>- <gray>Changer l'hologramme"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/hnpc pose <id> <type> <dark_gray>- <gray>Changer l'animation"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/hnpc reload <dark_gray>- <gray>Recharger la config"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "delete", "move", "equip", "rename", "pose", "reload");
        }
        if (args.length == 2) {
             // ID suggestion? Not easy to get IDs from here without public getter
             // But let's assume users know IDs or we could expose keys
             return Collections.emptyList();
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("equip")) {
                return Arrays.asList("helmet", "chestplate", "leggings", "boots", "hand", "offhand");
            }
            if (args[0].equalsIgnoreCase("pose")) {
                return Arrays.asList("default", "walking", "pointing", "look_down");
            }
        }
        return Collections.emptyList();
    }
}
