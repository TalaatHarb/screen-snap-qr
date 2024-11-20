package net.talaatharb.screensnapqr.mapper;

import org.mapstruct.Mapper;

import com.google.zxing.ResultPoint;

import net.talaatharb.screensnapqr.dtos.QRResultPointDto;

@Mapper
public interface ResultPointToQRResultPoint {

	QRResultPointDto fromResultPoint(ResultPoint resultPoint);
}
