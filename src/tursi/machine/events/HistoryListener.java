package tursi.machine.events;

import java.util.EventListener;

public interface HistoryListener extends EventListener {

  public void historyContentChanged();
  
}
