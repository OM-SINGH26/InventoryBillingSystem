package threads;

import models.InventoryManager;
import models.Product;
import filehandler.FileHandler;
import java.util.List;

// Multithreading - Method B: implements Runnable
public class StockMonitorThread implements Runnable {
    private InventoryManager inventory;
    private int              threshold;
    private volatile boolean running;

    public StockMonitorThread(InventoryManager inventory, int threshold) {
        this.inventory  = inventory;
        this.threshold  = threshold;
        this.running    = true;
    }

    @Override
    public void run() {
        System.out.println("[StockMonitor] Thread started - checking every 30 seconds");
        while (running) {
            try {
                Thread.sleep(30000); // check every 30 seconds
                checkLowStock();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
        System.out.println("[StockMonitor] Thread stopped.");
    }

    private void checkLowStock() {
        List<Product> lowStock = inventory.getLowStockProducts(threshold);
        if (!lowStock.isEmpty()) {
            System.out.println("\n[StockMonitor] LOW STOCK ALERT:");
            for (Product p : lowStock) {
                System.out.println("  -> " + p.getName() + " | Only " + p.getQuantity() + " left!");
                FileHandler.log("LOW STOCK: " + p.getName() + " qty=" + p.getQuantity());
            }
        }
    }

    public void stop() { this.running = false; }
}
