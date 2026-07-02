package com.studyplanner.service;

import com.studyplanner.config.FileUploadConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务
 */
@Service
public class FileUploadService {
    
    @Autowired
    private FileUploadConfig fileUploadConfig;
    
    // 允许的图片类型
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    
    // 最大文件大小：5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    /**
     * 上传头像
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 头像访问URL
     */
    public String uploadAvatar(MultipartFile file, Long userId) throws IOException {
        // 验证文件
        validateImageFile(file);
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFilename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        
        // 保存文件
        String subDir = "avatars";
        Path uploadPath = Paths.get(fileUploadConfig.getUploadPath(), subDir);
        
        // 确保目录存在
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 删除该用户旧的头像文件
        deleteOldAvatars(uploadPath, userId);
        
        // 保存新文件
        Path filePath = uploadPath.resolve(newFilename);
        file.transferTo(filePath.toFile());
        
        // 返回访问URL
        return fileUploadConfig.getUrlPrefix() + "/" + subDir + "/" + newFilename;
    }
    
    /**
     * 验证图片文件
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要上传的文件");
        }
        
        // 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小不能超过5MB");
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new RuntimeException("只支持 JPG、PNG、GIF、WEBP 格式的图片");
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * 删除用户旧的头像文件
     */
    private void deleteOldAvatars(Path uploadPath, Long userId) {
        File dir = uploadPath.toFile();
        if (dir.exists() && dir.isDirectory()) {
            File[] oldFiles = dir.listFiles((d, name) -> name.startsWith("avatar_" + userId + "_"));
            if (oldFiles != null) {
                for (File oldFile : oldFiles) {
                    oldFile.delete();
                }
            }
        }
    }
    
    private static final Color[] AVATAR_PALETTE = {
            new Color(0x4F, 0x8A, 0xF7),
            new Color(0x6A, 0x5A, 0xCD),
            new Color(0x20, 0xB2, 0xAA),
            new Color(0xE9, 0x67, 0x7C),
            new Color(0xF2, 0x9C, 0x3E),
            new Color(0x8E, 0x44, 0xAD),
            new Color(0x2E, 0x86, 0xDE),
            new Color(0x27, 0xAE, 0x60),
            new Color(0xD3, 0x54, 0x00),
            new Color(0x16, 0xA0, 0x85)
    };

    /**
     * 为新注册用户生成默认头像（首字母 + 随机纯色背景），存到 avatars 目录并返回 URL
     */
    public String generateDefaultAvatar(Long userId, String username) throws IOException {
        int size = 200;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color background = pickColor(username, userId);
            g.setColor(background);
            g.fill(new Ellipse2D.Float(0, 0, size, size));

            String letter = pickLetter(username);
            g.setColor(Color.WHITE);
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 100);
            g.setFont(font);
            java.awt.FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(letter);
            int x = (size - textWidth) / 2;
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(letter, x, y);
        } finally {
            g.dispose();
        }

        String subDir = "avatars";
        Path uploadPath = Paths.get(fileUploadConfig.getUploadPath(), subDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        deleteOldAvatars(uploadPath, userId);

        String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        Path filePath = uploadPath.resolve(filename);
        ImageIO.write(image, "png", filePath.toFile());

        return fileUploadConfig.getUrlPrefix() + "/" + subDir + "/" + filename;
    }

    private String pickLetter(String username) {
        if (username == null) {
            return "?";
        }
        for (int i = 0; i < username.length(); i++) {
            char c = username.charAt(i);
            if (!Character.isWhitespace(c)) {
                return String.valueOf(Character.toUpperCase(c));
            }
        }
        return "?";
    }

    private Color pickColor(String username, Long userId) {
        int seed = (username != null ? username.hashCode() : 0) ^ (userId != null ? userId.hashCode() : 0);
        int index = Math.floorMod(seed, AVATAR_PALETTE.length);
        return AVATAR_PALETTE[index];
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }
        
        // 从URL提取相对路径
        String relativePath = fileUrl.replace(fileUploadConfig.getUrlPrefix(), "");
        Path filePath = Paths.get(fileUploadConfig.getUploadPath(), relativePath);
        
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}
