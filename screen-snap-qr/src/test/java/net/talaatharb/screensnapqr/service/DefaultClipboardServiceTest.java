package net.talaatharb.screensnapqr.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DefaultClipboardServiceTest {

    @Test
    void testClipboardServiceMethodsDoNotThrow() {
        DefaultClipboardService service = new DefaultClipboardService();
        assertDoesNotThrow(service::getImage);
        assertNotNull(service.getFiles());
        assertDoesNotThrow(service::getText);
        assertDoesNotThrow(service::hasContent);
    }
}
