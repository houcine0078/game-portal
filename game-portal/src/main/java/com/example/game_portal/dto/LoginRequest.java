package com.example.game_portal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}

/*
 * Without Spring, there is no DTO. Credentials are read directly from the HTTP request
 * inside the Servlet, and validated manually:
 *
 *   String username = request.getParameter("username");
 *   String password = request.getParameter("password");
 *
 *   if (username == null || username.isEmpty()) {
 *       response.setStatus(400);
 *       response.getWriter().write("{\"error\":\"Username is required\"}");
 *       return;
 *   }
 *
 *   // Verify password with SHA-256 (no salt — less secure than BCrypt)
 *   String hashed = Base64.encode(MessageDigest.getInstance("SHA-256").digest(password.getBytes()));
 *   if (!hashed.equals(userFromDB.getPassword())) {
 *       response.setStatus(401);
 *       return;
 *   }
 *   // Then create an HttpSession and store the user in it
 *   HttpSession session = request.getSession(true);
 *   session.setAttribute("username", username);
 *
 * With Spring, @RequestBody deserialises the JSON automatically into this class,
 * @NotBlank validates it, and BCrypt handles password verification securely.
 */
