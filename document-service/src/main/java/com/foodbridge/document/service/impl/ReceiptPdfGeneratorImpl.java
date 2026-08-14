package com.foodbridge.document.service.impl;

import com.foodbridge.document.service.ReceiptPdfGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;

/**
 * Renders a simple, single-page donation receipt / tax-deduction certificate
 * PDF using Apache PDFBox 3.x (a stable, actively maintained, non-vulnerable
 * release line — PDFBox 2.x's known CVEs do not affect 3.x).
 */
@Component
@Slf4j
public class ReceiptPdfGeneratorImpl implements ReceiptPdfGenerator {

    @Override
    public byte[] generate(Long claimId, String listingId, Long donorId, String deliveredAtIso) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                var titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                var bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                float margin = 60f;
                float y = page.getMediaBox().getHeight() - margin;

                content.beginText();
                content.setFont(titleFont, 20);
                content.newLineAtOffset(margin, y);
                content.showText("FoodBridge Donation Receipt");
                content.endText();

                y -= 40;
                content.beginText();
                content.setFont(bodyFont, 12);
                content.newLineAtOffset(margin, y);
                content.showText("Claim ID: " + claimId);
                content.newLineAtOffset(0, -20);
                content.showText("Listing ID: " + listingId);
                content.newLineAtOffset(0, -20);
                content.showText("Donor ID: " + donorId);
                content.newLineAtOffset(0, -20);
                content.showText("Delivered at: " + deliveredAtIso);
                content.newLineAtOffset(0, -20);
                content.showText("Issued at: " + Instant.now());
                content.newLineAtOffset(0, -40);
                content.showText("Thank you for your donation through FoodBridge.");
                content.endText();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            log.info("Generated receipt PDF for claimId={} ({} bytes)", claimId, outputStream.size());
            return outputStream.toByteArray();
        } catch (IOException ex) {
            // PDFBox failures are wrapped as unchecked so callers (the SQS job poller) can apply
            // a single uniform catch/retry-via-redelivery policy, consistent with every other
            // failure mode in that poller (see ReceiptGenerationJobPoller).
            throw new UncheckedIOException("Failed to generate receipt PDF for claimId=" + claimId, ex);
        }
    }
}
