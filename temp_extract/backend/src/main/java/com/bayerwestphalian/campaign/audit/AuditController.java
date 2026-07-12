package com.bayerwestphalian.campaign.audit;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogView>>> listAuditLogs() {
        return ResponseEntity.ok(
                ApiResponse.success("Audit logs loaded", auditService.listAuditLogs()));
    }
}
