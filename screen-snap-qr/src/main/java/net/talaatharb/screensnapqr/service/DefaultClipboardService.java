package net.talaatharb.screensnapqr.service;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultClipboardService implements ClipboardService {

    @Override
    public BufferedImage getImage() {
        try {
            Clipboard fxClipboard = Clipboard.getSystemClipboard();
            if (fxClipboard.hasImage()) {
                Image fxImage = fxClipboard.getImage();
                if (fxImage != null) {
                    return SwingFXUtils.fromFXImage(fxImage, null);
                }
            }
        } catch (Exception e) {
            log.debug("JavaFX clipboard image access error: {}", e.getMessage());
        }

        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                java.awt.Image awtImage = (java.awt.Image) transferable.getTransferData(DataFlavor.imageFlavor);
                if (awtImage instanceof BufferedImage bi) {
                    return bi;
                } else if (awtImage != null) {
                    int width = awtImage.getWidth(null);
                    int height = awtImage.getHeight(null);
                    if (width > 0 && height > 0) {
                        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                        var g = bi.createGraphics();
                        g.drawImage(awtImage, 0, 0, null);
                        g.dispose();
                        return bi;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("AWT clipboard image access error: {}", e.getMessage());
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<File> getFiles() {
        try {
            Clipboard fxClipboard = Clipboard.getSystemClipboard();
            if (fxClipboard.hasFiles()) {
                List<File> files = fxClipboard.getFiles();
                if (files != null && !files.isEmpty()) {
                    return files;
                }
            }
        } catch (Exception e) {
            log.debug("JavaFX clipboard file list access error: {}", e.getMessage());
        }

        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
            }
        } catch (Exception e) {
            log.warn("AWT clipboard file list access error: {}", e.getMessage());
        }
        return List.of();
    }

    @Override
    public String getText() {
        try {
            Clipboard fxClipboard = Clipboard.getSystemClipboard();
            if (fxClipboard.hasString()) {
                String str = fxClipboard.getString();
                if (str != null) {
                    return str;
                }
            }
        } catch (Exception e) {
            log.debug("JavaFX clipboard text access error: {}", e.getMessage());
        }

        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) transferable.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            log.warn("AWT clipboard text access error: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean hasContent() {
        try {
            Clipboard fxClipboard = Clipboard.getSystemClipboard();
            if (fxClipboard.hasImage() || fxClipboard.hasFiles() || fxClipboard.hasString()) {
                return true;
            }
        } catch (Exception e) {
            log.debug("JavaFX clipboard hasContent check error: {}", e.getMessage());
        }

        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            return transferable != null && (
                    transferable.isDataFlavorSupported(DataFlavor.imageFlavor) ||
                    transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                    transferable.isDataFlavorSupported(DataFlavor.stringFlavor)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
