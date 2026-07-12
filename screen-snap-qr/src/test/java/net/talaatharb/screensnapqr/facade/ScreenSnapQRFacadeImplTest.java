package net.talaatharb.screensnapqr.facade;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

}
