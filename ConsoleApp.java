package console;

import exceptions.DuplicateProductException;
import filehandler.FileHandler;
import models.*;
import threads.AutoSaveThread;
import threads.StockMonitorThread;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;

public class ConsoleApp {

    static InventoryManager inventory   = new InventoryManager();
    static Admin            admin       = new Admin("A001", "Store Admin", "admin@store.com", "admin123");
    static int              billCounter = 1;

    // Use BufferedReader instead of Scanner to avoid NoSuchElementException
    static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    static String readLine() {
        try {
            String line = br.readLine();
            return (line == null) ? "" : line.trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static void main(String[] args) {

        // Multithreading - Method A: extends Thread
        AutoSaveThread autoSave = new AutoSaveThread(inventory, 30000);
        autoSave.start();

        // Multithreading - Method B: implements Runnable
        StockMonitorThread monitor = new StockMonitorThread(inventory, 3);
        Thread monitorThread = new Thread(monitor, "StockMonitor-Thread");
        monitorThread.setDaemon(true);
        monitorThread.start();

        if (inventory.getAllProducts().isEmpty()) loadSampleData();

        System.out.println("\n==========================================");
        System.out.println("    INVENTORY & BILLING SYSTEM v1.0");
        System.out.println("==========================================");
        System.out.println("  Admin: " + admin.getName());
        System.out.println("==========================================");

        boolean exit = false;
        while (!exit) {
            printMainMenu();
            String choice = readLine();
            switch (choice) {
                case "1": manageInventory();          break;
                case "2": createBill();               break;
                case "3": FileHandler.printAllBills(); break;
                case "4": inventory.printInventory(); break;
                case "5": showLowStock();             break;
                case "0":
                    exit = true;
                    autoSave.stopSaving();
                    break;
                default:
                    System.out.println("  Invalid option. Try again.\n");
            }
        }

        inventory.save();
        System.out.println("\n  Data saved. Goodbye!\n");
    }

    static void printMainMenu() {
        System.out.println("\n------------------------------------------");
        System.out.println("  1. Manage Inventory");
        System.out.println("  2. Create Bill");
        System.out.println("  3. View All Bills");
        System.out.println("  4. View Inventory");
        System.out.println("  5. Low Stock Alert");
        System.out.println("  0. Exit");
        System.out.println("------------------------------------------");
        System.out.print("  Enter choice: ");
    }

    // ── INVENTORY ─────────────────────────────────────────────────────
    static void manageInventory() {
        System.out.println("\n--- Inventory Menu ---");
        System.out.println("  a. Add Product");
        System.out.println("  b. Remove Product");
        System.out.println("  c. Update Stock");
        System.out.println("  d. Search Product");
        System.out.print("  Choice: ");
        String c = readLine();
        switch (c) {
            case "a": addProduct();    break;
            case "b": removeProduct(); break;
            case "c": updateStock();   break;
            case "d": searchProduct(); break;
            default:  System.out.println("  Invalid choice.");
        }
    }

    static void addProduct() {
        try {
            System.out.print("  Product Name : "); String name  = readLine();
            System.out.print("  Category     : "); String cat   = readLine();
            System.out.print("  Price (Rs.)  : "); double price = Double.parseDouble(readLine());
            System.out.print("  Quantity     : "); int    qty   = Integer.parseInt(readLine());

            if (name.isEmpty()) { System.out.println("  Name cannot be empty."); return; }
            if (price <= 0)     { System.out.println("  Price must be greater than 0."); return; }

            String id = inventory.generateProductId();
            Product p = new Product(id, name, cat, price, qty);
            inventory.addProduct(p);
            System.out.println("  Product added successfully! ID: " + id);
            FileHandler.log("Added product: " + name + " ID: " + id);

        } catch (DuplicateProductException e) {
            System.out.println("  Error: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("  Error: Please enter a valid number.");
        } catch (IllegalArgumentException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    static void removeProduct() {
        System.out.print("  Enter Product ID to remove: ");
        String id = readLine();
        if (inventory.removeProduct(id))
             System.out.println("  Product removed successfully.");
        else System.out.println("  Product not found with ID: " + id);
    }

    static void updateStock() {
        System.out.print("  Enter Product ID : "); String id = readLine();
        System.out.print("  Enter New Qty    : ");
        try {
            int qty = Integer.parseInt(readLine());
            inventory.updateStock(id, qty);
            System.out.println("  Stock updated successfully.");
        } catch (NumberFormatException e) {
            System.out.println("  Error: Invalid quantity.");
        }
    }

    static void searchProduct() {
        System.out.print("  Enter search keyword: ");
        String kw = readLine();
        var results = inventory.searchByName(kw);
        if (results.isEmpty())
            System.out.println("  No products found for: " + kw);
        else {
            System.out.println("\n  Search Results:");
            results.forEach(p -> System.out.println("  " + p));
        }
    }

    // ── BILLING ───────────────────────────────────────────────────────
    static void createBill() {
        System.out.println("\n--- New Bill ---");
        System.out.print("  Customer Name  : "); String cname  = readLine();
        System.out.print("  Customer Phone : "); String cphone = readLine();

        if (cname.isEmpty()) { System.out.println("  Customer name required."); return; }

        String custId = "C" + System.currentTimeMillis() % 10000;
        Customer cust = new Customer(custId, cname, "n/a", cphone);
        cust.register();

        String billId = String.format("B%04d", billCounter++);
        Bill   bill   = new Bill(billId, cust);

        inventory.printInventory();
        System.out.println("  Enter Product ID and quantity. Type 'done' when finished.");

        while (true) {
            System.out.print("  Product ID (or 'done'): ");
            String pid = readLine();
            if (pid.equalsIgnoreCase("done")) break;
            if (pid.isEmpty()) continue;

            Optional<Product> opt = inventory.getProduct(pid.toUpperCase());
            if (opt.isEmpty()) {
                System.out.println("  Product not found: " + pid);
                continue;
            }

            Product p = opt.get();
            System.out.print("  Quantity: ");
            try {
                int qty = Integer.parseInt(readLine());
                if (qty <= 0) {
                    System.out.println("  Quantity must be greater than 0.");
                } else if (qty > p.getQuantity()) {
                    System.out.println("  Only " + p.getQuantity() + " units in stock!");
                } else {
                    bill.addItem(p, qty);
                    System.out.println("  Added: " + p.getName() + " x" + qty +
                                       " = Rs." + (p.getPrice() * qty));
                }
            } catch (NumberFormatException e) {
                System.out.println("  Invalid quantity. Please enter a number.");
            }
        }

        if (bill.getItems().isEmpty()) {
            System.out.println("  No items added. Bill cancelled.");
            return;
        }

        bill.generateBill();
        FileHandler.saveBill(bill);
        inventory.save();
        System.out.println("  Bill saved successfully!");
    }

    static void showLowStock() {
        var low = inventory.getLowStockProducts(5);
        if (low.isEmpty()) {
            System.out.println("\n  All products are sufficiently stocked.\n");
        } else {
            System.out.println("\n  LOW STOCK PRODUCTS (less than 5 units):");
            System.out.println("  ------------------------------------------");
            low.forEach(p -> System.out.printf("  %-6s %-20s Qty: %d%n",
                    p.getProductId(), p.getName(), p.getQuantity()));
            System.out.println();
        }
    }

    // ── SAMPLE DATA ───────────────────────────────────────────────────
    static void loadSampleData() {
        try {
            inventory.addProduct(new Product("P001", "Basmati Rice (1kg)",  "Grocery",    85.00, 50));
            inventory.addProduct(new Product("P002", "Wheat Flour (5kg)",   "Grocery",   220.00, 30));
            inventory.addProduct(new Product("P003", "Sunflower Oil (1L)",  "Grocery",   150.00, 20));
            inventory.addProduct(new Product("P004", "Sugar (1kg)",         "Grocery",    50.00, 40));
            inventory.addProduct(new Product("P005", "Notebook (200pg)",    "Stationery", 60.00, 15));
            inventory.addProduct(new Product("P006", "Ball Pen (Pack/10)",  "Stationery", 45.00,  2));
            inventory.addProduct(new Product("P007", "Shampoo (200ml)",     "Personal",  120.00, 10));
            inventory.addProduct(new Product("P008", "Soap Bar (Pack/4)",   "Personal",   80.00,  8));
            System.out.println("  Sample data loaded successfully.");
        } catch (DuplicateProductException ignored) {}
    }
}
