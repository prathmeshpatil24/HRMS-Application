package com.hrms.security.jwt;

import com.hrms.security.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
             HttpServletRequest request,
             HttpServletResponse response,
             FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract Authorization header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String token = null;
        String username = null;
        try {
            // 2. Check if Bearer token is present and extract it
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                token = authHeader.replace(BEARER_PREFIX, "").trim();

                // 3. Validate token signature and extract username
                if (jwtTokenProvider.validateToken(token)) {
                    username = jwtTokenProvider.getUsernameFromToken(token);
                }
            }
            // 4. If username exists and no authentication is already present in SecurityContext
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                // Verify user is active and not locked
                if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Set authentication in Spring Security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context: {}", e.getMessage());
        }
        // 5. Always continue the filter chain
        filterChain.doFilter(request, response);


//        try {
//            String jwt = parseJwt(request);
//
//            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
//                String username = jwtTokenProvider.getUsernameFromToken(jwt);
//
//                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//
//                    if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
//                        UsernamePasswordAuthenticationToken authentication =
//                                new UsernamePasswordAuthenticationToken(
//                                        userDetails,
//                                        null,
//                                        userDetails.getAuthorities());
//
//                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                        SecurityContextHolder.getContext().setAuthentication(authentication);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("Cannot set user authentication in security context: {}", e.getMessage());
//        }
//
//        filterChain.doFilter(request, response);
    }

//    private String parseJwt(HttpServletRequest request) {
//        String headerAuth = request.getHeader(AUTHORIZATION_HEADER);
//
//        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(BEARER_PREFIX)) {
//            return headerAuth.substring(BEARER_PREFIX.length());
//        }
//
//        return null;
//    }
}
