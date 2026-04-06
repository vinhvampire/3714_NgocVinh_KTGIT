package com.Group117.hrm_system.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Bật @PreAuthorize / @RolesAllowed trong Controller & Service
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UpgradingDaoAuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            TaiKhoanRepository taiKhoanRepository) {

        return new UpgradingDaoAuthenticationProvider(
                passwordEncoder,
                userDetailsService,
                taiKhoanRepository
        );
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UpgradingDaoAuthenticationProvider authProvider) {
        return new ProviderManager(authProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                // ── CORS ──────────────────────────────────────────────────
                // Cho phép frontend (localhost:3000 / localhost:8080) gọi API
                // Thêm domain production vào allowedOrigins khi deploy
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(java.util.List.of(
                            "http://localhost:8080",
                            "http://localhost:3000",
                            "http://127.0.0.1:8080"
                    ));
                    config.setAllowedMethods(java.util.List.of(
                            "GET", "POST", "PUT", "DELETE", "OPTIONS"
                    ));
                    config.setAllowedHeaders(java.util.List.of("*"));
                    config.setAllowCredentials(true); // Bắt buộc để gửi cookie JWT
                    return config;
                }))

                // ── Exception Handling ────────────────────────────────────
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // API call → luôn trả JSON 401 (KHÔNG redirect)
                            if (request.getRequestURI() != null && request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                response.getWriter().write("{\"message\":\"Chưa xác thực\"}");
                                return;
                            }
                            // Page request → redirect login
                            response.sendRedirect("/login?error=unauthorized");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI() != null && request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                response.getWriter().write("{\"message\":\"Không đủ quyền\"}");
                                return;
                            }
                            response.sendRedirect("/login?error=forbidden");
                        })
                )

                // ── Session: Stateless (dùng JWT, không dùng session) ─────
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ── Authorization Rules ───────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // Public: auth endpoints + static resources + trang login
                        .requestMatchers(
                                "/api/auth/**",
                                "/login", "/", "/home",
                                "/forgot-password", "/forgot-password/sent", "/reset-password",
                                "/css/**", "/js/**", "/images/**",
                                "/uploads/**",
                                "/webjars/**", "/favicon.ico",
                                "/ws/**"
                        ).permitAll()

                        // Preflight OPTIONS — luôn permit
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── Thymeleaf Dashboard Pages ─────────────────────────
                        // Để permitAll ở tầng Security — việc kiểm tra JWT token
                        // và phân quyền theo role do DashboardController tự xử lý
                        // thông qua cookie "jwt_token".
                        //
                        // Lý do không dùng .hasRole() ở đây:
                        //   • JWT được đọc từ Cookie, không phải Authorization header
                        //   • JwtAuthenticationFilter inject SecurityContext nhưng
                        //     role check tập trung tại Controller cho dễ debug
                        //   • Nếu muốn enforce tại Security layer, dùng @PreAuthorize
                        //     trong từng Controller method thay vì cấu hình ở đây
                        .requestMatchers("/dashboard/**").permitAll()

                        // ── REST API — bắt buộc có JWT hợp lệ ───────────────
                        // Phân quyền chi tiết theo role dùng @PreAuthorize trong Controller:
                        //
                        //   DIRECTOR  → @PreAuthorize("hasRole('DIRECTOR')")
                        //   ADMIN     → @PreAuthorize("hasRole('ADMIN')")
                        //   HR        → @PreAuthorize("hasRole('HR')")
                        //   EMPLOYEE  → @PreAuthorize("hasRole('EMPLOYEE')")
                        //
                        // Ví dụ dùng nhiều role:
                        //   @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}