package com.textile.erp.customer.controller;

import com.textile.erp.customer.gst.GstLookupResponse;
import com.textile.erp.customer.gst.GstService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gst")
@RequiredArgsConstructor
@Tag(name = "GST Registry Lookup", description = "Endpoints for looking up and auto-filling taxpayer information from government GST registry")
@SecurityRequirement(name = "bearerAuth")
public class GstController {

    private final GstService gstService;

    @GetMapping("/{gstin}/lookup")
    @Operation(
            summary = "Lookup GSTIN Details",
            description = "Calls the secure backend GST verification service to retrieve registered legal name, trade name, and principal place of business for auto-populating Customer or Shipping Party records. The client never calls RapidAPI directly."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GST details successfully retrieved and normalized"),
            @ApiResponse(responseCode = "400", description = "Invalid GSTIN format or bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "GSTIN not found in registry"),
            @ApiResponse(responseCode = "409", description = "Service timeout or rate limit exceeded")
    })
    public ResponseEntity<GstLookupResponse> lookupGstin(
            @Parameter(description = "15-character Indian Goods and Services Tax Identification Number", example = "33AAACG0561D1ZW")
            @PathVariable("gstin") String gstin) {
        return ResponseEntity.ok(gstService.lookupGstin(gstin));
    }
}
