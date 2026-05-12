package com.example.game_portal.filter;

import com.example.game_portal.security.UserDetailsServiceImpl;
import com.example.game_portal.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT intercalé dans la chaîne de filtres Spring Security.
 * Extrait le token Bearer, le valide, et charge l'utilisateur dans le SecurityContext.
 *
 * =========================================================
 * SANS SPRING (filtre Servlet manuel) :
 * =========================================================
 *   On implémente javax.servlet.Filter et on le déclare dans web.xml :
 *
 *     public class ManualAuthFilter implements Filter {
 *
 *         @Override
 *         public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
 *                 throws IOException, ServletException {
 *             HttpServletRequest  httpReq = (HttpServletRequest) req;
 *             HttpServletResponse httpRes = (HttpServletResponse) res;
 *
 *             // Option 1 : Vérification de session (stateful)
 *             HttpSession session = httpReq.getSession(false);
 *             if (session == null || session.getAttribute("user") == null) {
 *                 httpRes.sendError(401, "Non authentifié");
 *                 return; // stop la chaîne
 *             }
 *             String role = (String) session.getAttribute("role");
 *             httpReq.setAttribute("currentRole", role); // passage à la Servlet
 *
 *             // Option 2 : Vérification JWT manuelle (stateless)
 *             String header = httpReq.getHeader("Authorization");
 *             if (header == null || !header.startsWith("Bearer ")) {
 *                 httpRes.sendError(401, "Token manquant");
 *                 return;
 *             }
 *             String token = header.substring(7);
 *             try {
 *                 Claims claims = Jwts.parser()
 *                     .setSigningKey(SECRET_KEY).build()
 *                     .parseClaimsJws(token).getBody();
 *                 httpReq.setAttribute("username", claims.getSubject());
 *                 httpReq.setAttribute("role", claims.get("role"));
 *             } catch (JwtException e) {
 *                 httpRes.sendError(401, "Token invalide");
 *                 return;
 *             }
 *             chain.doFilter(req, res);
 *         }
 *     }
 *
 *   Enregistrement dans web.xml :
 *     <filter>
 *       <filter-name>ManualAuthFilter</filter-name>
 *       <filter-class>com.example.filter.ManualAuthFilter</filter-class>
 *     </filter>
 *     <filter-mapping>
 *       <filter-name>ManualAuthFilter</filter-name>
 *       <url-pattern>/api/protected/*</url-pattern>
 *     </filter-mapping>
 *
 * =========================================================
 * AVEC SPRING SECURITY :
 * =========================================================
 *   OncePerRequestFilter garantit une seule exécution par requête HTTP
 *   (même en cas de forward/include Servlet).
 *
 *   SecurityContextHolder.getContext().setAuthentication(auth) :
 *     → Spring Security reconnaît l'utilisateur pour TOUTE la requête
 *     → @PreAuthorize, .hasRole(), Principal, etc. fonctionnent automatiquement
 *
 * =========================================================
 * COMPARAISON CHAÎNE DE FILTRES :
 * =========================================================
 *   SANS SPRING :
 *     Requête → Filter1 → Filter2 → Servlet (chaîne linéaire, web.xml)
 *     Ordre des filtres difficile à maîtriser, pas de gestion d'exception centralisée.
 *
 *   AVEC SPRING SECURITY :
 *     Requête → DelegatingFilterProxy → SecurityFilterChain :
 *       [ChannelProcessingFilter]
 *       [SecurityContextPersistenceFilter]
 *       [JwtAuthFilter]  ← notre filtre, inséré AVANT UsernamePasswordAuthenticationFilter
 *       [UsernamePasswordAuthenticationFilter]
 *       [ExceptionTranslationFilter]  ← gère 401/403 automatiquement
 *       [FilterSecurityInterceptor]   ← contrôle d'accès basé sur les rôles
 *     La position et l'ordre sont gérés par addFilterBefore() dans SecurityConfig.
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. OncePerRequestFilter évite les doubles exécutions (forward/include)
 *   2. Le SecurityContext est threadLocal → sécurisé en environnement concurrent
 *   3. Pas de web.xml : enregistrement programmatique dans SecurityConfig
 *   4. @Component + injection automatique des dépendances (JwtUtil, UserDetailsService)
 *   5. Gestion des erreurs 401/403 centralisée par Spring Security
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Intercepte chaque requête HTTP pour vérifier le token JWT.
     *
     * SANS SPRING :
     *   Logique identique mais écrite dans Filter.doFilter() avec gestion
     *   manuelle de session ou de token, et without SecurityContext.
     *   Les attributs de sécurité doivent être propagés manuellement
     *   via request.setAttribute() vers les Servlets aval.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1. Extraction du token depuis l'en-tête Authorization
        //    SANS SPRING : même code, mais le résultat est stocké en session
        //    ou passé via request.setAttribute()
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 2. Extraction du username depuis le token
        //    SANS SPRING : session.getAttribute("username")
        String username = jwtUtil.extractUsername(token);

        // 3. Authentification si pas encore en cours pour cette requête
        //    SecurityContextHolder.getContext() est threadLocal → sécurisé
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 4. Chargement de l'utilisateur (avec ses rôles) depuis la base de données
            //    SANS SPRING : query SQL manuelle dans chaque filtre
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 5. Validation du token (signature + expiration + username)
            //    SANS SPRING : vérification manuelle de l'expiration de session :
            //      if (session.getLastAccessedTime() + timeout < System.currentTimeMillis())
            if (jwtUtil.validateToken(token, userDetails)) {

                // 6. Création de l'objet d'authentification avec les rôles réels
                //    SANS SPRING : stockage en session de l'utilisateur et du rôle
                //    AVEC SPRING : le SecurityContext est accessible partout dans l'application
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities() // rôles chargés depuis User.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Met l'authentification dans le contexte de sécurité threadLocal
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 7. Passage au filtre suivant dans la chaîne Spring Security
        //    SANS SPRING : chain.doFilter() ou appel Servlet suivant
        chain.doFilter(request, response);
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE / SERVLET — JwtAuthFilter manuel (javax.servlet.Filter)
 * =====================================================================
 *
 * En J2EE, on implémente directement javax.servlet.Filter.
 * Pas de OncePerRequestFilter, pas de SecurityContextHolder, pas d'injection.
 *
 *   package com.example.filter;
 *
 *   import com.example.util.JwtUtil;
 *   import com.example.dao.UserDAO;
 *   import com.example.model.User;
 *   import io.jsonwebtoken.*;
 *   import io.jsonwebtoken.security.Keys;
 *   import jakarta.servlet.*;
 *   import jakarta.servlet.http.*;
 *   import java.io.IOException;
 *   import java.nio.charset.StandardCharsets;
 *   import java.util.Optional;
 *
 *   // Déclaration dans web.xml (pas d'annotation @Component) :
 *   // <filter>
 *   //   <filter-name>JwtFilter</filter-name>
 *   //   <filter-class>com.example.filter.JwtAuthFilterJ2EE</filter-class>
 *   // </filter>
 *   // <filter-mapping>
 *   //   <filter-name>JwtFilter</filter-name>
 *   //   <url-pattern>/api/*</url-pattern>
 *   // </filter-mapping>
 *
 *   public class JwtAuthFilterJ2EE implements Filter {
 *
 *       // Pas d'injection de dépendances — instanciation manuelle
 *       private static final String SECRET = "game-portal-jwt-secret-key-must-be-32-bytes";
 *       private UserDAO userDAO = new UserDAO();
 *
 *       // URLs publiques — pas de protection (Spring : .permitAll() dans SecurityConfig)
 *       private static final java.util.Set<String> PUBLIC_PATHS = new java.util.HashSet<>(
 *           java.util.Arrays.asList("/api/auth/login", "/api/auth/register",
 *                                   "/api/scores/leaderboard")
 *       );
 *
 *       @Override
 *       public void init(FilterConfig filterConfig) throws ServletException {}
 *
 *       @Override
 *       public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
 *               throws IOException, ServletException {
 *
 *           HttpServletRequest  httpReq = (HttpServletRequest)  req;
 *           HttpServletResponse httpRes = (HttpServletResponse) res;
 *
 *           String path = httpReq.getRequestURI().substring(httpReq.getContextPath().length());
 *
 *           // Routes publiques — pas de vérification (Spring : .requestMatchers().permitAll())
 *           if (PUBLIC_PATHS.contains(path) || path.startsWith("/api/games")) {
 *               chain.doFilter(req, res);
 *               return;
 *           }
 *
 *           // Lecture de l'en-tête Authorization
 *           String authHeader = httpReq.getHeader("Authorization");
 *           if (authHeader == null || !authHeader.startsWith("Bearer ")) {
 *               sendUnauthorized(httpRes, "Token JWT manquant");
 *               return;
 *           }
 *
 *           String token = authHeader.substring(7);
 *           String username;
 *           String role;
 *
 *           // Parsing et validation du JWT (même bibliothèque JJWT)
 *           try {
 *               javax.crypto.SecretKey key = Keys.hmacShaKeyFor(
 *                   SECRET.getBytes(StandardCharsets.UTF_8));
 *               Claims claims = Jwts.parser()
 *                   .verifyWith(key).build()
 *                   .parseSignedClaims(token).getPayload();
 *               username = claims.getSubject();
 *               role     = claims.get("role", String.class);
 *           } catch (JwtException e) {
 *               sendUnauthorized(httpRes, "Token JWT invalide ou expiré");
 *               return;
 *           }
 *
 *           // Vérification que l'utilisateur existe toujours en base
 *           // Spring : UserDetailsServiceImpl.loadUserByUsername()
 *           Optional<User> userOpt = userDAO.findByUsername(username);
 *           if (userOpt.isEmpty()) {
 *               sendUnauthorized(httpRes, "Utilisateur introuvable");
 *               return;
 *           }
 *
 *           // Propagation manuelle vers les Servlets aval
 *           // Spring : SecurityContextHolder.getContext().setAuthentication(authToken)
 *           httpReq.setAttribute("currentUser", userOpt.get());
 *           httpReq.setAttribute("currentRole", role);
 *
 *           // Vérification des routes ADMIN (Spring : .hasRole("ADMIN") ou @PreAuthorize)
 *           if (path.startsWith("/api/admin") && !"ROLE_ADMIN".equals(role)) {
 *               httpRes.setStatus(HttpServletResponse.SC_FORBIDDEN);
 *               httpRes.setContentType("application/json");
 *               httpRes.getWriter().write("{\"error\":\"Accès réservé aux administrateurs\"}");
 *               return;
 *           }
 *
 *           chain.doFilter(req, res); // continue vers la Servlet
 *       }
 *
 *       private void sendUnauthorized(HttpServletResponse response, String message)
 *               throws IOException {
 *           response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *           response.getWriter().write("{\"error\":\"" + message + "\"}");
 *       }
 *
 *       @Override
 *       public void destroy() {}
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring JwtAuthFilter   : ~30 lignes — injection auto, SecurityContext, OncePerRequest
 *   J2EE JwtAuthFilterJ2EE : ~90 lignes — tout manuel, déclaration web.xml, propagation
 *                            manuelle via request.setAttribute()
 *   Double exécution       : Spring OncePerRequestFilter protège ; J2EE : à gérer soi-même
 *   Ordre des filtres      : Spring addFilterBefore() ; J2EE : ordre d'url-pattern dans web.xml
 * =====================================================================
 */

