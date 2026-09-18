package com.textile.erp.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.textile.erp.auth.service.JwtService;
import com.textile.erp.customer.gst.GstLookupResponse;
import com.textile.erp.customer.gst.GstProperties;
import com.textile.erp.customer.gst.GstServiceImpl;
import com.textile.erp.user.entity.RoleName;
import java.lang.reflect.Field;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class GstLookupIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private GstProperties gstProperties;

    @Autowired
    private GstServiceImpl gstService;

    private MockMvc mockMvc;
    private String userToken;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userToken = jwtService.generateAccessToken(
                UUID.randomUUID(), UUID.randomUUID(), "sales@test.com", Set.of(RoleName.TENANT_ADMIN));

        // Extract RestTemplate from GstServiceImpl to attach MockRestServiceServer
        Field restTemplateField = GstServiceImpl.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) restTemplateField.get(gstService);

        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DisplayName("Should normalize RapidAPI GST response into GstLookupResponse")
    void shouldNormalizeRapidApiResponse() {
        String gstin = "24AAACH7409R1ZZ";
        String mockRapidApiResponse = """
            {
                "flag": true,
                "message": "GSTIN verified successfully",
                "data": {
                    "legal_name": "RELIANCE INDUSTRIES LIMITED",
                    "trade_name": "RELIANCE TEXTILES",
                    "status": "Active",
                    "pradr": {
                        "addr": {
                            "bno": "3rd Floor, Maker Chambers IV",
                            "st": "Nariman Point",
                            "loc": "South Mumbai",
                            "city": "Mumbai",
                            "dst": "Mumbai",
                            "stcd": "Maharashtra",
                            "pncd": "400021"
                        }
                    }
                }
            }
            """;

        mockServer.expect(requestTo("https://gst-return-status.p.rapidapi.com/free/gstin/" + gstin))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-rapidapi-host", gstProperties.getHost()))
                .andRespond(withSuccess(mockRapidApiResponse, MediaType.APPLICATION_JSON));

        GstLookupResponse result = gstService.lookupGstin(gstin);

        assertThat(result.getGstin()).isEqualTo(gstin);
        assertThat(result.getLegalName()).isEqualTo("RELIANCE INDUSTRIES LIMITED");
        assertThat(result.getTradeName()).isEqualTo("RELIANCE TEXTILES");
        assertThat(result.getCity()).isEqualTo("Mumbai");
        assertThat(result.getState()).isEqualTo("Maharashtra");
        assertThat(result.getPincode()).isEqualTo("400021");
        assertThat(result.getAddressLine1()).isEqualTo("3rd Floor, Maker Chambers IV Nariman Point");
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when RapidAPI returns 404 or inactive")
    void shouldHandleGstinNotFound() {
        String gstin = "24AAACH7409R1ZZ";
        mockServer.expect(requestTo("https://gst-return-status.p.rapidapi.com/free/gstin/" + gstin))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> gstService.lookupGstin(gstin))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Should require authentication for GST lookup endpoint")
    void shouldRequireAuthForGstEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/gst/24AAACH7409R1ZZ/lookup"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject invalid GSTIN syntax with 400 Bad Request")
    void shouldRejectInvalidGstinSyntax() throws Exception {
        mockMvc.perform(get("/api/v1/gst/INVALID_GSTIN_123/lookup")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
