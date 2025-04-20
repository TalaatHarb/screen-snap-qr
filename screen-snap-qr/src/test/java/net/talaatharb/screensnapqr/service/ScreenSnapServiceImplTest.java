package net.talaatharb.screensnapqr.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.talaatharb.screensnapqr.config.HelperBeans;

class ScreenSnapServiceImplTest {
    
    ScreenSnapService screenSnapService;
    
    @BeforeEach
    void setup() {
        screenSnapService = HelperBeans.buildScreenSnapService();
    }

    @Test
    void testTakeSnapshot() throws Exception {
        var image = screenSnapService.takeSnapshot();
        assertNotNull(image);
    }

}
