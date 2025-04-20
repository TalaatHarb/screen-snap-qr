package net.talaatharb.screensnapqr.service;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ScreenSnapServiceImpl implements ScreenSnapService {
    
    private final GraphicsEnvironment graphicsEnvironment;

	@Override
	public BufferedImage takeSnapshot() throws Exception {
		Rectangle2D allScreenBounds = new Rectangle2D.Double();
		for (GraphicsDevice gd : graphicsEnvironment.getScreenDevices()) {
		  for (GraphicsConfiguration graphicsConfiguration : gd.getConfigurations()) {
		    Rectangle2D.union(allScreenBounds, graphicsConfiguration.getBounds(), allScreenBounds);
		  }
		}

		Robot robot = new Robot();
		return robot.createScreenCapture(allScreenBounds.getBounds());
	}

}
