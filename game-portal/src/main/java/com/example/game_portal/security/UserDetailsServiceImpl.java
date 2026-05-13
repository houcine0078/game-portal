package com.example.game_portal.security;

import com.example.game_portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}

/*
 * Without Spring Security, you would manually verify the user in a Servlet Filter.
 * The filter would check the session on every request to protected routes:
 *
 *   public class AuthFilter implements Filter {
 *       public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
 *               throws IOException, ServletException {
 *           HttpServletRequest  request  = (HttpServletRequest)  req;
 *           HttpServletResponse response = (HttpServletResponse) res;
 *
 *           HttpSession session = request.getSession(false);
 *           if (session == null || session.getAttribute("username") == null) {
 *               response.setStatus(401);
 *               response.getWriter().write("{\"error\":\"Not authenticated\"}");
 *               return; // stop here, don't continue to the Servlet
 *           }
 *           chain.doFilter(req, res); // user is logged in, continue
 *       }
 *   }
 *
 * With Spring, implementing this one method is enough — Spring Security
 * automatically calls it when verifying a JWT token or processing a login.
 */
