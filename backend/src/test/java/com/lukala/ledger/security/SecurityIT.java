package com.lukala.ledger.security;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukala.ledger.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.web.FilterChainProxy;

/**
 * End-to-end auth/authorization: login issues a JWT, and role rules on
 * {@code /api/v1/**} are enforced (401 anonymous, 403 for an under-privileged
 * role, 2xx for a permitted one).
 */
class SecurityIT extends AbstractPostgresIT {

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy springSecurityFilterChain;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mvc;

    private MockMvc mockMvc() {
        if (mvc == null) {
            mvc = MockMvcBuilders.webAppContextSetup(context)
                    .addFilters(springSecurityFilterChain)
                    .build();
        }
        return mvc;
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.lukala.ledger.security.dto.LoginRequest(username, password));
        String json = mockMvc().perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return node.get("accessToken").asText();
    }

    @Test
    void loginReturnsToken() throws Exception {
        mockMvc().perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.lukala.ledger.security.dto.LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc().perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCannotWrite() throws Exception {
        String token = login("viewer", "viewer123");
        mockMvc().perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"type\":\"ASSET\",\"currency\":\"USD\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void serviceCanRead() throws Exception {
        String token = login("service", "service123");
        mockMvc().perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
