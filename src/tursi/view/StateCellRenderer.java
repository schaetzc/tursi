package tursi.view;

import java.awt.*;
import javax.swing.*;

public class StateCellRenderer implements ListCellRenderer<String> {
  
  private RuleCell cell;
  private StateTypeTester tester;

  private boolean markCur;
  private boolean markStart;
  private boolean markBreak;
  private boolean markEnd;
  
  private JComboBox<String> comboBox;
  
  public StateCellRenderer(JComboBox<String> comboBox, StateTypeTester tester) {
    this.comboBox = comboBox;
    this.cell = new RuleCell();
    this.tester = tester;
  }
  
  @Override
  public Component getListCellRendererComponent(JList<? extends String> list, String value,
      int index, boolean isSelected, boolean cellHasFocus) {
    
    String st = value == null ? "" : value;

    if (isSelected || comboBox.isPopupVisible() && markCur && tester.isCurrentState(st)) {
      cell.setForeground(list.getSelectionForeground());
      cell.setBackground(list.getSelectionBackground());
    } else {
      cell.setForeground(list.getForeground());
      cell.setBackground(list.getBackground());
    }    
    
    if (markStart) {
      cell.showIcon(RuleCell.START, tester.isStartState(st));
    } else {
      cell.hideIcon(RuleCell.START);
    }
    if (markBreak) {
      cell.showIcon(RuleCell.BREAK, tester.isBreakState(st));
    } else {
      cell.hideIcon(RuleCell.BREAK);
    }
    if (markEnd) {
      cell.showIcon(RuleCell.END, tester.isEndState(st));
    } else {
      cell.hideIcon(RuleCell.END);
    }

    cell.setFont(list.getFont());
    cell.setText(st);

    return cell;
  }

  public boolean setMarks(boolean markCur, boolean markStart, boolean markBreak, boolean markEnd) {
    boolean changed = false;
    if (this.markCur != markCur) {
      changed = true;
      this.markCur = markCur;
    }
    if (this.markStart != markStart) {
      changed = true;
      this.markStart = markStart;
    }
    if (this.markBreak != markBreak) {
      changed = true;
      this.markBreak = markBreak;
    }
    if (this.markEnd != markEnd) {
      changed = true;
      this.markEnd = markEnd;
    }
    return changed;
  }

}
