package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/segments")
public class SegmentController {

    private final SegmentService segmentService;

    public SegmentController(SegmentService segmentService) {
        this.segmentService = segmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SegmentView>>> listSegments(
            @Valid @ModelAttribute SegmentSearchRequest searchRequest) {
        List<SegmentView> segments = segmentService.searchSegments(searchRequest.toCriteria());

        return ResponseEntity.ok(ApiResponse.success("Segments loaded", segments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SegmentView>> getSegment(@PathVariable UUID id) {
        SegmentView segment = segmentService.findById(id);

        return ResponseEntity.ok(ApiResponse.success("Segment loaded", segment));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SegmentView>> createSegment(
            @Valid @RequestBody CreateSegmentRequest request) {
        SegmentView segment = segmentService.createSegment(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Segment created", segment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SegmentView>> updateSegment(
            @PathVariable UUID id, @Valid @RequestBody UpdateSegmentRequest request) {
        SegmentView segment = segmentService.updateSegment(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Segment updated", segment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSegment(@PathVariable UUID id) {
        segmentService.deleteSegment(id);

        return ResponseEntity.ok(ApiResponse.success("Segment deleted", null));
    }

    /**
     * Audience preview with eligibility applied (KB FR-054 / FR-055 / FR-079, item 208).
     *
     * <p>This is the only segment HTTP endpoint that returns contactable audience members. There is
     * intentionally no REST endpoint for criteria-only {@code findMatchingCustomers}; criteria
     * matches alone are never exposed as a final campaign audience without eligibility checks.
     */
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<SegmentPreviewView>> previewSegment(
            @Valid @RequestBody SegmentPreviewRequest request) {
        SegmentPreviewView preview = segmentService.previewSegment(request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Segment preview loaded", preview));
    }
}