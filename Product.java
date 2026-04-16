package models;

public class Product {
    private String productId;
    private String name;
    private String category;
    private double price;
    private int    quantity;

    public Product(String productId, String name, String category,
                   double price, int quantity) {
        this.productId = productId;
        this.name      = name;
        this.category  = category;
        this.price     = price;
        this.quantity  = quantity;
    }

    public String getProductId() { return productId; }
    public String getName()      { return name;      }
    public String getCategory()  { return category;  }
    public double getPrice()     { return price;     }
    public int    getQuantity()  { return quantity;  }

    public void setPrice(double price)       { this.price    = price;    }
    public void setQuantity(int quantity)    { this.quantity = quantity; }
    public void setName(String name)         { this.name     = name;     }
    public void setCategory(String category) { this.category = category; }

    public boolean isInStock()          { return quantity > 0;     }
    public void reduceStock(int amount) { this.quantity -= amount; }
    public void addStock(int amount)    { this.quantity += amount; }

    public String toCSV() {
        return productId + "," + name + "," + category + "," + price + "," + quantity;
    }

    public static Product fromCSV(String csv) {
        String[] parts = csv.split(",");
        return new Product(parts[0], parts[1], parts[2],
                           Double.parseDouble(parts[3]),
                           Integer.parseInt(parts[4]));
    }

    @Override
    public String toString() {
        return String.format("[%s] %-20s | %-10s | Rs.%-8.2f | Stock: %d",
                productId, name, category, price, quantity);
    }
}
