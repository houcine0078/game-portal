package com.example.game_portal.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitaire pour la génération et la validation des tokens JWT.
 *
 * =========================================================
 * SANS SPRING (gestion de session HTTP classique) :
 * =========================================================
 *   Il n'y aurait pas de JWT. L'authentification est gérée par session :
 *
 *     // Connexion → création de session
 *     HttpSession session = request.getSession(true);
 *     session.setAttribute("username", user.getUsername());
 *     session.setAttribute("role", user.getRole().name());
 *     session.setMaxInactiveInterval(1800); // 30 min
 *
 *     // Chaque requête → vérification de session
 *     HttpSession session = request.getSession(false);
 *     if (session == null || session.getAttribute("username") == null) {
 *         response.sendError(401); return;
 *     }
 *     String role = (String) session.getAttribute("role");
 *     if (!"ADMIN".equals(role)) { response.sendError(403); return; }
 *
 *     // Déconnexion
 *     session.invalidate();
 *
 * =========================================================
 * AVEC JWT (stateless) :
 * =========================================================
 *   Token auto-contenu signé (HMAC-SHA256) :
 *     Header  : {"alg":"HS256","typ":"JWT"}
 *     Payload : {"sub":"alice","role":"ADMIN","iat":...,"exp":...}
 *     Signature : HMAC-SHA256(base64(header)+"."+base64(payload), secret)
 *
 *   Le serveur ne stocke RIEN → scalable horizontalement.
 *   Le client envoie : "Authorization: Bearer <token>" à chaque requête.
 *
 * =========================================================
 * DIFFÉRENCES CLÉS :
 * =========================================================
 *   Session :
 *     - État stocké côté serveur (mémoire ou base Redis/DB)
 *     - Identifié par JSESSIONID (cookie) → CSRF possible
 *     - Non scalable sans session partagée (sticky sessions ou Redis)
 *
 *   JWT :
 *     - Stateless → le serveur n'a rien à stocker
 *     - Pas de cookie → pas de CSRF (mais XSS si mal stocké)
 *     - Contient username ET rôle → pas de query DB à chaque requête
 *     - Révocation difficile (nécessite une blacklist)
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. @Value injecte la clé secrète depuis application.properties
 *      (externalisable via variables d'environnement en production)
 *   2. Bibliothèque JJWT (io.jsonwebtoken) offre une API fluide et sécurisée
 *   3. Intégration transparente dans la chaîne de filtres Spring Security
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /*
     * SANS SPRING : constante codée en dur.
     * AVEC SPRING : @Value lit depuis application.properties ou env var.
     */
    @Value("${jwt.expiration:86400000}")
    private long expirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Génère un token JWT contenant le nom d'utilisateur et son rôle.
     *
     * SANS SPRING : pas de JWT. Création d'une session HTTP :
     *   session.setAttribute("username", username);
     *   session.setAttribute("role", role);
     *
     * AVEC JWT : toutes les infos sont encodées dans le token lui-même.
     * Le serveur peut vérifier l'authenticité sans accès à la base de données.
     */
    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("ROLE_USER");

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    /**
     * Surcharge pour la compatibilité avec l'ancien code (username seul).
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    /**
     * Extrait le nom d'utilisateur (claim "sub") du token.
     *
     * SANS SPRING : session.getAttribute("username")
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extrait le rôle (claim "role") du token.
     * Permet d'éviter une requête en base pour connaître le rôle.
     */
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Valide le token : signature correcte + non expiré.
     *
     * SANS SPRING : session != null && !session.isNew() && session.getAttribute("user") != null
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE / SERVLET — Gestion de session HTTP (sans JWT)
 * =====================================================================
 *
 * En J2EE classique, l'authentification est stateful (session serveur).
 * Il n'y a pas de JWT : le JSESSIONID cookie est l'identifiant de session.
 *
 * CLASSE SessionManager (équivalent fonctionnel de JwtUtil pour J2EE)
 * ---------------------------------------------------------------------
 *
 *   package com.example.util;
 *
 *   import jakarta.servlet.http.*;
 *   import java.security.MessageDigest;
 *   import java.util.Base64;
 *
 *   public class SessionManager {
 *
 *       private static final int SESSION_TIMEOUT_SECONDS = 30 * 60; // 30 min
 *
 *       // Équivalent de generateToken(UserDetails) — crée une session HTTP
 *       // JWT : retourne un String (token) → stocké côté client
 *       // Session : crée un objet server-side → JSESSIONID envoyé en cookie
 *       public static void createSession(HttpServletRequest request, User user) {
 *           HttpSession session = request.getSession(true); // crée si absente
 *           session.setAttribute("username", user.getUsername());
 *           session.setAttribute("email",    user.getEmail());
 *           session.setAttribute("role",     user.getRole());
 *           session.setAttribute("userId",   user.getId());
 *           session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
 *           // Le serveur envoie automatiquement : Set-Cookie: JSESSIONID=<id>; HttpOnly
 *       }
 *
 *       // Équivalent de extractUsername(token) — lit depuis la session
 *       // JWT : décode le token (Base64 + vérification HMAC)
 *       // Session : lookup en mémoire serveur par JSESSIONID du cookie
 *       public static String extractUsername(HttpServletRequest request) {
 *           HttpSession session = request.getSession(false);
 *           if (session == null) return null;
 *           return (String) session.getAttribute("username");
 *       }
 *
 *       // Équivalent de extractRole(token)
 *       public static String extractRole(HttpServletRequest request) {
 *           HttpSession session = request.getSession(false);
 *           if (session == null) return null;
 *           return (String) session.getAttribute("role");
 *       }
 *
 *       // Équivalent de validateToken(token, userDetails) — vérifie la session
 *       // JWT : vérifie la signature HMAC + la date d'expiration dans le token
 *       // Session : vérifie que la session existe et n'est pas expirée (Tomcat gère)
 *       public static boolean isAuthenticated(HttpServletRequest request) {
 *           HttpSession session = request.getSession(false);
 *           return session != null
 *               && session.getAttribute("username") != null
 *               && !session.isNew();
 *       }
 *
 *       // Équivalent de l'expiration JWT — vérification manuelle si nécessaire
 *       // (Tomcat invalide automatiquement après setMaxInactiveInterval)
 *       public static boolean isSessionExpired(HttpServletRequest request) {
 *           HttpSession session = request.getSession(false);
 *           if (session == null) return true;
 *           long lastAccess  = session.getLastAccessedTime();
 *           long timeout     = session.getMaxInactiveInterval() * 1000L;
 *           return (System.currentTimeMillis() - lastAccess) > timeout;
 *       }
 *
 *       // Équivalent de "déconnexion" — invalidation du token JWT côté client
 *       // JWT : le client supprime le token (le serveur ne peut pas le révoquer)
 *       // Session : invalidation côté serveur — immédiatement effective
 *       public static void destroySession(HttpServletRequest request) {
 *           HttpSession session = request.getSession(false);
 *           if (session != null) session.invalidate();
 *       }
 *
 *       // Hachage SHA-256 (sans sel — MOINS SÉCURISÉ que BCrypt)
 *       // Spring utilise BCryptPasswordEncoder avec sel aléatoire automatique
 *       public static String hashPassword(String plainPassword) {
 *           try {
 *               MessageDigest md = MessageDigest.getInstance("SHA-256");
 *               byte[] hash = md.digest(plainPassword.getBytes("UTF-8"));
 *               return Base64.getEncoder().encodeToString(hash);
 *           } catch (Exception e) {
 *               throw new RuntimeException("Erreur de hachage", e);
 *           }
 *       }
 *
 *       // Vérification mot de passe (SHA-256 — vulnérable aux rainbow tables)
 *       // Spring BCrypt : chaque hash est unique (sel aléatoire 128 bits)
 *       public static boolean verifyPassword(String plainPassword, String storedHash) {
 *           return hashPassword(plainPassword).equals(storedHash);
 *       }
 *   }
 *
 *   // Utilisation dans LoginServlet :
 *   // SessionManager.createSession(request, user);
 *
 *   // Utilisation dans AuthFilter :
 *   // if (!SessionManager.isAuthenticated(request)) { response.sendError(401); return; }
 *   // String role = SessionManager.extractRole(request);
 *   // if (!"ADMIN".equals(role)) { response.sendError(403); return; }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   JWT Spring     : token signé → stateless, scalable, contient toutes les infos
 *   Session J2EE   : état côté serveur → non scalable sans Redis/cluster de session
 *   BCrypt Spring  : sel automatique → résistant rainbow tables
 *   SHA-256 J2EE   : sans sel → vulnérable (même password = même hash)
 *   Révocation     : Session → session.invalidate() immédiat ; JWT → impossible sans blacklist
 * =====================================================================
 */

