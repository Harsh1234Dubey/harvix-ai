package com.interviewai.storage;

import com.interviewai.exception.BadRequestException;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.domain.StoredFile;
import com.interviewai.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final StoredFileRepository storedFileRepository;

    @Value("${app.storage.root}")
    private String storageRoot;

    @Value("${app.storage.max-file-size-mb:20}")
    private long maxFileSizeMb;

    public StoredFile store(MultipartFile file, String category, Long uploadedBy, String entityType, Long entityId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }
        if (file.getSize() > maxFileSizeMb * 1024 * 1024) {
            throw new BadRequestException("File exceeds maximum allowed size of " + maxFileSizeMb + "MB");
        }
        try {
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String ext = extensionOf(original);
            String fileName = UUID.randomUUID() + ext;
            Path targetDir = root().resolve(category).resolve(YearMonth.now().toString());
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(fileName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            StoredFile stored = new StoredFile();
            stored.setOriginalName(original);
            stored.setStoragePath(target.toAbsolutePath().toString());
            stored.setMimeType(file.getContentType());
            stored.setSizeBytes(file.getSize());
            stored.setEntityType(entityType);
            stored.setEntityId(entityId);
            if (uploadedBy != null) {
                var user = new com.interviewai.domain.User();
                user.setId(uploadedBy);
                stored.setUploadedBy(user);
            }
            return storedFileRepository.save(stored);
        } catch (IOException ex) {
            throw new BadRequestException("Failed to store file: " + ex.getMessage());
        }
    }

    public Resource load(UUID uuid) {
        StoredFile stored = storedFileRepository.findByUuid(uuid)
                .orElseThrow(() -> ResourceNotFoundException.of("StoredFile", uuid));
        try {
            Path path = Path.of(stored.getStoragePath());
            if (!Files.exists(path) || !path.toAbsolutePath().toString().startsWith(root().toAbsolutePath().toString())) {
                throw new ResourceNotFoundException("File no longer exists on disk");
            }
            return new UrlResource(path.toUri());
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Failed to read file");
        }
    }

    public void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private Path root() {
        return Path.of(storageRoot).toAbsolutePath().normalize();
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }
}
