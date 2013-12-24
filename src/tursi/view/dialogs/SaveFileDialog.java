package tursi.view.dialogs;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * A extension of the file chooser, which asks to overwrite, when choosing
 * an existing file. When choosing directories, they open them instead of
 * asking to overwrite it (which would also be an possible option).
 * 
 * It's recommended <b>not</b> to use 
 * {@link #showOpenDialog(java.awt.Component)}, since users would be confused
 * by the overwrite confirmation dialog, when they were only expecting to read
 * an file.
 */
public class SaveFileDialog extends JFileChooser {

  private static final long serialVersionUID = 1L;

  public SaveFileDialog(String f, String extDetail, String ext) {
    this(new File(f), extDetail, ext);
  }
  
  public SaveFileDialog(File f, String extDetail, String ext) {
    super(f);
    javax.swing.filechooser.FileFilter defaultFilter = getFileFilter();
    addChoosableFileFilter(new FileNameExtensionFilter(extDetail, ext));
    setFileFilter(defaultFilter);
  }
  
  @Override public void approveSelection() {
    File f = getSelectedFile();
    if (f.isDirectory()) {
      setCurrentDirectory(f);
    } else  if (confirmOverwrite(f)) {
      super.approveSelection();
    }
  }

  public boolean confirmOverwrite(File f) {
    if (!f.exists()) { return true; }
    String title = "Overwrite existing file?";
    String msg = "The file \"" + f.getName() + "\" already exists.\n"
        + "Do you want to replace the existing file?";
    int ret = JOptionPane.showConfirmDialog(this, msg, title,
        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    return ret == JOptionPane.YES_OPTION;
  }
  
}
