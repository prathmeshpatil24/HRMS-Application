package com.hrms.security.jwt;

import com.hrms.security.user.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6351655468576D5A7134743777217A25432A");
        jwtProperties.setExpirationMs(3600000L); // 1 hour
        jwtProperties.setIssuer("hrms-test");

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate valid JWT token and parse username & claims correctly")
    void generateAndValidateToken() {
        CustomUserDetails userDetails = CustomUserDetails.builder()
                .id(10L)
                .firstName("Alice")
                .lastName("Smith")
                .username("alicesmith")
                .email("alice@hrms.com")
                .password("secret")
                .active(true)
                .accountNonLocked(true)
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("USER_READ")
                ))
                .build();

        String token = jwtTokenProvider.generateTokenForUserDetails(userDetails);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("alicesmith", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals(10L, jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void validateToken_InvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.string"));
    }
}
