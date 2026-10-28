package com.tdb.booking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.entity.BookingStatus;
import com.tdb.booking.entity.DarshanBooking;
import com.tdb.booking.entity.DarshanSlot;
import com.tdb.booking.entity.SlotMaster;
import com.tdb.booking.entity.TempleMaster;
import com.tdb.booking.repository.DarshanBookingRepository;
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
class DarshanBookingControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private TempleMasterRepository templeMasterRepository;

    @Autowired private SlotMasterRepository slotMasterRepository;

    @Autowired private DarshanSlotRepository darshanSlotRepository;

    @Autowired private DarshanBookingRepository darshanBookingRepository;

    private TempleMaster temple;
    private SlotMaster slotMaster1;
    private DarshanSlot activeSlot;
    private DarshanSlot limitedSlot;
    private DarshanSlot inactiveSlot;

    @BeforeEach
    void setUp() {
        darshanBookingRepository.deleteAll();
        darshanSlotRepository.deleteAll();
        slotMasterRepository.deleteAll();
        templeMasterRepository.deleteAll();

        temple =
                templeMasterRepository.save(
                        TempleMaster.builder()
                                .templeCode("TIR_001")
                                .templeName("Tirumala Temple")
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());

        slotMaster1 =
                slotMasterRepository.save(
                        SlotMaster.builder()
                                .temple(temple)
                                .slotName("Special Entry")
                                .startTime(LocalTime.of(10, 0))
                                .endTime(LocalTime.of(11, 0))
                                .defaultCapacity(10)
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());

        SlotMaster slotMaster2 =
                slotMasterRepository.save(
                        SlotMaster.builder()
                                .temple(temple)
                                .slotName("Afternoon Entry")
                                .startTime(LocalTime.of(14, 0))
                                .endTime(LocalTime.of(15, 0))
                                .defaultCapacity(2)
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());

        activeSlot =
                darshanSlotRepository.save(
                        DarshanSlot.builder()
                                .temple(temple)
                                .slotMaster(slotMaster1)
                                .slotDate(LocalDate.now().plusDays(2))
                                .capacity(10)
                                .bookedCount(0)
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());

        limitedSlot =
                darshanSlotRepository.save(
                        DarshanSlot.builder()
                                .temple(temple)
                                .slotMaster(slotMaster2)
                                .slotDate(LocalDate.now().plusDays(2))
                                .capacity(2)
                                .bookedCount(0)
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());

        inactiveSlot =
                darshanSlotRepository.save(
                        DarshanSlot.builder()
                                .temple(temple)
                                .slotMaster(slotMaster1)
                                .slotDate(LocalDate.now().plusDays(3))
                                .capacity(10)
                                .bookedCount(0)
                                .isActive(false)
                                .createdBy(UUID.randomUUID())
                                .build());
    }

    @Test
    @DisplayName("Request for 3 tickets when 10 are available - Booking succeeds")
    void bookTickets_Success() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(activeSlot.getId())
                        .devoteeName("Arun")
                        .phoneNumber("9876543210")
                        .ticketCount(3)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId", notNullValue()))
                .andExpect(jsonPath("$.darshanSlotId", is(activeSlot.getId().toString())))
                .andExpect(jsonPath("$.devoteeName", is("Arun")))
                .andExpect(jsonPath("$.phoneNumber", is("9876543210")))
                .andExpect(jsonPath("$.ticketCount", is(3)))
                .andExpect(jsonPath("$.bookingStatus", is("CONFIRMED")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName(
            "Request for 3 tickets when only 2 are available - Booking rejected (409"
                    + " INSUFFICIENT_CAPACITY)")
    void bookTickets_InsufficientCapacity_Fails() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(limitedSlot.getId())
                        .devoteeName("Arun")
                        .phoneNumber("9876543210")
                        .ticketCount(3)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("INSUFFICIENT_CAPACITY")))
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("Request with ticketCount = 0 - Validation error")
    void bookTickets_ZeroTickets_Fails() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(activeSlot.getId())
                        .devoteeName("Arun")
                        .phoneNumber("9876543210")
                        .ticketCount(0)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Request with negative ticket count - Validation error")
    void bookTickets_NegativeTickets_Fails() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(activeSlot.getId())
                        .devoteeName("Arun")
                        .phoneNumber("9876543210")
                        .ticketCount(-1)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Blank devotee name - Validation error")
    void bookTickets_BlankDevoteeName_Fails() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(activeSlot.getId())
                        .devoteeName("  ")
                        .phoneNumber("9876543210")
                        .ticketCount(2)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Invalid phone number - Validation error")
    void bookTickets_InvalidPhone_Fails() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(activeSlot.getId())
                        .devoteeName("Arun")
                        .phoneNumber("invalid-phone")
                        .ticketCount(2)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Invalid slot UUID - Validation error (400 INVALID_REQUEST)")
    void bookTickets_InvalidSlotUuid_Fails() throws Exception {
        String invalidJson =
                """
                {
                  "darshanSlotId": "invalid-uuid",
                  "devoteeName": "Arun",
                  "phoneNumber": "9876543210",
                  "ticketCount": 2
                }
                """;

        mockMvc.perform(
                        post("/api/v1/darshan-bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_REQUEST")));
    }

    @Test
    @DisplayName("Nonexistent slot - Not found (404 SLOT_NOT_FOUND)")
    void bookTickets_NonexistentSlot_Fails() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(UUID.randomUUID())
                        .devoteeName("Arun")
                        .phoneNumber("9876543210")
                        .ticketCount(2)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("SLOT_NOT_FOUND")));
    }

    @Test
    @DisplayName("Inactive slot - Booking rejected (409 SLOT_INACTIVE)")
    void bookTickets_InactiveSlot_Fails() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(inactiveSlot.getId())
                        .devoteeName("Arun")
                        .phoneNumber("9876543210")
                        .ticketCount(2)
                        .build();

        mockMvc.perform(
                        post("/api/v1/darshan-bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("SLOT_INACTIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/darshan-bookings/{bookingId} - Success")
    void getBooking_Success() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(activeSlot.getId())
                        .devoteeName("Arun")
                        .phoneNumber("9876543210")
                        .ticketCount(3)
                        .build();

        String responseJson =
                mockMvc.perform(
                                post("/api/v1/darshan-bookings")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String bookingId = objectMapper.readTree(responseJson).get("bookingId").asText();

        mockMvc.perform(get("/api/v1/darshan-bookings/{bookingId}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId", is(bookingId)))
                .andExpect(jsonPath("$.darshanSlotId", is(activeSlot.getId().toString())))
                .andExpect(jsonPath("$.devoteeName", is("Arun")))
                .andExpect(jsonPath("$.phoneNumber", is("9876543210")))
                .andExpect(jsonPath("$.ticketCount", is(3)))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/darshan-bookings/{bookingId} - Not Found")
    void getBooking_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/darshan-bookings/{bookingId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("BOOKING_NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/v1/darshan-bookings/{bookingId} - Invalid UUID")
    void getBooking_InvalidUUID() throws Exception {
        mockMvc.perform(get("/api/v1/darshan-bookings/{bookingId}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_REQUEST")));
    }

    @Test
    @DisplayName(
            "PATCH /api/v1/darshan-bookings/{bookingId}/cancel - Confirmed booking, slot not"
                    + " started -> Cancels and releases tickets")
    void cancelBooking_Success_ReleasesTickets() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(activeSlot.getId())
                        .devoteeName("Arun")
                        .phoneNumber("9876543210")
                        .ticketCount(3)
                        .build();

        String responseJson =
                mockMvc.perform(
                                post("/api/v1/darshan-bookings")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String bookingIdStr = objectMapper.readTree(responseJson).get("bookingId").asText();
        UUID bookingId = UUID.fromString(bookingIdStr);

        // Pre-cancellation check: slot capacity reflects 3 booked tickets
        DarshanSlot slotBefore = darshanSlotRepository.findById(activeSlot.getId()).orElseThrow();
        assertThat(slotBefore.getBookedCount()).isEqualTo(3);
        assertThat(slotBefore.getAvailableCount()).isEqualTo(7);

        // Perform cancellation
        mockMvc.perform(patch("/api/v1/darshan-bookings/{bookingId}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId", is(bookingIdStr)))
                .andExpect(jsonPath("$.bookingStatus", is("CANCELLED")))
                .andExpect(jsonPath("$.ticketCount", is(3)));

        // Post-cancellation check: slot bookedCount decreased back to 0, availableCount is 10
        DarshanSlot slotAfter = darshanSlotRepository.findById(activeSlot.getId()).orElseThrow();
        assertThat(slotAfter.getBookedCount()).isEqualTo(0);
        assertThat(slotAfter.getAvailableCount()).isEqualTo(10);

        // Booking record in repository is marked CANCELLED
        DarshanBooking cancelledBooking =
                darshanBookingRepository.findById(bookingId).orElseThrow();
        assertThat(cancelledBooking.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName(
            "PATCH /api/v1/darshan-bookings/{bookingId}/cancel - Booking already cancelled ->"
                + " Idempotent, returns existing cancelled booking without releasing tickets again")
    void cancelBooking_AlreadyCancelled_Idempotent() throws Exception {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(activeSlot.getId())
                        .devoteeName("Devotee Idempotent")
                        .phoneNumber("9876543210")
                        .ticketCount(4)
                        .build();

        String responseJson =
                mockMvc.perform(
                                post("/api/v1/darshan-bookings")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String bookingIdStr = objectMapper.readTree(responseJson).get("bookingId").asText();
        UUID bookingId = UUID.fromString(bookingIdStr);

        // First cancellation -> cancels and releases 4 tickets
        mockMvc.perform(patch("/api/v1/darshan-bookings/{bookingId}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus", is("CANCELLED")));

        DarshanSlot slotAfterFirstCancel =
                darshanSlotRepository.findById(activeSlot.getId()).orElseThrow();
        assertThat(slotAfterFirstCancel.getBookedCount()).isEqualTo(0);
        assertThat(slotAfterFirstCancel.getAvailableCount()).isEqualTo(10);

        // Second cancellation -> idempotent return, does not double-decrement or release tickets
        // again
        mockMvc.perform(patch("/api/v1/darshan-bookings/{bookingId}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId", is(bookingIdStr)))
                .andExpect(jsonPath("$.bookingStatus", is("CANCELLED")));

        DarshanSlot slotAfterSecondCancel =
                darshanSlotRepository.findById(activeSlot.getId()).orElseThrow();
        assertThat(slotAfterSecondCancel.getBookedCount()).isEqualTo(0);
        assertThat(slotAfterSecondCancel.getAvailableCount()).isEqualTo(10);
    }

    @Test
    @DisplayName(
            "PATCH /api/v1/darshan-bookings/{bookingId}/cancel - Slot has already started ->"
                    + " Rejects cancellation with 409 Conflict")
    void cancelBooking_SlotAlreadyStarted_RejectsWith409() throws Exception {
        // Create past slot (started yesterday)
        DarshanSlot pastSlot =
                darshanSlotRepository.save(
                        DarshanSlot.builder()
                                .temple(temple)
                                .slotMaster(slotMaster1)
                                .slotDate(LocalDate.now().minusDays(1))
                                .capacity(10)
                                .bookedCount(2)
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());

        DarshanBooking pastBooking =
                darshanBookingRepository.save(
                        DarshanBooking.builder()
                                .darshanSlot(pastSlot)
                                .devoteeName("Devotee Past")
                                .phoneNumber("9876543210")
                                .ticketCount(2)
                                .bookingStatus(BookingStatus.CONFIRMED)
                                .createdBy(UUID.randomUUID())
                                .build());

        mockMvc.perform(patch("/api/v1/darshan-bookings/{bookingId}/cancel", pastBooking.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("SLOT_ALREADY_STARTED")))
                .andExpect(jsonPath("$.status", is(409)));

        // Verify booking remains CONFIRMED and slot bookedCount remains unchanged
        DarshanBooking unalteredBooking =
                darshanBookingRepository.findById(pastBooking.getId()).orElseThrow();
        assertThat(unalteredBooking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);

        DarshanSlot unalteredSlot = darshanSlotRepository.findById(pastSlot.getId()).orElseThrow();
        assertThat(unalteredSlot.getBookedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName(
            "PATCH /api/v1/darshan-bookings/{bookingId}/cancel - Nonexistent booking ID -> Returns"
                    + " 404 Not Found")
    void cancelBooking_NotFound_Returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/darshan-bookings/{bookingId}/cancel", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("BOOKING_NOT_FOUND")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName(
            "PATCH /api/v1/darshan-bookings/{bookingId}/cancel - Invalid UUID string -> Returns 400"
                    + " Bad Request")
    void cancelBooking_InvalidUUID_Returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/darshan-bookings/{bookingId}/cancel", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.status", is(400)));
    }
}
