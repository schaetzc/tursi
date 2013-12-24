package tursi.export;

import java.io.*;

import tursi.machine.*;

public class TapeTXTExporter {
  
  private final String ln = String.format("%n"); 
  private final boolean rangeInfo;
  private final boolean headerInfo;
  
  public TapeTXTExporter(boolean rangeInfo, boolean headerInfo) {
    this.rangeInfo = rangeInfo;
    this.headerInfo = headerInfo;
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
    if (rangeInfo && headerInfo) { w.write(", "); }
    if (headerInfo) { w.write("header " + tape.getPos()); }
    if (rangeInfo || headerInfo) { w.write(ln); }
    
    int sec1First, sec1Length; // section with negativ cells
    int sec2First, sec2Length; // section with non-negativ cells
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
