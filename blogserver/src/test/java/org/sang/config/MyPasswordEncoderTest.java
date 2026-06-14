package org.sang.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

import static org.junit.jupiter.api.Assertions.*;

class MyPasswordEncoderTest {

    private MyPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new MyPasswordEncoder();
    }

    // ========== encode tests ==========

    @Test
    void encode_shouldReturnMd5Hash() {
        String result = encoder.encode("password123");
        String expected = DigestUtils.md5DigestAsHex("password123".getBytes());
        assertEquals(expected, result);
    }

    @Test
    void encode_sameInput_shouldProduceSameOutput() {
        String result1 = encoder.encode("test");
        String result2 = encoder.encode("test");
        assertEquals(result1, result2);
    }

    @Test
    void encode_emptyString_shouldReturnMd5OfEmpty() {
        String result = encoder.encode("");
        String expected = DigestUtils.md5DigestAsHex("".getBytes());
        assertEquals(expected, result);
    }

    @Test
    void encode_differentInput_shouldProduceDifferentOutput() {
        String result1 = encoder.encode("password1");
        String result2 = encoder.encode("password2");
        assertNotEquals(result1, result2);
    }

    // ========== matches tests ==========

    @Test
    void matches_correctPassword_shouldReturnTrue() {
        String encoded = encoder.encode("mypassword");
        assertTrue(encoder.matches("mypassword", encoded));
    }

    @Test
    void matches_wrongPassword_shouldReturnFalse() {
        String encoded = encoder.encode("mypassword");
        assertFalse(encoder.matches("wrongpassword", encoded));
    }

    @Test
    void matches_tamperedHash_shouldReturnFalse() {
        assertFalse(encoder.matches("mypassword", "tamperedhash123"));
    }
}
