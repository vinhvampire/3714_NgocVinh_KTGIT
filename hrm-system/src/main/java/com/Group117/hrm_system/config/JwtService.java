package com.Group117.hrm_system.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    // Khóa bí mật (Phải đủ dài trên 32 ký tự)
    private static final String SECRET_STRING = "DayLaChuoiBiMatSieuCapVipPro_KhongDuocTietLo_2026";

    // Tạo đối tượng Key dùng chung để tiết kiệm tài nguyên
    private final Key key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    // 1. Tạo Token
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 giờ
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. Trích xuất Username
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 3. Trích xuất thông tin bất kỳ (Hàm bổ trợ)
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    // 4. Kiểm tra Token còn hạn hay không
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            System.out.println("Token không hợp lệ: " + e.getMessage());
            return false;
        }
    }
}