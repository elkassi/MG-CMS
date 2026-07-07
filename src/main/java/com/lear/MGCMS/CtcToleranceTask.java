package com.lear.MGCMS;

// ...existing imports...

import com.lear.MGCMS.services.ctc.FilesService;
import com.lear.ctc.domain.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

//@Component
public class CtcToleranceTask {

    private static final Logger log = LoggerFactory.getLogger(CtcToleranceTask.class);

    @Autowired
    private FilesService filesService;

    @Value("${lear.pltfolder}")
    private String pltfolder;




    @Scheduled(fixedRate = 1000 * 60* 60*24)
    private void verificationCTC() {
        String projet = "AU546";
        String type = "supplier kit leather";
        List<String> arrPatterns = filesService.findPatternByProjetAndTypeAsLaminated(projet, type );
        System.out.println("Patterns found: " + arrPatterns.size());
        for(String pattern : arrPatterns) {
            String filePath = pltfolder + "/" + pattern + ".plt";
            Double height = getMax(filePath, pattern);
            if (height != null) {
                Double min1 = null, max1 = null;


                if(height > 0 && height < 150) {
                    min1 = -2.0;
                    max1 = 2.0;
                } else if(height >= 150 && height < 300) {
                    min1 = -2.5;
                    max1 = 2.5;
                } else if(height >= 300 && height < 600) {
                    min1 = -3.0;
                    max1 = 3.0;
                } else if(height >= 600 ) {
                    min1 = -4.0;
                    max1 = 4.0;
                }

                if(min1 != null && max1 != null) {
                    System.out.println(pattern + " Max : "+ height + " min1: "+ min1 + " max1: " + max1);
                    filesService.updatePatternByProjetAndTypeAsLaminated(projet, type, pattern, min1, max1);
                }
            } else {
                System.out.println("Could not calculate height for pattern: " + pattern);
            }
        }
    }
//    @Scheduled(fixedRate = 1000 * 60)
    private void verificationFile() {
        String filePath = "D:/LEAR/PLT Files/C67286-B.plt";
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Recherche toutes les commandes PU/PD dans la ligne
                String[] tokens = line.split(";");
                for (String token : tokens) {
                    token = token.trim();
                    if (token.startsWith("PU") || token.startsWith("PD")) {
                        String coords = token.substring(2);
                        if (!coords.isEmpty()) {
                            String[] pairs = coords.split(",");
                            // Les coordonnées peuvent être multiples, séparées par des virgules
                            for (int i = 0; i < pairs.length - 1; i += 2) {
                                try {
                                    double x = Double.parseDouble(pairs[i]);
                                    double y = Double.parseDouble(pairs[i + 1]);
                                    if (x < minX) minX = x;
                                    if (x > maxX) maxX = x;
                                    if (y < minY) minY = y;
                                    if (y > maxY) maxY = y;
                                } catch (NumberFormatException e) {
                                    // Ignore les erreurs de parsing
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("CtcToleranceTask file read failed", e);
        }
        double width = maxX - minX;
        double height = maxY - minY;
        // Conversion en millimètres (1 unité PLT = 0,0254 mm)
        double widthMM = width * 0.0254;
        double heightMM = height * 0.0254;
        System.out.println("Min X: " + minX + ", Max X: " + maxX);
        System.out.println("Min Y: " + minY + ", Max Y: " + maxY);
        System.out.println("Dimensions (unités PLT): largeur = " + width + ", hauteur = " + height);
        System.out.println("Dimensions (mm): largeur = " + widthMM + ", hauteur = " + heightMM);
    }

    Double getHauteur(String pltFile) {
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        try (BufferedReader br = new BufferedReader(new FileReader(pltFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(";");
                for (String token : tokens) {
                    token = token.trim();
                    if (token.startsWith("PU") || token.startsWith("PD")) {
                        String coords = token.substring(2);
                        if (!coords.isEmpty()) {
                            String[] pairs = coords.split(",");
                            for (int i = 1; i < pairs.length; i += 2) {
                                try {
                                    double y = Double.parseDouble(pairs[i]);
                                    if (y < minY) minY = y;
                                    if (y > maxY) maxY = y;
                                } catch (NumberFormatException e) {
                                    // Ignore les erreurs de parsing
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("CtcToleranceTask file read failed", e);
        }
        double height = maxY - minY;
        // Conversion en millimètres (1 unité PLT = 0,0254 mm)
        double heightMM = height * 0.0254;
//        System.out.println("Min Y: " + minY + ", Max Y: " + maxY);
//        System.out.println("Hauteur (unités PLT): " + height);
//        System.out.println("Hauteur (mm): " + heightMM);
        return heightMM;
    }

    Double getMax(String pltFile, String pattern) {
        //in this we get the max between height and width
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        try (BufferedReader br = new BufferedReader(new FileReader(pltFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(";");
                for (String token : tokens) {
                    token = token.trim();
                    if (token.startsWith("PU") || token.startsWith("PD")) {
                        String coords = token.substring(2);
                        if (!coords.isEmpty()) {
                            String[] pairs = coords.split(",");
                            for (int i = 0; i < pairs.length - 1; i += 2) {
                                try {
                                    double x = Double.parseDouble(pairs[i]);
                                    double y = Double.parseDouble(pairs[i + 1]);
                                    if (x < minX) minX = x;
                                    if (x > maxX) maxX = x;
                                    if (y < minY) minY = y;
                                    if (y > maxY) maxY = y;
                                } catch (NumberFormatException e) {
                                    // Ignore les erreurs de parsing
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(pattern+" : Not Found ");
            return null;
        }
        double width = maxX - minX;
        double height = maxY - minY;
        // Conversion en millimètres (1 unité PLT = 0,0254 mm)
        double widthMM = width * 0.0254;
        double heightMM = height * 0.0254;
        System.out.println(pattern+" : largeur = " + widthMM + ", hauteur = " + heightMM);
        return Math.max(widthMM, heightMM);
    }

}
