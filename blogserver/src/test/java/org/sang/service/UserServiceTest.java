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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RolesMapper rolesMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("testuser");
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(1L, "admin"));
        currentUser.setRoles(roles);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    // ========== loadUserByUsername tests ==========

    @Test
    void loadUserByUsername_userFound_shouldReturnUserWithRoles() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encoded");
        when(userMapper.loadUserByUsername("admin")).thenReturn(user);

        List<Role> roles = Arrays.asList(new Role(1L, "超级管理员"));
        when(rolesMapper.getRolesByUid(1L)).thenReturn(roles);

        UserDetails result = userService.loadUserByUsername("admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        User resultUser = (User) result;
        assertEquals(1, resultUser.getRoles().size());
        assertEquals("超级管理员", resultUser.getRoles().get(0).getName());
    }

    @Test
    void loadUserByUsername_userNotFound_shouldReturnEmptyUser() {
        when(userMapper.loadUserByUsername("unknown")).thenReturn(null);

        UserDetails result = userService.loadUserByUsername("unknown");

        assertNotNull(result);
        assertNull(result.getUsername());
    }

    @Test
    void loadUserByUsername_multipleRoles() {
        User user = new User();
        user.setId(2L);
        user.setUsername("multi");
        user.setPassword("pass");
        when(userMapper.loadUserByUsername("multi")).thenReturn(user);

        List<Role> roles = Arrays.asList(
                new Role(1L, "超级管理员"),
                new Role(2L, "普通用户")
        );
        when(rolesMapper.getRolesByUid(2L)).thenReturn(roles);

        UserDetails result = userService.loadUserByUsername("multi");

        User resultUser = (User) result;
        assertEquals(2, resultUser.getRoles().size());
        assertEquals(2, resultUser.getAuthorities().size());
    }

    // ========== reg tests ==========

    @Test
    void reg_success_shouldReturnZero() {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("password123");

        when(userMapper.loadUserByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userMapper.reg(any(User.class))).thenReturn(1L);
        when(rolesMapper.addRoles(any(), any())).thenReturn(1);

        int result = userService.reg(user);

        assertEquals(0, result);
        assertTrue(user.isEnabled());
        assertEquals("encoded_password", user.getPassword());
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void reg_duplicateUsername_shouldReturnOne() {
        User user = new User();
        user.setUsername("existing");
        when(userMapper.loadUserByUsername("existing")).thenReturn(new User());

        int result = userService.reg(user);

        assertEquals(1, result);
        verify(userMapper, never()).reg(any());
    }

    @Test
    void reg_roleInsertionFailure_shouldReturnTwo() {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("pass");

        when(userMapper.loadUserByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userMapper.reg(any(User.class))).thenReturn(1L);
        when(rolesMapper.addRoles(any(), any())).thenReturn(0);

        int result = userService.reg(user);

        assertEquals(2, result);
    }

    @Test
    void reg_userInsertionFailure_shouldReturnTwo() {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("pass");

        when(userMapper.loadUserByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userMapper.reg(any(User.class))).thenReturn(0L);
        when(rolesMapper.addRoles(any(), any())).thenReturn(1);

        int result = userService.reg(user);

        assertEquals(2, result);
    }

    // ========== updateUserEmail tests ==========

    @Test
    void updateUserEmail_shouldDelegate() {
        when(userMapper.updateUserEmail("test@test.com", 1L)).thenReturn(1);

        int result = userService.updateUserEmail("test@test.com");

        assertEquals(1, result);
        verify(userMapper).updateUserEmail("test@test.com", 1L);
    }

    // ========== getUserByNickname tests ==========

    @Test
    void getUserByNickname_shouldDelegateToMapper() {
        User user = new User();
        user.setNickname("admin");
        when(userMapper.getUserByNickname("admin")).thenReturn(Collections.singletonList(user));

        List<User> result = userService.getUserByNickname("admin");

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getNickname());
    }

    // ========== getAllRole tests ==========

    @Test
    void getAllRole_shouldReturnAllRoles() {
        List<Role> roles = Arrays.asList(
                new Role(1L, "超级管理员"),
                new Role(2L, "普通用户")
        );
        when(userMapper.getAllRole()).thenReturn(roles);

        List<Role> result = userService.getAllRole();

        assertEquals(2, result.size());
    }

    // ========== updateUserEnabled tests ==========

    @Test
    void updateUserEnabled_shouldDelegate() {
        when(userMapper.updateUserEnabled(true, 1L)).thenReturn(1);

        int result = userService.updateUserEnabled(true, 1L);

        assertEquals(1, result);
    }

    // ========== deleteUserById tests ==========

    @Test
    void deleteUserById_shouldDelegate() {
        when(userMapper.deleteUserById(1L)).thenReturn(1);

        int result = userService.deleteUserById(1L);

        assertEquals(1, result);
    }

    // ========== updateUserRoles tests ==========

    @Test
    void updateUserRoles_shouldDeleteAndSet() {
        Long[] rids = {1L, 2L};
        when(userMapper.deleteUserRolesByUid(1L)).thenReturn(1);
        when(userMapper.setUserRoles(rids, 1L)).thenReturn(2);

        int result = userService.updateUserRoles(rids, 1L);

        assertEquals(2, result);
        verify(userMapper).deleteUserRolesByUid(1L);
        verify(userMapper).setUserRoles(rids, 1L);
    }

    // ========== getUserById tests ==========

    @Test
    void getUserById_shouldReturnUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        when(userMapper.getUserById(1L)).thenReturn(user);

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("test", result.getUsername());
    }
}
