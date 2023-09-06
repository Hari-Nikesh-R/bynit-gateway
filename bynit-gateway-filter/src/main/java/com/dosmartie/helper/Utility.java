package com.dosmartie.helper;

import com.dosmartie.common.Roles;

public class Utility {
    public static Roles roleConvertor(String role) {
        return switch (role) {
            case "ADMIN" -> Roles.ADMIN;
            case "CUSTOMER" -> Roles.CUSTOMER;
            case "SUPER_ADMIN" -> Roles.SUPER_ADMIN;
            default -> Roles.MERCHANT;
        };
    }
}