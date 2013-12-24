package tursi.machine;

public class RuleNotFoundException extends Exception {
  
  private static final long serialVersionUID = 1L;
  
  private RuleTrigger trigger;
  
  public RuleNotFoundException(RuleTrigger trigger) {
    this.trigger = trigger;
  }
  
  public RuleTrigger getTrigger() {
    return trigger;
  }
  
}
