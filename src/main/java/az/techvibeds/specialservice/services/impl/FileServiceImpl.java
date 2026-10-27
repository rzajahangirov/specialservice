package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.services.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        // Orijinal fayl adı
        String name = file.getOriginalFilename();

        // Unikal ad yaratmaq (UUID)
        String randomID = UUID.randomUUID().toString();
        String fileName1 = randomID.concat(name.substring(name.lastIndexOf(".")));

        // Tam yol
        String filePath = path + File.separator + fileName1;

        // Qovluğu yaratmaq (əgər yoxdursa)
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }

        // Faylı kopyalamaq
        Files.copy(file.getInputStream(), Paths.get(filePath));

        return fileName1;
    }

    @Override
    public InputStream getResource(String path, String fileName) throws IOException {
        String fullPath = path + File.separator + fileName;
        File file = new File(fullPath);
        if (!file.exists()) {
            throw new ResourceNotFoundException("Fayl tapılmadı: " + fileName);
        }
        return new FileInputStream(fullPath);
    }

    @Override
    public boolean deleteFile(String path, String fileName) {
        String fullPath = path + File.separator + fileName;
        File file = new File(fullPath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
