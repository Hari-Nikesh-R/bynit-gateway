package com.dosmartie.helper;

import com.dosmartie.common.Roles;

import java.util.Optional;

public class Utility {
    public static Roles roleConvertor(Optional<String> role) {
        return role.map(userRole -> {
            return switch (userRole) {
                case "ADMIN" -> Roles.ADMIN;
                case "CUSTOMER" -> Roles.CUSTOMER;
                case "SUPER_ADMIN" -> Roles.SUPER_ADMIN;
                default -> Roles.MERCHANT;
            };
        }).orElseGet(() -> Roles.CUSTOMER);
    }
}