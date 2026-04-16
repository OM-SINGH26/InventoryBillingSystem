package models;

import exceptions.DuplicateProductException;
import filehandler.FileHandler;

import java.util.*;

public class InventoryManager {
    private Map<String, Product> products = new LinkedHashMap<>();
    private int productCounter = 1;

    public InventoryManager() {
        List<Product> saved = FileHandler.loadProducts();
        for (Product p : saved) {
            products.put(p.getProductId(), p);
        }
        // Always recalculate counter from all loaded IDs
        updateCounter();
    }

    // Scans all existing IDs and sets counter to max + 1
    private void updateCounter() {
        int maxId = 0;
        for (String id : products.keySet()) {
            try {
                String numOnly = id.replaceAll("[^0-9]", "");
                if (!numOnly.isEmpty()) {
                    int num = Integer.parseInt(numOnly);
                    if (num > maxId) maxId = num;
                }
            } catch (NumberFormatException ignored) {}
        }
        productCounter = maxId + 1;
    }

    // Generates next available ID — skips any that already exist
    public String generateProductId() {
        String id;
        do {
            id = String.format("P%03d", productCounter++);
        } while (products.containsKey(id)); // skip if ID already taken
        return id;
    }

    public void addProduct(Product p) throws DuplicateProductException {
        if (products.containsKey(p.getProductId())) {
            throw new DuplicateProductException(p.getProductId());
        }
        if (p.getPrice() <= 0) throw new IllegalArgumentException("Price must be > 0");
        products.put(p.getProductId(), p);
        FileHandler.log("Product added: " + p.getProductId());
        save();
    }

    public boolean removeProduct(String productId) {
        if (products.remove(productId) != null) {
            save();
            return true;
        }
        return false;
    }

    public Optional<Product> getProduct(String productId) {
        return Optional.ofNullable(products.get(productId));
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public List<Product> searchByName(String keyword) {
        List<Product> result = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.getName().toLowerCase().contains(keyword.toLowerCase())) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Product> getLowStockProducts(int threshold) {
        List<Product> low = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.getQuantity() < threshold) low.add(p);
        }
        return low;
    }

    public void updateStock(String productId, int newQty) {
        Product p = products.get(productId);
        if (p != null) {
            p.setQuantity(newQty);
            save();
        }
    }

    public void save() {
        FileHandler.saveProducts(getAllProducts());
    }

    public void printInventory() {
        System.out.println("\n===============================================================");
        System.out.printf("  %-6s %-20s %-12s %-10s %-8s%n",
                "ID", "Name", "Category", "Price", "Stock");
        System.out.println("---------------------------------------------------------------");
        if (products.isEmpty()) {
            System.out.println("  No products in inventory.");
        } else {
            for (Product p : products.values()) {
                System.out.printf("  %-6s %-20s %-12s Rs.%-7.2f %-8d%n",
                        p.getProductId(), p.getName(), p.getCategory(),
                        p.getPrice(), p.getQuantity());
            }
        }
        System.out.println("===============================================================\n");
    }
}
