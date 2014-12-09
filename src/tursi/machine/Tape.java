package tursi.machine;

import java.util.ArrayList;

import tursi.machine.events.*;

/**
 * Tape of a turing machine, consisting of infinite numbered, read and writable
 * cells. Works like an array of chars, but can be written and read on every
 * index (including negative ones). New cells are filled, according to a
 * specified, repetitive pattern (can also be only one character).
 * For convenience, the position of a head (read and write) is stored, allowing
 * access to the tape, without fiddling with indices. The current position
 * starts at cell 0.
 *
 * The tape is implemented by two dynamic arrays. One for the negative indices
 * (tapes left side) and one for the natural ones (including 0) (right side).
 */
public class Tape {
	
  /** Current position on the tape. Starts with 0. */
	private int pos = 0;
	
	/**
	 * Leftmost and rightmost cell ever accessed with current position (but not
	 * necessarily be read or modified). Random access on cells does not modify
	 * these variables.
	 */
	private int leftmost = 0, rightmost = 0;

	/**
	 * Dynamic array for negative cells.
	 * Cell -1 refers to index 0, -2 to 1 and so on. Convert cell number {@code c}
	 * to an index using {@code -1-c}. Works also for converting indices to cell
	 * numbers.
	 */
	private char[] left;
	
	/** Dynamic array for natural cells (including 0). */
	private char[] right;
	
	/**
   * Initial fill pattern for all cells.
   * Cell 0 will contain the first char from the pattern, cell 1 the second one
   * and so on. The fill pattern will loop, when indices exceed its length.
   * Negative cells are filled fitting to this loop. E.g. {@code abc[a]bcabc},
   * when {@code abc} was the fill pattern and {@code [a]} cell 0.
   */
  private final char[] fillPattern;
	
  /** Expansion factor for the dynamic arrays. */
  private final double expFactor;
  
  private ArrayList<TapeListener> listeners = new ArrayList<TapeListener>();
  
	/**
	 * Create a new Tape with a given fill pattern. 
	 * @param fill Tapes initial content. Must not be empty.
	 * 
	 * @see #fillPattern
	 */
	public Tape(String fillPattern) {
	  this(50, 100, 1.5, fillPattern);
	}
	
	/**
	 * Create a new Tape with a given fill pattern and options for the dynamic
	 * arrays.
	 * @param leftLen     Initial length for the left dynamic array. Must be >= 0.
	 * @param rightLen    Initial length for the right dynamic array. Must be > 0.
	 * @param expFactor   Expansion factor for the dynamic arrays.
	 *                    Should be greater than 1 or the array will only be
	 *                    expanded to the needed cell, which is inefficient.
	 * @param fillPattern Tapes initial content. Must not be empty.
	 * 
	 * @see #fillPattern
	 */
	public Tape(int leftLen, int rightLen, double expFactor, String fillPattern) {
		if (leftLen < 0 || rightLen < 1) {
			throw new IllegalArgumentException("Tape to short");
		} else if (fillPattern == null || fillPattern.isEmpty()) {
			throw new IllegalArgumentException("No fill pattern");
		}
		this.left        = new char[leftLen];
		this.right       = new char[rightLen];
		this.expFactor   = expFactor;
		this.fillPattern = fillPattern.toCharArray(); 
		for (int i = 0; i < leftLen; ++i) { left[i] = calcfillAt(i, true); }
		for (int i = 0; i < rightLen; ++i) { right[i] = calcfillAt(i, false); }
	}
	
	/**
	 * Add a TapeListener to this tape. TapeListeners will be informed about
	 * possible changes (values on this Tape and the position of its head).
	 * @param tl TapeListener to be added.
	 */
	public void addTapeListener(TapeListener tl) {
	  if (tl != null) {
	    listeners.add(tl);
	  }
	}
	
