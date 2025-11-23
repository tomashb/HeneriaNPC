package fr.heneria.npc;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class NPCManager {

    private final HeneriaNPC plugin;
    private final NamespacedKey npcKey;
    private final MiniMessage miniMessage;

    public NPCManager(HeneriaNPC plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "heneria_npc");
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void reload() {
        plugin.reloadConfig();
        spawnAllNPCs();
    }

    public void removeAllNPCs() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }

    public void spawnAllNPCs() {
        removeAllNPCs(); // Clean up first

        File configFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!configFile.exists()) {
            plugin.saveResource("npcs.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection npcsSection = config.getConfigurationSection("npcs");

        if (npcsSection == null) return;

        for (String key : npcsSection.getKeys(false)) {
            spawnNPC(key, npcsSection.getConfigurationSection(key));
        }
    }

    private void spawnNPC(String id, ConfigurationSection section) {
        if (section == null) return;

        String locationStr = section.getString("location");
        Location location = parseLocation(locationStr);
        if (location == null) {
            plugin.getLogger().warning("Invalid location for NPC " + id);
            return;
        }

        // Spawn ArmorStand
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setPersistent(false); // Ensure they don't persist on restart/unload to avoid duplication
        tagEntity(armorStand, id);

        // Apply Settings
        ConfigurationSection settings = section.getConfigurationSection("settings");
        if (settings != null) {
            armorStand.setArms(settings.getBoolean("show_arms", true));
            armorStand.setSmall(settings.getBoolean("small", false));
            armorStand.setBasePlate(!settings.getBoolean("hide_base_plate", false));
            // armorStand.setGravity(false); // Managed by logic usually, but user asked for configurable.
            // Prompt says: setGravity(false) (Pour qu'il flotte si besoin). Let's default to false (no gravity) for static NPCs
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
            armorStand.setRemoveWhenFarAway(false);
        } else {
             // Defaults
            armorStand.setArms(true);
            armorStand.setBasePlate(false);
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
             armorStand.setRemoveWhenFarAway(false);
        }

        // Apply Equipment
        applyEquipment(armorStand, section.getConfigurationSection("equipment"));

        // Hologram (TextDisplay)
        ConfigurationSection holoSection = section.getConfigurationSection("hologram");
        if (holoSection != null) {
            spawnHologram(location, id, holoSection);
        }
    }

    private void applyEquipment(ArmorStand armorStand, ConfigurationSection equipment) {
        if (equipment == null) return;

        EntityEquipment inv = armorStand.getEquipment();
        if (inv == null) return;

        // Helmet (Special handling for HDB)
        if (equipment.isConfigurationSection("helmet")) {
            ConfigurationSection helmetSection = equipment.getConfigurationSection("helmet");
            String hdbId = helmetSection.getString("hdb_id");
            if (hdbId != null) {
                try {
                    ItemStack head = plugin.getHdbApi().getItemHead(hdbId);
                    if (head != null) {
                        inv.setHelmet(head);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load HDB head: " + hdbId, e);
                }
            }
        } else if (equipment.isString("helmet")) {
             inv.setHelmet(parseItem(equipment.getString("helmet")));
        }

        inv.setChestplate(parseItem(equipment.getString("chestplate")));
        inv.setLeggings(parseItem(equipment.getString("leggings")));
        inv.setBoots(parseItem(equipment.getString("boots")));
        inv.setItemInMainHand(parseItem(equipment.getString("main_hand")));
        inv.setItemInOffHand(parseItem(equipment.getString("off_hand")));
    }

    private void spawnHologram(Location baseLocation, String id, ConfigurationSection config) {
        double offsetY = config.getDouble("offset_y", 2.3);
        List<String> lines = config.getStringList("lines");

        if (lines.isEmpty()) return;

        // We use a single TextDisplay. Multi-line is supported natively.
        Location holoLoc = baseLocation.clone().add(0, offsetY, 0);
        TextDisplay textDisplay = (TextDisplay) baseLocation.getWorld().spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
        textDisplay.setPersistent(false); // Ensure they don't persist on restart/unload
        tagEntity(textDisplay, id + "_holo"); // Tag it too so it gets removed

        textDisplay.setBillboard(TextDisplay.Billboard.CENTER); // Always face player

        // Combine lines
        Component content = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            content = content.append(miniMessage.deserialize(lines.get(i)));
            if (i < lines.size() - 1) {
                content = content.append(Component.newline());
            }
        }

        textDisplay.text(content);
        textDisplay.setGravity(false); // Floats
        textDisplay.setSeeThrough(false);
        textDisplay.setShadowed(false); // Make cleaner
        // textDisplay.setBackgroundColor(Color.fromARGB(0,0,0,0)); // Transparent background default usually
    }

    private ItemStack parseItem(String materialName) {
        if (materialName == null) return null;
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material != null) {
            return new ItemStack(material);
        }
        return null;
    }

    private void tagEntity(Entity entity, String id) {
        entity.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, id);
    }

    public boolean isNPC(Entity entity) {
        return entity.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING);
    }

    public String getNPCId(Entity entity) {
        return entity.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
    }

    private Location parseLocation(String str) {
        if (str == null) return null;
        String[] parts = str.split(",");
        if (parts.length < 4) return null;

        World world = Bukkit.getWorld(parts[0].trim());
        if (world == null) return null;

        try {
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            float yaw = 0;
            float pitch = 0;
            if (parts.length > 4) yaw = Float.parseFloat(parts[4].trim());
            if (parts.length > 5) pitch = Float.parseFloat(parts[5].trim());

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
