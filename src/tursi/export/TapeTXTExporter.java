package tursi.export;

import java.io.*;

import tursi.machine.*;

public class TapeTXTExporter {
  
  private final String ln = String.format("%n"); 
  private final boolean rangeInfo;
  private final boolean headInfo;
  
  /**
   * Create a new txt export filter.
   * @param rangeInfo Print informations about the printed section of the tap (e.g. range). 
   * @param headInfo Print informations about the read/write head (e.g. current position).
   */
  public TapeTXTExporter(boolean rangeInfo, boolean headInfo) {
    this.rangeInfo = rangeInfo;
    this.headInfo = headInfo;
  }
  
  public void export(Writer w, Tape tape, int start, int length)
      throws IOException {
    
    if (length < 1) {
      throw new IllegalArgumentException("tape section length = " + length);
    }
    
    if (rangeInfo) {
      int end = start + length - 1;
      w.write("tape (" + start + ", " + length + ", " + end + ")");
    }
    if (rangeInfo && headInfo) { w.write(", "); }
    if (headInfo) { w.write("head " + tape.getPos()); }
    if (rangeInfo || headInfo) { w.write(ln); }
    
    int sec1First, sec1Length; // section with negative cells
    int sec2First, sec2Length; // section with non-negative cells
    if (start < 0) {
      sec1First = start;
      sec2First = 0;
      if (length < -start) {
        sec1Length = length;
        sec2Length = 0;
      } else {
        sec1Length = -start;
        sec2Length = start + length; // start is negative
      }
    } else {
      sec1First  = 0;
      sec1Length = 0;
      sec2First  = start; 
      sec2Length = length;
    }
    
    w.write(tape.read(sec1First, sec1Length));
    w.write(ln);
    w.write(tape.read(sec2First, sec2Length));
    w.write(ln);
  }
  
  
}
