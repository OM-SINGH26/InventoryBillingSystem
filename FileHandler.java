package filehandler;

import models.Product;
import models.Bill;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {

    private static final String PRODUCT_FILE = "data/products.csv";
    private static final String BILL_FILE    = "data/bills.txt";
    private static final String LOG_FILE     = "data/system.log";

    static {
        new File("data").mkdirs();
    }

    public static void saveProducts(List<Product> products) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(PRODUCT_FILE))) {
            bw.write("productId,name,category,price,quantity");
            bw.newLine();
            for (Product p : products) {
                bw.write(p.toCSV());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving products: " + e.getMessage());
        }
    }

    public static List<Product> loadProducts() {
        List<Product> products = new ArrayList<>();
        File file = new File(PRODUCT_FILE);
        if (!file.exists()) return products;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    products.add(Product.fromCSV(line));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading products: " + e.getMessage());
        }
        return products;
    }

    public static void saveBill(Bill bill) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BILL_FILE, true))) {
            bw.write(bill.toCSV());
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Error saving bill: " + e.getMessage());
        }
    }

    public static void printAllBills() {
        File file = new File(BILL_FILE);
        if (!file.exists()) { System.out.println("No bills found."); return; }
        System.out.println("\n--- All Bills ---");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 5)
                        System.out.printf("BillID: %-10s Customer: %-10s Total: Rs.%-10s Status: %s%n",
                                parts[0], parts[1], parts[3], parts[4]);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading bills: " + e.getMessage());
        }
        System.out.println("-----------------\n");
    }

    public static void log(String message) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            bw.write("[" + java.time.LocalDateTime.now() + "] " + message);
            bw.newLine();
        } catch (IOException ignored) {}
    }
}
