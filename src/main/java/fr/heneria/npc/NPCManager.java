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
import org.bukkit.util.EulerAngle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class NPCManager {

    private final HeneriaNPC plugin;
    private final NamespacedKey npcKey;
    private final MiniMessage miniMessage;

    private final Map<String, ArmorStand> activeNPCs = new HashMap<>();
    private final Map<String, String> npcPoses = new HashMap<>();

    public NPCManager(HeneriaNPC plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "heneria_npc");
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void reload() {
        spawnAllNPCs();
    }

    public void removeAllNPCs() {
        // Remove entities in world based on tracking
        for (ArmorStand as : activeNPCs.values()) {
            if (as != null && as.isValid()) {
                as.remove();
            }
        }
        activeNPCs.clear();
        npcPoses.clear();

        // Fallback cleanup for any other entities tagged
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
        armorStand.setPersistent(false);
        tagEntity(armorStand, id);

        // Apply Settings
        ConfigurationSection settings = section.getConfigurationSection("settings");
        if (settings != null) {
            armorStand.setArms(settings.getBoolean("show_arms", true));
            armorStand.setSmall(settings.getBoolean("small", false));
            armorStand.setBasePlate(!settings.getBoolean("hide_base_plate", false));
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
            armorStand.setRemoveWhenFarAway(false);
        } else {
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

        // Register
        activeNPCs.put(id, armorStand);
        String pose = section.getString("pose", "default");
        npcPoses.put(id, pose);
    }

    private void spawnHologram(Location baseLocation, String id, ConfigurationSection config) {
        double offsetY = config.getDouble("offset_y", 2.3);
        List<String> lines = config.getStringList("lines");

        if (lines.isEmpty()) return;

        Location holoLoc = baseLocation.clone().add(0, offsetY, 0);
        TextDisplay textDisplay = (TextDisplay) baseLocation.getWorld().spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
        textDisplay.setPersistent(false);
        tagEntity(textDisplay, id + "_holo");

        textDisplay.setBillboard(TextDisplay.Billboard.CENTER);

        Component content = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            content = content.append(miniMessage.deserialize(lines.get(i)));
            if (i < lines.size() - 1) {
                content = content.append(Component.newline());
            }
        }

        textDisplay.text(content);
        textDisplay.setGravity(false);
        textDisplay.setSeeThrough(false);
        textDisplay.setShadowed(false);
    }

    private void applyEquipment(ArmorStand armorStand, ConfigurationSection equipment) {
        if (equipment == null) return;
        EntityEquipment inv = armorStand.getEquipment();
        if (inv == null) return;

        if (equipment.isConfigurationSection("helmet")) {
            ConfigurationSection helmetSection = equipment.getConfigurationSection("helmet");
            String hdbId = helmetSection.getString("hdb_id");
            if (hdbId != null) {
                try {
                    ItemStack head = plugin.getHdbApi().getItemHead(hdbId);
                    if (head != null) inv.setHelmet(head);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load HDB head: " + hdbId, e);
                }
            }
        } else if (equipment.isString("helmet")) {
             inv.setHelmet(parseItem(equipment.getString("helmet")));
        }

        if (equipment.contains("chestplate")) inv.setChestplate(parseItem(equipment.getString("chestplate")));
        if (equipment.contains("leggings")) inv.setLeggings(parseItem(equipment.getString("leggings")));
        if (equipment.contains("boots")) inv.setBoots(parseItem(equipment.getString("boots")));
        if (equipment.contains("main_hand")) inv.setItemInMainHand(parseItem(equipment.getString("main_hand")));
        if (equipment.contains("off_hand")) inv.setItemInOffHand(parseItem(equipment.getString("off_hand")));
    }

    // --- Dynamic Management Methods ---

    public void createNPC(String id, String skinData, Location location) {
        if (activeNPCs.containsKey(id)) return;

        File configFile = new File(plugin.getDataFolder(), "npcs.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        String path = "npcs." + id;
        config.set(path + ".location", locationToString(location));
        config.set(path + ".settings.show_arms", true);
        config.set(path + ".settings.hide_base_plate", true);

        // Try HDB ID first, else create simple helmet if needed, or leave empty
        if (skinData.matches("\\d+")) {
             config.set(path + ".equipment.helmet.hdb_id", skinData);
        } else {
            // Placeholder for player name skin support if needed later, or just raw material
             config.set(path + ".equipment.helmet", "PLAYER_HEAD");
        }

        config.set(path + ".hologram.offset_y", 2.3);
        config.set(path + ".hologram.lines", List.of("<white>NPC " + id));
        config.set(path + ".pose", "default");

        saveConfig(config, configFile);

        spawnNPC(id, config.getConfigurationSection(path));
    }

    public void deleteNPC(String id) {
        ArmorStand as = activeNPCs.remove(id);
        if (as != null) {
            as.remove();
            // Remove hologram too
            for (Entity e : as.getNearbyEntities(1, 3, 1)) {
                if (e instanceof TextDisplay && isNPC(e)) {
                    String holoId = getNPCId(e);
                    if ((id + "_holo").equals(holoId)) {
                        e.remove();
                    }
                }
            }
        }
        npcPoses.remove(id);

        File configFile = new File(plugin.getDataFolder(), "npcs.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("npcs." + id, null);
        saveConfig(config, configFile);
    }

    public void moveNPC(String id, Location location) {
        ArmorStand as = activeNPCs.get(id);
        if (as == null) return;

        as.teleport(location);
        // Teleport hologram
        for (Entity e : as.getNearbyEntities(1, 3, 1)) {
            if (e instanceof TextDisplay && isNPC(e)) {
                String holoId = getNPCId(e);
                if ((id + "_holo").equals(holoId)) {
                    double offsetY = 2.3; // Default guess, or read from config
                    // Best to just respawn it or teleport relative.
                    // Simplest: Respawn NPC fully to ensure config consistency.
                }
            }
        }

        File configFile = new File(plugin.getDataFolder(), "npcs.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("npcs." + id + ".location", locationToString(location));
        saveConfig(config, configFile);

        // Respawn to sync everything perfectly
        deleteNPC(id); // Deletes from map/world but config is already updated above? No wait.
        // Re-read config to respawn
        spawnNPC(id, config.getConfigurationSection("npcs." + id));
    }

    public void updateEquipment(String id, String slot, ItemStack item) {
        ArmorStand as = activeNPCs.get(id);
        if (as == null) return;

        EntityEquipment eq = as.getEquipment();
        if (eq == null) return;

        File configFile = new File(plugin.getDataFolder(), "npcs.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String path = "npcs." + id + ".equipment.";

        switch (slot.toLowerCase()) {
            case "helmet":
                eq.setHelmet(item);
                config.set(path + "helmet", item.getType().name()); // Simple material save for now
                break;
            case "chest":
            case "chestplate":
                eq.setChestplate(item);
                config.set(path + "chestplate", item.getType().name());
                break;
            case "legs":
            case "leggings":
                eq.setLeggings(item);
                config.set(path + "leggings", item.getType().name());
                break;
            case "boots":
                eq.setBoots(item);
                config.set(path + "boots", item.getType().name());
                break;
            case "hand":
            case "main_hand":
                eq.setItemInMainHand(item);
                config.set(path + "main_hand", item.getType().name());
                break;
            case "offhand":
            case "off_hand":
                eq.setItemInOffHand(item);
                config.set(path + "off_hand", item.getType().name());
                break;
        }
        saveConfig(config, configFile);
    }

    public void renameNPC(String id, List<String> lines) {
        // Update config
        File configFile = new File(plugin.getDataFolder(), "npcs.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("npcs." + id + ".hologram.lines", lines);
        saveConfig(config, configFile);

        // Respawn to update hologram
        moveNPC(id, activeNPCs.get(id).getLocation());
    }

    public void setPose(String id, String pose) {
        if (!activeNPCs.containsKey(id)) return;
        npcPoses.put(id, pose);

        File configFile = new File(plugin.getDataFolder(), "npcs.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("npcs." + id + ".pose", pose);
        saveConfig(config, configFile);
    }

    // --- Animation Logic ---

    public void tickAnimations() {
        double time = (System.currentTimeMillis() / 1000.0) * 4; // Animation speed factor

        for (Map.Entry<String, ArmorStand> entry : activeNPCs.entrySet()) {
            String id = entry.getKey();
            ArmorStand as = entry.getValue();
            if (as == null || !as.isValid()) continue;

            String pose = npcPoses.getOrDefault(id, "default");

            switch (pose.toLowerCase()) {
                case "walking":
                case "marche":
                    double angle = Math.sin(time) * 0.5;
                    as.setLeftLegPose(new EulerAngle(angle, 0, 0));
                    as.setRightLegPose(new EulerAngle(-angle, 0, 0));
                    as.setLeftArmPose(new EulerAngle(-angle, 0, 0));
                    as.setRightArmPose(new EulerAngle(angle, 0, 0));
                    break;
                case "pointing":
                case "pointer":
                    as.setRightArmPose(new EulerAngle(-Math.PI / 2, 0, 0));
                    as.setLeftArmPose(EulerAngle.ZERO);
                    as.setLeftLegPose(EulerAngle.ZERO);
                    as.setRightLegPose(EulerAngle.ZERO);
                    break;
                case "look_down":
                case "regard_bas":
                    as.setHeadPose(new EulerAngle(Math.PI / 4, 0, 0));
                    break;
                default:
                    // Reset if needed, or just leave as statue
                    if (!as.getRightArmPose().equals(EulerAngle.ZERO)) {
                         as.setRightArmPose(EulerAngle.ZERO);
                         as.setLeftArmPose(EulerAngle.ZERO);
                         as.setLeftLegPose(EulerAngle.ZERO);
                         as.setRightLegPose(EulerAngle.ZERO);
                         as.setHeadPose(EulerAngle.ZERO);
                    }
                    break;
            }
        }
    }

    private void saveConfig(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save npcs.yml!");
            e.printStackTrace();
        }
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

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + ", " +
               loc.getX() + ", " +
               loc.getY() + ", " +
               loc.getZ() + ", " +
               loc.getYaw() + ", " +
               loc.getPitch();
    }

    public boolean exists(String id) {
        return activeNPCs.containsKey(id);
    }

    public java.util.Set<String> getNPCIds() {
        return activeNPCs.keySet();
    }
}
