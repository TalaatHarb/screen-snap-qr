package net.talaatharb.screensnapqr.service;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ScreenSnapServiceImpl implements ScreenSnapService {
    
    private final GraphicsEnvironment graphicsEnvironment;
    
    private final Robot robot;

	@Override
	public BufferedImage takeSnapshot() throws Exception {
		Rectangle2D allScreenBounds = new Rectangle2D.Double();
		for (GraphicsDevice gd : graphicsEnvironment.getScreenDevices()) {
		  for (GraphicsConfiguration graphicsConfiguration : gd.getConfigurations()) {
		    Rectangle2D.union(allScreenBounds, graphicsConfiguration.getBounds(), allScreenBounds);
		  }
		}

		return takeSnapshot(allScreenBounds.getBounds());
	}

	@Override
	public BufferedImage takeSnapshot(Rectangle captureBounds) throws Exception {
		return robot.createScreenCapture(captureBounds);
	}

}
