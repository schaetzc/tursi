package tursi.view;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Renders the transition table (5 cols) or history table (6 cols).
 * The table model has to be of the form:
 * (String)state, (Character)read, (Character)write, (Integer)move, (String)next
 * or (for the history table)
 * (Integer)step, (String)state, (Character)read, ...
 */
public class TableRenderer implements TableCellRenderer {

  private final int colState;
  private final int colMove;
  private final int colNextState;

  private final RuleCell cell;

  private boolean markCur;
  private boolean markStart;
  private boolean markBreak;
  private boolean markEnd;
  private StateTypeTester tester;
  private AliasConverter aliasConv;
  
  public static TableRenderer makeRuleRenderer(
      StateTypeTester tester, AliasConverter aliasConv) {
    
    return new TableRenderer(tester, aliasConv, 0, 3, 4);
  }
  
  public static TableRenderer makeHistRenderer(
      StateTypeTester tester, AliasConverter aliasConv) {
    
    return new TableRenderer(tester, aliasConv, 1, 4, 5);
  }
  
  private TableRenderer(StateTypeTester tester, AliasConverter aliasConv,
      int colState, int colMove, int colNextState) {
    
    this.cell = new RuleCell();
    this.colState     = colState;
    this.colMove      = colMove;
    this.colNextState = colNextState;
    this.tester    = tester;
    this.aliasConv = aliasConv;
  }
  
  public boolean setMarks(boolean markCur,
      boolean markStart, boolean markBreak, boolean markEnd) {
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
  
  
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int col) {

    String st = table.getModel().getValueAt(row, colState).toString();
    if (markCur && tester.isCurrentState(st)) {
      cell.setForeground(table.getSelectionForeground());
      cell.setBackground(table.getSelectionBackground());
    } else {
      cell.setForeground(table.getForeground());
      cell.setBackground(table.getBackground());
    }

    boolean stCol = col == colState || col == colNextState;
    String text;
    if (col == colMove) {
      text = (value == null) ? "" : aliasConv.convertToAlias((Integer) value);
    } else {
      text = (value == null) ? "" : value.toString();
    }
    if (stCol && markStart) {
      cell.showIcon(RuleCell.START, tester.isStartState(text));
    } else {
      cell.hideIcon(RuleCell.START);
    }
    if (stCol && markBreak) {
      cell.showIcon(RuleCell.BREAK, tester.isBreakState(text));
    } else {
      cell.hideIcon(RuleCell.BREAK);
    }
    if (stCol && markEnd) {
      cell.showIcon(RuleCell.END, tester.isEndState(text));
    } else {
      cell.hideIcon(RuleCell.END);
    }
    
    cell.setFont(table.getFont());
    cell.setText(text);

    return cell;
  }
}
