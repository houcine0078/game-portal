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

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token    = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        chain.doFilter(request, response);
    }
}

/*
 * Without Spring Security, you would write a plain Servlet Filter to check the JWT manually:
 *
 *   public class JwtAuthFilterJ2EE implements Filter {
 *       public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
 *               throws IOException, ServletException {
 *           HttpServletRequest  request  = (HttpServletRequest)  req;
 *           HttpServletResponse response = (HttpServletResponse) res;
 *
 *           String authHeader = request.getHeader("Authorization");
 *           if (authHeader == null || !authHeader.startsWith("Bearer ")) {
 *               response.setStatus(401);
 *               response.getWriter().write("{\"error\":\"Missing token\"}");
 *               return;
 *           }
 *
 *           String token = authHeader.substring(7);
 *           try {
 *               Claims claims = Jwts.parser().verifyWith(SECRET_KEY).build()
 *                   .parseSignedClaims(token).getPayload();
 *               request.setAttribute("username", claims.getSubject());
 *               request.setAttribute("role",     claims.get("role"));
 *           } catch (JwtException e) {
 *               response.setStatus(401);
 *               response.getWriter().write("{\"error\":\"Invalid token\"}");
 *               return;
 *           }
 *           chain.doFilter(req, res);
 *       }
 *   }
 *   // Also registered in web.xml for every protected URL pattern.
 *
 * With Spring, this filter is registered automatically via @Component + addFilterBefore()
 * in SecurityConfig, and SecurityContextHolder makes the user available everywhere.
 */
