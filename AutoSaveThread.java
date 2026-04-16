package threads;

import models.InventoryManager;

// Multithreading - Method A: extends Thread
public class AutoSaveThread extends Thread {
    private InventoryManager inventory;
    private volatile boolean  running;
    private int               intervalMs;

    public AutoSaveThread(InventoryManager inventory, int intervalMs) {
        super("AutoSave-Thread");
        this.inventory   = inventory;
        this.running     = true;
        this.intervalMs  = intervalMs;
        this.setDaemon(true);
    }

    @Override
    public void run() {
        System.out.println("[AutoSave] Thread started - saving every " +
                           (intervalMs / 1000) + " seconds");
        while (running) {
            try {
                Thread.sleep(intervalMs);
                inventory.save();
                System.out.println("[AutoSave] Inventory auto-saved.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    public void stopSaving() {
        this.running = false;
        this.interrupt();
    }
}
