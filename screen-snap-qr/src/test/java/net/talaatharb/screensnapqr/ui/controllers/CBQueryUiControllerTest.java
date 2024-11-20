package net.talaatharb.screensnapqr.ui.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(MockitoExtension.class)
class ScreenSnapQRUiControllerTest {

	@InjectMocks
	private MainUiController controller;

	@Test
	void test() {
		assertTrue(true);
	}

}
