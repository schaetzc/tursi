package tursi.view.events;

import java.util.EventObject;

public class ScrollModeEvent extends EventObject {

  private static final long serialVersionUID = 1L;

  public final int scrollMode;
    
  public ScrollModeEvent(Object source, int scrollMode) {
    super(source);
    this.scrollMode = scrollMode;
  }
  
}
