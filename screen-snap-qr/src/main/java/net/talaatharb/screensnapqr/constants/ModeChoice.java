package net.talaatharb.screensnapqr.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ModeChoice {

	SCREEN(0, "Screen"), SELECTION(1, "Selection Box"), WINDOW(2, "Select Window");
	
	private final Integer order;
	private final String text;
}
