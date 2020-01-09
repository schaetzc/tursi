package tursi.machine;

public class MinHistory implements History {

  private long steps;
  private Rule lastStep;
  
  public MinHistory() {
    clear();
  }
  
  public Rule get() {
    return lastStep;
  }
  
  @Override
  public void push(Rule r) {
    ++steps;
    lastStep = r;
  }

  @Override
  public Rule pop() {
    if (lastStep != null) {
      --steps; 
      Rule r   = lastStep; 
      lastStep = null;
      return r;
    } else {
      return null;
    }
  }

  @Override
  public long steps() {
    return steps;
  }

  @Override
  public boolean isEmpty() {
    return lastStep == null;
  }

  @Override
  public void clear() {
    steps    = 0;
    lastStep = null;
  }

}
