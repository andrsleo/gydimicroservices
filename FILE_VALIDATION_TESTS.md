# Pruebas de Validación de Archivos - GYDI Microservices

## Implementación de Seguridad

### FileValidator - Validación Completa de Archivos

El nuevo `FileValidator` proporciona validación de seguridad en múltiples niveles:

1. ✅ **Magic Number Verification** - Verifica los primeros bytes del archivo
2. ✅ **Extension Whitelist** - Solo permite extensiones seguras
3. ✅ **Content-Type Validation** - Valida el MIME type
4. ✅ **Size Limits** - 10MB para imágenes, 100MB para videos
5. ✅ **Path Traversal Protection** - Previene ataques ../../../etc/passwd
6. ✅ **Null Byte Protection** - Detecta null bytes en nombres de archivo

---

## Magic Numbers Implementados

### Imágenes Soportadas

| Formato | MIME Type | Magic Number (Hex) | Extensiones |
|---------|-----------|-------------------|-------------|
| JPEG | `image/jpeg` | `FF D8 FF` | .jpg, .jpeg |
| PNG | `image/png` | `89 50 4E 47 0D 0A 1A 0A` | .png |
| GIF | `image/gif` | `47 49 46 38` ("GIF8") | .gif |
| WebP | `image/webp` | `52 49 46 46` ("RIFF") | .webp |
| BMP | `image/bmp` | `42 4D` ("BM") | .bmp |

### Videos Soportados

| Formato | MIME Type | Magic Number (Hex) | Extensiones |
|---------|-----------|-------------------|-------------|
| MP4 | `video/mp4` | `00 00 00 18 66 74 79 70` | .mp4 |
| QuickTime | `video/quicktime` | `00 00 00 14 66 74 79 70` | .mov |
| AVI | `video/x-msvideo` | `52 49 46 46` ("RIFF") | .avi |
| WebM | `video/webm` | `1A 45 DF A3` | .webm |

---

## Pruebas de Seguridad

### Test 1: Subir imagen legítima (debe funcionar)

```bash
# Crear una imagen PNG válida
convert -size 100x100 xc:blue test-valid.png

# Subir
curl -X POST http://localhost:8080/api/v1/properties/1/images \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test-valid.png"

# Respuesta esperada: 200 OK con URL del archivo
```

---

### Test 2: Ataque de MIME Type Spoofing (debe fallar)

```bash
# Crear un archivo .jsp malicioso
echo '<%@ page import="java.io.*" %><% Runtime.getRuntime().exec("ls"); %>' > shell.jsp

# Intentar subirlo como imagen (falsificando Content-Type)
curl -X POST http://localhost:8080/api/v1/properties/1/images \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@shell.jsp;type=image/jpeg"

# Respuesta esperada: 400 Bad Request
# Error: "File content does not match declared type. Possible file type spoofing detected."
```

**Por qué falla:**
- El Content-Type dice `image/jpeg` (FF D8 FF)
- Pero el archivo empieza con `<` (3C en hex)
- Magic number no coincide → BLOQUEADO ✅

---

### Test 3: Extensión peligrosa (debe fallar)

```bash
# Intentar subir archivo con extensión peligrosa
curl -X POST http://localhost:8080/api/v1/properties/1/images \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@malicious.jsp"

# Respuesta esperada: 400 Bad Request
# Error: "Invalid file extension. Allowed extensions: .jpg, .jpeg, .png, .gif, .webp, .bmp"
```

---

### Test 4: Path Traversal Attack (debe fallar)

```bash
# Intentar subir archivo con path traversal en el nombre
convert -size 100x100 xc:red "../../../../etc/passwd.png"

curl -X POST http://localhost:8080/api/v1/properties/1/images \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@../../../../etc/passwd.png"

# Respuesta esperada: 400 Bad Request
# Error: "Invalid filename: path traversal detected"
```

---

### Test 5: Archivo muy grande (debe fallar)

```bash
# Crear imagen de 15MB (límite es 10MB)
dd if=/dev/zero of=large.jpg bs=1M count=15

curl -X POST http://localhost:8080/api/v1/properties/1/images \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@large.jpg"

# Respuesta esperada: 400 Bad Request
# Error: "File size exceeds maximum allowed size of 10 MB"
```

