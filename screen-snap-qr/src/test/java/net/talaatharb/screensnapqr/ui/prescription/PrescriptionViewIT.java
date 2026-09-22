package net.talaatharb.screensnapqr.ui.prescription;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

class PrescriptionViewIT extends ApplicationTest {

    @Test
    void testBuildRendersPrescriptionSections() {
        final PrescriptionDocument document = PrescriptionDocument.builder()
                .rootElementName("P")
                .headerAttributes(Map.of("SC", "AB", "SN", "1234567"))
                .prescriptionDetails(Map.of("FN", "Ion", "LN", "Popescu"))
                .pharmacyDetails(Map.of("CC", "777666"))
                .prescribedMedications(List.of(Map.of("AS", "Paracetamolum", "Q", "20")))
                .dispensedMedications(List.of(Map.of("D", "W12345", "IQ", "20")))
                .build();

        final Node[] result = new Node[1];
        interact(() -> result[0] = PrescriptionView.build(document));

        assertNotNull(result[0]);
        assertInstanceOf(ScrollPane.class, result[0]);
    }
}