	/**
	 * Remove a given TapeListener, so that it will not be informed about further
	 * events. If the same listener was added multiple times, only the first
	 * one will be removed.
	 * @param tl TapeListener to be removed.
	 */
	public void removeTapeListener(TapeListener tl) {
	  listeners.remove(tl);
	}
	
	/** Remove all TapeListeners from this tape. */
	public void removeAllTapeListener() {
    listeners.clear();
  }
	
  /**
   * Inform all TapeListeners about a changed states (values on this tape and
   * the position of its head).
   * Mind, that you should check for real changes, before calling this method.
   */
	public void fireTapeEvent() {
	  for (TapeListener tl : listeners) {
	    tl.tapeChanged();
	  }
	}
	
	/**
	 * Change the current position by a relative distance.
	 * @param cells Movement of the head in cells. Use positive numbers to move
	 *              to the right, and negative numbers to move to the left.
	 */
	public void move(int cells) {
		setPos(pos + cells);
	}
	
	/**
	 * Set the current position to a absolute position.
	 * @param cell New position as a cell number.
	 */
	public void setPos(int cell) {
	  if (pos != cell) {
	    pos = cell;
	    updateBorders();
	    fireTapeEvent();
	  }
	}
	
	/** @return Current position as a cell number. */
	public int getPos() {
	  return pos;
	}
	
	/** @return All time minimal value of the current position variable. */
	public int getLeftmost() {
	  return leftmost;
	}
	
	/** @return All time maximal value of the current position variable. */
	public int getRightmost() {
	  return rightmost;
	}
	
	/**
	 * Update the variables {@link #leftmost} and {@link #rightmost}.
	 * Call this after {@link #pos} was changed.
	 */
	private void updateBorders() {
	  if (pos < leftmost) {
	    leftmost = pos;
	  } else if (pos > rightmost) {
	    rightmost = pos;
	  }
	}
	
	/**
	 * Read the cell at the current position.
	 * @return Cells content.
	 */
	public char read() {
	  return read(pos);
	}
	
	/**
	 * Read a specified cell.
	 * @param cell Number of the cell to be read.
	 * @return Content of the specified cell.
	 */
	public char read(int cell) {
		ensureCell(cell);
		if (cell < 0) {
			return left[-1-cell];
		} else {
		  return right[cell];
		}
	}
	
	/**
	 * Read a sequent part of the tape.
	 * This is more efficient than many single reads, because the lengths of the
	 * dynamic arrays don't need to be checked for every cell.
	 * 
	 * @param startCell First cell to be read.
	 * @param cells     Number of cells to be read. Must be positive, or an empty
	 *                  array will be returned.
	 * @return Array containing a part of the tape. First element will be content
	 *         of {@code startCell}, the second {@code startCell+1} and so on. 
	 */
	public char[] read(int startCell, int cells) {
	  if (cells < 1) { return new char[0]; }
	  char[] part = new char[cells];
	  ensureCell(startCell);
	  ensureCell(startCell + cells - 1);
	  for (int i = 0; i < cells; ++i) {
	    int c = startCell + i;
	    part[i] = c < 0 ? left[-1-c] : right[c];
	  }
	  return part;
	}
	
	/**
	 * Write to the cell at the current position.
	 * @param value Value to be written into this cell.
	 */
	public void write(char value) {
	  write(pos, value);
	}
	
	/**
	 * Write to a specified cell.
	 * @param cell  Number of the cell to be written to.
	 * @param value Value to be written into this cell.
	 */
	public void write(int cell, char value) {
	  if (writeWithoutListeners(cell, value)) {
	    fireTapeEvent();	    
	  }
	}
	
	/**
   * Write to a specified cell, but don't inform any listeners. This can be
   * used to accumulate multiple writing operations in one TapeEvent.
   * @param cell  Number of the cell to be written to.
   * @param value Value to be written into this cell.
   * @return Cell's value has changed.
   */
	private boolean writeWithoutListeners(int cell, char value) {
	  ensureCell(cell);
	  int old;
    if (cell < 0) {
      cell = -1-cell;
      old = left[cell];
      left[cell] = value;
    } else {
      old = right[cell];
      right[cell] = value;
    }
    return old != value;
	}
	
