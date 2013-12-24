package tursi.view.dialogs;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.List;

import tursi.parsing.*;
import tursi.view.Misc;

public class ParserResultDialog {

  /** String to be displayed instead of the line, if it is unknown. */
  private final String noLine = "-";
  private final int gap = 12;
  
  public ParserResultDialog(JFrame owner, String fileName,
      ParserException error, List<ParserException> warnings) {
    
    int len = fileName.length();
    if (len > 30) {
      fileName = "..." + fileName.substring(len - 1 - 27, len);
    }
    
    final JDialog dialog = new JDialog(
        owner, "Parsing result '" + fileName + "'");
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    
    boolean errorOccured = error != null;
    Icon icn = errorOccured ? UIManager.getIcon("OptionPane.errorIcon")
        : UIManager.getIcon("OptionPane.warningIcon");
    String statMsg = makeStatMsg(fileName, errorOccured);
    String countMsg = makeCountMsg(errorOccured, warnings.size());
    JScrollPane errTabScrollPane = makeErrTable(error);
    JScrollPane warnTabScrollPane = makeWarnTable(warnings);

    JButton btnOK = new JButton("OK");
    btnOK.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        dialog.setVisible(true);
        dialog.dispose();
      }
    });
 
    JPanel topMsgStack = makeBoxPanel(BoxLayout.Y_AXIS);
    topMsgStack.add(Box.createVerticalGlue());
    topMsgStack.add(new JLabel(statMsg));
    topMsgStack.add(Box.createVerticalGlue());
    topMsgStack.add(new JLabel(countMsg));
    topMsgStack.add(Box.createVerticalGlue());
    JPanel top = makeBoxPanel(BoxLayout.X_AXIS);
    top.add(new JLabel(icn));
    top.add(Box.createRigidArea(new Dimension(gap, 0)));
    top.add(topMsgStack);
    
    JPanel btm = makeBoxPanel(BoxLayout.X_AXIS);
    btm.add(Box.createGlue());
    btm.add(btnOK);
    
    JPanel ctr = new JPanel(new BorderLayout(0, gap));
    if (errTabScrollPane != null) {
      ctr.add(errTabScrollPane, BorderLayout.NORTH);
    }
    ctr.add(warnTabScrollPane, BorderLayout.CENTER);

    JPanel p = new JPanel(new BorderLayout(0, gap));
    p.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));
    dialog.getContentPane().add(p);

    p.add(top, BorderLayout.NORTH);
    p.add(btm, BorderLayout.SOUTH);
    Dimension minDialogSize = dialog.getPreferredSize();
    p.add(ctr, BorderLayout.CENTER);
    
    Dimension prefDialogSize = new Dimension(minDialogSize);
    prefDialogSize.height *= (error != null) ? 2.5 : 2;
    dialog.setMinimumSize(minDialogSize);
    dialog.setPreferredSize(prefDialogSize);
    dialog.setSize(prefDialogSize);
    dialog.setLocationRelativeTo(owner);
    dialog.setVisible(true);
  }
  
  private static String makeStatMsg(String fileName, boolean error) {
    String msg = error ? "Couldn't parse" : "Sucessfully parsed";
    msg += " file '" + fileName + "'.";
    return msg;
  }
  
  private static String makeCountMsg(boolean error, int warnings) {
    String msg = (error ? "An error and " : "") + warnings + " warning";  
    if (warnings != 1) { msg += "s"; }
    msg += " occured.";
    return msg;
  }
  
  private JScrollPane makeErrTable(ParserException error) { 
    if (error == null) { return null; }
    
    Object[][] model = {{
      error.lineAvailable() ? Integer.toString(error.getLine()) : noLine,
      error.getMessage()
    }};
    JTable tab = new JTable(model, new String[]{"Line", "Error"});
    tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    Misc.adjustColumnWidth(tab);
    tab.getTableHeader().setReorderingAllowed(false);
    tab.setEnabled(false);
    tab.setPreferredScrollableViewportSize(tab.getPreferredSize());
    
    JScrollPane sp = new JScrollPane(tab);
    sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    return sp;
  }
  
  private JScrollPane makeWarnTable(List<ParserException> warnings) {
    Object[][] model = new Object[warnings.size()][2]; 
    int i = 0;
    for (ParserException warning : warnings) {
      model[i][0] = warning.lineAvailable() ?
          Integer.toString(warning.getLine()) : noLine;
      model[i][1] = warning.getMessage();
      ++i;
    }
    JTable tab = new JTable(model, new String[]{"Line", "Warning"});
    tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    Misc.adjustColumnWidth(tab);
    tab.getTableHeader().setReorderingAllowed(false);
    tab.setEnabled(false);
    tab.setPreferredScrollableViewportSize(tab.getPreferredSize());
    
    JScrollPane sp = new JScrollPane(tab);
    sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    return sp;
  }
  
  private static JPanel makeBoxPanel(int alignment) {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, alignment));
    return p;
  }

}