---

### Test 6: Null Byte Injection (debe fallar)

```bash
# Intentar usar null byte para bypass de extensión
# Nombre: image.jpg\0.jsp (null byte entre jpg y jsp)

# En Python (para crear el archivo con null byte)
python3 << 'EOF'
with open("image.jpg\x00.jsp", "wb") as f:
    f.write(b"\xFF\xD8\xFF" + b"fake jpeg data")
EOF

curl -X POST http://localhost:8080/api/v1/properties/1/images \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@image.jpg%00.jsp"

# Respuesta esperada: 400 Bad Request
# Error: "Invalid filename: contains null bytes"
```

---

### Test 7: Polyglot File Attack (debe fallar o detectarse)

```bash
# Un polyglot es un archivo válido en múltiples formatos
# Por ejemplo: un archivo que es tanto JPG como PHP

# Crear polyglot simple
echo -ne '\xFF\xD8\xFF\xE0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00' > poly.jpg
echo '<?php system($_GET["cmd"]); ?>' >> poly.jpg

curl -X POST http://localhost:8080/api/v1/properties/1/images \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@poly.jpg;type=image/jpeg"

# Si pasa la validación:
#   - El archivo se almacena
#   - PERO no se ejecutará como PHP porque se sirve como imagen
#   - Protección adicional: nunca ejecutar archivos subidos
```

---

## Pruebas Automatizadas (JUnit)

### Crear test de validación

```java
// src/test/java/com/affiliate/rentals/gydi/shared/security/FileValidatorTest.java

package com.affiliate.rentals.gydi.shared.security;

import com.affiliate.rentals.gydi.shared.infrastructure.storage.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileValidatorTest {

    private FileValidator fileValidator;

    @BeforeEach
    void setUp() {
        fileValidator = new FileValidator();
    }

    @Test
    void shouldAcceptValidJpegImage() {
        // Valid JPEG magic number: FF D8 FF
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            jpegBytes
        );

        assertDoesNotThrow(() -> fileValidator.validateImage(file));
    }

    @Test
    void shouldRejectFileWithWrongMagicNumber() {
        // Wrong magic number (plain text, not JPEG)
        byte[] fakeJpeg = "This is not a JPEG".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "fake.jpg",
            "image/jpeg",
            fakeJpeg
        );

        StorageException exception = assertThrows(
            StorageException.class,
            () -> fileValidator.validateImage(file)
        );

        assertTrue(exception.getMessage().contains("File content does not match declared type"));
    }

    @Test
    void shouldRejectDangerousExtension() {
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "shell.jsp",
            "image/jpeg",
            jpegBytes
        );

        StorageException exception = assertThrows(
            StorageException.class,
            () -> fileValidator.validateImage(file)
        );

        assertTrue(exception.getMessage().contains("Invalid file extension"));
    }

    @Test
    void shouldRejectPathTraversal() {
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "../../../etc/passwd.jpg",
            "image/jpeg",
            jpegBytes
        );

        StorageException exception = assertThrows(
            StorageException.class,
            () -> fileValidator.validateImage(file)
        );

        assertTrue(exception.getMessage().contains("path traversal detected"));
    }

    @Test
    void shouldRejectOversizedFile() {
        // Create 11MB file (exceeds 10MB limit)
        byte[] largeBytes = new byte[11 * 1024 * 1024];
        largeBytes[0] = (byte) 0xFF;
        largeBytes[1] = (byte) 0xD8;
        largeBytes[2] = (byte) 0xFF;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "large.jpg",
            "image/jpeg",
            largeBytes
        );

        StorageException exception = assertThrows(
            StorageException.class,
            () -> fileValidator.validateImage(file)
        );

        assertTrue(exception.getMessage().contains("exceeds maximum allowed size"));
    }

    @Test
    void shouldAcceptValidPngImage() {
        // Valid PNG magic number: 89 50 4E 47 0D 0A 1A 0A
        byte[] pngBytes = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D // Additional bytes
        };

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            pngBytes
        );

        assertDoesNotThrow(() -> fileValidator.validateImage(file));
    }

    @Test
    void shouldSanitizeFilename() {
        String dangerous = "../../../etc/passwd";
        String sanitized = fileValidator.sanitizeFilename(dangerous);

        assertFalse(sanitized.contains(".."));
        assertFalse(sanitized.contains("/"));
    }
}
```

