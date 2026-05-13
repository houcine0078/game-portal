package com.example.game_portal.service;

import com.example.game_portal.dto.AuthResponse;
import com.example.game_portal.dto.LoginRequest;
import com.example.game_portal.dto.RegisterRequest;
import com.example.game_portal.entity.Role;
import com.example.game_portal.entity.User;
import com.example.game_portal.repository.UserRepository;
import com.example.game_portal.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà enregistré");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message("Inscription réussie")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message("Connexion réussie")
                .build();
    }
}

/*
 * Without Spring, the register logic would live directly inside a Servlet.
 * Here is what the doPost() method would look like for registration:
 *
 *   protected void doPost(HttpServletRequest request, HttpServletResponse response)
 *           throws IOException {
 *       // Read JSON body manually — Spring does this with @RequestBody
 *       String username = json.getString("username");
 *       String email    = json.getString("email");
 *       String password = json.getString("password");
 *
 *       // Validate manually — Spring does this with @Valid + @NotBlank
 *       if (username.length() < 3) { response.setStatus(400); return; }
 *
 *       // Hash password with SHA-256, no salt (Spring uses BCrypt with automatic salt)
 *       String hashedPassword = Base64.encode(MessageDigest.getInstance("SHA-256").digest(password.getBytes()));
 *
 *       // Insert with JDBC — Spring uses userRepository.save(user)
 *       PreparedStatement ps = conn.prepareStatement("INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)");
 *       ps.setString(1, username); ps.setString(2, email);
 *       ps.setString(3, hashedPassword); ps.setString(4, "USER");
 *       ps.executeUpdate();
 *
 *       // Build JSON response manually — Spring returns AuthResponse and Jackson serialises it
 *       response.getWriter().write("{\"message\":\"Registered\"}");
 *   }
 *
 * Spring's @Transactional also means the entire register() method is wrapped in a database
 * transaction — if anything fails, all changes are automatically rolled back.
 */
