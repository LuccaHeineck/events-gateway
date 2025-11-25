package com.eventos.eventsgateway.filter;

import io.jsonwebtoken.*;

public class JwtUtils {

    public static void validateToken(String token, String secret) {
        Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token);
    }
}
