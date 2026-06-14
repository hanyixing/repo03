package org.sang.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sang.bean.Role;
import org.sang.bean.User;
import org.sang.mapper.RolesMapper;
import org.sang.mapper.UserMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}, the Spring Security {@code UserDetailsService}.
 *
 * Covers the authentication entry point (loadUserByUsername) and registration,
 * which must hash the password before persisting and reject duplicate usernames.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserMapper userMapper;

    @Mock
    RolesMapper rolesMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @BeforeEach
    void setUpCurrentUser() {
        User principal = new User();
        principal.setId(1L);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loadUserByUsername_existingUser_attachesRoles() {
        User stored = new User();
        stored.setId(42L);
        stored.setUsername("admin");
        List<Role> roles = Arrays.asList(new Role(1L, "admin"));

        when(userMapper.loadUserByUsername("admin")).thenReturn(stored);
        when(rolesMapper.getRolesByUid(42L)).thenReturn(roles);

        UserDetails details = userService.loadUserByUsername("admin");

        assertSame(stored, details);
        assertSame(roles, ((User) details).getRoles());
        assertEquals("admin", details.getUsername());
    }

    @Test
    void loadUserByUsername_unknownUser_returnsBlankUserAndSkipsRoleLookup() {
        when(userMapper.loadUserByUsername("ghost")).thenReturn(null);

        UserDetails details = userService.loadUserByUsername("ghost");

        // A blank User (not null) is returned so the credential check still runs
        // and fails, rather than throwing and leaking that the user is unknown.
        assertNotNull(details);
        assertNull(details.getUsername());
        verify(rolesMapper, never()).getRolesByUid(any());
    }

    @Test
    void reg_duplicateUsername_returnsOneWithoutTouchingPassword() {
        User candidate = new User();
        candidate.setUsername("dup");
        candidate.setPassword("raw");
        when(userMapper.loadUserByUsername("dup")).thenReturn(new User());

        int result = userService.reg(candidate);

        assertEquals(1, result);
        verify(passwordEncoder, never()).encode(any());
        verify(userMapper, never()).reg(any());
    }

    @Test
    void reg_newUser_encodesPasswordEnablesAccountAndReturnsZero() {
        User candidate = new User();
        candidate.setUsername("newbie");
        candidate.setPassword("raw");

        when(userMapper.loadUserByUsername("newbie")).thenReturn(null);
        when(passwordEncoder.encode("raw")).thenReturn("HASHED");
        when(userMapper.reg(candidate)).thenReturn(1L);
        when(rolesMapper.addRoles(any(String[].class), any())).thenReturn(1);

        int result = userService.reg(candidate);

        assertEquals(0, result);
        assertEquals("HASHED", candidate.getPassword(), "raw password must be replaced by the hash");
        assertTrue(candidate.isEnabled(), "new user must be enabled");
        verify(passwordEncoder).encode("raw");
    }

    @Test
    void reg_persistenceFails_returnsTwo() {
        User candidate = new User();
        candidate.setUsername("newbie");
        candidate.setPassword("raw");

        when(userMapper.loadUserByUsername("newbie")).thenReturn(null);
        when(passwordEncoder.encode("raw")).thenReturn("HASHED");
        when(userMapper.reg(candidate)).thenReturn(0L); // insert affected no rows
        when(rolesMapper.addRoles(any(String[].class), any())).thenReturn(1);

        int result = userService.reg(candidate);

        assertEquals(2, result);
    }

    @Test
    void updateUserEmail_appliesToCurrentUser() {
        when(userMapper.updateUserEmail("me@example.com", 1L)).thenReturn(1);

        int result = userService.updateUserEmail("me@example.com");

        assertEquals(1, result);
        verify(userMapper).updateUserEmail("me@example.com", 1L);
    }
}
