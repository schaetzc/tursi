package tursi.machine.events;

import java.util.EventListener;

public interface StateListener extends EventListener {

  public void stateChanged();
  
}
