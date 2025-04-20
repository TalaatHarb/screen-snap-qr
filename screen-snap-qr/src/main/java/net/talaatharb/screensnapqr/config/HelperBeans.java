package net.talaatharb.screensnapqr.config;

import java.awt.GraphicsEnvironment;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.talaatharb.screensnapqr.facade.ScreenSnapQRFacade;
import net.talaatharb.screensnapqr.facade.ScreenSnapQRFacadeImpl;
import net.talaatharb.screensnapqr.service.QRService;
import net.talaatharb.screensnapqr.service.QRServiceImpl;
import net.talaatharb.screensnapqr.service.ScreenSnapService;
import net.talaatharb.screensnapqr.service.ScreenSnapServiceImpl;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HelperBeans {

	public static final ObjectMapper buildObjectMapper() {
		return JsonMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS) // ignore case
				.enable(SerializationFeature.INDENT_OUTPUT) // pretty format for json
				.addModule(new JavaTimeModule()) // time module
				.build();
	}

	public static final ScreenSnapService buildScreenSnapService(){
		return new ScreenSnapServiceImpl(GraphicsEnvironment.getLocalGraphicsEnvironment());
	}

	public static final QRService buildQRService(){
		return new QRServiceImpl(MapperBeans.getResultMapper());
	}

	public static final ScreenSnapQRFacade buildScreenSnapQRFacade() {
		return new ScreenSnapQRFacadeImpl(buildScreenSnapService(), buildQRService());
	}
}
