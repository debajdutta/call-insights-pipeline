package com.callinsights.gatewayservice.web;

import com.callinsights.gatewayservice.model.dto.AuditEntryResponse;
import com.callinsights.gatewayservice.model.dto.CallDetailResponse;
import com.callinsights.gatewayservice.model.dto.CallSummaryResponse;
import com.callinsights.gatewayservice.model.dto.RegenerateRequest;
import com.callinsights.gatewayservice.security.JwtAuthFilter;
import com.callinsights.gatewayservice.service.ArtifactActionService;
import com.callinsights.gatewayservice.service.CatalogQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CatalogQueryService catalogQueryService;
    private final ArtifactActionService artifactActionService;

    public CallController(CatalogQueryService catalogQueryService, ArtifactActionService artifactActionService) {
        this.catalogQueryService = catalogQueryService;
        this.artifactActionService = artifactActionService;
    }

    @GetMapping
    public List<CallSummaryResponse> listCalls() {
        return catalogQueryService.listCalls();
    }

    @GetMapping("/{callId}")
    public CallDetailResponse getCallDetail(@PathVariable String callId) {
        return catalogQueryService.getCallDetail(callId)
                .orElseThrow(() -> new NotFoundException("No such call: " + callId));
    }

    @GetMapping("/{callId}/audit")
    public List<AuditEntryResponse> getAuditLog(@PathVariable String callId) {
        return catalogQueryService.getAuditLog(callId);
    }

    @PostMapping("/{callId}/artifacts/{artifactType}/regenerate")
    public ResponseEntity<Map<String, String>> regenerate(@PathVariable String callId,
                                                            @PathVariable String artifactType,
                                                            @RequestBody RegenerateRequest request,
                                                            HttpServletRequest httpRequest) {
        String requestedBy = authenticatedUsername(httpRequest);
        artifactActionService.regenerate(callId, artifactType, request.model(), requestedBy);
        return ResponseEntity.accepted().body(Map.of(
                "status", "regeneration-requested", "callId", callId, "artifactType", artifactType));
    }

    @DeleteMapping("/{callId}/artifacts/{artifactType}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String callId,
                                                        @PathVariable String artifactType,
                                                        HttpServletRequest httpRequest) {
        String deletedBy = authenticatedUsername(httpRequest);
        int deletedVersion = artifactActionService.delete(callId, artifactType, deletedBy);
        return ResponseEntity.ok(Map.of(
                "status", "deleted", "callId", callId, "artifactType", artifactType, "deletedVersion", deletedVersion));
    }

    private String authenticatedUsername(HttpServletRequest request) {
        return (String) request.getAttribute(JwtAuthFilter.AUTHENTICATED_USERNAME_ATTRIBUTE);
    }
}
