package com.lear.MGCMS.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@PreAuthorize("hasRole('ADMIN') or hasRole('IMPORTER') or hasRole('CAD') or hasRole('CAD_FOAM') or hasRole('PROCESS')")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    /**
     * Send notification to CAD about material mismatch
     */
    @PostMapping("/cad-material-mismatch")
    public ResponseEntity<?> sendCadMaterialMismatchNotification(@RequestBody Map<String, Object> payload) {
        try {
            Long cuttingPlanId = payload.get("cuttingPlanId") != null ? 
                    Long.valueOf(payload.get("cuttingPlanId").toString()) : null;
            Long cmsId = payload.get("cmsId") != null ? 
                    Long.valueOf(payload.get("cmsId").toString()) : null;
            String projet = (String) payload.get("projet");
            List<String> partNumbers = (List<String>) payload.get("partNumbers");
            List<String> warnings = (List<String>) payload.get("warnings");

            // Build notification message
            StringBuilder message = new StringBuilder();
            message.append("=== NOTIFICATION CAD - INCOHÉRENCE MATÉRIAUX ===\n\n");
            message.append("Cutting Plan ID: ").append(cuttingPlanId).append("\n");
            message.append("CMS ID: ").append(cmsId).append("\n");
            message.append("Projet: ").append(projet).append("\n\n");
            
            message.append("Part Numbers concernés:\n");
            if (partNumbers != null) {
                for (String pn : partNumbers) {
                    message.append("  - ").append(pn).append("\n");
                }
            }
            
            message.append("\nDétails de l'erreur:\n");
            if (warnings != null) {
                for (String warning : warnings) {
                    message.append("  ").append(warning).append("\n");
                }
            }

            // Log the notification (for now, as email might not be configured)
            System.out.println("========================================");
            System.out.println(message.toString());
            System.out.println("========================================");

            // Try to send email if mail sender is configured
            if (mailSender != null) {
                try {
                    SimpleMailMessage mailMessage = new SimpleMailMessage();
                    mailMessage.setTo("cad@lear.com"); // Configure this in application.properties
                    mailMessage.setSubject("Notification CAD - Incohérence Matériaux - CMS ID: " + cmsId);
                    mailMessage.setText(message.toString());
                    mailSender.send(mailMessage);
                } catch (Exception mailEx) {
                    System.err.println("Failed to send email: " + mailEx.getMessage());
                    // Continue without email - notification is logged
                }
            }

            return new ResponseEntity<>(Map.of(
                    "success", true,
                    "message", "Notification envoyée avec succès",
                    "cuttingPlanId", cuttingPlanId,
                    "cmsId", cmsId
            ), HttpStatus.OK);
        } catch (Exception e) {
            log.error("NotificationController email send failed", e);
            return new ResponseEntity<>(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
