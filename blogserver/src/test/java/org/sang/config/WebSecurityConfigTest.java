package org.sang.config;

import org.junit.jupiter.api.Test;
import org.sang.bean.Role;
import org.sang.bean.User;
import org.sang.mapper.*;
import org.sang.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ArticleMapper articleMapper;

    @MockBean
    private CategoryMapper categoryMapper;

    @MockBean
    private TagsMapper tagsMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private RolesMapper rolesMapper;

    @Test
    void loginPage_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/login_page"))
                .andExpect(status().isOk());
    }

    @Test
    void login_badCredentials_shouldReturnFailure() throws Exception {
        User user = new User();
        user.setUsername("baduser");
        user.setPassword("some_other_encoded_password");
        user.setEnabled(true);
        user.setRoles(new ArrayList<>());
        when(userService.loadUserByUsername("baduser")).thenReturn(user);

        mockMvc.perform(post("/login")
                        .param("username", "baduser")
                        .param("password", "badpass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.msg").value("登录失败"));
    }

    @Test
    void logout_shouldBeAccessible() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void staticResources_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedAccess_shouldRedirectToLoginPage() throws Exception {
        mockMvc.perform(get("/admin/article/all"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login_page"));
    }

    @Test
    void unauthenticatedAccess_anyRequest_shouldRedirect() throws Exception {
        mockMvc.perform(get("/some/protected/resource"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login_page"));
    }

    @Test
    void nonAdminAccessingAdminEndpoint_shouldGet403() throws Exception {
        User nonAdmin = createUserWithRole("user", "普通用户");

        mockMvc.perform(get("/admin/users")
                        .with(user(nonAdmin)))
                .andExpect(status().isForbidden());
    }

    private User createUserWithRole(String username, String roleName) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword("encoded_password");
        user.setEnabled(true);
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(1L, roleName));
        user.setRoles(roles);
        return user;
    }
}
