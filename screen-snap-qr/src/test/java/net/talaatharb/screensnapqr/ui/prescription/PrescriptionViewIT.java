package net.talaatharb.screensnapqr.ui.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;

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

    @Test
    void testFieldValuesAreCopyableTextFields() {
        final PrescriptionDocument document = PrescriptionDocument.builder()
                .rootElementName("P")
                .headerAttributes(Map.of("SC", "AB"))
                .build();

        final Node[] result = new Node[1];
        final javafx.stage.Stage[] stageRef = new javafx.stage.Stage[1];
        interact(() -> {
            result[0] = PrescriptionView.build(document);
            final javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(new javafx.scene.Scene((Parent) result[0], 600, 400));
            stage.show();
            stageRef[0] = stage;
        });

        final ScrollPane scrollPane = (ScrollPane) result[0];
        final List<TextField> textFields = findTextFields((Parent) scrollPane.getContent());

        assertFalse(textFields.isEmpty());
        assertTrue(textFields.stream().anyMatch(field -> "AB".equals(field.getText())));
        assertTrue(textFields.stream().allMatch(field -> !field.isEditable()));

        interact(() -> stageRef[0].close());
    }

    private static List<TextField> findTextFields(Parent parent) {
        final List<TextField> found = new java.util.ArrayList<>();
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof TextField textField) {
                found.add(textField);
            } else if (child instanceof Parent childParent) {
                found.addAll(findTextFields(childParent));
            }
        }
        return found;
    }
}
