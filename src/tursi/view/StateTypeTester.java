package tursi.view;

public interface StateTypeTester {

  public boolean isStartState(String s);
  
  public boolean isEndState(String s);
  
  public boolean isBreakState(String s);
  
  public boolean isCurrentState(String s);
}
