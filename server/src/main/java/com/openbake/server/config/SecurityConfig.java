package com.openbake.server.config;

import com.openbake.server.repository.UserRepository;
import com.openbake.server.security.JwtAccessDeniedHandler;
import com.openbake.server.security.JwtAuthEntryPoint;
import com.openbake.server.security.JwtAuthenticationFilter;
import com.openbake.server.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppProperties appProperties;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final JwtAuthEntryPoint authEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(AppProperties appProperties, JwtService jwtService, UserRepository userRepository,
                           JwtAuthEntryPoint authEntryPoint, JwtAccessDeniedHandler accessDeniedHandler) {
        this.appProperties = appProperties;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/health", "/seed", "/docs/**", "/openapi.json", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/products", "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/*/stream").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/delivery/estimate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cart/validate").permitAll()
                        .requestMatchers("/api/payments/payu/hosted", "/api/payments/payu/callback/**").permitAll()
                        .requestMatchers("/media/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        // Everything else is the embedded web app (static HTML/JS/CSS,
                        // client-side-routed SPA pages) — it enforces its own
                        // page-level auth checks in the browser, same as when it was
                        // hosted separately from this API.
                        .anyRequest().permitAll())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        boolean wildcard = "*".equals(appProperties.getAllowedOrigins()) || !appProperties.isProduction();
        if (wildcard) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(Arrays.stream(appProperties.getAllowedOrigins().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("X-Request-ID", "X-Response-Time"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
