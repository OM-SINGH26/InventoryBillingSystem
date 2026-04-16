package models;

public class BillItem {
    private Product product;
    private int     quantity;
    private double  subtotal;

    public BillItem(Product product, int quantity) {
        this.product  = product;
        this.quantity = quantity;
        this.subtotal = product.getPrice() * quantity;
    }

    public Product getProduct()  { return product;  }
    public int     getQuantity() { return quantity; }
    public double  getSubtotal() { return subtotal; }

    @Override
    public String toString() {
        return String.format("%-20s x%-3d @ Rs.%-8.2f = Rs.%.2f",
                product.getName(), quantity, product.getPrice(), subtotal);
    }
}
