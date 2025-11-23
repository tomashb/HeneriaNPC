package fr.heneria.npc;

import org.bukkit.scheduler.BukkitRunnable;

public class NPCAnimator extends BukkitRunnable {

    private final NPCManager npcManager;

    public NPCAnimator(NPCManager npcManager) {
        this.npcManager = npcManager;
    }

    @Override
    public void run() {
        npcManager.tickAnimations();
    }
}
