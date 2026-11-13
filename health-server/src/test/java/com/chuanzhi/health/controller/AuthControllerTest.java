package com.chuanzhi.health.controller;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");

        ResponseEntity<Result> res = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login", req, Result.class);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(200, res.getBody().getCode());
    }

    @Test
    void shouldRejectWrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrong");

        ResponseEntity<Result> res = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login", req, Result.class);

        // Either 400 (BusinessException) or 401 (Spring Security) depending on implementation
        assertTrue(res.getStatusCode() == HttpStatus.UNAUTHORIZED || res.getStatusCode() == HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() {
        ResponseEntity<String> res = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/dashboard/stats", String.class);

        assertTrue(res.getStatusCode() == HttpStatus.FORBIDDEN || res.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }
}
