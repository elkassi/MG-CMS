package com.lear.MGCMS.controller.dispatcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

import com.lear.MGCMS.services.OrdonnancementService;
import com.lear.MGCMS.services.dispatcher.ContinuousDispatchOptimizerService;
import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.EngineMode;
import com.lear.MGCMS.services.dispatcher.EngineState;
import com.lear.MGCMS.services.dispatcher.LiveChargeDto;
import com.lear.MGCMS.services.dispatcher.LiveChargeService;
import com.lear.MGCMS.services.dispatcher.ZoneLoadDto;
import com.lear.MGCMS.services.dispatcher.ZoneLoadService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
class WorkbenchControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DispatcherProperties dispatcherProperties;
    @MockBean private LiveChargeService liveChargeService;
    @MockBean private ZoneLoadService zoneLoadService;
    @MockBean private OrdonnancementService ordonnancementService;
    @MockBean private ContinuousDispatchOptimizerService optimizerService;

    @BeforeEach
    void setUp() {
        when(dispatcherProperties.isEnabled()).thenReturn(true);
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
    void data_returns200_withFourTopLevelKeys_whenAuthorized() throws Exception {
        loginAs("PROCESS");

        LiveChargeDto liveCharge = new LiveChargeDto(
            LocalDateTime.now(), LocalDate.now(), 2, 460,
            new LiveChargeDto.TotalsDto(0, 0, 0, 0, 0.0, 0.0),
            Collections.emptyList(), Collections.emptyList());
        when(liveChargeService.compute()).thenReturn(liveCharge);

        ZoneLoadDto zoneLoad = new ZoneLoadDto(
            LocalDate.now(), 2, Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(),
            new ZoneLoadDto.EquilibreSummaryDto(
                0.0, null, 0.0, 0.0, null, null, "GREEN", "GREEN", Collections.emptyMap()),
            new ZoneLoadDto.ThresholdsDto(80.0, 95.0, 10.0, 20.0, 15.0, 30.0));
        when(zoneLoadService.computeMatrix(any(LocalDate.class), anyInt())).thenReturn(zoneLoad);

        Map<String, Object> gantt = new LinkedHashMap<>();
        gantt.put("tasks", Collections.emptyList());
        when(ordonnancementService.getTimelineData(anyInt(), anyInt(), anyList())).thenReturn(gantt);

        when(optimizerService.getState()).thenReturn(EngineState.IDLE);
        when(optimizerService.getMode()).thenReturn(EngineMode.CONTINUOUS);
        when(optimizerService.getCurrentRunId()).thenReturn(null);
        when(optimizerService.getIteration()).thenReturn(0);
        when(optimizerService.getCurrentSpread()).thenReturn(0.0);
        when(optimizerService.getCurrentRawSpread()).thenReturn(0.0);
        when(optimizerService.getCurrentStdDev()).thenReturn(0.0);
        when(optimizerService.getCurrentMedian()).thenReturn(0.0);
        when(optimizerService.getInitialSpread()).thenReturn(0.0);
        when(optimizerService.getLastImprovement()).thenReturn(0.0);

        mockMvc.perform(get("/api/workbench/data?date=2026-05-07&shift=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.liveCharge").exists())
            .andExpect(jsonPath("$.zoneLoad").exists())
            .andExpect(jsonPath("$.gantt").exists())
            .andExpect(jsonPath("$.engineState").exists())
            .andExpect(jsonPath("$.engineState.state").value("IDLE"))
            .andExpect(jsonPath("$.engineState.mode").value("CONTINUOUS"));
    }

    @Test
    void data_returns404_whenDispatcherDisabled() throws Exception {
        loginAs("PROCESS");
        when(dispatcherProperties.isEnabled()).thenReturn(false);

        mockMvc.perform(get("/api/workbench/data?date=2026-05-07&shift=2"))
            .andExpect(status().isNotFound());
    }

    @Test
    void data_returns403_whenUnauthorizedRole() throws Exception {
        loginAs("CAD");

        // With addFilters=false, @PreAuthorize throws AccessDeniedException
        // instead of translating to HTTP 403. We assert the exception type.
        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.util.NestedServletException.class,
            () -> mockMvc.perform(get("/api/workbench/data?date=2026-05-07&shift=2"))
        );
    }

    @Test
    void data_returns400_whenShiftOutOfRange() throws Exception {
        loginAs("PROCESS");

        mockMvc.perform(get("/api/workbench/data?date=2026-05-07&shift=5"))
            .andExpect(status().isBadRequest());
    }
}
