package net.talaatharb.screensnapqr.ui.capture;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DefaultFileChooserProviderTest {

    @Test
    void testInstantiation() {
        DefaultFileChooserProvider provider = new DefaultFileChooserProvider();
        assertNotNull(provider);
    }
}
