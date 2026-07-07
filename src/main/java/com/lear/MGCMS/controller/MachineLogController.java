package com.lear.MGCMS.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/machineLog")
@PreAuthorize("hasRole('ADMIN') or hasRole('PROCESS') or hasRole('MAINTENANCE') or hasRole('INDICATEUR')")
public class MachineLogController {

    @Value("${lear.lectraHistoryFolder:F:\\\\LectraHistory}")
    private String lectraHistoryFolder;

    private boolean isPathSafe(String folderName, String fileName) {
        if (folderName != null && (folderName.contains("..") || folderName.contains("/") || folderName.contains("\\"))) {
            return false;
        }
        if (fileName != null && (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\"))) {
            return false;
        }
        return true;
    }

    private boolean isPathWithinBase(File file, File baseDir) throws IOException {
        Path filePath = file.getCanonicalFile().toPath();
        Path basePath = baseDir.getCanonicalFile().toPath();
        return filePath.startsWith(basePath);
    }

    /**
     * Get folder names only - lightweight endpoint that doesn't scan files.
     * Used for initial page load to avoid slow file system scanning.
     */
    @GetMapping("/folderNames")
    public ResponseEntity<?> getFolderNames() {
        try {
            File folder = new File(lectraHistoryFolder);
            if (!folder.exists() || !folder.isDirectory()) {
                return new ResponseEntity<>("Folder not found: " + lectraHistoryFolder, HttpStatus.NOT_FOUND);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            File[] subFolders = folder.listFiles(File::isDirectory);
            
            if (subFolders != null) {
                for (File subFolder : subFolders) {
                    Map<String, Object> folderInfo = new HashMap<>();
                    folderInfo.put("name", subFolder.getName());
                    result.add(folderInfo);
                }
                result.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));
            }

            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error reading folder: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/folders")
    public ResponseEntity<?> getFolders() {
        try {
            File folder = new File(lectraHistoryFolder);
            if (!folder.exists() || !folder.isDirectory()) {
                return new ResponseEntity<>("Folder not found: " + lectraHistoryFolder, HttpStatus.NOT_FOUND);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            File[] subFolders = folder.listFiles(File::isDirectory);
            
            if (subFolders != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                for (File subFolder : subFolders) {
                    Map<String, Object> folderInfo = new HashMap<>();
                    folderInfo.put("name", subFolder.getName());
                    folderInfo.put("lastModified", sdf.format(new Date(subFolder.lastModified())));
                    folderInfo.put("lastModifiedTimestamp", subFolder.lastModified());
                    
                    // Get the latest log file in the folder
                    File[] logFiles = subFolder.listFiles((dir, name) -> name.endsWith(".txt") || name.endsWith(".log"));
                    if (logFiles != null && logFiles.length > 0) {
                        // Find the most recently modified log file
                        File latestLog = Arrays.stream(logFiles)
                            .max(Comparator.comparingLong(File::lastModified))
                            .orElse(null);
                        
                        if (latestLog != null) {
                            folderInfo.put("latestLogFile", latestLog.getName());
                            folderInfo.put("latestLogModified", sdf.format(new Date(latestLog.lastModified())));
                            folderInfo.put("latestLogModifiedTimestamp", latestLog.lastModified());
                        }
                    }
                    
                    // Count total files
                    File[] allFiles = subFolder.listFiles();
                    folderInfo.put("fileCount", allFiles != null ? allFiles.length : 0);
                    
                    result.add(folderInfo);
                }
                
                // Sort by name (machine name like AA1, AA2, etc.)
                result.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));
            }

            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error reading folder: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/folder/{folderName}")
    public ResponseEntity<?> getFolderContents(
            @PathVariable String folderName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String prefix) {
        try {
            if (!isPathSafe(folderName, null)) {
                return new ResponseEntity<>("Invalid folder name", HttpStatus.BAD_REQUEST);
            }
            
            File baseDir = new File(lectraHistoryFolder);
            File folder = new File(lectraHistoryFolder + File.separator + folderName);
            
            if (!isPathWithinBase(folder, baseDir)) {
                return new ResponseEntity<>("Access denied", HttpStatus.FORBIDDEN);
            }
            
            if (!folder.exists() || !folder.isDirectory()) {
                return new ResponseEntity<>("Folder not found: " + folderName, HttpStatus.NOT_FOUND);
            }

            File[] files = folder.listFiles();
            if (files == null) {
                files = new File[0];
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            // Convert to list and apply filters
            List<File> fileList = Arrays.stream(files)
                .filter(file -> {
                    if (fileName != null && !fileName.isEmpty()) {
                        return file.getName().toLowerCase().contains(fileName.toLowerCase());
                    }
                    return true;
                })
                .filter(file -> {
                    if (prefix != null && !prefix.isEmpty()) {
                        return file.getName().toLowerCase().startsWith(prefix.toLowerCase());
                    }
                    return true;
                })
                // Sort by last modified (most recent first)
                .sorted((a, b) -> Long.compare(b.lastModified(), a.lastModified()))
                .collect(Collectors.toList());
            
            int totalFiles = fileList.size();
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, totalFiles);
            
            List<Map<String, Object>> result = new ArrayList<>();
            if (start < totalFiles) {
                for (int i = start; i < end; i++) {
                    File file = fileList.get(i);
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("isDirectory", file.isDirectory());
                    fileInfo.put("lastModified", sdf.format(new Date(file.lastModified())));
                    fileInfo.put("lastModifiedTimestamp", file.lastModified());
                    fileInfo.put("size", file.length());
                    result.add(fileInfo);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("files", result);
            response.put("totalFiles", totalFiles);
            response.put("page", page);
            response.put("size", size);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error reading folder: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/read/{folderName}/{fileName}")
    public ResponseEntity<?> readFile(@PathVariable String folderName, @PathVariable String fileName) {
        try {
            if (!isPathSafe(folderName, fileName)) {
                return new ResponseEntity<>("Invalid folder or file name", HttpStatus.BAD_REQUEST);
            }
            
            File baseDir = new File(lectraHistoryFolder);
            File file = new File(lectraHistoryFolder + File.separator + folderName + File.separator + fileName);
            
            if (!isPathWithinBase(file, baseDir)) {
                return new ResponseEntity<>("Access denied", HttpStatus.FORBIDDEN);
            }
            
            if (!file.exists() || file.isDirectory()) {
                return new ResponseEntity<>("File not found: " + fileName, HttpStatus.NOT_FOUND);
            }

            // Check if file is too large (limit to 5MB for reading)
            if (file.length() > 5 * 1024 * 1024) {
                return new ResponseEntity<>("File is too large to read (max 5MB). Please download instead.", HttpStatus.BAD_REQUEST);
            }

            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return new ResponseEntity<>(content, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Error reading file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/download/{folderName}/{fileName}")
    public ResponseEntity<?> downloadFile(@PathVariable String folderName, @PathVariable String fileName) {
        try {
            if (!isPathSafe(folderName, fileName)) {
                return new ResponseEntity<>("Invalid folder or file name", HttpStatus.BAD_REQUEST);
            }
            
            File baseDir = new File(lectraHistoryFolder);
            File file = new File(lectraHistoryFolder + File.separator + folderName + File.separator + fileName);
            
            if (!isPathWithinBase(file, baseDir)) {
                return new ResponseEntity<>("Access denied", HttpStatus.FORBIDDEN);
            }
            
            if (!file.exists() || file.isDirectory()) {
                return new ResponseEntity<>("File not found: " + fileName, HttpStatus.NOT_FOUND);
            }

            Resource resource = new FileSystemResource(file);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .body(resource);
        } catch (Exception e) {
            return new ResponseEntity<>("Error downloading file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
