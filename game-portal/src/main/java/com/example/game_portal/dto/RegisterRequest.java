package com.example.game_portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}

/*
 * Without Spring, there is no DTO with automatic validation.
 * All validation is written manually inside the Servlet:
 *
 *   // Read the JSON body manually
 *   StringBuilder body = new StringBuilder();
 *   String line;
 *   while ((line = request.getReader().readLine()) != null) body.append(line);
 *   JSONObject json = new JSONObject(body.toString());
 *   String username = json.optString("username", "").trim();
 *   String email    = json.optString("email",    "").trim();
 *   String password = json.optString("password", "").trim();
 *
 *   // Validate manually — repeated for every field, in every Servlet
 *   if (username.length() < 3) {
 *       response.setStatus(400);
 *       response.getWriter().write("{\"error\":\"Username must be at least 3 characters\"}");
 *       return;
 *   }
 *   if (!email.contains("@")) { ... }
 *   if (password.length() < 6) { ... }
 *
 * With Spring, @RequestBody reads the JSON automatically, @Valid runs all the
 * annotation checks, and any failure returns a 400 with the error messages — no if/else needed.
 */
