package net.talaatharb.screensnapqr.ui.capture;

import java.io.File;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

public class DefaultFileChooserProvider implements FileChooserProvider {

    @Override
    public File showOpenDialog(Window owner, String title, ExtensionFilter... filters) {
        FileChooser fileChooser = new FileChooser();
        if (title != null) {
            fileChooser.setTitle(title);
        }
        if (filters != null && filters.length > 0) {
            fileChooser.getExtensionFilters().addAll(filters);
        }
        return fileChooser.showOpenDialog(owner);
    }
}
