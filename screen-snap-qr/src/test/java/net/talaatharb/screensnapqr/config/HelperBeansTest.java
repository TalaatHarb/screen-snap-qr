package net.talaatharb.screensnapqr.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class HelperBeansTest {

    @Test
    void testObjectMapperCreation() {
        assertNotNull(HelperBeans.buildObjectMapper());
    }
    
    @Test
    void testScreenSnapServiceCreation() {
        assertNotNull(HelperBeans.buildScreenSnapService());
    }
    
    @Test
    void testScreenSnapQRFacadeCreation() {
        assertNotNull(HelperBeans.buildScreenSnapQRFacade());
    }

}
