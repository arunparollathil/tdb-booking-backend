package com.tdb.booking.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tdb.booking.dto.DarshanSlotRequestDto;
import com.tdb.booking.entity.SlotMaster;
import com.tdb.booking.entity.TempleMaster;
import com.tdb.booking.repository.DarshanSlotRepository;
import com.tdb.booking.repository.SlotMasterRepository;
import com.tdb.booking.repository.TempleMasterRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DarshanSlotControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private TempleMasterRepository templeMasterRepository;

    @Autowired private SlotMasterRepository slotMasterRepository;

    @Autowired private DarshanSlotRepository darshanSlotRepository;

    private TempleMaster activeTemple;
    private SlotMaster activeSlotMaster;

    @BeforeEach
    void setUp() {
        darshanSlotRepository.deleteAll();
        slotMasterRepository.deleteAll();
        templeMasterRepository.deleteAll();

        activeTemple =
                templeMasterRepository.save(
                        TempleMaster.builder()
                                .templeCode("TMPL_001")
                                .templeName("Sri Krishna Temple")
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());

        activeSlotMaster =
                slotMasterRepository.save(
                        SlotMaster.builder()
                                .temple(activeTemple)
                                .slotName("Morning Darshan")
                                .startTime(LocalTime.of(9, 0))
                                .endTime(LocalTime.of(10, 0))
                                .defaultCapacity(10)
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());
    }

    @Test
    @DisplayName("POST /api/v1/darshan-slots - Success")
    void createSlot_Success() throws Exception {
        DarshanSlotRequestDto request =
                DarshanSlotRequestDto.builder()
                        .templeId(activeTemple.getId())
                        .slotMasterId(activeSlotMaster.getId())
                        .slotDate(LocalDate.now().plusDays(5))
                        .capacity(10)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-slots")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.templeId", is(activeTemple.getId().toString())))
                .andExpect(jsonPath("$.slotMasterId", is(activeSlotMaster.getId().toString())))
                .andExpect(jsonPath("$.slotDate", is(request.getSlotDate().toString())))
                .andExpect(jsonPath("$.startTime", is("09:00:00")))
                .andExpect(jsonPath("$.endTime", is("10:00:00")))
                .andExpect(jsonPath("$.capacity", is(10)))
                .andExpect(jsonPath("$.bookedCount", is(0)))
                .andExpect(jsonPath("$.availableCount", is(10)))
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/darshan-slots - Fails when duplicate slot for date exists")
    void createSlot_Duplicate_Fails() throws Exception {
        LocalDate date = LocalDate.now().plusDays(2);
        DarshanSlotRequestDto request =
                DarshanSlotRequestDto.builder()
                        .templeId(activeTemple.getId())
                        .slotMasterId(activeSlotMaster.getId())
                        .slotDate(date)
                        .capacity(10)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-slots")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/v1/darshan-slots")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("DUPLICATE_SLOT")));
    }

    @Test
    @DisplayName("POST /api/v1/darshan-slots - Fails when temple not found")
    void createSlot_TempleNotFound_Fails() throws Exception {
        DarshanSlotRequestDto request =
                DarshanSlotRequestDto.builder()
                        .templeId(UUID.randomUUID())
                        .slotMasterId(activeSlotMaster.getId())
                        .slotDate(LocalDate.now().plusDays(2))
                        .capacity(10)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-slots")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("TEMPLE_NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/v1/darshan-slots - Fails when validation fails")
    void createSlot_ValidationFails() throws Exception {
        DarshanSlotRequestDto request =
                DarshanSlotRequestDto.builder()
                        .templeId(null)
                        .slotMasterId(null)
                        .slotDate(LocalDate.now().minusDays(1))
                        .capacity(0)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-slots")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.details", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/darshan-slots/{slotId} - Success")
    void getSlot_Success() throws Exception {
        DarshanSlotRequestDto createRequest =
                DarshanSlotRequestDto.builder()
                        .templeId(activeTemple.getId())
                        .slotMasterId(activeSlotMaster.getId())
                        .slotDate(LocalDate.now().plusDays(3))
                        .capacity(15)
                        .build();

        String responseJson =
                mockMvc.perform(
                                post("/api/v1/darshan-slots")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(createRequest)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String slotId = objectMapper.readTree(responseJson).get("id").asText();

        mockMvc.perform(get("/api/v1/darshan-slots/{slotId}", slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(slotId)))
                .andExpect(jsonPath("$.startTime", is("09:00:00")))
                .andExpect(jsonPath("$.capacity", is(15)))
                .andExpect(jsonPath("$.availableCount", is(15)));
    }

    @Test
    @DisplayName("GET /api/v1/darshan-slots/{slotId} - Not Found")
    void getSlot_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/darshan-slots/{slotId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("SLOT_NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/v1/darshan-slots/{slotId} - Invalid UUID")
    void getSlot_InvalidUUID() throws Exception {
        mockMvc.perform(get("/api/v1/darshan-slots/{slotId}", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_REQUEST")));
    }
}
