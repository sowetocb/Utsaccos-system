package com.saccos_system.util;

import com.saccos_system.config.JwtConfig;
import com.saccos_system.model.SystemUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenUtil {

    private final JwtConfig jwtConfig;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }

    // Generate token with roles
    public String generateToken(SystemUser user, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("username", user.getUsername());
        claims.put("fullName", user.getProfile().getFirstName() + " " + user.getProfile().getLastName());
        claims.put("memberNumber", user.getProfile().getMemberNumber());
        claims.put("roles", roles);

        String primaryRole = determinePrimaryRole(roles);
        claims.put("role", primaryRole);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration() * 1000))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Original method for backward compatibility
    public String generateToken(SystemUser user) {
        List<String> defaultRoles = List.of("MEMBER");
        return generateToken(user, defaultRoles);
    }

    private String determinePrimaryRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "MEMBER";
        }
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles.contains("LOAN_OFFICER")) {
            return "LOAN_OFFICER";
        }
        if (roles.contains("ACCOUNTANT")) {
            return "ACCOUNTANT";
        }
        return "MEMBER";
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Boolean validateToken(String token, SystemUser user) {
        final String username = getUsernameFromToken(token);
        return (username.equals(user.getUsername()) && !isTokenExpired(token));
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("roles", List.class);
    }

    public boolean hasRole(String token, String role) {
        List<String> roles = getRolesFromToken(token);
        return roles != null && roles.contains(role);
    }
}