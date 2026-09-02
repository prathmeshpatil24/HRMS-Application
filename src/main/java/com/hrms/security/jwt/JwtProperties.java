package com.hrms.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.security.jwt")
public class JwtProperties {

    /**
     * Secret key for signing JWT tokens (HMAC-SHA256/512).
     */
    private String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6351655468576D5A7134743777217A25432A";

    /**
     * Access token expiration time in milliseconds (e.g., 24 hours = 86400000 ms).
     */
    private long expirationMs = 86400000L;

    /**
     * Refresh token expiration time in milliseconds (e.g., 7 days = 604800000 ms).
     */
    private long refreshTokenExpirationMs = 604800000L;

    /**
     * Token issuer identification.
     */
    private String issuer = "hrms-application";
}
