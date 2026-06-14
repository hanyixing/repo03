package org.sang.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MyPasswordEncoder}, the credential encoder wired into
 * Spring Security's authentication. The encoder must produce a stable MD5 hash
 * and only report a match for the correct raw password.
 */
class MyPasswordEncoderTest {

    private MyPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new MyPasswordEncoder();
    }

    @Test
    void encode_producesKnownMd5Hash() {
        // MD5("123") is a well-known constant; this pins the algorithm.
        assertEquals("202cb962ac59075b964b07152d234b70", encoder.encode("123"));
    }

    @Test
    void encode_outputIsThirtyTwoCharHex() {
        String hash = encoder.encode("any-password");
        assertEquals(32, hash.length());
        assertTrue(hash.matches("[0-9a-f]{32}"));
    }

    @Test
    void encode_isDeterministic() {
        assertEquals(encoder.encode("secret"), encoder.encode("secret"));
    }

    @Test
    void matches_correctPassword_returnsTrue() {
        String encoded = encoder.encode("secret");
        assertTrue(encoder.matches("secret", encoded));
    }

    @Test
    void matches_wrongPassword_returnsFalse() {
        String encoded = encoder.encode("secret");
        assertFalse(encoder.matches("wrong", encoded));
    }
}
