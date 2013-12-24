package tursi.machine;

import javax.swing.table.AbstractTableModel;

import tursi.machine.events.HistoryListener;

/**
 * Stack-like History, which can only store a fixed number of entries.
 * However, the maximum size can be changed, when in use.
 * If the maximum size is reached, every {@code push} discards the oldest entry
 * to make room for the new one. 
 * 
 * TODO
 * TODO update comments (because this class also is the stack)
 * TODO
 */
public class LimitedHistory extends AbstractTableModel implements History {
  
  private static final long serialVersionUID = 1L;
  
  /** Index of the oldest element in the stack. */
  private int start = 0;
  /** Number of elements in the stack. */
  private int size = 0;
  /** Array, used as the stack (cycles over the borders). */
  private Rule[] stack;
  
  
  /** Number of taken steps (can be {@code > deque.size()}). */
  private long steps = 0;
  
  private HistoryListener historyListener = null;
  
  /**
   * Creates a new LimitedHistory with given maximal size.
   * @param max Maximal number of entries in this history (must be > 0).
   */
  public LimitedHistory(int max) {
    if (max < 1) { throw new IllegalArgumentException("max < 1"); }
    stack = new Rule[max];
  }
  
  /** @return Number of rules, currently stored. */
  public int size() {
    return size;
  }
  
  /**
   * Gets a specified entry. {@code 0} is the oldest stored entry,
   * {@code size()} the newest one.
   * Remember, that {@code 0} is not always the first step! You can calculate
   * the step from the index using {@link #stepAt(int)}.
   * @param i Index (0 is oldest)
   * @return Taken step at this index.
   */
  public Rule get(int i) {
    if (i < 0 || i >= size) { throw new IndexOutOfBoundsException("" + i); }
    return stack[(start + i) % stack.length];
  }
  
  /**
   * Calculates which step (1st, 2nd, 3rd, ...) is stored at index {@code i}.
   * @param i Index of the step.
   * @return Step's number, starting from 1.
   */
  public long stepAt(int i) {
    // +1, because first step (1) has index 0
    return steps - size + i + 1;
  }
  
  /** @return Maximum size of the stack. */ 
  public int getMaxSize() {
    return stack.length;
  }
  
  /**
   * Changes the maximum number of entries in this history.
   * If the {@code currentSize > max}, the {@code currentSize - max} oldest
   * items will be removed. 
   * @param max
   */
  public void setMaxSize(int max) {
    if (max < 1) { throw new IllegalArgumentException("max < 1"); }
    if (max != stack.length) {
      int newSize = max < size ? max : size;
      int deleted = size - newSize;
      Rule[] newStack = new Rule[max];
      for (int i = 0; i < newSize; ++i) { // keep newest elements
        newStack[i] = stack[(start + deleted + i) % stack.length];
      }
      start = 0;
      size  = newSize;
      stack = newStack;
      if (deleted > 0) {
        fireHistoryContentChanged();
        fireTableRowsDeleted(size, size + deleted - 1);
      }
    }
  }
  
  @Override
  public void push(Rule r) {
    if (r == null) { throw new IllegalArgumentException("null"); }
    ++steps;
    if (size < stack.length) {
      stack[(start + size) % stack.length] = r;
      ++size;
      fireTableRowsInserted(size-1, size-1);
    } else {
      stack[start] = r;
      start = (start + 1) % stack.length;
      fireTableRowsDeleted(0, 0);
      //this.fireTableRowsInserted(size-1, size-1); // not necessary
    }
    fireHistoryContentChanged();
  }

  @Override
  public Rule pop() {
    if (isEmpty()) {
      return null;
    } else {
      --steps;
      --size;
      fireHistoryContentChanged();
      fireTableRowsDeleted(size, size);
      return stack[(start + size) % stack.length];
    }
  }

  @Override
  public long steps() {
    return steps;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }
  
  @Override
  public void clear() {
    if (size > 0 || steps > 0) {
      steps = 0;
      start = 0;
      size = 0;
      fireHistoryContentChanged();
      fireTableStructureChanged();
    }
  }
  
  public boolean isComplete() {
    return steps <= size;
  }

  // Methods for history listener //////////////////////////////////////////////
  
  public void setHistoryListener(HistoryListener historyListener) {
    this.historyListener = historyListener;
  }
  
  public HistoryListener getHistoryListener() {
    return historyListener;
  }
  
  private void fireHistoryContentChanged() {
    if (historyListener != null) {
      historyListener.historyContentChanged();      
    }
  }
  
  // Methods for Swing's TableModel ////////////////////////////////////////////
  
  @Override
  public int getColumnCount() {
    return 6; // number of step + 5 fields from Rule
  }

  @Override
  public int getRowCount() {
    return size;
  }

  @Override
  public Object getValueAt(int row, int col) {
    Rule r = get(row);
    switch (col) {
      case 0: return stepAt(row);
      case 1: return r.trigger.state;
      case 2: return r.trigger.read;
      case 3: return r.action.write;
      case 4: return r.action.move;
      case 5: return r.action.nextState;
      default: throw new IndexOutOfBoundsException("column " + col);
    }
  }
  
  @Override
  public String getColumnName(int col) {
    switch (col) {
      case 0: return "step";
      case 1: return "last state";
      case 2: return "read";
      case 3: return "write";
      case 4: return "move";
      case 5: return "state";
      default: throw new IndexOutOfBoundsException("column " + col);
    }
  }

}
