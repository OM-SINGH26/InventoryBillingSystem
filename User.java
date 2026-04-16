package models;

import interfaces.Registrable;

public class User implements Registrable {
    protected String userId;
    protected String name;
    protected String email;
    protected boolean registered;

    public User(String userId, String name, String email) {
        this.userId     = userId;
        this.name       = name;
        this.email      = email;
        this.registered = false;
    }

    @Override
    public void register() {
        this.registered = true;
        System.out.println("User registered: " + name);
    }

    @Override
    public boolean isRegistered() { return registered; }

    @Override
    public String getRegistrationId() { return userId; }

    public String getUserId() { return userId; }
    public String getName()   { return name;   }
    public String getEmail()  { return email;  }

    public void setName(String name)   { this.name  = name;  }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "User[ID=" + userId + ", Name=" + name + ", Email=" + email + "]";
    }
}
