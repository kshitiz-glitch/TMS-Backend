package com.kshitizgaur.tms.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kshitizgaur.tms.dto.request.BidRequestDTO;
import com.kshitizgaur.tms.dto.request.BookingRequestDTO;
import com.kshitizgaur.tms.dto.request.LoadRequestDTO;
import com.kshitizgaur.tms.dto.request.TransporterRequestDTO;
import com.kshitizgaur.tms.entity.enums.WeightUnit;

/**
 * Integration tests for the complete API flow.
 * Tests the full end-to-end workflow: Create Load → Register Transporter →
 * Submit Bid → Create Booking.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private LoadRequestDTO loadRequest;
    private TransporterRequestDTO transporterRequest;

    @BeforeEach
    void setUp() {
        loadRequest = LoadRequestDTO.builder()
                .shipperId("SHIPPER001")
                .loadingCity("Mumbai")
                .unloadingCity("Delhi")
                .loadingDate(LocalDateTime.now().plusDays(5))
                .productType("Electronics")
                .weight(5000.0)
                .weightUnit(WeightUnit.KG)
                .truckType("TRAILER")
                .noOfTrucks(3)
                .build();

        transporterRequest = TransporterRequestDTO.builder()
                .companyName("ABC Transport")
                .rating(4.5)
                .availableTrucks(List.of(
                        TransporterRequestDTO.TruckCapacityDTO.builder()
                                .truckType("TRAILER")
                                .count(10)
                                .build()))
                .build();
    }

    @Test
    @DisplayName("Full workflow: Load → Transporter → Bid → Booking")
    void fullWorkflow_ShouldComplete() throws Exception {
        // 1. Create Load
        MvcResult loadResult = mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.loadingCity").value("Mumbai"))
                .andReturn();

        String loadResponse = loadResult.getResponse().getContentAsString();
        UUID loadId = UUID.fromString(objectMapper.readTree(loadResponse).get("loadId").asText());

        // 2. Register Transporter
        MvcResult transporterResult = mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transporterRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("ABC Transport"))
                .andReturn();

        String transporterResponse = transporterResult.getResponse().getContentAsString();
        UUID transporterId = UUID.fromString(objectMapper.readTree(transporterResponse).get("transporterId").asText());

        // 3. Submit Bid
        BidRequestDTO bidRequest = BidRequestDTO.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(50000.0)
                .trucksOffered(2)
                .build();

        MvcResult bidResult = mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bidRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String bidResponse = bidResult.getResponse().getContentAsString();
        UUID bidId = UUID.fromString(objectMapper.readTree(bidResponse).get("bidId").asText());

        // Verify load status changed to OPEN_FOR_BIDS
        mockMvc.perform(get("/load/" + loadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.load.status").value("OPEN_FOR_BIDS"));

        // 4. Create Booking
        BookingRequestDTO bookingRequest = BookingRequestDTO.builder()
                .bidId(bidId)
                .build();

        mockMvc.perform(post("/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.allocatedTrucks").value(2));

        // Verify transporter's trucks were deducted
        mockMvc.perform(get("/transporter/" + transporterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTrucks[0].count").value(8)); // 10 - 2 = 8
    }

    @Test
    @DisplayName("Should return 400 when creating load with invalid data")
    void createLoad_ShouldReturn400_WhenInvalidData() throws Exception {
        LoadRequestDTO invalidRequest = LoadRequestDTO.builder()
                .shipperId("") // Invalid: blank
                .loadingCity("Mumbai")
                .build();

        mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when load not found")
    void getLoad_ShouldReturn404_WhenNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/load/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return paginated loads")
    void getLoads_ShouldReturnPaginated() throws Exception {
        // Create first load
        mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isCreated());

        // Create second load
        loadRequest.setShipperId("SHIPPER002");
        mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isCreated());

        // Get paginated loads
        mockMvc.perform(get("/load")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Should filter loads by shipperId")
    void getLoads_ShouldFilterByShipperId() throws Exception {
        // Create load for SHIPPER001
        mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isCreated());

        // Create load for SHIPPER002
        loadRequest.setShipperId("SHIPPER002");
        mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isCreated());

        // Filter by SHIPPER001
        mockMvc.perform(get("/load")
                .param("shipperId", "SHIPPER001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Should cancel load successfully")
    void cancelLoad_ShouldSucceed() throws Exception {
        // Create load
        MvcResult result = mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        UUID loadId = UUID.fromString(objectMapper.readTree(response).get("loadId").asText());

        // Cancel load
        mockMvc.perform(patch("/load/" + loadId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Should return best bids sorted by score")
    void getBestBids_ShouldReturnSortedByScore() throws Exception {
        // Create load
        MvcResult loadResult = mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID loadId = UUID.fromString(
                objectMapper.readTree(loadResult.getResponse().getContentAsString()).get("loadId").asText());

        // Create first transporter (high rating)
        TransporterRequestDTO transporter1 = TransporterRequestDTO.builder()
                .companyName("Premium Transport")
                .rating(5.0)
                .availableTrucks(List.of(
                        TransporterRequestDTO.TruckCapacityDTO.builder()
                                .truckType("TRAILER")
                                .count(10)
                                .build()))
                .build();

        MvcResult t1Result = mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transporter1)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID t1Id = UUID.fromString(
                objectMapper.readTree(t1Result.getResponse().getContentAsString()).get("transporterId").asText());

        // Create second transporter (low rating)
        TransporterRequestDTO transporter2 = TransporterRequestDTO.builder()
                .companyName("Budget Transport")
                .rating(3.0)
                .availableTrucks(List.of(
                        TransporterRequestDTO.TruckCapacityDTO.builder()
                                .truckType("TRAILER")
                                .count(10)
                                .build()))
                .build();

        MvcResult t2Result = mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transporter2)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID t2Id = UUID.fromString(
                objectMapper.readTree(t2Result.getResponse().getContentAsString()).get("transporterId").asText());

        // Submit bid from high-rated transporter (higher rate)
        BidRequestDTO bid1 = BidRequestDTO.builder()
                .loadId(loadId)
                .transporterId(t1Id)
                .proposedRate(60000.0)
                .trucksOffered(1)
                .build();

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bid1)))
                .andExpect(status().isCreated());

        // Submit bid from low-rated transporter (lower rate)
        BidRequestDTO bid2 = BidRequestDTO.builder()
                .loadId(loadId)
                .transporterId(t2Id)
                .proposedRate(40000.0)
                .trucksOffered(1)
                .build();

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bid2)))
                .andExpect(status().isCreated());

        // Get best bids (should be sorted by score)
        mockMvc.perform(get("/load/" + loadId + "/best-bids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].score").exists());
    }

    @Test
    @DisplayName("Should prevent duplicate bids from same transporter")
    void submitBid_ShouldPreventDuplicates() throws Exception {
        // Create load
        MvcResult loadResult = mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID loadId = UUID.fromString(
                objectMapper.readTree(loadResult.getResponse().getContentAsString()).get("loadId").asText());

        // Create transporter
        MvcResult transporterResult = mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transporterRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID transporterId = UUID.fromString(objectMapper.readTree(transporterResult.getResponse().getContentAsString())
                .get("transporterId").asText());

        // Submit first bid
        BidRequestDTO bidRequest = BidRequestDTO.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(50000.0)
                .trucksOffered(1)
                .build();

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bidRequest)))
                .andExpect(status().isCreated());

        // Try to submit duplicate bid
        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bidRequest)))
                .andExpect(status().isConflict()); // 409 Conflict
    }

    @Test
    @DisplayName("Should prevent bidding on cancelled load")
    void submitBid_ShouldFailOnCancelledLoad() throws Exception {
        // Create load
        MvcResult loadResult = mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID loadId = UUID.fromString(
                objectMapper.readTree(loadResult.getResponse().getContentAsString()).get("loadId").asText());

        // Cancel load
        mockMvc.perform(patch("/load/" + loadId + "/cancel"))
                .andExpect(status().isOk());

        // Create transporter
        MvcResult transporterResult = mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transporterRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID transporterId = UUID.fromString(objectMapper.readTree(transporterResult.getResponse().getContentAsString())
                .get("transporterId").asText());

        // Try to submit bid on cancelled load
        BidRequestDTO bidRequest = BidRequestDTO.builder()
                .loadId(loadId)
                .transporterId(transporterId)
                .proposedRate(50000.0)
                .trucksOffered(1)
                .build();

        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bidRequest)))
                .andExpect(status().isBadRequest()); // 400 Bad Request
    }
}
