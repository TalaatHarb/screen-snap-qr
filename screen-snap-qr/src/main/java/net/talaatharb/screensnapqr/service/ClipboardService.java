package net.talaatharb.screensnapqr.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public interface ClipboardService {

    BufferedImage getImage();

    List<File> getFiles();

    String getText();

    boolean hasContent();
}
