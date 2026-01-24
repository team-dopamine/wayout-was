package kr.wayout.global.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import kr.wayout.domain.member.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtProvider {

    private final String secret;
    private final long refreshExpiration;
    private final long accessExpiration;
    private final String issuer;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-expiration}") long accessExpiration,
                       @Value("${jwt.refresh-expiration}") long refreshExpiration,
                       @Value("${jwt.issuer}") String issuer) {
        this.secret = secret;
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.issuer = issuer;
    }

    public String createAccessToken(String email, Role role) {
        return createToken(email, role, accessExpiration);
    }

    public String createRefreshToken(String email, Role role) {
        return createToken(email, role, refreshExpiration);
    }

    private String createToken(String email, Role role, long expiration) {
        Claims claims = Jwts.claims();
        claims.setSubject(email);
        claims.put("role", role.name());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(issuer)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret.getBytes())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            if (token == null) {
                return false;
            }
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(secret.getBytes())
                    .build()
                    .parseClaimsJws(token);

            // 만료 시간이 현재 시간보다 이전인지 확인
            return !claims.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Role getRole(String token) {
        String roleStr = (String) Jwts.parserBuilder()
                .setSigningKey(secret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");

        return Role.valueOf(roleStr);  // String → Role enum 변환
    }

    public long getExpirationTime(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .getTime();
    }
}
