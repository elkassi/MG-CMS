package com.lear.MGCMS.controller.dispatcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.dispatcher.UserZoneRepository;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.UserZoneService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
class UserZoneControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserZoneService userZoneService;
    @Autowired private UserService userService;
    @MockBean private ZoneRepository zoneRepository;
    @MockBean private UserZoneRepository userZoneRepository;
    @MockBean private DispatcherProperties properties;

    @BeforeEach
    void setUp() {
        when(properties.isEnabled()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void all_returnsPaginatedList_withJoinedFields() throws Exception {
        loginAs("ADMIN");

        Object[] row = new Object[]{
            "M123", "John", "Doe", "ZA", Zone.Category.STRICT, true, "admin", LocalDateTime.now()
        };
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(userZoneRepository.findAllActiveJoined(eq(null), eq(null), any(PageRequest.class)))
            .thenReturn(new PageImpl<Object[]>(rows, PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/userZone/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].matricule").value("M123"))
            .andExpect(jsonPath("$.content[0].firstName").value("John"))
            .andExpect(jsonPath("$.content[0].lastName").value("Doe"))
            .andExpect(jsonPath("$.content[0].zoneNom").value("ZA"))
            .andExpect(jsonPath("$.content[0].category").value("STRICT"))
            .andExpect(jsonPath("$.content[0].isDefault").value(true))
            .andExpect(jsonPath("$.content[0].assignedBy").value("admin"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void all_filtersByMatricule_andZoneNom() throws Exception {
        loginAs("PROCESS");

        when(userZoneRepository.findAllActiveJoined(eq("M123"), eq("ZA"), any(PageRequest.class)))
            .thenReturn(new PageImpl<Object[]>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/userZone/all?matricule=M123&zoneNom=ZA&page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void all_returns403_whenUnauthorizedRole() throws Exception {
        loginAs("CAD");

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.util.NestedServletException.class,
            () -> mockMvc.perform(get("/api/userZone/all")));
    }

    @Test
    void all_returns404_whenDispatcherDisabled() throws Exception {
        loginAs("ADMIN");
        when(properties.isEnabled()).thenReturn(false);

        mockMvc.perform(get("/api/userZone/all"))
            .andExpect(status().isNotFound());
    }

    @Test
    void setDefault_delegatesToAssign_withIsDefaultTrue() throws Exception {
        loginAs("ADMIN");

        // Ensure a user with matricule M123 exists in the DB so the controller
        // can resolve it via the real UserService.
        User target = userService.findByUsername("M123");
        if (target == null) {
            target = new User();
            target.setUsername("M123");
            target.setMatricule("M123");
            target.setPassword("test");
            target.setFirstName("Test");
            target.setLastName("User");
            target = userService.saveUser(target);
        }

        Zone zone = new Zone();
        zone.setNom("ZA");
        when(zoneRepository.findByObjId("ZA")).thenReturn(zone);

        mockMvc.perform(post("/api/userZone/setDefault")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricule\":\"M123\",\"zoneNom\":\"ZA\"}"))
            .andExpect(status().isOk());

        verify(userZoneService).assign(any(User.class), eq(zone), any(), eq(true));
    }

    @Test
    void setDefault_returns403_whenUnauthorizedRole() throws Exception {
        loginAs("CHEF_DE_ZONE");

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.util.NestedServletException.class,
            () -> mockMvc.perform(post("/api/userZone/setDefault")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricule\":\"M123\",\"zoneNom\":\"ZA\"}")));
    }

    @Test
    void setDefault_returns404_whenDispatcherDisabled() throws Exception {
        loginAs("ADMIN");
        when(properties.isEnabled()).thenReturn(false);

        mockMvc.perform(post("/api/userZone/setDefault")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricule\":\"M123\",\"zoneNom\":\"ZA\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void setDefault_returns400_whenMissingFields() throws Exception {
        loginAs("ADMIN");

        mockMvc.perform(post("/api/userZone/setDefault")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricule\":\"M123\"}"))
            .andExpect(status().isBadRequest());
    }
}
