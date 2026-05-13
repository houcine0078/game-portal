package com.example.game_portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String username;
    private String email;
    private String role;
    private String message;
}

/*
 * Without Spring, there is no DTO that gets serialised automatically.
 * The JSON response is built by hand inside the Servlet:
 *
 *   response.setContentType("application/json");
 *   response.setCharacterEncoding("UTF-8");
 *   response.setStatus(200);
 *   response.getWriter().write(
 *       "{\"token\":\""    + token              + "\"," +
 *       "\"username\":\"" + user.getUsername() + "\"," +
 *       "\"role\":\""     + user.getRole()     + "\"," +
 *       "\"message\":\"Login successful\"}"
 *   );
 *
 * With Spring, the controller just returns an AuthResponse object,
 * and Jackson serialises it to JSON automatically with the correct Content-Type header.
 */
