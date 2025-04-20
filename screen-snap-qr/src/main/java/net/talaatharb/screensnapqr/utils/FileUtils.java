package net.talaatharb.screensnapqr.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUtils {

    public static final BufferedImage readImageFromResources(String resourceFilePath) throws IOException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream is = classLoader.getResourceAsStream(resourceFilePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceFilePath);
            }
            return ImageIO.read(is);
        }
    }
}
