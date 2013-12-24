package tursi.export;

import java.io.*;

import tursi.machine.LimitedHistory;
import tursi.machine.Rule;
import tursi.view.AliasConverter;

public class HistTSVExporter {

  private final String ln = String.format("%n"); 
  private final boolean header;
  private final AliasConverter conv;
  
  public HistTSVExporter(boolean header, AliasConverter conv) {
    this.header = header;
    this.conv   = conv;
  }
  
  public void export(Writer w, LimitedHistory hist) throws IOException {
    if (header) {
      w.write("step\tlast state\tread\twrite\tmove\tstate" + ln);
    }
    for (int i = 0; i < hist.size(); ++i) {
      Rule r = hist.get(i);
      w.write(Long.toString(hist.stepAt(i)) + "\t");
      w.write(r.trigger.state + "\t");
      w.write(r.trigger.read + "\t");
      w.write(r.action.write + "\t");
      w.write(conv.convertToAlias(r.action.move) + "\t");
      w.write(r.action.nextState + ln);
    }
  }
  
}