**Ejecutar tests:**
```bash
./mvnw test -Dtest=FileValidatorTest
```

---

## Defensa en Profundidad

### Capas de Seguridad Implementadas

1. **Validación de entrada (FileValidator)** ✅
   - Magic numbers
   - Extensiones
   - Content-Type
   - Tamaño

2. **Almacenamiento seguro** ✅
   - Archivos nunca se ejecutan
   - Se sirven con headers correctos
   - Nombres de archivo sanitizados

3. **Acceso controlado** ✅
   - Solo usuarios autenticados
   - Rate limiting en subidas
   - Logs de todas las subidas

4. **Headers de seguridad** (siguiente tarea)
   - Content-Type correcto
   - X-Content-Type-Options: nosniff
   - Content-Disposition: attachment (para descargas)

---

## Protección contra Ataques Comunes

### ❌ Ataque: Subir Shell PHP/JSP
**Protección:**
1. Extensión bloqueada (solo .jpg, .png, etc.)
2. Magic number no coincide
3. Archivo no se ejecuta (se sirve como imagen)

### ❌ Ataque: Path Traversal (../../../etc/passwd)
**Protección:**
1. Detectado por FileValidator
2. Nombre sanitizado antes de guardar
3. Path se normaliza con `.toAbsolutePath().normalize()`

### ❌ Ataque: Double Extension (.jpg.php)
**Protección:**
1. Validación de extensión usa `lastIndexOf('.')`
2. Solo la última extensión se valida

### ❌ Ataque: Null Byte Injection (image.jpg\0.php)
**Protección:**
1. FileValidator detecta null bytes
2. Archivo rechazado inmediatamente

### ❌ Ataque: ZIP Bomb
**Protección:**
1. Límite de tamaño de archivo (10MB/100MB)
2. Archivos ZIP no permitidos

---

## Monitoreo en Producción

### Logs importantes

```bash
# Ver intentos de subida maliciosa
grep "File upload rejected" /var/log/gydi/application.log

# Ejemplos de log:
# WARN - File upload rejected: magic number validation failed. Filename: shell.jsp
# WARN - File upload rejected: path traversal attempt detected. Filename: ../../../etc/passwd.png
# WARN - File upload rejected: invalid extension. Filename: malicious.jsp
```

### Métricas

1. **Archivos rechazados por día**
   - Alto → Posible ataque
   - Bajo → Normal

2. **Tipos de rechazo**
   - Magic number: Spoofing attempt
   - Extension: Archivo peligroso
   - Path traversal: Ataque sofisticado

3. **IPs con muchos rechazos**
   - Bloquear automáticamente
   - Investigar actividad

---

## Mejoras Futuras

### 1. Análisis antivirus en la nube

```java
// Integrar con ClamAV o VirusTotal
public void validateImage(MultipartFile file) {
    // Validaciones actuales
    fileValidator.validateImage(file);

    // Escaneo antivirus
    if (antivirusService.scan(file).isMalicious()) {
        throw new StorageException("Malware detected");
    }
}
```

### 2. Validación de contenido de imagen

```java
// Usar ImageIO para validar que es una imagen real
try {
    BufferedImage image = ImageIO.read(file.getInputStream());
    if (image == null) {
        throw new StorageException("File is not a valid image");
    }
} catch (IOException e) {
    throw new StorageException("Failed to parse image");
}
```

### 3. Watermarking automático

```java
// Agregar watermark a imágenes para prevenir uso no autorizado
public String uploadFile(MultipartFile file, String folder) {
    fileValidator.validateImage(file);

    BufferedImage image = ImageIO.read(file.getInputStream());
    BufferedImage watermarked = watermarkService.addWatermark(image);

    // Upload watermarked image
}
```

---

## Referencias

- [OWASP File Upload Security](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)
- [List of File Signatures (Wikipedia)](https://en.wikipedia.org/wiki/List_of_file_signatures)
- [CWE-434: Unrestricted Upload of File with Dangerous Type](https://cwe.mitre.org/data/definitions/434.html)

---

**Última actualización:** Noviembre 2025
**Versión:** 1.0
**Clase:** FileValidator.java
