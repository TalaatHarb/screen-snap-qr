package net.talaatharb.screensnapqr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;

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
    
    @Mock
    Robot robot;

    @Test
    void testTakeSnapshot() throws Exception {
        var image = new BufferedImage(800, 600, ColorSpace.TYPE_RGB);
        Rectangle screenBound = new Rectangle(800, 600);
        when(graphicsEnvironment.getScreenDevices()).thenReturn(new GraphicsDevice[] { graphicsDevice });
        when(graphicsDevice.getConfigurations()).thenReturn(new GraphicsConfiguration[] {graphicsConfiguration});
        when(graphicsConfiguration.getBounds()).thenReturn(screenBound);
        when(robot.createScreenCapture(screenBound)).thenReturn(image);

        var result = screenSnapService.takeSnapshot();

        assertNotNull(result);
        assertThat(result.equals(image));
    }

}
