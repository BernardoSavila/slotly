package com.bernardoavila.slotly.resource;

import com.bernardoavila.slotly.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class ResourceControllerTest extends PostgresTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ResourceRepository resourceRepository;

    private Long existingId;

    @BeforeEach
    void setUp() {
        existingId = resourceRepository.save(new Resource("Sala Azul", "Com projetor")).getId();
    }

    @Test
    @DisplayName("POST creates a resource and returns 201")
    void createsResource() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"name":"Sala Verde","description":"Sem projetor"}
                                 """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Sala Verde"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST with a blank name returns 400 naming the field")
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"name":"","description":"no name"}
                                 """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("name is required"));
    }

    @Test
    @DisplayName("GET by id returns the resource")
    void findsById() throws Exception {
        mockMvc.perform(get("/api/resources/{id}", existingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sala Azul"));
    }

    @Test
    @DisplayName("GET with an unknown id returns 404")
    void returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/resources/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    @DisplayName("DELETE returns 204 and removes the resource")
    void deletesResource() throws Exception {
        mockMvc.perform(delete("/api/resources/{id}", existingId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/resources/{id}", existingId))
                .andExpect(status().isNotFound());
    }
}
