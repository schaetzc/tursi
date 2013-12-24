package tursi.parsing;

public class ParserException extends Exception {
  
  private static final long serialVersionUID = 1L;
  
  private int line;
  
  public ParserException(String msg) {
    super(msg);
    line = -1;
  }
  
  public void setLine(int line) {
    this.line = line;
  }
  
  public boolean lineAvailable() {
    return line > 0;
  }
  
  public int getLine() {
    return line;
  }
  
  public String toString() {
    if (lineAvailable()) { return line + ": " + getMessage(); }
    return getMessage();
  }
  
}
