package tursi.view.dialogs;

import java.io.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import tursi.console.Console;
import tursi.view.Misc;


public class UnexpectedErrorDialog {

  private final int hGap = 2;
  private final int vGap = 4;
  private final int borderGap = 12;
  
  public UnexpectedErrorDialog(Thread t, Throwable e) {
    final JDialog dialog = new JDialog();
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    
    JPanel topY = Misc.makeBoxPanel(BoxLayout.Y_AXIS);
    topY.add(new JLabel("An unexpected error occured!"));
    topY.add(Box.createVerticalStrut(vGap));
    topY.add(new JLabel("You can help and mail this error to the author."));
    JPanel top = Misc.makeBoxPanel(BoxLayout.X_AXIS);
    top.add(new JLabel(UIManager.getIcon("OptionPane.errorIcon")));
    top.add(Box.createHorizontalGlue());
    top.add(Box.createHorizontalStrut(borderGap));
    top.add(topY);
    top.add(Box.createHorizontalStrut(borderGap));
    top.add(Box.createHorizontalGlue());
    
    JTextArea tf = new JTextArea("Thread: " + t + "\n" + stackTraceToString(e));
    tf.setEditable(false);
    JScrollPane ctr = new JScrollPane(tf);
    int topPrefWidth = top.getPreferredSize().width;
    ctr.setPreferredSize(new Dimension(topPrefWidth, topPrefWidth / 2));
    
    JButton btnExit = new JButton("Exit programm");
    btnExit.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        System.exit(Console.ERR_UNKNOWN);
      }
    });
    JButton btnContinue = new JButton("Continue anyway");
    btnContinue.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        dialog.dispose();
      }
    });
    JPanel btm = Misc.makeBoxPanel(BoxLayout.X_AXIS);
    btm.add(Box.createHorizontalGlue());
    btm.add(btnExit);
    btm.add(Box.createHorizontalStrut(hGap));
    btm.add(btnContinue);
    
    JPanel p = new JPanel(new BorderLayout(0, borderGap));
    p.add(top, BorderLayout.NORTH);
    p.add(ctr, BorderLayout.CENTER);
    p.add(btm, BorderLayout.SOUTH);
    p.setBorder(BorderFactory.createEmptyBorder(
        borderGap, borderGap, borderGap, borderGap));

    dialog.getContentPane().add(p);
    
    btnExit.requestFocus();
    
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);
  }
  
  private static String stackTraceToString(Throwable e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
}
