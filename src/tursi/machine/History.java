package tursi.machine;

/** Stores taken rules of a turing machine. Histories work like a stack. */
public interface History {

  /**
   * Adds a Rule to the top of this history and increments the step counter.
   * Value {@code null} is forbidden, because it signals an empty History,
   * when retrieving taken rules with {@link #pop()}.  
   */
  public void push(Rule r);

  /**
   * Returnes and removes the topmost rule and decrements the step counter.
   * If this History is empty, {@code null} will be returned and the step
   * counter won't be affected.
   * @return The topmost Rule, or {@code null}.
   */
  public Rule pop();
  
  /**
   * Returns the number of taken steps, since last reset.
   * This number must not match the number of stored Rules (for instance,
   * {@link LimitedHistory} discards old rules, if a maximum is reached).
   * @return Number of steps.
   */
  public long steps();
  
  /**
   * Determines, if the stack is empty. However, the step counter don't have to
   * be 0 in this case.
   * @return This History contains no rules.
   */
  public boolean isEmpty();
  
  /** Removes all entries in this History and resets the step counter to 0. */
  public void clear();
}
