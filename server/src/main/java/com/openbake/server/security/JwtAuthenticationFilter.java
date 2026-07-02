package com.openbake.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openbake.server.entity.User;
import com.openbake.server.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mirrors backend/app/utils/jwt.py's get_current_user dependency: a missing Authorization
 * header is left for JwtAuthEntryPoint (403 "Not authenticated", matching HTTPBearer's
 * auto_error), while a present-but-invalid/expired token is rejected here immediately with
 * the same 401 "Could not validate credentials" / 404 "User not found" bodies FastAPI returns.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        Claims claims;
        try {
            claims = jwtService.verify(token, "access");
        } catch (JwtException | IllegalArgumentException e) {
            writeError(response, 401, "Could not validate credentials");
            return;
        }

        String userId = claims.getSubject();
        if (userId == null) {
            writeError(response, 401, "Invalid token payload");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            writeError(response, 404, "User not found");
            return;
        }

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase(Locale.ROOT)));
        var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(MAPPER.writeValueAsString(Map.of("detail", detail)));
    }
}
