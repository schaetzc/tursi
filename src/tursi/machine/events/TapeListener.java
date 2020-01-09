package tursi.machine.events;

import java.util.EventListener;

public interface TapeListener extends EventListener {

  public void tapeChanged();

}
