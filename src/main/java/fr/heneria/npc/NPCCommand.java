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
import java.util.stream.Collectors;

public class NPCCommand implements CommandExecutor, TabCompleter {

    private final HeneriaNPC plugin;
    private final MiniMessage miniMessage;

    public NPCCommand(HeneriaNPC plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("heneria.admin")) {
            sender.sendMessage(plugin.getNpcManager().parseColor("<red>Vous n'avez pas la permission d'exécuter cette commande."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getNpcManager().parseColor("<red>Cette commande doit être exécutée par un joueur."));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                plugin.getNpcManager().reload();
                player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#55ff55:#00aa00>Succès</gradient>] <gray>Configuration rechargée avec succès."));
                break;

            case "creer":
                if (args.length < 3) {
                    player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Usage: /hnpc creer <id> <skin>"));
                    return true;
                }
                plugin.getNpcManager().createNPC(args[1], args[2], player.getLocation());
                player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#55ff55:#00aa00>Succès</gradient>] <gray>Le NPC <white>" + args[1] + " <gray>a été créé."));
                break;

            case "supprimer":
                if (args.length < 2) {
                    player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Usage: /hnpc supprimer <id>"));
                    return true;
                }
                if (!plugin.getNpcManager().exists(args[1])) {
                    player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Ce NPC n'existe pas."));
                    return true;
                }
                plugin.getNpcManager().deleteNPC(args[1]);
                player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#55ff55:#00aa00>Succès</gradient>] <gray>Le NPC <white>" + args[1] + " <gray>a été supprimé."));
                break;

            case "tpici":
                if (args.length < 2) {
                    player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Usage: /hnpc tpici <id>"));
                    return true;
                }
                if (!plugin.getNpcManager().exists(args[1])) {
                    player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Ce NPC n'existe pas."));
                    return true;
                }
                plugin.getNpcManager().moveNPC(args[1], player.getLocation());
                player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#55ff55:#00aa00>Succès</gradient>] <gray>Le NPC <white>" + args[1] + " <gray>a été téléporté ici."));
                break;

            case "equiper":
                if (args.length < 3) {
                    player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Usage: /hnpc equiper <id> <slot>"));
                    return true;
                }
                if (!plugin.getNpcManager().exists(args[1])) {
                    player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Ce NPC n'existe pas."));
                    return true;
                }

                // Map French slot names to internal names
                String slot = args[2].toLowerCase();
                String internalSlot = switch (slot) {
                    case "tete" -> "helmet";
                    case "torse" -> "chestplate";
                    case "jambes" -> "leggings";
                    case "bottes" -> "boots";
                    case "main" -> "main_hand";
                    case "main_off" -> "off_hand";
                    default -> slot;
                };

                plugin.getNpcManager().updateEquipment(args[1], internalSlot, player.getInventory().getItemInMainHand());
                player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#55ff55:#00aa00>Succès</gradient>] <gray>Équipement mis à jour pour <white>" + args[1] + "<gray> (Slot: " + slot + ")."));
                break;

            case "renommer":
                if (args.length < 3) {
                    player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Usage: /hnpc renommer <id> <texte...>"));
                    return true;
                }
                if (!plugin.getNpcManager().exists(args[1])) {
                    player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Ce NPC n'existe pas."));
                    return true;
                }
                String fullText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                // Note: parseColor handles the color parsing now. We don't need manual & -> § replacement
                // unless we want to normalize it before sending to manager, but manager handles it on spawn.
                // However, we pass raw lines to manager. Manager uses parseColor on spawn.

                // We split by '|' for multi-line support
                List<String> lines = new ArrayList<>();
                for (String line : fullText.split("\\|")) {
                    lines.add(line);
                }

                plugin.getNpcManager().renameNPC(args[1], lines);
                player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#55ff55:#00aa00>Succès</gradient>] <gray>Hologramme mis à jour pour <white>" + args[1] + "<gray>."));
                break;

            case "pose":
                if (args.length < 3) {
                     player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Usage: /hnpc pose <id> <defaut|marche|pointer|regard_bas>"));
                     return true;
                }
                if (!plugin.getNpcManager().exists(args[1])) {
                    player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#ff5555:#aa0000>Erreur</gradient>] <red>Ce NPC n'existe pas."));
                    return true;
                }
                plugin.getNpcManager().setPose(args[1], args[2]);
                player.sendMessage(plugin.getNpcManager().parseColor("<dark_gray>[<gradient:#55ff55:#00aa00>Succès</gradient>] <gray>Pose définie sur <white>" + args[2] + "<gray> pour <white>" + args[1] + "<gray>."));
                break;

            case "liste":
                player.sendMessage(miniMessage.deserialize("<gradient:#ffaa00:#ffff55>NPCs Actifs:</gradient> <gray>" + String.join(", ", plugin.getNpcManager().getNPCIds())));
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getNpcManager().parseColor("<strikethrough><gradient:#ffaa00:#ffff55>-----------</gradient></strikethrough> <gold>HENERIA NPC <strikethrough><gradient:#ffff55:#ffaa00>-----------</gradient></strikethrough>"));
        sender.sendMessage(plugin.getNpcManager().parseColor("<yellow>/hnpc creer <id> <skin> <gray>- Créer un NPC"));
        sender.sendMessage(plugin.getNpcManager().parseColor("<yellow>/hnpc tpici <id> <gray>- Téléporter ici"));
        sender.sendMessage(plugin.getNpcManager().parseColor("<yellow>/hnpc renommer <id> <texte> <gray>- Changer le nom (Couleurs OK)"));
        sender.sendMessage(plugin.getNpcManager().parseColor("<yellow>/hnpc equiper <id> <slot> <gray>- Équiper l'item en main"));
        sender.sendMessage(plugin.getNpcManager().parseColor("<yellow>/hnpc pose <id> <type> <gray>- Changer l'animation"));
        sender.sendMessage(plugin.getNpcManager().parseColor("<yellow>/hnpc supprimer <id> <gray>- Supprimer un NPC"));
        sender.sendMessage(plugin.getNpcManager().parseColor("<yellow>/hnpc liste <gray>- Liste des NPCs"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("creer", "supprimer", "tpici", "equiper", "renommer", "pose", "liste", "reload"), args[0]);
        }

        if (args.length == 2) {
             String sub = args[0].toLowerCase();
             if (Arrays.asList("supprimer", "tpici", "equiper", "renommer", "pose").contains(sub)) {
                 return filter(new ArrayList<>(plugin.getNpcManager().getNPCIds()), args[1]);
             }
             return Collections.emptyList();
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("equiper")) {
                return filter(Arrays.asList("tete", "torse", "jambes", "bottes", "main", "main_off"), args[2]);
            }
            if (args[0].equalsIgnoreCase("pose")) {
                return filter(Arrays.asList("defaut", "marche", "pointer", "regard_bas"), args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
