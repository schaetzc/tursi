package tursi.machine;

import java.util.Map;
import java.util.Set;

import tursi.machine.events.*;

/**
 * Turing machine with tape and program (rule table). 
 * TODO further details to this implementation 
 */
public class Machine {

  /** Tape, including head's position. */
  private Tape tape;
  /** Program (table of rules). Trigger is the key, Action the value. */
  private Map<RuleTrigger,Rule> table;
  /** Machine's current state. */
  private String state;

  /** Machine's start state. */
  private String startState;
  /** Machine's break states. */
  private Set<String> breakStates;
  /** Machine's end states. */
  private Set<String> endStates;
  
  /**
   * Wildcard for {@code read} and {@code write} from rules.
   * If a Trigger couldn't be found, replace it's {@code read} with this and
   * search again.
   * If an Action has this as {@code write}, write nothing to the tape (or
   * write the symbol, which was there before).
   */
  private char wildcard;
  
  /**
   * All taken steps. Rules with wildcards should be replaced with the actual
   * taken rule, so that it's possible to undo the steps by executing the
   * inverted rules.
   */
  private History history;
  
  /** Listener for this machine. Inform it about state changes. */
  private StateListener stateListener = null;
  
  /**
   * Create a new turing machine. Non of the given attributes (except for the
   * tape) will be modified. All original references are kept!
   * @param tape        Tape
   * @param table       Program / table of rules
   * @param startState  Start state 
   * @param breakStates Break states
   * @param endStates   End states
   * @param wildcard    Wildcard for rule's {@code read} and {@code write}.
   *                    Use {@code 0} (not {@code '0'}), when not needed. 
   * @param history     History 
   */
  public Machine(Tape tape, Map<RuleTrigger,Rule> table, String startState, 
      Set<String> breakStates, Set<String> endStates, char wildcard,
      History history) {
    this.tape  = tape;
    this.table = table;
    this.state = startState;
    this.startState  = startState;
    this.breakStates = breakStates;
    this.endStates   = endStates;
    this.wildcard    = wildcard;
    this.history     = history;
  }

  /**
   * Sets the StateListener for this machine. Only one StateListener can be set.
   * To remove it, set the StateLsitener to null.  
   * @param stateListener StateListener to be set or null to remove it.
   */
  public void setStateListener(StateListener stateListener) {
    this.stateListener = stateListener;
  }
  
  /**
   * Returns the current StateListener (can be null).
   * @return Current StateListener or null.
   */
  public StateListener getStateListener() {
    return this.stateListener;
  }
  
  /**
   * Informs the StateListener about a changed state.
   * Mind, that you should check for real changes, before calling this method.
   * For the fired event, the current state will be used. Make sure, to set it
   * before this method is called.
   */
  public void fireStateEvent() {
    if (stateListener != null) {
      stateListener.stateChanged();
    }
  }
  
  /** @return Reference to the original tape. */
  public Tape getTape() {
    return tape;
  }

  /** @return Original reference to the history. */
  public History getHistory() {
    return history;
  }
  
  /**
   * Replace machine's current tape with a new one. This will reset the history.
   * @param tape New tape to operate on.
   */
  public void setTape(Tape tape) {
    history.clear();
    this.tape = tape;
  }
  
  /** @return Reference to the original rule table.*/
  public Map<RuleTrigger,Rule> getTable() {
    return table;
  }
  
  public void step() throws RuleNotFoundException {
    Rule r = nextRule();
    tape.write(r.action.write);
    tape.move(r.action.move);
    if (state != r.action.nextState) {
      state = r.action.nextState;
      fireStateEvent();
    }
    history.push(r);
  }
  
  private Rule nextRule() throws RuleNotFoundException {
    char read = tape.read();
    RuleTrigger t = new RuleTrigger(state, read);
    Rule r = table.get(t);
    if (r == null) {
      r = table.get(new RuleTrigger(state, wildcard));
      if (r == null) {
        throw new RuleNotFoundException(t);
      }
    }
    return r.replaceWildcard(wildcard, read);
  }

  // TODO add to documentation, that wildcards aren't processed
  public boolean undo() {
    Rule r = history.pop();
    if (r == null) { return true; }
    if (state != r.trigger.state) {
      state = r.trigger.state;
      fireStateEvent();
    }
    tape.move(-r.action.move);
    tape.write(r.trigger.read);
    return false;
  }
  
  public boolean inBreakState() {
    return breakStates.contains(state);
  }
  
  public boolean inEndState() {
    return endStates.contains(state);
  }
    
  public String getState() {
    return state;
  }
  
  /**
   * Resets this machine by changing the currentState to the startState and
   * resetting the history.
   */
  public void reset() {
    setState(startState);
    history.clear();
  }
  
  /**
   * Set's the current state to the specified one. This will clear the history,
   * if the specified state was not the current one. The position and content
   * of the tape remains the same.
   * @param state State to be set as current state.
   */
  public void setState(String state) {
    if (this.state != state) {
      history.clear();
      this.state = state;
      fireStateEvent();
    }
  }

}
