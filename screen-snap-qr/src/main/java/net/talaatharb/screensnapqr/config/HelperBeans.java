package net.talaatharb.screensnapqr.config;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.screensnapqr.facade.ScreenSnapQRFacade;
import net.talaatharb.screensnapqr.facade.ScreenSnapQRFacadeImpl;
import net.talaatharb.screensnapqr.service.QRService;
import net.talaatharb.screensnapqr.service.QRServiceImpl;
import net.talaatharb.screensnapqr.service.ScreenSnapService;
import net.talaatharb.screensnapqr.service.ScreenSnapServiceImpl;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HelperBeans {

	public static final ObjectMapper buildObjectMapper() {
		return JsonMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS) // ignore case
				.enable(SerializationFeature.INDENT_OUTPUT) // pretty format for json
				.addModule(new JavaTimeModule()) // time module
				.build();
	}

	public static final ScreenSnapService buildScreenSnapService(){
		try {
            return new ScreenSnapServiceImpl(GraphicsEnvironment.getLocalGraphicsEnvironment(), buildRobot());
        } catch (AWTException e) {
            log.error("Failed to create robot, {}", e.getMessage());
        }
		
		return new ScreenSnapServiceImpl(GraphicsEnvironment.getLocalGraphicsEnvironment(), null);
	}
	
	public static final Robot buildRobot() throws AWTException{
        return new Robot();
    }

	public static final QRService buildQRService(){
		return new QRServiceImpl(MapperBeans.getResultMapper());
	}

	public static final ScreenSnapQRFacade buildScreenSnapQRFacade() {
		return new ScreenSnapQRFacadeImpl(buildScreenSnapService(), buildQRService());
	}
}
