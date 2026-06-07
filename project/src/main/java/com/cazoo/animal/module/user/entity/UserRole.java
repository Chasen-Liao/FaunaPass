package com.cazoo.animal.module.user.entity;

public enum UserRole {
    ADMIN, STUDENT;
    public String authority() { return "ROLE_" + name(); }
}
