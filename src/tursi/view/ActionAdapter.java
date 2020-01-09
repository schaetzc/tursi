package tursi.view;

import javax.swing.Action;
import java.beans.PropertyChangeListener;

public abstract class ActionAdapter implements Action {

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getValue(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void putValue(String key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setEnabled(boolean b) {
    throw new UnsupportedOperationException();
  }

}
