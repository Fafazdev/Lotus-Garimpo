package lotus.security;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class FileUploadValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private FileUploadValidator() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de imagem não pode ser vazio.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Tamanho máximo permitido: 5 MB.");
        }
        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido. Use: JPG, PNG, GIF ou WebP.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Tipo MIME não permitido.");
        }
    }

    public static String generateSafeFilename(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        return UUID.randomUUID().toString() + "." + ext.toLowerCase(Locale.ROOT);
    }

    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
