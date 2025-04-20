package net.talaatharb.screensnapqr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import net.talaatharb.screensnapqr.config.HelperBeans;
import net.talaatharb.screensnapqr.utils.FileUtils;

@ExtendWith(MockitoExtension.class)
class QRServiceImplTest {
    
    QRService qrService;
        
    @BeforeEach
    void setup() {
        qrService = HelperBeans.buildQRService();
    }

    @Test
    void testGetAllQRCodeContents() throws IOException {
        var image = FileUtils.readImageFromResources("sample_qr.png");
        var result = qrService.getAllQRCodeContents(image);
        assertThat(!result.isEmpty());
        assertThat("http://en.m.wikipedia.org".equals(result.getFirst().getText()));
    }

}
