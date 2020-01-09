package tursi.console;

/** Used to store print options, not to print the options. */
public class PrintOptions {
  
  public final boolean s;
  public final boolean t;
  public final boolean r;
  
  public PrintOptions(boolean s, boolean t, boolean r) {
    this.s = s;
    this.t = t;
    this.r = r;
  }
  
  public PrintOptions or(PrintOptions o) {
    return new PrintOptions(this.s || o.s, this.t || o.t, this.r || o.r);
  }
  
  @Override
  public String toString() {
    String msg = "";
    if (s) { msg += "s"; }
    if (t) { msg += "t"; }
    if (r) { msg += "r"; }
    return msg;
  }
}
