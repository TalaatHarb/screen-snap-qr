package net.talaatharb.screensnapqr.ui.capture;

import java.io.File;

import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

public interface FileChooserProvider {

    File showOpenDialog(Window owner, String title, ExtensionFilter... filters);
}
