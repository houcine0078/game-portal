package com.example.game_portal.config;

import com.example.game_portal.filter.JwtAuthFilter;
import com.example.game_portal.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration centrale de Spring Security.
 *
 * =========================================================
 * SANS SPRING SECURITY (approche Servlet/Filtre manuel) :
 * =========================================================
 *   Toute la sécurité serait écrite manuellement :
 *
 *   1. FILTRE D'AUTHENTIFICATION MANUEL (web.xml) :
 *      <filter>
 *        <filter-name>AuthFilter</filter-name>
 *        <filter-class>com.example.filter.AuthFilter</filter-class>
 *      </filter>
 *      <filter-mapping>
 *        <filter-name>AuthFilter</filter-name>
 *        <url-pattern>/api/protected/*</url-pattern>  <!-- routes protégées seulement -->
 *      </filter-mapping>
 *
 *   2. GESTION DE SESSION MANUELLE :
 *      HttpSession session = request.getSession(false);
 *      if (session == null || session.getAttribute("user") == null) {
 *          response.sendError(401, "Non authentifié");
 *          return;
 *      }
 *
 *   3. VÉRIFICATION DES RÔLES MANUELLE dans chaque Servlet :
 *      String role = (String) session.getAttribute("role");
 *      if (!"ADMIN".equals(role)) {
 *          response.sendError(403, "Accès refusé");
 *          return;
 *      }
 *
 *   4. PROTECTION CSRF MANUELLE :
 *      // Génération d'un token CSRF à la création du formulaire
 *      String csrfToken = UUID.randomUUID().toString();
 *      session.setAttribute("csrfToken", csrfToken);
 *      // Vérification à chaque POST :
 *      String sentToken = request.getParameter("_csrf");
 *      if (!csrfToken.equals(sentToken)) { response.sendError(403); return; }
 *
 *   5. ENCODAGE DU MOT DE PASSE MANUEL :
 *      // MD5 ou SHA sans sel → MAUVAISE PRATIQUE
 *      MessageDigest md = MessageDigest.getInstance("SHA-256");
 *      byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
 *      String encoded = Base64.getEncoder().encodeToString(hash);
 *      // Pas de salage → vulnérable aux rainbow tables
 *
 * =========================================================
 * AVEC SPRING SECURITY :
 * =========================================================
 *   Tout est configuré en un seul endroit, de façon déclarative.
 *   La chaîne de filtres interne Spring Security comprend ~15 filtres :
 *     DisableEncodeUrlFilter
 *     WebAsyncManagerIntegrationFilter
 *     SecurityContextHolderFilter
 *     HeaderWriterFilter          ← en-têtes sécurité (X-Frame-Options, etc.)
 *     CorsFilter                  ← CORS (configurable ici)
 *     CsrfFilter                  ← CSRF (désactivé pour API REST/JWT)
 *     [JwtAuthFilter]             ← NOTRE FILTRE, ajouté avant le suivant
 *     UsernamePasswordAuthenticationFilter
 *     ExceptionTranslationFilter  ← 401/403 automatiques
 *     AuthorizationFilter         ← contrôle d'accès par rôle
 *
 * =========================================================
 * AVANTAGES SPRING SECURITY :
 * =========================================================
 *   1. Configuration centralisée (une classe vs dozens de Servlets/Filtres)
 *   2. BCrypt avec sel automatique (bien supérieur à MD5/SHA manuel)
 *   3. CSRF intégré (désactivé ici car API stateless JWT)
 *   4. En-têtes de sécurité HTTP automatiques (X-Content-Type-Options, etc.)
 *   5. @PreAuthorize permet l'autorisation déclarative dans les contrôleurs
 *   6. SessionCreationPolicy.STATELESS → aucune session créée (scalable)
 *   7. AuthenticationManager gère tous les types d'authentification (DAO, LDAP, OAuth2...)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Active @PreAuthorize, @Secured sur les méthodes des contrôleurs
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Encodeur de mot de passe BCrypt.
     *
     * SANS SPRING : MessageDigest.getInstance("SHA-256") sans sel → vulnérable.
     * AVEC SPRING : BCrypt génère automatiquement un sel aléatoire 16 bytes
     *   → chaque hash est unique même pour des mots de passe identiques.
     *   → résistant aux attaques par dictionnaire et rainbow tables.
     *
     * Exemple BCrypt hash : $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
     *   $2a = version BCrypt
     *   $10 = coût (2^10 = 1024 itérations) → lent intentionnellement
     *   N9qo... = sel aléatoire (22 chars)
     *   IjZAg... = hash résultant
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10); // cost factor = 10 (recommandé)
    }

    /**
     * Provider d'authentification DAO.
     * Relie Spring Security à notre UserDetailsService et PasswordEncoder.
     *
     * SANS SPRING :
     *   Vérification manuelle dans la Servlet de login :
     *     User user = userDAO.findByUsername(username);
     *     if (user != null && SHA256(password).equals(user.getPassword())) { ... }
     *
     * AVEC SPRING :
     *   DaoAuthenticationProvider appelle automatiquement :
     *     1. userDetailsService.loadUserByUsername(username)
     *     2. passwordEncoder.matches(rawPassword, storedHash)
     *   Et lève BadCredentialsException si échec → réponse 401 automatique.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager — point d'entrée de l'authentification.
     *
     * SANS SPRING : logique d'authentification dispersée dans chaque Servlet de login.
     * AVEC SPRING : un seul AuthenticationManager, réutilisable partout.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Chaîne de filtres de sécurité principale.
     *
     * =========================================================
     * ROUTES PROTÉGÉES (autorisation par rôle) :
     * =========================================================
     *   SANS SPRING : vérification manuelle dans chaque Servlet ou filtre :
     *     String role = (String) session.getAttribute("role");
     *     if (!"ADMIN".equals(role)) { response.sendError(403); return; }
     *
     *   AVEC SPRING : déclaratif dans .authorizeHttpRequests() ou @PreAuthorize.
     *     .hasRole("ADMIN") → vérifie automatiquement ROLE_ADMIN dans le SecurityContext
     *     .authenticated()  → vérifie qu'un utilisateur est connecté
     *     .permitAll()      → accès libre sans authentification
     *
     * =========================================================
     * CSRF :
     * =========================================================
     *   Désactivé car l'API utilise JWT (stateless, pas de cookies de session).
     *   SANS SPRING : protection CSRF via token caché dans formulaire HTML.
     *   AVEC SPRING JWT : le token Bearer dans l'en-tête Authorization
     *     remplace le cookie JSESSIONID → pas de CSRF possible.
     *
     * =========================================================
     * SESSIONS :
     * =========================================================
     *   SANS SPRING : session HTTP par défaut (JSESSIONID cookie).
     *   AVEC SPRING JWT : STATELESS → aucune session créée côté serveur.
     *     Avantage : scalabilité horizontale (load balancer sans session sticky).
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // --- CORS : autorise les requêtes depuis le frontend Angular ---
            // SANS SPRING : header manuel "Access-Control-Allow-Origin" dans chaque Servlet
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // --- CSRF désactivé pour API REST stateless (JWT) ---
            // SANS SPRING : token CSRF dans chaque formulaire + vérification manuelle
            .csrf(csrf -> csrf.disable())

            // --- Pas de session HTTP serveur (stateless JWT) ---
            // SANS SPRING : HttpSession automatique avec JSESSIONID
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // --- Règles d'autorisation par route ---
            .authorizeHttpRequests(auth -> auth
                // Routes publiques — aucune authentification requise
                // SANS SPRING : routes exclues du filtre d'auth dans web.xml
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/games/**",
                    "/api/scores/leaderboard"
                ).permitAll()

                // Routes réservées aux ADMIN
                // SANS SPRING : if (!"ADMIN".equals(session.getAttribute("role"))) sendError(403)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Toutes les autres routes nécessitent une authentification
                // SANS SPRING : if (session.getAttribute("user") == null) sendError(401)
                .anyRequest().authenticated()
            )

            // --- Provider d'authentification ---
            .authenticationProvider(authenticationProvider())

            // --- Notre filtre JWT s'exécute avant le filtre login form de Spring ---
            // SANS SPRING : ordre des filtres dans web.xml (difficile à gérer)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuration CORS pour autoriser le frontend Angular (localhost:4200).
     *
     * SANS SPRING : en-têtes ajoutés manuellement dans chaque Servlet :
     *   response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
     *   response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
     *   response.setHeader("Access-Control-Allow-Headers", "*");
     *   if ("OPTIONS".equals(request.getMethod())) { response.setStatus(200); return; }
     *
     * AVEC SPRING : configuré une seule fois ici, appliqué à toutes les routes.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE — Configuration de sécurité via web.xml + Filtres manuels
 * =====================================================================
 *
 * En J2EE, toute la sécurité est configurée dans web.xml + des classes Filter.
 * Il n'existe pas de SecurityFilterChain ni de BCryptPasswordEncoder intégré.
 *
 * ── 1. web.xml — Enregistrement des filtres et Servlets ──────────────
 *
 *   <?xml version="1.0" encoding="UTF-8"?>
 *   <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.0">
 *
 *     <!-- Filtre CORS — équivalent de corsConfigurationSource() -->
 *     <filter>
 *       <filter-name>CorsFilter</filter-name>
 *       <filter-class>com.example.filter.CorsFilterJ2EE</filter-class>
 *     </filter>
 *     <filter-mapping>
 *       <filter-name>CorsFilter</filter-name>
 *       <url-pattern>/*</url-pattern>
 *     </filter-mapping>
 *
 *     <!-- Filtre JWT — équivalent du JwtAuthFilter Spring -->
 *     <filter>
 *       <filter-name>JwtFilter</filter-name>
 *       <filter-class>com.example.filter.JwtAuthFilterJ2EE</filter-class>
 *     </filter>
 *     <filter-mapping>
 *       <filter-name>JwtFilter</filter-name>
 *       <url-pattern>/api/*</url-pattern>
 *     </filter-mapping>
 *
 *     <!-- Filtre de rôle ADMIN — équivalent de .hasRole("ADMIN") -->
 *     <filter>
 *       <filter-name>AdminFilter</filter-name>
 *       <filter-class>com.example.filter.AdminRoleFilter</filter-class>
 *     </filter>
 *     <filter-mapping>
 *       <filter-name>AdminFilter</filter-name>
 *       <url-pattern>/api/admin/*</url-pattern>
 *       <url-pattern>/api/games/import</url-pattern>
 *     </filter-mapping>
 *
 *     <!-- Servlets -->
 *     <servlet>
 *       <servlet-name>RegisterServlet</servlet-name>
 *       <servlet-class>com.example.servlet.RegisterServlet</servlet-class>
 *     </servlet>
 *     <servlet-mapping>
 *       <servlet-name>RegisterServlet</servlet-name>
 *       <url-pattern>/api/auth/register</url-pattern>
 *     </servlet-mapping>
 *
 *     <servlet>
 *       <servlet-name>LoginServlet</servlet-name>
 *       <servlet-class>com.example.servlet.LoginServlet</servlet-class>
 *     </servlet>
 *     <servlet-mapping>
 *       <servlet-name>LoginServlet</servlet-name>
 *       <url-pattern>/api/auth/login</url-pattern>
 *     </servlet-mapping>
 *
 *     <!-- Session timeout globale — équivalent de setMaxInactiveInterval() -->
 *     <session-config>
 *       <session-timeout>30</session-timeout>  <!-- minutes -->
 *     </session-config>
 *
 *   </web-app>
 *
 * ── 2. CorsFilterJ2EE — équivalent de corsConfigurationSource() ──────
 *
 *   public class CorsFilterJ2EE implements Filter {
 *       @Override
 *       public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
 *               throws IOException, ServletException {
 *           HttpServletResponse response = (HttpServletResponse) res;
 *           HttpServletRequest  request  = (HttpServletRequest)  req;
 *
 *           // Spring définit ces en-têtes automatiquement avec CorsConfiguration
 *           response.setHeader("Access-Control-Allow-Origin",  "http://localhost:4200");
 *           response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
 *           response.setHeader("Access-Control-Allow-Headers", "*");
 *           response.setHeader("Access-Control-Allow-Credentials", "true");
 *
 *           // Gestion des requêtes préliminaires OPTIONS (preflight)
 *           if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
 *               response.setStatus(HttpServletResponse.SC_OK);
 *               return; // ne pas continuer la chaîne pour OPTIONS
 *           }
 *           chain.doFilter(req, res);
 *       }
 *   }
 *
 * ── 3. AdminRoleFilter — équivalent de .hasRole("ADMIN") ─────────────
 *
 *   public class AdminRoleFilter implements Filter {
 *       @Override
 *       public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
 *               throws IOException, ServletException {
 *           HttpServletRequest  httpReq = (HttpServletRequest)  req;
 *           HttpServletResponse httpRes = (HttpServletResponse) res;
 *
 *           // Lecture du rôle propagé par JwtAuthFilterJ2EE via request.setAttribute()
 *           // Spring Security : le rôle est dans SecurityContextHolder (threadLocal)
 *           String role = (String) httpReq.getAttribute("currentRole");
 *           if (!"ROLE_ADMIN".equals(role)) {
 *               httpRes.setStatus(HttpServletResponse.SC_FORBIDDEN);
 *               httpRes.setContentType("application/json");
 *               httpRes.getWriter().write("{\"error\":\"Réservé aux administrateurs\"}");
 *               return;
 *           }
 *           chain.doFilter(req, res);
 *       }
 *   }
 *
 * ── 4. Encodage du mot de passe sans Spring (SHA-256 sans sel) ────────
 *
 *   // Dans RegisterServlet — Spring utilise BCryptPasswordEncoder (avec sel)
 *   MessageDigest md = MessageDigest.getInstance("SHA-256");
 *   String hashedPassword = Base64.getEncoder().encodeToString(
 *       md.digest(rawPassword.getBytes("UTF-8")));
 *   // PROBLÈME : même mot de passe → même hash → vulnérable aux rainbow tables
 *   // BCrypt Spring : hash différent à chaque appel grâce au sel aléatoire 128 bits
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring SecurityConfig    : 1 classe Java, 60 lignes → toute la sécurité configurée
 *   J2EE web.xml + filtres  : web.xml + 3 classes Filter + code dans chaque Servlet
 *                             → plusieurs centaines de lignes, risque d'oubli
 *   BCrypt Spring            : sel automatique, résistant aux attaques
 *   SHA-256 J2EE             : sans sel, vulnérable aux rainbow tables et brute force
 *   Ordre filtres Spring     : addFilterBefore() précis et typé
 *   Ordre filtres J2EE       : <filter-mapping> dans web.xml, ordre moins prévisible
 * =====================================================================
 */

