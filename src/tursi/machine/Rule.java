package tursi.machine;

/**
 * Rule of a turing machine (part of the "action table" or also called
 * "transition function").
 * This consists of a {@ink Trigger} and an {@link RuleAction}.
 */
public class Rule {

  /** First two fields of this rule. */
  public final RuleTrigger trigger;
  /** Last three fields of this rule. */
  public final RuleAction action;
  
  /**
   * Create a new rule by creating a new trigger and action.
   * @param state     Machine's current state
   * @param read      Read symbol
   * @param write     Symbol to be written
   * @param move      Movement of the head
   * @param nextState Machine's state after this 
   */
  public Rule(String state, char read, char write, int move, String nextState) {
    this.trigger = new RuleTrigger(state, read);
    this.action = new RuleAction(write, move, nextState);
  }
  
  /**
   * Creates a new rule from existing trigger and action.
   * @param trigger Trigger for this rule
   * @param action  Action for this rule
   */
  public Rule(RuleTrigger trigger, RuleAction action) {
    if (trigger == null || action == null) {
      throw new IllegalArgumentException("incomplete rule");
    }
    this.trigger = trigger;
    this.action = action;
  }

  @Override
  public int hashCode() {
    return 961 + 31 * trigger.hashCode() + action.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    Rule r = (Rule) o;
    return trigger.equals(r.trigger) && action.equals(r.action);
  }
  
  @Override
  public String toString() {
    return "(" + trigger.state + ", " + trigger.read + ", " 
        + action.write + ", " + action.move + ", " + action.nextState + ")";
  }

  /**
   * Replaces wildcards in fields {@code read} and {@code write} with the actual
   * value. The original object (this) will not be modified, instead a new one
   * will be created.
   * Use this, before adding a rule to the history.
   * 
   * @param wildcard Wildcard to be replaced
   * @param actual   Replacement for the wildcard 
   * @return This Rule, if the wildcard did not occur,
   *         or else a new Rule with replaced wildcards.  
   */
  public Rule replaceWildcard(char wildcard, char actual) {
    boolean r = trigger.read != wildcard;
    boolean w = action.write != wildcard;
    if (r & w) { return this; }
    RuleTrigger t = r ? trigger : new RuleTrigger(trigger.state, actual);
    RuleAction  a = w ? action : new RuleAction(actual, action.move, action.nextState);
    return new Rule(t, a);
  }

  
  
}
