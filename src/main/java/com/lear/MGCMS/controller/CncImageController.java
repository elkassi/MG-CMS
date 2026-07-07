package com.lear.MGCMS.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Serves CNC reference images (PN Cuir, Fil Couture CNC) shown in the pattern detail
 * of /cncPs and /cncControl. The image file is named by characters 11-13 (1-based) of
 * the code, with any extension (e.g. L003206673CXNAF -> CXN.png).
 * Mounted under /api/public/** (permitAll) so plain <img src> tags load without the JWT header.
 */
@RestController
@RequestMapping("/api/public/cncImages")
public class CncImageController {

    @Value("${lear.cncLeatherImagesFolder:}")
    private String leatherFolder;

    @Value("${lear.cncFilCoutureImagesFolder:}")
    private String filCoutureFolder;

    @GetMapping
    public ResponseEntity<byte[]> image(@RequestParam String kind, @RequestParam String value) {
        String code = extractCode(value);
        if (code == null) return ResponseEntity.notFound().build();

        String folder = "filCouture".equalsIgnoreCase(kind) ? filCoutureFolder : leatherFolder;
        if (folder == null || folder.trim().isEmpty()) return ResponseEntity.notFound().build();

        // Match <code>.<anything>, case-insensitive. `code` is a fixed 3-char slice and is
        // only compared against existing file names (never used to build a path) -> no traversal.
        File[] matches = new File(folder.trim()).listFiles((d, name) -> {
            int dot = name.lastIndexOf('.');
            String base = dot >= 0 ? name.substring(0, dot) : name;
            return base.equalsIgnoreCase(code);
        });
        if (matches == null || matches.length == 0) return ResponseEntity.notFound().build();

        try {
            File img = matches[0]; // ponytail: first match wins if several extensions exist
            byte[] bytes = Files.readAllBytes(img.toPath());
            return ResponseEntity.ok()
                    .contentType(contentType(img.getName()))
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Image code = characters 11-13 (1-based) of the value, or null if shorter than 13. */
    static String extractCode(String value) {
        String v = value == null ? "" : value.trim();
        return v.length() >= 13 ? v.substring(10, 13) : null;
    }

    private MediaType contentType(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (n.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (n.endsWith(".bmp")) return MediaType.valueOf("image/bmp");
        if (n.endsWith(".webp")) return MediaType.valueOf("image/webp");
        if (n.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
