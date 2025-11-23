package fr.heneria.npc;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class HeneriaNPC extends JavaPlugin {

    private static HeneriaNPC instance;
    private NPCManager npcManager;
    private HeadDatabaseAPI hdbApi;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Vérifier si HeadDatabase est présent
        if (getServer().getPluginManager().getPlugin("HeadDatabase") == null) {
            getLogger().severe("ERREUR CRITIQUE: HeadDatabase n'est pas installé !");
            getLogger().severe("Le plugin HeneriaNPC va s'éteindre.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.hdbApi = new HeadDatabaseAPI();

        // Initialize Manager
        this.npcManager = new NPCManager(this);

        // Register Commands
        getCommand("henerianpc").setExecutor(new NPCCommand(this));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new NPCListener(this), this);

        // Register BungeeCord Channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Load Config and Spawn NPCs
        saveDefaultConfig();
        saveResource("npcs.yml", false); // Ensure npcs.yml is created
        npcManager.reload();

        getLogger().info("HeneriaNPC has been enabled!");
    }

    @Override
    public void onDisable() {
        if (npcManager != null) {
            npcManager.removeAllNPCs();
        }
        getLogger().info("HeneriaNPC has been disabled!");
    }

    public static HeneriaNPC getInstance() {
        return instance;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public HeadDatabaseAPI getHdbApi() {
        return hdbApi;
    }
}
