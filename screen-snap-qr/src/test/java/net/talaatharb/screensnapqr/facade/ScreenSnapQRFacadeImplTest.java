package net.talaatharb.screensnapqr.facade;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.talaatharb.screensnapqr.service.ImportService;
import net.talaatharb.screensnapqr.service.QRService;
import net.talaatharb.screensnapqr.service.ScreenSnapService;

@ExtendWith(MockitoExtension.class)
class ScreenSnapQRFacadeImplTest {
    
    @InjectMocks
    ScreenSnapQRFacadeImpl screenSnapQRFacade;
    
    @Mock
    ScreenSnapService screenSnapService;
    
    @Mock
    QRService qrService;

    @Mock
    ImportService importService;

    @Test
    void testGetAllQRCodesFromScreen() throws Exception {
        var image = new BufferedImage(800, 600, ColorSpace.TYPE_RGB);
        when(screenSnapService.takeSnapshot()).thenReturn(image);
        
        screenSnapQRFacade.getAllQRCodesFromScreen();
        
        verify(screenSnapService).takeSnapshot();
        verify(qrService).getAllQRCodeContents(image);
    }

    @Test
    void testGetAllQRCodesFromScreenBounds() throws Exception {
        var image = new BufferedImage(400, 300, ColorSpace.TYPE_RGB);
        var bounds = new Rectangle(10, 20, 400, 300);
        when(screenSnapService.takeSnapshot(bounds)).thenReturn(image);

        screenSnapQRFacade.getAllQRCodesFromScreen(bounds);

        verify(screenSnapService).takeSnapshot(bounds);
        verify(qrService).getAllQRCodeContents(image);
    }

    @Test
    void testGetAllQRCodesFromImage() throws Exception {
        var image = new BufferedImage(200, 200, ColorSpace.TYPE_RGB);
        screenSnapQRFacade.getAllQRCodesFromImage(image);
        verify(importService).importFromImage(image);
    }

    @Test
    void testGetAllQRCodesFromImageFile() throws Exception {
        File file = new File("test.png");
        screenSnapQRFacade.getAllQRCodesFromImageFile(file);
        verify(importService).importFromImageFile(file);
    }

    @Test
    void testGetAllQRCodesFromPdfFile() throws Exception {
        File file = new File("test.pdf");
        screenSnapQRFacade.getAllQRCodesFromPdfFile(file);
        verify(importService).importFromPdfFile(file);
    }

    @Test
    void testGetAllQRCodesFromPdfFileWithRange() throws Exception {
        File file = new File("test.pdf");
        screenSnapQRFacade.getAllQRCodesFromPdfFile(file, 1, 3);
        verify(importService).importFromPdfFile(file, 1, 3);
    }

    @Test
    void testGetAllQRCodesFromClipboard() throws Exception {
        screenSnapQRFacade.getAllQRCodesFromClipboard();
        verify(importService).importFromClipboard();
    }
}
