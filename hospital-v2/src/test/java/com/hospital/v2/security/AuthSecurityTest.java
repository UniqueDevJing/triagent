package com.hospital.v2.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:hospital_auth;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
@AutoConfigureMockMvc
class AuthSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void anonymousCannotReadAdminApi() throws Exception {
        mvc.perform(get("/api/admin/tools"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patientCanReadOwnProfileButNotAdminOrAnotherPatient() throws Exception {
        String token = login("patient", "123456");
        mvc.perform(get("/api/patient/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("张小明"));
        mvc.perform(get("/api/admin/tools").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/patient/profile").param("patientId", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    private String login(String username, String password) throws Exception {
        String body = json.createObjectNode().put("username", username).put("password", password).toString();
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(response);
        return root.path("data").path("token").asText();
    }
}
