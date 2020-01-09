package tursi.view.dialogs;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tursi.view.Misc;

public class TapeSectionDialog {
  
  
  private static boolean lastAvailable = false;
  private static int lastLeft;
  private static int lastLength;

  private final int bigGap = 20;
  private final int hGap   = 10;
  private final int vGap   =  4;
  
  private final JPanel panel;
  
  private final SpinnerNumberModel left;
  private final SpinnerNumberModel length;
  private final SpinnerNumberModel right;
  private boolean ignoreChanges = false;
  
  private final JButton btnLeftmost;
  private final JButton btnRightmost;
  
  private final int leftmost;
  private final int rightmost;
  
  private TapeSectionDialog(int leftmost, int rightmost) {
    panel = new JPanel();

    this.leftmost  = leftmost;
    this.rightmost = rightmost;

    left = new SpinnerNumberModel(
        lastAvailable ? lastLeft : leftmost,
        Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
    length = new SpinnerNumberModel(
        lastAvailable ? lastLength : rightmost - leftmost + 1,
        1, Integer.MAX_VALUE, 1);
    right = new SpinnerNumberModel(
        lastAvailable ? lastLeft + lastLength - 1 : rightmost,
        Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
    
    btnLeftmost  = new JButton("leftmost");
    btnRightmost = new JButton("rightmost");
    addListeners();
    assemble();
  }
  
  private void addListeners() {
    left.addChangeListener(new ChangeListener() {
      @Override public void stateChanged(ChangeEvent e) {
        if (ignoreChanges) { return; }
        int newLeftVal   = left.getNumber().intValue();
        int oldRightVal  = right.getNumber().intValue();
        int newRightVal  = newLeftVal > oldRightVal ? newLeftVal : oldRightVal;
        int newLengthVal = newRightVal - newLeftVal + 1;
        ignoreChanges = true;
        length.setValue(newLengthVal);
        right.setValue(newRightVal);
        ignoreChanges = false;
      }
    });
    length.addChangeListener(new ChangeListener() {
      @Override public void stateChanged(ChangeEvent e) {
        if (ignoreChanges) { return; }
        int newLengthVal = length.getNumber().intValue();
        int oldLeftVal   = left.getNumber().intValue();
        int newRightVal  = oldLeftVal + newLengthVal - 1;
        ignoreChanges = true;
        right.setValue(newRightVal);
        ignoreChanges = false;
      }
    });
    right.addChangeListener(new ChangeListener() {
      @Override public void stateChanged(ChangeEvent e) {
        if (ignoreChanges) { return; }
        int oldLeftVal  = left.getNumber().intValue();
        int newRightVal = right.getNumber().intValue();
        int newLeftVal  = newRightVal < oldLeftVal ? newRightVal : oldLeftVal;
        int newLengthVal = newRightVal - newLeftVal + 1;
        ignoreChanges = true;
        left.setValue(newLeftVal);
        length.setValue(newLengthVal);
        ignoreChanges = false;
      }
    });
    btnLeftmost.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        left.setValue(leftmost);
      }
    });
    btnRightmost.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        right.setValue(rightmost);
      }
    });
  }
  
  private void assemble() {
    JLabel msg = new JLabel("Tape section to be exported:");

    JSpinner left = new JSpinner(this.left);
    JSpinner length = new JSpinner(this.length);
    JSpinner right = new JSpinner(this.right);
    Misc.setPlainNumEditor(left);
    Misc.setPlainNumEditor(length);
    Misc.setPlainNumEditor(right);
    JPanel p = new JPanel(new GridLayout(3, 3, hGap, vGap));
    p.add(new JLabel("first cell", JLabel.LEFT));
    p.add(new JLabel("number of cells", JLabel.CENTER));
    p.add(new JLabel("last cell", JLabel.RIGHT));
    int width = p.getPreferredSize().width;
    p.add(left);
    p.add(length);
    p.add(right);
    p.add(btnLeftmost);
    p.add(new JPanel());
    p.add(btnRightmost);

    panel.setLayout(new BorderLayout(0, bigGap));
    panel.add(msg, BorderLayout.NORTH);
    panel.add(p, BorderLayout.CENTER);
    int height = panel.getPreferredSize().height;
    panel.setSize(width, height);
    left.setPreferredSize(left.getSize());
    length.setPreferredSize(length.getSize());
    right.setPreferredSize(right.getSize());
  }
  
  /**
   * Shows this (modal) dialog.
   * The user can select one section from the tape by specifying, the first and
   * last cell or one of them and the length of the section.
   * @return Choosed section or {@code null} if canceld.
   */
  public static Section show(JFrame owner, int leftmost, int rightmost) {
    TapeSectionDialog tsd = new TapeSectionDialog(leftmost, rightmost);
    int r = JOptionPane.showConfirmDialog(owner,tsd.panel, "Export tape",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (r == JOptionPane.OK_OPTION) {
      lastLeft   = tsd.left.getNumber().intValue();
      lastLength = tsd.length.getNumber().intValue();
      lastAvailable = true;
      return new Section(lastLeft, lastLength);
    } else {
      return null;
    }
  } 

  public static class Section {
    public final int start;
    public final int length;
    
    public Section(int start, int length) {
      this.start = start;
      this.length = length;
    }
    
    @Override
    public String toString() {
      int right = start + length - 1;
      return "Section: (" + start + ", " + length + ", " + right + ")";
    }
  } 
  
}













