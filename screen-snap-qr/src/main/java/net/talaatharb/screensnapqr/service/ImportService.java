package net.talaatharb.screensnapqr.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

public interface ImportService {

    List<QRCodeResultDto> importFromImage(BufferedImage image) throws IOException;

    List<QRCodeResultDto> importFromImageFile(File file) throws IOException;

    List<QRCodeResultDto> importFromPdfFile(File file) throws IOException;

    List<QRCodeResultDto> importFromPdfFile(File file, int startPage, int endPage) throws IOException;

    List<QRCodeResultDto> importFromClipboard() throws IOException;
}
