package tursi.machine;

/**
 * First part of a turing machine rule.
 * This defines the event, which triggers the machine to do something. E.g.:
 * I'm in state X and read an Y. Searching for the right trigger (with according
 * {@link RuleAction}) is vital for the turing machine in each step.
 */
public class RuleTrigger {

  /** Machine's current state. */
  public final String state;
  
  /** Symbol, which was read by the machine. */
  public final char read;
  
  /** Precalculated hash for this object. Often use is expected. */
  private final int hash;
  
  /**
   * Create a new Trigger from given state and read symbol. 
   * @param state Machine's state. Must not be null.
   * @param read  Symbol, which was read by the machine.
   */
  public RuleTrigger(String state, char read) {
    if (state == null) {
      throw new IllegalArgumentException("null is not a state");
    }
    this.state = state;
    this.read = read;
    this.hash = 1681 + 41 * state.hashCode() + read;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    RuleTrigger t = (RuleTrigger) o;
    return read == t.read && state.equals(t.state);
  }
  
  @Override
  public String toString() {
    return "(" + state + ", " + read + " /";
  }
  
}
