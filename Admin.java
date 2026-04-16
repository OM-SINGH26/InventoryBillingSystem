package models;

public class Admin extends User {
    private String adminCode;
    private int    accessLevel;

    public Admin(String userId, String name, String email, String adminCode) {
        super(userId, name, email);
        this.adminCode   = adminCode;
        this.accessLevel = 5;
        this.registered  = true;
    }

    public boolean authenticate(String code) {
        return this.adminCode.equals(code);
    }

    public int getAccessLevel() { return accessLevel; }

    @Override
    public String toString() {
        return "Admin[ID=" + userId + ", Name=" + name + ", AccessLevel=" + accessLevel + "]";
    }
}
