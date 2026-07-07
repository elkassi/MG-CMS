package com.lear.MGCMS.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@RestController
@RequestMapping("/api/storageHistory")
public class StorageHistoryController {

    private static final Logger log = LoggerFactory.getLogger(StorageHistoryController.class);

    // a param called nom and in it body have a file  called file
    @PostMapping("/uploadFile")
    public void uploadFile(
            @RequestParam("nom") String nom,
            @RequestParam("file") MultipartFile file) {
//        String directoryPath  = "C:\\Users\\melghazi\\Desktop\\storage";
        String directoryPath  = "F:\\LectraHistory";
        // check if in that folder an other folder called nom exists, if not create it
        // then save the file in that folder
        File folder = new File(directoryPath, nom.trim().toUpperCase());
        if (!folder.exists()) {
            folder.mkdir();
        }
        // save now the file in that folder
//        File fileToSave = new File(folder, file.getOriginalFilename());
        String originalFilename = file.getOriginalFilename();
        File fileToSave  = new File(folder, "temp_" + file.getOriginalFilename());

        try {
            // Save the file temporarily
            file.transferTo(fileToSave);

            filterAndSaveLines(fileToSave, originalFilename);

        } catch (Exception e) {
            log.error("StorageHistoryController upload failed for {}", originalFilename, e);
        }
    }

    private void filterAndSaveLines(File fileToFilter, String originalFilename) throws IOException {
        String keyword = "<InputListViewModel/>";
        // filter from  fileToFilter all lines that contain keyword and keyword2
        // and save them in a same file
        File fileToSave = new File(fileToFilter.getParent(), originalFilename);
        try (BufferedReader reader = new BufferedReader(new FileReader(fileToFilter));
             BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(keyword)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            log.error("StorageHistoryController filterAndSaveLines failed for {}", originalFilename, e);
        }

        // while fileToFilter is not deleted, delete it
        while (!fileToFilter.delete()) {
            System.out.println("Could not delete file: " + fileToFilter.getName());
        }


    }


}
