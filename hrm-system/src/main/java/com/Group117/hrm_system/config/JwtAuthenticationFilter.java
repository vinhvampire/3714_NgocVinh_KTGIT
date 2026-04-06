package com.Group117.hrm_system.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // 1. Bỏ qua các path công khai (Public Paths)
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Trích xuất Token (Header hoặc Cookie)
        String jwt = extractToken(request);

        // 3. Nếu không có token, cho đi tiếp để Spring Security xử lý 403 sau
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    // Log nhẹ để biết auth thành công khi dev
                    // System.out.println(">>> Authenticated: " + username); 
                }
            }
        } catch (Exception e) {
            System.err.println(">>> JWT Auth Error: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/") || path.equals("/login") || path.equals("/")
                || path.startsWith("/css/") || path.startsWith("/js/") 
                || path.startsWith("/images/") || path.equals("/favicon.ico");
    }

    private String extractToken(HttpServletRequest request) {
        // Ưu tiên Header cho các call API từ JS
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (isValid(token)) return token;
        }

        // Fallback query param để hỗ trợ các trang có access_token trên URL
        String qToken = request.getParameter("access_token");
        if (isValid(qToken)) return qToken;

        // Fallback sang Cookie cho việc chuyển trang Thymeleaf
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    if (isValid(cookie.getValue())) return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isValid(String t) {
        return t != null && !t.isEmpty() && !t.equalsIgnoreCase("null") && !t.equalsIgnoreCase("undefined");
    }
}