	/**
	 * Write multiple values to multiple, sequent cells, starting from a
	 * specified cell. First char of the String will be written to cell
	 * {@code startCell}, the second one to {@code startCell+1} and so on.
	 * @param startCell First cell to be written to.
	 * @param values    Values to be written.
	 */
	public void write(int startCell, String values) {
	  if (values.isEmpty()) { return; }
		ensureCell(startCell);
		ensureCell(startCell + values.length());
		boolean changed = false;
		for(int i = 0; i < values.length(); ++i) {
		  if (writeWithoutListeners(startCell + i, values.charAt(i))) {
		    changed = true;
		  }		  
		}
		if (changed) { fireTapeEvent(); }
	}

	/**
	 * Write multiple values to multiple, sequent cells, starting from a
	 * calculated cell, so that the last value will be written to a specified one
	 * (writeTw stands for "write towards"). Last char of the String will be
	 * written to {@code endCell}, second last to {@code endCell-1} and so on.
	 * @param endCell Last cell to be written to.
	 * @param values  Values to be written.
	 */
	public void writeTw(int endCell, String values) {
	  write(endCell - values.length() + 1, values);
	}
	
	/**
	 * Prints the complete tape in two lines.
	 * First line contains all negative numbered cells, the second the others. 
	 */
	public String toString() {		
	  StringBuilder sb = new StringBuilder();
	  sb.append(left);
	  sb.reverse();
	  sb.append("\n");
	  sb.append(right);
	  return sb.toString();
	}

	/**
	 * Ensures, that a specified cell exists. If it doesn't, the dynamic array
	 * will be expanded to this cell or beyond it.
	 * @param cell Cell to be ensured.
	 */
	private void ensureCell(int cell) {
	  if (cell >= right.length) {
			right = expandTo(cell, right, false);
	  } else if (-1-cell >= left.length) { // convert cell number to index
		  left = expandTo(-1-cell, left, true); 
		}
	}
	
	/**
	 * Expands an array to a given length or beyond (using {@link #expFactor}).
	 * Old values will be copied, new fields are filled using
	 * {@link #fillPattern}, which can be inverted, when expanding {@link #left}. 
	 * 
	 * @param minIdx     Index (not a cell number!), which should be usable at
	 *                   least. This is the minimal length + 1 of the new array.
	 * @param src        Old array.
	 * @param invertFill Invert {@link #fillPattern}.
	 *                   Used, when filling {@link #left}.
	 * @return New, expanded or array or {@code src} if the length was sufficient.
	 */
	private char[] expandTo(int minIdx, char[] src, boolean invertFill) {
	  if(minIdx < src.length) { return src; }
	  char[] a = new char[Math.max((int)(src.length * expFactor), minIdx + 1)];
		System.arraycopy(src, 0, a, 0, src.length);
		for (int i = src.length; i < a.length; ++i) {
		  a[i] = calcfillAt(i, invertFill);
		}
		return a;
	}
	
	/**
	 * Retrieves an specified element from an virtual, infinite array, consisting
	 * of the looped {@link #fillPattern}.
	 * @param i      Index for the virtual infinite array. Must not be negative.
	 * @param invert Invert the fill pattern before looping.
	 *               Used, when filling {@link #left}.
	 * @return Fill for the specified index.
	 * 
	 * @see #fillPattern
	 */
	private char calcfillAt(int i, boolean invert) {
		if(invert) {
		  // next multiple of fillPattern.length which is > i
		  int l = fillPattern.length * (i / fillPattern.length + 1);
		  return fillPattern[l - i - 1];
		} else {
		  return fillPattern[i % fillPattern.length];		  
		}
	}
	
}