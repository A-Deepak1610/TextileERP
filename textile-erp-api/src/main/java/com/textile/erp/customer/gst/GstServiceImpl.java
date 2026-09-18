package com.textile.erp.customer.gst;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GstServiceImpl implements GstService {

    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private final GstProperties gstProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public GstServiceImpl(GstProperties gstProperties) {
        this.gstProperties = gstProperties;
        this.objectMapper = new ObjectMapper();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(gstProperties.getTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(gstProperties.getTimeoutMs()));
        this.restTemplate = new RestTemplate(factory);
    }

    // Secondary constructor for testing / manual injection
    public GstServiceImpl(GstProperties gstProperties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.gstProperties = gstProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public GstLookupResponse lookupGstin(String gstin) {
        if (gstin == null || gstin.trim().isEmpty()) {
            throw new IllegalArgumentException("GSTIN cannot be blank");
        }

        String normalizedGstin = gstin.trim().toUpperCase();
        if (!GSTIN_PATTERN.matcher(normalizedGstin).matches()) {
            throw new IllegalArgumentException("Invalid GSTIN format: '" + normalizedGstin + "'. Must be 15 alphanumeric characters.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", gstProperties.getKey());
        headers.set("x-rapidapi-host", gstProperties.getHost());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String targetUrl = gstProperties.getUrl() + normalizedGstin;

        try {
            log.info("Executing GST lookup for GSTIN: {}", normalizedGstin);
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new NoSuchElementException("GST details not found for GSTIN: " + normalizedGstin);
            }

            return parseNormalizedGstData(normalizedGstin, response.getBody());

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("GSTIN not found in registry: {}", normalizedGstin);
            throw new NoSuchElementException("GST details not found for GSTIN: " + normalizedGstin);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("RapidAPI GST lookup returned HTTP status {}: {}", ex.getStatusCode(), ex.getStatusText());
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new IllegalStateException("GST Lookup service rate limit exceeded. Please try again later.");
            }
            throw new IllegalArgumentException("GST service could not verify GSTIN: " + normalizedGstin + " (" + ex.getStatusText() + ")");
        } catch (ResourceAccessException ex) {
            log.error("RapidAPI GST lookup request timed out or network error: {}", ex.getMessage());
            throw new IllegalStateException("GST Lookup service is currently unreachable or timed out. Please enter details manually.");
        } catch (Exception ex) {
            log.error("Unexpected error during GST lookup for GSTIN {}: {}", normalizedGstin, ex.getMessage());
            throw new IllegalArgumentException("Error processing GST lookup: " + ex.getMessage());
        }
    }

    private GstLookupResponse parseNormalizedGstData(String gstin, String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);

            // Handle potential nested wrapper: e.g. { "data": { ... } } or root object
            JsonNode dataNode = root.has("data") ? root.get("data") : root;

            String legalName = extractText(dataNode, "lgnm", "legal_name", "tradeName", "trade_name");
            String tradeName = extractText(dataNode, "tradeNam", "trade_name", "lgnm", "legal_name");

            // Extract principal address (pradr -> addr)
            JsonNode pradrNode = dataNode.path("pradr");
            JsonNode addrNode = pradrNode.has("addr") ? pradrNode.get("addr") : dataNode.path("addr");

            String addressLine1 = null;
            String addressLine2 = null;
            String city = null;
            String district = null;
            String state = null;
            String stateCode = gstin.substring(0, 2);
            String pincode = null;
            String country = "India";

            if (!addrNode.isMissingNode() && addrNode.isObject()) {
                String bno = extractText(addrNode, "bno", "door_no");
                String bnm = extractText(addrNode, "bnm", "building_name");
                String st = extractText(addrNode, "st", "street");
                String loc = extractText(addrNode, "loc", "location");

                StringBuilder line1 = new StringBuilder();
                if (bno != null) line1.append(bno).append(" ");
                if (bnm != null) line1.append(bnm).append(" ");
                if (st != null) line1.append(st);
                addressLine1 = line1.toString().trim();

                addressLine2 = loc;
                city = extractText(addrNode, "city", "dst", "location");
                district = extractText(addrNode, "dst", "district");
                state = extractText(addrNode, "stcd", "state");
                pincode = extractText(addrNode, "pncd", "pincode", "postal_code");
            } else {
                // If flat address structure
                addressLine1 = extractText(dataNode, "address_line1", "address");
                addressLine2 = extractText(dataNode, "address_line2");
                city = extractText(dataNode, "city");
                district = extractText(dataNode, "district");
                state = extractText(dataNode, "state");
                pincode = extractText(dataNode, "pincode");
            }

            if (legalName == null || legalName.isBlank()) {
                legalName = tradeName;
            }

            return GstLookupResponse.builder()
                    .gstin(gstin)
                    .legalName(legalName)
                    .tradeName(tradeName)
                    .addressLine1(addressLine1)
                    .addressLine2(addressLine2)
                    .city(city)
                    .district(district)
                    .state(state)
                    .stateCode(stateCode)
                    .pincode(pincode)
                    .country(country)
                    .build();

        } catch (Exception ex) {
            log.error("Failed to parse RapidAPI GST JSON: {}", ex.getMessage());
            throw new IllegalArgumentException("Unable to parse GST registry response: " + ex.getMessage());
        }
    }

    private String extractText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.hasNonNull(fieldName)) {
                String val = node.get(fieldName).asText().trim();
                if (!val.isEmpty() && !"null".equalsIgnoreCase(val)) {
                    return val;
                }
            }
        }
        return null;
    }
}
