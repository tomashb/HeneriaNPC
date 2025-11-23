package fr.heneria.npc;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.io.File;
import java.util.List;

public class NPCListener implements Listener {

    private final HeneriaNPC plugin;

    public NPCListener(HeneriaNPC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked());
    }

    private void handleInteract(Player player, Entity clicked) {
        if (!(clicked instanceof ArmorStand)) return;

        NPCManager manager = plugin.getNpcManager();
        if (!manager.isNPC(clicked)) return;

        String id = manager.getNPCId(clicked);
        executeActions(player, id);
    }

    // Prevent damaging NPCs
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (plugin.getNpcManager().isNPC(event.getEntity())) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
                 // Optional: Trigger action on Left Click too?
                 // Usually NPCs are right click, but sometimes left click is expected.
                 // For now, let's keep it clean and just cancel damage.
            }
        }
    }

    private void executeActions(Player player, String npcId) {
        File configFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!configFile.exists()) return;

        ConfigurationSection config = YamlConfiguration.loadConfiguration(configFile).getConfigurationSection("npcs." + npcId);
        if (config == null) return;

        List<String> actions = config.getStringList("actions");
        for (String action : actions) {
            String[] parts = action.split(":", 2);
            if (parts.length < 2) continue;

            String type = parts[0].trim().toUpperCase();
            String value = parts[1].trim();

            switch (type) {
                case "CONNECT":
                    sendToServer(player, value);
                    break;
                case "MESSAGE":
                    player.sendMessage(MiniMessage.miniMessage().deserialize(value));
                    break;
                case "CMD":
                case "COMMAND":
                    player.performCommand(value);
                    break;
                case "CONSOLE":
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.replace("%player%", player.getName()));
                    break;
            }
        }
    }

    private void sendToServer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}
