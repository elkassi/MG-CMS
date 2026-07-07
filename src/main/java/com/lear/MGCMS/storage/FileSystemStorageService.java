package com.lear.MGCMS.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import com.lear.MGCMS.exceptions.StorageException;
import com.lear.MGCMS.exceptions.StorageFileNotFoundException;
import com.lear.MGCMS.storage.StorageService;

@Service
public class FileSystemStorageService implements StorageService {

	private final Path rootLocation;

	@Value("${storage.lear.storageFile}")
	private String storageFileLocation;

	@Autowired
	public FileSystemStorageService(@Value("${storage.lear.storageFile}") String storageFileLocation) {
		this.rootLocation = Paths.get(storageFileLocation);
	}

	/*@Autowired
	public FileSystemStorageService() {
		System.out.println("storageFileLocation: "+storageFileLocation);
		this.rootLocation = Paths.get(storageFileLocation);
	}*/

//	@Override
//	public void store(MultipartFile file, String filename) {
//		try {
//			if (file.isEmpty()) {
//				throw new StorageException("Failed to store empty file.");
//			}
//			Path destinationFile = this.rootLocation.resolve(
//					filename)
//					.normalize().toAbsolutePath();
//			if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
//				// This is a security check
//				throw new StorageException(
//						"Cannot store file outside current directory.");
//			}
//			try (InputStream inputStream = file.getInputStream()) {
//				Files.copy(inputStream, destinationFile,
//					StandardCopyOption.REPLACE_EXISTING);
//			}
//		}
//		catch (IOException e) {
//			throw new StorageException("Failed to store file.", e);
//		}
//	}

	@Override
	public void store(MultipartFile file, String filename) {
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file.");
			}

			Path destinationFile = this.rootLocation.resolve(filename).normalize().toAbsolutePath();
			if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
				// This is a security check
				throw new StorageException("Cannot store file outside current directory.");
			}
			System.out.println("File size: " + (file.getSize() / 1024)  + " KB");

			if (file.getSize() > 1 * 1024 * 1024 && isImage(file)) {
				System.out.println("Compressing image: " + file.getOriginalFilename());
				// Compress the image
				File compressedFile = new File(destinationFile.toString());
				Thumbnails.of(file.getInputStream())
						.scale(0.7)
						//.size(1920, 1080) // Adjust the size as needed
						.outputQuality(0.5) // Adjust the quality as needed to achieve the desired size
						.toFile(compressedFile);
				System.out.println("Compressed file size: " + (compressedFile.length() / 1024)  + " KB");
				// Check the size of the compressed file
				while (compressedFile.length() > 1 * 1024 * 1024) {
					Thumbnails.of(compressedFile)
							.scale(0.7) // Further reduce the size
							.outputQuality(0.5)
							.toFile(compressedFile);
					System.out.println("Compressed file size: " + (compressedFile.length() / 1024)  + " KB");

				}

				// Ensure the compressed file is saved
				Files.copy(compressedFile.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
			} else {
				try (InputStream inputStream = file.getInputStream()) {
					Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException e) {
			throw new StorageException("Failed to store file.", e);
		}
	}

	private boolean isImage(MultipartFile file) {
		String contentType = file.getContentType();
		return contentType != null && contentType.startsWith("image");
	}


	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.rootLocation, 1)
				.filter(path -> !path.equals(this.rootLocation))
				.map(this.rootLocation::relativize);
		}
		catch (IOException e) {
			throw new StorageException("Failed to read stored files", e);
		}

	}

	@Override
	public Path load(String filename) {
		return rootLocation.resolve(filename);
	}

	@Override
	public Resource loadAsResource(String filename) {
		try {
			Path file = load(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			}
			else {
				throw new StorageFileNotFoundException(
						"Could not read file: " + filename);

			}
		}
		catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename, e);
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(rootLocation.toFile());
	}

	@Override
	public void init() {
		try {
			if(!Files.exists(rootLocation)) {
				Files.createDirectories(rootLocation);
			}
		}
		catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}
	
}
