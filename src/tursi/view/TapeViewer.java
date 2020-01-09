package tursi.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import tursi.machine.*;
import tursi.machine.events.*;
import tursi.view.events.*;

import java.util.ArrayList;


/**
 * Renders a section of the tape, which can be scrolled.
 * The style of tables is used for consistent look.
 */
public class TapeViewer extends JComponent
    implements MouseInputListener, MouseWheelListener, TapeListener {
  
  private static final long serialVersionUID = 1L;
  
  public final static int SCROLL_NONE      = 0;
  public final static int SCROLL_BORDERS   = 1;
  public final static int SCROLL_IMMEDIATE = 2;
  
  private Tape tape;
  /**
   * Number of boxes to the border, when scrolling starts.
   * This number is the number of boxes between the head (exclusive)
   * and the border (not visible).
   */
  private int scrollBorder = 2;
  /** Number of cells, to be colored with a slightly darker background. */
  private int stripeSize   = 10;
  private int scrollMode;
  
  private final Color fgColor; 
  private final Color bgColorTape;
  private final Color bgColorTapeStripe;
  private final Color bgColorHead; 
  
  private final Font  font;
  private final FontMetrics metrics;
  private final int charHeight;  
  private final int boxCenterBaseline;
  private final int boxSize;
  private final int top;
  private final int bottom;

  // Following values will be initialized, when the panel is drawn
  /** First (partially) visible cell. */
  private int firstCell;
  private int headBox;
  /** Complete visible cells (+2 partially or not visible cells). */
  private int paintedBoxes;
  /** x-Position of first painted box' left side (<= 0). */
  private int xFirstBox;
  /** Current box under the cursor (< 0 means, nothing is hovered). */
  private int hoveredBox = -1; 
  
  /** Factor for cell size relative to char size. */
  private final double boxSizeFactor = 1.45;
  
  private final Polygon arrowDownPoly;
  private final Polygon arrowLeftPoly;
  private final Polygon arrowRightPoly;
  
  private int draggedBox = -1;
  
  private ArrayList<ScrollModeListener> listeners =
      new ArrayList<ScrollModeListener>();
  
  private int frameLength = 33; // = 1000/(fps rate) => approx. 30 fps
  private long lastRepaint = 0;
  private boolean repaintScheduled = false;
  
  private static Color slightlyDarker(Color c) {
    float hsb[] = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    hsb[2] -= 0.1; // reduce brightness
    if (hsb[2] < 0) { hsb[2] = 0; }
    return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
  }
  
  public TapeViewer(Tape tape, int frameLength, int cellsPerStripe) {
    setFocusable(true);
    addMouseMotionListener(this);
    addMouseListener(this);
    addMouseWheelListener(this);
    tape.addTapeListener(this);
    
    this.tape = tape;
    this.frameLength = frameLength;
    this.stripeSize = cellsPerStripe;
    
    //Used to pick the right colors
    //UIManger would be better, but returns null sometimes (depends on L&F)
    JTable palette = new JTable();
    fgColor = palette.getForeground();
    bgColorTape = palette.getBackground();
    bgColorTapeStripe = slightlyDarker(bgColorTape);
    bgColorHead = Misc.opaque(palette.getSelectionBackground());
    
    font = UIManager.getFont("Table.font");
    metrics = this.getFontMetrics(font);
    
    charHeight = metrics.getAscent() + metrics.getDescent();
    //Assume, that chars aren't wider than tall
    boxSize = (int) (charHeight * boxSizeFactor);
    
    boxCenterBaseline = (int) ((boxSize - charHeight + 1) * 0.5)
    		                + metrics.getAscent(); //+ 1  * 0.5 -> ceil  
    top = boxSize;
    bottom = 2 * boxSize;
    int headSize = boxSize/2;
    int translate = (boxSize-headSize)/2;
    headSize += headSize % 2; // headSize should be even
    int[] px = {0, headSize, headSize, headSize/2, 0};
    int[] py = {0, 0, headSize/2, headSize, headSize/2};
    arrowDownPoly = new Polygon(px, py, 5);
    arrowDownPoly.translate(translate, translate);
    
    px = new int[]{headSize/2, headSize, headSize, headSize/2, 0};
    py = new int[]{0, 0, headSize, headSize, headSize/2};
    arrowLeftPoly = new Polygon(px, py, 5);
    arrowLeftPoly.translate(translate, translate);
    
    px = new int[]{0, headSize/2, headSize, headSize/2, 0};
    py = new int[]{0, 0, headSize/2, headSize, headSize};
    arrowRightPoly = new Polygon(px, py, 5);
    arrowRightPoly.translate(translate, translate);

    //-1, so that a half cell is drawn left and right
    this.setMinimumSize(new Dimension(8*boxSize-1, 3*boxSize));
    this.setPreferredSize(new Dimension(30*boxSize-1, 3*boxSize));
    this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3*boxSize));
    
    //Number with biggest distance to pos (mind the overflow!)
    firstCell = tape.getPos() + Integer.MAX_VALUE;
    //Centers the head, when painted the first time
    scrollMode = SCROLL_IMMEDIATE;
  }
  
  public int getFrameLength() {
    return frameLength;
  }
  
  /**
   * ...
   * If a frame is already scheduled, changes will be applied with the next one.
   * @param frameLength
   */
  public void setFrameLength(int frameLength) {
    this.frameLength = frameLength;
  }
  
  public void repaintWithNextFrame() {
    if (!isVisible() || repaintScheduled) { return; }
    final long wait = lastRepaint + frameLength - System.currentTimeMillis();
    if (wait <= 0) {
      repaint();
    } else {
      repaintScheduled = true;
      new Thread("TapeViewer repaint") {
        @Override
        public void run() {
          try {
            sleep(wait);            
          } catch (InterruptedException e) {
            return;
          }
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              repaint();
              repaintScheduled = false;
            }
          });
        }
      }.start();   
    }
  }
  
  @Override
  public void paint(Graphics g) {
    g.setColor(getBackground());
    g.fillRect(0, 0, getWidth(), getHeight());
    g.setFont(font);
    g.setColor(fgColor);
    
    //Width of the canvas measured in cells
    double widthInBoxes = (double) getWidth() / boxSize;
    int completeBoxes = (int) widthInBoxes; // floor
    paintedBoxes = completeBoxes + 2;
    //Distribute cropped boxes (max. 1) to both sides
    //=> 2 cropped boxes with same width
    int croppedBoxWidth = (int) ((widthInBoxes-completeBoxes)/2 * boxSize); 
    xFirstBox = croppedBoxWidth - boxSize;

    //scroll
    final int headCell = tape.getPos();
    if (scrollMode == SCROLL_NONE) {
      headBox = headCell - firstCell;      
    } else if (scrollMode == SCROLL_BORDERS) {
      if (headCell < firstCell + scrollBorder) {
        firstCell = headCell - scrollBorder;
      } else if (headCell > firstCell + paintedBoxes - 1 - scrollBorder) {
        firstCell = headCell - paintedBoxes + 1 + scrollBorder;
      }
      headBox = headCell - firstCell;
    } else if (scrollMode == SCROLL_IMMEDIATE) {
      // center head, if not fully visible
      if (headBox <= 0 || headBox >= paintedBoxes-1) {
        headBox = paintedBoxes / 2;
      }
      firstCell = headCell - headBox; 
    }

    char[] chars = tape.read(firstCell, paintedBoxes);

    int cy = top + boxCenterBaseline;
    //draw cells
    for (int i = 0; i < paintedBoxes; ++i) {
      int x = xFirstBox + i * boxSize; // left border of visible cell i
      boolean onStripe = onStripe(firstCell + i);
      g.setColor(onStripe ? bgColorTapeStripe : bgColorTape);
      g.fillRect(x, top, boxSize, boxSize);
      g.setColor(onStripe ? bgColorTape : bgColorTapeStripe); // inverted
      g.drawLine(x, top, x, bottom);
      g.setColor(fgColor);
      //+1 ceils the number, which looks better,
      //because the border eats up space at the left side of the cell.
      int cx = x + (int) ((boxSize - metrics.charWidth(chars[i]) + 1) * 0.5); 
      g.drawChars(chars, i, 1, cx, cy);      
    }
    g.drawLine(0, top, getWidth()-1, top);
    g.drawLine(0, bottom, getWidth()-1, bottom);
    
    //draw head
    if (headBox <= 0) {
      g.setColor(bgColorHead);
      arrowLeftPoly.translate(croppedBoxWidth, 0);
      g.drawPolygon(arrowLeftPoly);
      arrowLeftPoly.translate(-croppedBoxWidth, 0);
    } else if (headBox >= paintedBoxes-1) {
      g.setColor(bgColorHead);
      int translate = getWidth() - croppedBoxWidth - boxSize;  
      arrowRightPoly.translate(translate, 0);
      g.drawPolygon(arrowRightPoly);
      arrowRightPoly.translate(-translate, 0);
    }
    // Maybe it's partially visible
    if (partiallyVisible(headBox)) {
      int translate = xFirstBox + (headCell - firstCell) * boxSize;
      arrowDownPoly.translate(translate, 0);
      g.setColor(bgColorHead);
      g.fillPolygon(arrowDownPoly);
      g.setColor(fgColor);
      g.drawPolygon(arrowDownPoly);
      arrowDownPoly.translate(-translate, 0);
    }
    
    g.setColor(fgColor);
    // mark leftmost cell
    int leftmostBox = tape.getLeftmost() - firstCell;
    if (partiallyVisible(leftmostBox)) {
      int x = xFirstBox + leftmostBox * boxSize;
      int y = top - boxSize/2;
      g.drawLine(x, y, x, top);
      g.drawLine(x, y, x + boxSize/2, top);
    }
     // mark rightmost cell
    int rightmostBox = tape.getRightmost() - firstCell;
    if (partiallyVisible(rightmostBox)) {
      int x = xFirstBox + (rightmostBox + 1) * boxSize;
      int y = top - boxSize/2;
      g.drawLine(x, y, x, top);
      g.drawLine(x, y, x - boxSize/2, top);
    }
    // mark cell 0
    int box0 = -firstCell;
    if (headBox != box0 && partiallyVisible(box0)) {
      int x = xFirstBox + box0 * boxSize;
      //+1 ceils the number, which looks better,
      //because the border eats up space at the left side of the cell.
      int cx = x + (int) ((boxSize - metrics.charWidth('0') + 1) * 0.5); 
      g.drawString("0", cx, boxCenterBaseline);
    }
    
    // highlight box and label it with it's index
    int highlightedBox;
    boolean highlight = false;
    if (partiallyVisible(hoveredBox)) {
      highlightedBox = hoveredBox;
      highlight = true;
    } else {
      highlightedBox = headBox;
      highlight = fullyVisible(headBox);
    }
    if (highlight) {
      int hx = xFirstBox + highlightedBox * boxSize; 
      g.setColor(bgColorHead);
      g.drawLine(hx, top-1, hx+boxSize, top-1);
      g.drawLine(hx, bottom+1, hx+boxSize, bottom+1);
      String s = Integer.toString(firstCell + highlightedBox);
      int w = metrics.stringWidth(s);
      int sx = hx + (int)((boxSize-w+1)*0.5);
      int sy = bottom + boxCenterBaseline;
      if (sx < 0) {
        sx = 0;
      } else if (sx + w > getWidth()) {
        sx = getWidth() - w;
      }
      g.setColor(fgColor);
      g.drawString(s, sx, sy);
    }

    lastRepaint = System.currentTimeMillis();
  }
  
  private boolean onStripe(int cell) {
    if (stripeSize == 0) {
      return false;
    } else {
      int i;
      if (cell < 0) {
        i = ((-cell-1) / stripeSize + 1);
      } else {
        i = (cell / stripeSize); 
      }
      return (i % 2 == 0) ^ (stripeSize < 0);
    }
  }
  
  private int boxAt(int x) {
    return (x - xFirstBox) / boxSize;
  }
  
  /**
   * Calculates visibility of a box.
   * A Box is partially visible, if more than 0 pixels of it are shown.
   * @param box Box to be investigated.
   * @return The Box is complete or partially (at borders) visible. 
   */
  private boolean partiallyVisible(int box) {
    return 0 <= box && box <= paintedBoxes-1;
  }
  
  private boolean fullyVisible(int box) {
    return 0 < box && box < paintedBoxes-1;
  }
  
  private boolean inScrollArea(int box) {
    return scrollBorder < box && box < paintedBoxes-1-scrollBorder;
  }
  
  //----------------------------
  
  public void setTape(Tape tape) {
    this.tape.removeTapeListener(this);
    this.tape = tape;
    this.tape.addTapeListener(this);
    
    //Center, when loading a new tape
    //scrollTo(tape.getPos());
  }
  
  public int getStripeSize() {
    return stripeSize;
  }
  
  public void setStripeSize(int cellsPerStripe) {
    this.stripeSize = cellsPerStripe;
    repaintWithNextFrame();
  }
  
  /**
   * Scroll, so that given cell will be centered.
   * @param cell Cell to be centered.
   */
  public void scrollTo(int cell) {
    scroll(firstCell - (cell - paintedBoxes/2));
    repaintWithNextFrame();
  }
  
  /**
   * Look at 'boxes' to the right.
   * @param boxes Distance to be moved to the right (in boxes).
   */
  public void scroll(int boxes) {
    if (boxes != 0) {
      firstCell -= boxes;
      headBox += boxes;
      if (scrollMode == SCROLL_BORDERS && !inScrollArea(headBox) ||
          scrollMode == SCROLL_IMMEDIATE && !fullyVisible(headBox)) {
        scrollMode = SCROLL_NONE;
        fireScrollModeChanged();
      }
      repaintWithNextFrame();
    }
  }
  
  public void addScrollModeListener(ScrollModeListener sml) {
    if (sml != null) {
      listeners.add(sml);      
    }
  }

  public void fireScrollModeChanged() {
    ScrollModeEvent e = new ScrollModeEvent(this, scrollMode);
    for (ScrollModeListener listener : listeners) {
      listener.scrollModeChanged(e);
    }
  }
  
  public void setScrollMode(int scrollMode) {
    if (scrollMode < SCROLL_NONE || scrollMode > SCROLL_IMMEDIATE) {
      throw new IllegalArgumentException("unknown scroll mode " + scrollMode);
    } else if (scrollMode != this.scrollMode) {
      this.scrollMode = scrollMode;
      fireScrollModeChanged();
      repaint();
    }
  }

  public long getLastUpdate() {
    return lastRepaint;
  }
  
  // ---------------------------- 
  
  private boolean primaryMousePressed(MouseEvent e) {
    return (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0; 
  }
  
  @Override
  public void mouseDragged(MouseEvent e) {
    if (primaryMousePressed(e)) {
      int box = boxAt(e.getX());
      if (box != draggedBox) {
        scroll(box - draggedBox);
        draggedBox = box;
        hoveredBox = box;
        repaintWithNextFrame();
      }      
    }
  }
  
  @Override
  public void mouseMoved(MouseEvent e) {
    int box = boxAt(e.getX());
    if (hoveredBox != box) {
      hoveredBox = box;
      repaintWithNextFrame();
    }   
  }

  // ----------------------------
  
  @Override
  public void mousePressed(MouseEvent e) {
	//Takes the focus from text fields, which enables keyboard controls for TM.
	//KeyEvents for this Panel are added in tursi.view.GUI
	requestFocus();
    if (e.getButton() == MouseEvent.BUTTON1) {
      draggedBox = boxAt(e.getPoint().x);     
      //repaint(); //Not necessary - mouse already moved to this point
    }
  }

  //Needed when dragging the mouse outside this component 
  @Override
  public void mouseReleased(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON1 && !contains(e.getPoint())) {
      hoveredBox = -1;
      repaintWithNextFrame(); // was dragged before, so hoveredBox was not -1
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {
    if (!primaryMousePressed(e)) {
      hoveredBox = -1;
      repaintWithNextFrame();
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // function for double clicking a cell?
  }

  // ---------
 
  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    scroll(-e.getWheelRotation()); // scroll wheel down to look to the right  
    repaintWithNextFrame();
  }

  // --------------
  
  @Override
  public void tapeChanged() {
    repaintWithNextFrame();
  }

}
