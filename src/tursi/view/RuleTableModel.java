package tursi.view;

import javax.swing.table.*;
import tursi.machine.*;

public class RuleTableModel extends AbstractTableModel {

  private static final long serialVersionUID = 1L;

  private Rule[] rules;
  
  public RuleTableModel(Rule[] rules) {
    this.rules = rules;
  }
  
  /**
   *  Change the complete model.
   *  This is a shortcut for table.setModel(new RuleTableModel(...))).
   *  @param rules New data of the model
   */
  public void setValues(Rule[] rules) {
    this.rules = rules;
    fireTableDataChanged();
  }
  
  @Override
  public int getColumnCount() {
    return 5;
  }

  @Override
  public int getRowCount() {
    return rules.length;
  }

  @Override
  public Object getValueAt(int row, int col) {
    Rule r = rules[row];
    switch (col) {
      case 0: return r.trigger.state;
      case 1: return r.trigger.read;
      case 2: return r.action.write;
      case 3: return r.action.move;
      case 4: return r.action.nextState;
      default: throw new IndexOutOfBoundsException("column " + col);
    }
  }
  
  @Override
  public String getColumnName(int col) {
    switch (col) {
      case 0: return "state";
      case 1: return "read";
      case 2: return "write";
      case 3: return "move";
      case 4: return "next state";
      default: throw new IndexOutOfBoundsException("column " + col);
    }
  }

}
