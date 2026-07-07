package com.lear.MGCMS.controller.logistics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.logistics.LogisticsReleaseService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
class LogisticsReleaseControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DispatcherProperties dispatcherProperties;
    @MockBean private LogisticsReleaseService logisticsReleaseService;

    @BeforeEach
    void setUp() {
        when(dispatcherProperties.isEnabled()).thenReturn(true);
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("sequences", Collections.emptyList());
        sample.put("totals", Collections.emptyMap());
        when(logisticsReleaseService.build(any(LocalDate.class), anyInt())).thenReturn(sample);
        Map<String, Object> commit = new LinkedHashMap<>();
        commit.put("success", true);
        commit.put("picklistId", "PL-test");
        commit.put("allocationCount", 1);
        when(logisticsReleaseService.commit(any(LocalDate.class), anyInt(), anyList(), any()))
                .thenReturn(commit);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "user", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void candidates_returns200_whenAuthorized() throws Exception {
        loginAs("VALID_QN_LOGISTIQUE");
        mockMvc.perform(get("/api/logistics/release/candidates?date=2026-06-01&shift=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sequences").exists())
            .andExpect(jsonPath("$.totals").exists());
    }

    @Test
    void candidates_returns404_whenDisabled() throws Exception {
        loginAs("PROCESS");
        when(dispatcherProperties.isEnabled()).thenReturn(false);
        mockMvc.perform(get("/api/logistics/release/candidates?date=2026-06-01&shift=2"))
            .andExpect(status().isNotFound());
    }

    @Test
    void candidates_returns400_whenShiftOutOfRange() throws Exception {
        loginAs("PROCESS");
        mockMvc.perform(get("/api/logistics/release/candidates?date=2026-06-01&shift=5"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void candidates_forbiddenRole_throws() throws Exception {
        loginAs("CAD");
        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.util.NestedServletException.class,
            () -> mockMvc.perform(get("/api/logistics/release/candidates?date=2026-06-01&shift=2"))
        );
    }

    @Test
    void commit_returnsPicklist_whenAuthorized() throws Exception {
        loginAs("VALID_QN_LOGISTIQUE");
        mockMvc.perform(post("/api/logistics/release/commit")
                .contentType("application/json")
                .content("{\"date\":\"2026-06-01\",\"shift\":2,\"sequences\":[\"SEQ1\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.picklistId").value("PL-test"))
            .andExpect(jsonPath("$.allocationCount").value(1));
    }
}
