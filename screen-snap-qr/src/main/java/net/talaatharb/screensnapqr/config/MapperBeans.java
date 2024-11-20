package net.talaatharb.screensnapqr.config;

import org.mapstruct.factory.Mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.talaatharb.screensnapqr.mapper.ResultToQRCodeResultMapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MapperBeans {

	public static final ResultToQRCodeResultMapper getResultMapper() {
		return Mappers.getMapper(ResultToQRCodeResultMapper.class);
	}
}
