package io.github.kowx712.mmuautoqr.models;

import androidx.annotation.NonNull;

public class User {
    private String id;
    private String name;
    private String userId;
    private String password;
    private boolean isActive;

    public User() {
    }

    public User(String id, String name, String userId, String password) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.password = password;
        this.isActive = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public boolean isActive() {
        return isActive;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @NonNull
    @Override
    public String toString() {
        return name + " (" + userId + ")";
    }
}