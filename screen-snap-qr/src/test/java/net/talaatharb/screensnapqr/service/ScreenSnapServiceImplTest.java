package net.talaatharb.screensnapqr.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScreenSnapServiceImplTest {

    @InjectMocks
    ScreenSnapServiceImpl screenSnapService;

    @Mock
    GraphicsEnvironment graphicsEnvironment;

    @Mock
    GraphicsDevice graphicsDevice;
    
    @Mock
    GraphicsConfiguration graphicsConfiguration;

    @Test
    void testTakeSnapshot() throws Exception {
        Rectangle screenBound = new Rectangle(800, 600);
        when(graphicsEnvironment.getScreenDevices()).thenReturn(new GraphicsDevice[] { graphicsDevice });
        when(graphicsDevice.getConfigurations()).thenReturn(new GraphicsConfiguration[] {graphicsConfiguration});
        when(graphicsConfiguration.getBounds()).thenReturn(screenBound);

        var image = screenSnapService.takeSnapshot();

        assertNotNull(image);
    }

}
