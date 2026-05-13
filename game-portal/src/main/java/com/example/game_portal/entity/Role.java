package com.example.game_portal.entity;

public enum Role {
    USER,
    ADMIN
}

/*
 * Without Spring, roles would be plain String constants checked manually in every Servlet:
 *
 *   public class Roles {
 *       public static final String USER  = "USER";
 *       public static final String ADMIN = "ADMIN";
 *   }
 *
 *   // In every protected Servlet:
 *   String role = (String) session.getAttribute("role");
 *   if (!Roles.ADMIN.equals(role)) {
 *       response.sendError(403, "Access denied");
 *       return;
 *   }
 *
 * With Spring, @PreAuthorize("hasRole('ADMIN')") on a controller method replaces
 * this manual check in every Servlet — no repeated code.
 */
