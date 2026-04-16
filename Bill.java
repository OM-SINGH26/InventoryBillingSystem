package models;

import interfaces.Payable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Bill implements Payable {
    private String          billId;
    private Customer        customer;
    private List<BillItem>  items;
    private double          taxRate;
    private LocalDateTime   dateTime;
    private String          status;

    public Bill(String billId, Customer customer) {
        this.billId   = billId;
        this.customer = customer;
        this.items    = new ArrayList<>();
        this.taxRate  = 0.18;
        this.dateTime = LocalDateTime.now();
        this.status   = "PENDING";
    }

    public void addItem(Product product, int qty) {
        items.add(new BillItem(product, qty));
        product.reduceStock(qty);
    }

    @Override
    public double calculateTotal() {
        double subtotal = items.stream().mapToDouble(BillItem::getSubtotal).sum();
        return subtotal + (subtotal * taxRate);
    }

    public double getSubtotal() {
        return items.stream().mapToDouble(BillItem::getSubtotal).sum();
    }

    public double getTax() {
        return getSubtotal() * taxRate;
    }

    @Override
    public void generateBill() {
        String border = "=".repeat(55);
        System.out.println("\n" + border);
        System.out.println("       INVENTORY & BILLING SYSTEM");
        System.out.println(border);
        System.out.printf("Bill No : %s%n", billId);
        System.out.printf("Date    : %s%n",
                dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        System.out.printf("Customer: %s  |  Phone: %s%n",
                customer.getName(), customer.getPhone());
        System.out.println("-".repeat(55));
        System.out.printf("%-20s %4s %10s %12s%n", "Item", "Qty", "Rate", "Amount");
        System.out.println("-".repeat(55));
        for (BillItem item : items) {
            System.out.printf("%-20s %4d %10.2f %12.2f%n",
                    item.getProduct().getName(), item.getQuantity(),
                    item.getProduct().getPrice(), item.getSubtotal());
        }
        System.out.println("-".repeat(55));
        System.out.printf("%-36s %12.2f%n", "Subtotal:", getSubtotal());
        System.out.printf("%-36s %12.2f%n", "GST (18%):", getTax());
        System.out.println("=".repeat(55));
        System.out.printf("%-36s %12.2f%n", "TOTAL:", calculateTotal());
        System.out.println(border + "\n");
        this.status = "PAID";
        customer.addPurchase(calculateTotal());
    }

    public String toCSV() {
        return billId + "|" + customer.getUserId() + "|" +
               dateTime.toString() + "|" + calculateTotal() + "|" + status;
    }

    public String          getBillId()   { return billId;   }
    public String          getStatus()   { return status;   }
    public Customer        getCustomer() { return customer; }
    public List<BillItem>  getItems()    { return items;    }
}
