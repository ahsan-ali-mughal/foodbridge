package com.foodbridge.document.controller;

import com.foodbridge.document.dto.ReceiptResponse;
import com.foodbridge.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Donation receipt / impact certificate retrieval")
public class ReceiptController {

    private final DocumentService documentService;

    public ReceiptController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{claimId}/receipt")
    @Operation(summary = "Fetch a claim's receipt status and (if generated) a pre-signed download URL")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable Long claimId) {
        return ResponseEntity.ok(documentService.getByClaimId(claimId));
    }
}
