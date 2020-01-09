package tursi.machine;

/**
 * Second part of a turing machine rule.
 * This defines the action, to be taken after an Event
 * (alias {@link RuleTrigger}) occured.
 */
public class RuleAction {

  /** Symbol to be written. */
  public final char write;
  
  /** Move the reading head by x cells, after writing. @see Tape#move(int) */
  public final int move;
  
  /** Go to this state, after writing and moving. */
  public final String nextState;
  
  /**
   * Create a new Action.
   * @param write     Symbol to be written.
   * @param move      Movement of the head (see also {@link Tape#move(int)}).
   * @param nextState Machine's state after this action.
   */
  public RuleAction(char write, int move, String nextState) {
    if (nextState == null) {
      throw new IllegalArgumentException("null is not a state");
    }
    this.write = write;
    this.move = move;
    this.nextState = nextState;
  }

  @Override
  public int hashCode() {
    return 29791 + 961 * move + 31 * nextState.hashCode() + write;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    RuleAction a = (RuleAction) o;
    return write == a.write && move == a.move && nextState.equals(a.nextState);
  }

  @Override
  public String toString() {
    return "/ " + write + ", " + move + ", " + nextState + ")";
  }
}
