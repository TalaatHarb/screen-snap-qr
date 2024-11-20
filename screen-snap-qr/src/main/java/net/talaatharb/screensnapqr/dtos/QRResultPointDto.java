package net.talaatharb.screensnapqr.dtos;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class QRResultPointDto {
	  private final float x;
	  private final float y;
}
