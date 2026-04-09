package pl.zieleeksw.quizmi.auth.domain

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date
import java.util.UUID
import java.util.function.Function

@Component
class JwtFacade(
    private val jwtProperties: JwtProperties
) {
    private val signingKey: Key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secretKey))

    fun generateAccessToken(email: String): String {
        return generateToken(email, jwtProperties.expiration.accessTokenMs, JwtTokenType.ACCESS)
    }

    fun generateRefreshToken(email: String): String {
        return generateToken(email, jwtProperties.expiration.refreshTokenMs, JwtTokenType.REFRESH)
    }

    fun isAccessTokenValid(
        token: String,
        email: String
    ): Boolean {
        return isTokenValid(token, email, JwtTokenType.ACCESS)
    }

    fun isRefreshTokenValid(
        token: String,
        email: String
    ): Boolean {
        return isTokenValid(token, email, JwtTokenType.REFRESH)
    }

    fun extractEmail(token: String): String {
        return extractClaim(token, Claims::getSubject)
    }

    private fun isTokenValid(
        token: String,
        email: String,
        tokenType: JwtTokenType
    ): Boolean {
        return extractEmail(token) == email &&
            extractTokenType(token) == tokenType &&
            !isTokenExpired(token)
    }

    private fun generateToken(
        email: String,
        validityMs: Long,
        tokenType: JwtTokenType
    ): String {
        val claims = mutableMapOf<String, Any>(
            "jti" to UUID.randomUUID().toString(),
            "type" to tokenType.name
        )
        val now = Date()

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + validityMs))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()
    }

    private fun extractTokenType(token: String): JwtTokenType {
        val value = extractAllClaims(token)["type"] as? String
            ?: throw IllegalArgumentException("Token type is missing.")

        return JwtTokenType.valueOf(value)
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }

    private fun extractExpiration(token: String): Date {
        return extractClaim(token, Claims::getExpiration)
    }

    private fun <T> extractClaim(
        token: String,
        resolver: Function<Claims, T>
    ): T {
        return resolver.apply(extractAllClaims(token))
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .body
    }
}
