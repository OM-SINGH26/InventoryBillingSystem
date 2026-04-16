package models;

public class Customer extends User {
    private String phone;
    private double totalPurchases;

    public Customer(String userId, String name, String email, String phone) {
        super(userId, name, email);
        this.phone          = phone;
        this.totalPurchases = 0.0;
    }

    public void addPurchase(double amount) { this.totalPurchases += amount; }

    public double getTotalPurchases() { return totalPurchases; }
    public String getPhone()          { return phone;          }

    @Override
    public String toString() {
        return "Customer[ID=" + userId + ", Name=" + name +
               ", Phone=" + phone + ", Total=" + totalPurchases + "]";
    }
}
