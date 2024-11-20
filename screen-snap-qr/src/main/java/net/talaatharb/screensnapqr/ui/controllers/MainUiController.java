package net.talaatharb.screensnapqr.ui.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.screensnapqr.config.HelperBeans;
import net.talaatharb.screensnapqr.facade.ScreenSnapQRFacade;

@Slf4j
@RequiredArgsConstructor
public class MainUiController implements Initializable {

	private final ScreenSnapQRFacade screenSnapQRFacade;

	public MainUiController() {
		screenSnapQRFacade = HelperBeans.buildScreenSnapQRFacade();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		log.info("Initializing UI application Main window controller...");

	}
	
	@FXML
	void newQRSnap() {
		try {
			var result = screenSnapQRFacade.getAllQRCodesFromScreen();
			log.info(result.toString());
		} catch (Exception e) {
			log.error("Unable to take screen shot due to: {}", e.getMessage());
		}
	}
}
