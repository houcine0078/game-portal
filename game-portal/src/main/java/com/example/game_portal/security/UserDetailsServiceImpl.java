package com.example.game_portal.security;

import com.example.game_portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implémentation de UserDetailsService — pont entre Spring Security et la base de données.
 *
 * =========================================================
 * SANS SPRING SECURITY :
 * =========================================================
 *   Le chargement de l'utilisateur se fait manuellement dans chaque Servlet
 *   ou dans un filtre personnalisé :
 *
 *     // AuthFilter.java (filtre Servlet manuel)
 *     public class AuthFilter implements Filter {
 *         private UserDAO userDAO = new UserDAO();
 *
 *         @Override
 *         public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
 *                 throws IOException, ServletException {
 *             HttpServletRequest  httpReq  = (HttpServletRequest) req;
 *             HttpServletResponse httpRes  = (HttpServletResponse) res;
 *
 *             // 1. Vérification de la session HTTP
 *             HttpSession session = httpReq.getSession(false);
 *             if (session == null || session.getAttribute("user") == null) {
 *                 httpRes.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Non authentifié");
 *                 return; // stoppe la chaîne
 *             }
 *             chain.doFilter(req, res); // passe à la Servlet suivante
 *         }
 *     }
 *
 *   Ce filtre est enregistré dans web.xml :
 *     <filter>
 *       <filter-name>AuthFilter</filter-name>
 *       <filter-class>com.example.filter.AuthFilter</filter-class>
 *     </filter>
 *     <filter-mapping>
 *       <filter-name>AuthFilter</filter-name>
 *       <url-pattern>/api/protected/*</url-pattern>
 *     </filter-mapping>
 *
 * =========================================================
 * AVEC SPRING SECURITY :
 * =========================================================
 *   UserDetailsService est le seul contrat à implémenter.
 *   Spring Security appelle loadUserByUsername() automatiquement :
 *     - lors de l'authentification (AuthenticationManager)
 *     - lors du chargement depuis le token JWT (JwtAuthFilter)
 *
 *   Notre entité User implémente UserDetails → le retour est direct.
 *
 * =========================================================
 * COMPARAISON CHAÎNE DE FILTRES :
 * =========================================================
 *   SANS SPRING :
 *     Filtre manuel → Servlet (chaîne linéaire, gestion manuelle)
 *     Chaque vérification (auth, rôle, CSRF...) doit être codée explicitement.
 *
 *   AVEC SPRING SECURITY :
 *     SecurityFilterChain interne (~20 filtres pré-configurés) :
 *       SecurityContextPersistenceFilter
 *       → UsernamePasswordAuthenticationFilter  (remplacé par JwtAuthFilter)
 *       → ExceptionTranslationFilter            (401/403 automatiques)
 *       → FilterSecurityInterceptor             (contrôle d'accès)
 *       → ...
 *     Notre JwtAuthFilter s'insère AVANT UsernamePasswordAuthenticationFilter.
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. Un seul point de chargement de l'utilisateur (pas dispersé dans toutes les Servlets)
 *   2. Intégration automatique avec AuthenticationManager et tous les mécanismes Spring Security
 *   3. Cache d'authentification dans le SecurityContextHolder (pas de rechargement DB par requête)
 *   4. @Transactional disponible → lecture dans la même transaction JPA si nécessaire
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Charge un utilisateur depuis la base de données pour Spring Security.
     * Appelé automatiquement par le framework lors de l'authentification.
     *
     * SANS SPRING : équivaut à userDAO.findByUsername() + vérification manuelle
     *   if (user == null) response.sendError(401, "Utilisateur non trouvé");
     *
     * AVEC SPRING : l'exception UsernameNotFoundException est interceptée
     *   automatiquement par ExceptionTranslationFilter → réponse 401 propre.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé : " + username));
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE / SERVLET — AuthFilter manuel (sans UserDetailsService)
 * =====================================================================
 *
 * En J2EE, il n'existe pas de UserDetailsService ni de SecurityContext.
 * Le chargement de l'utilisateur et la vérification de l'auth se font
 * manuellement dans un javax.servlet.Filter déclaré dans web.xml.
 *
 *   package com.example.filter;
 *
 *   import com.example.dao.UserDAO;
 *   import com.example.model.User;
 *   import jakarta.servlet.*;
 *   import jakarta.servlet.http.*;
 *   import java.io.IOException;
 *   import java.util.Optional;
 *
 *   // Déclaration dans web.xml :
 *   // <filter>
 *   //   <filter-name>AuthFilter</filter-name>
 *   //   <filter-class>com.example.filter.AuthFilter</filter-class>
 *   // </filter>
 *   // <filter-mapping>
 *   //   <filter-name>AuthFilter</filter-name>
 *   //   <url-pattern>/api/protected/*</url-pattern>
 *   // </filter-mapping>
 *
 *   public class AuthFilter implements Filter {
 *
 *       // Instancié manuellement — pas d'injection de dépendances
 *       // Spring injecte UserRepository via @RequiredArgsConstructor
 *       private UserDAO userDAO = new UserDAO();
 *
 *       @Override
 *       public void init(FilterConfig filterConfig) throws ServletException {
 *           // Initialisation du filtre (équivalent @PostConstruct)
 *       }
 *
 *       @Override
 *       public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
 *               throws IOException, ServletException {
 *
 *           HttpServletRequest  httpReq = (HttpServletRequest)  req;
 *           HttpServletResponse httpRes = (HttpServletResponse) res;
 *
 *           // OPTION A : Authentification par session HTTP (stateful)
 *           HttpSession session = httpReq.getSession(false);
 *           if (session == null || session.getAttribute("username") == null) {
 *               httpRes.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *               httpRes.setContentType("application/json");
 *               httpRes.getWriter().write("{\"error\":\"Non authentifié\"}");
 *               return; // bloque la chaîne — la Servlet ne sera pas appelée
 *           }
 *
 *           // Récupération de l'utilisateur depuis la session
 *           // Spring charge l'utilisateur une fois via loadUserByUsername()
 *           // et le met dans le SecurityContextHolder (threadLocal)
 *           String username = (String) session.getAttribute("username");
 *           Optional<User> userOpt = userDAO.findByUsername(username);
 *           if (userOpt.isEmpty()) {
 *               session.invalidate(); // session corrompue
 *               httpRes.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *               httpRes.getWriter().write("{\"error\":\"Utilisateur introuvable\"}");
 *               return;
 *           }
 *
 *           // Passage de l'utilisateur aux Servlets suivantes via attribut de requête
 *           // Spring Security utilise SecurityContextHolder.getContext().getAuthentication()
 *           httpReq.setAttribute("currentUser", userOpt.get());
 *           httpReq.setAttribute("currentRole", session.getAttribute("role"));
 *
 *           chain.doFilter(req, res); // passe à la Servlet suivante
 *       }
 *
 *       @Override
 *       public void destroy() {
 *           // Nettoyage des ressources (connexions, caches...)
 *       }
 *   }
 *
 *   // Dans la Servlet protégée, récupération de l'utilisateur :
 *   // User currentUser = (User) request.getAttribute("currentUser");
 *   // String role = (String) request.getAttribute("currentRole");
 *   // if (!"ADMIN".equals(role)) { response.sendError(403); return; }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring UserDetailsService : 1 méthode → Spring gère la vérification + le contexte
 *   J2EE AuthFilter           : ~60 lignes de vérification, propagation manuelle via
 *                               request.setAttribute(), déclaration dans web.xml obligatoire
 *   ThreadLocal               : Spring SecurityContextHolder est threadLocal automatique
 *                               J2EE : propagation manuelle via request.setAttribute()
 * =====================================================================
 */
