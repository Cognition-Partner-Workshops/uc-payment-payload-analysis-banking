package com.cbarkinozer.onlinebankingrestapi.app.sec.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenGenerator {

    @Value("${onlinebankingrestapi.jwt.security.app.key}")
    private String APP_KEY;

    @Value("${onlinebankingrestapi.jwt.security.expire.time}")
    private Long EXPIRE_TIME;

    private static final int MIN_APP_KEY_BYTES = 64;

    @PostConstruct
    void validateAppKey() {
        if (APP_KEY == null || APP_KEY.isBlank()
                || APP_KEY.getBytes(StandardCharsets.UTF_8).length < MIN_APP_KEY_BYTES) {
            throw new IllegalStateException("JWT signing key (JWT_SIGNING_KEY) must be set and at least "
                    + MIN_APP_KEY_BYTES + " bytes long");
        }
    }

    public String generateJwtToken(Authentication authentication){

        JwtUserDetails jwtUserDetails = (JwtUserDetails) authentication.getPrincipal();
        Date expireDate = new Date(new Date().getTime() + EXPIRE_TIME);

        String token = Jwts.builder()
                .setSubject(Long.toString(jwtUserDetails.getId()))
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, APP_KEY.getBytes(StandardCharsets.UTF_8))
                .compact();

        return token;
    }

    public Long findUserIdByToken(String token){

        Jws<Claims> claimsJws = parseToken(token);

        String userIdStr = claimsJws
                .getBody()
                .getSubject();

        return Long.parseLong(userIdStr);
    }

    private Jws<Claims> parseToken(String token) {
        Jws<Claims> claimsJws = Jwts.parser()
                .setSigningKey(APP_KEY.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token);
        return claimsJws;
    }

    public boolean validateToken(String token){

        boolean isValid;

        try {
            Jws<Claims> claimsJws = parseToken(token);

            isValid = !isTokenExpired(claimsJws);
        } catch (Exception e){
            isValid = false;
        }

        return isValid;
    }

    private boolean isTokenExpired(Jws<Claims> claimsJws) {

        Date expirationDate = claimsJws.getBody().getExpiration();

        return expirationDate.before(new Date());
    }
}
