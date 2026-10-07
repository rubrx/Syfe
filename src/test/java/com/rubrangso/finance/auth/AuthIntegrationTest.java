package com.rubrangso.finance.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rubrangso.finance.auth.dto.LoginRequest;
import com.rubrangso.finance.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full session lifecycle: register → login → protected call with cookie → logout → 401.
 * Uses real Spring context and H2 in-memory database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("full session lifecycle: register → login → authenticated call → logout → 401")
    void sessionLifecycle() throws Exception {
        // Register
        var registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Bob", "bob@example.com", "password1", null));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("bob@example.com"));

        // Login — capture the mock session (MockMvc simulates session; Tomcat would set JSESSIONID cookie)
        var loginBody = objectMapper.writeValueAsString(
                new LoginRequest("bob@example.com", "password1"));

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // Authenticated request using the session
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // After logout the session is invalidated — next request returns 401
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("unauthenticated request to protected endpoint returns 401 JSON")
    void protectedEndpoint_unauthenticated_returns401Json() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("duplicate registration returns 409")
    void register_duplicate_returns409() throws Exception {
        var body = objectMapper.writeValueAsString(
                new RegisterRequest("Alice", "alice@example.com", "password1", null));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
