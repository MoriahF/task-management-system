package com.taskmanagement.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class JwtTokenValidator {

    private final String userPoolId;
    private final String region;
    private final String issuerUri;
    private final JwkProvider jwkProvider;

    public JwtTokenValidator(
            @Value("${aws.cognito.user-pool-id}") String userPoolId,
            @Value("${aws.cognito.region}") String region) throws Exception {

        this.userPoolId = userPoolId;
        this.region = region;
        this.issuerUri = String.format("https://cognito-idp.%s.amazonaws.com/%s", region, userPoolId);

        String jwksUrl = issuerUri + "/.well-known/jwks.json";
        this.jwkProvider = new JwkProviderBuilder(new URL(jwksUrl))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build();

        log.info("JWT Token Validator initialized for issuer: {}", issuerUri);
    }

    public DecodedJWT validateToken(String token) throws Exception {
        log.debug("Validating JWT token");

        DecodedJWT unverifiedJwt = JWT.decode(token);
        String keyId = unverifiedJwt.getKeyId();

        if (keyId == null) {
            throw new IllegalArgumentException("Token does not contain key ID");
        }

        Jwk jwk = jwkProvider.get(keyId);
        RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

        Algorithm algorithm = Algorithm.RSA256(new RSAKeyProvider() {
            @Override
            public RSAPublicKey getPublicKeyById(String keyId) {
                return publicKey;
            }

            @Override
            public RSAPrivateKey getPrivateKey() {
                return null; 
            }

            @Override
            public String getPrivateKeyId() {
                return null; 
            }
        });

        DecodedJWT verifiedJwt = JWT.require(algorithm)
                .withIssuer(issuerUri)
                .build()
                .verify(token);

        log.debug("Token validated successfully for subject: {}", verifiedJwt.getSubject());
        return verifiedJwt;
    }

    public String getSubject(DecodedJWT jwt) {
        return jwt.getSubject();
    }
    
    public String getEmail(DecodedJWT jwt) {
        String email = jwt.getClaim("email").asString();
        if (email == null || email.isEmpty()) {
            email = jwt.getClaim("cognito:username").asString();
        }
        return email;
    }
    
    public String getRole(DecodedJWT jwt) {
        String customRole = jwt.getClaim("custom:role").asString();
        if (customRole != null && !customRole.isEmpty()) {
            return customRole;
        }

        var groups = jwt.getClaim("cognito:groups").asList(String.class);
        if (groups != null && !groups.isEmpty()) {
            return groups.get(0).toUpperCase();
        }
        return "USER";
    }

    public String getName(DecodedJWT jwt) {
        String name = jwt.getClaim("name").asString();
        if (name == null || name.isEmpty()) {
            name = jwt.getClaim("cognito:username").asString();
        }
        if (name == null || name.isEmpty()) {
            String email = jwt.getClaim("email").asString();
            if (email != null && email.contains("@")) {
                name = email.substring(0, email.indexOf("@"));
            }
        }
        return name;
    }
}