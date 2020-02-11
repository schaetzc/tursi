package tursi.machine;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import static org.junit.Assert.*;
import org.junit.Test;

import tursi.machine.LimitedHistory;
import tursi.machine.Machine;
import tursi.machine.Rule;
import tursi.machine.RuleNotFoundException;
import tursi.machine.RuleTrigger;
import tursi.machine.Tape;

public class MachineTest {  
  
  // 4 state beaver (benchmark)
  //@Test
  public void test() {
    Tape tape = new Tape("*");
    Map<RuleTrigger,Rule> table = createTable( // 4 state beaver
          new Rule("A", '*', 'I', -1, "2"),
          new Rule("A", '*', 'I', -1, "2"),
          new Rule("A", 'I', '*', -1, "3"),
          new Rule("2", '*', 'I',  1, "A"),
          new Rule("2", 'I', 'I', -1, "A"),
          new Rule("3", '*', 'I', -1, "E"), // write can be either '*' or 'I' (two different beavers)
          new Rule("3", 'I', 'I', -1, "4"),
          new Rule("4", '*', 'I',  1, "4"),
          new Rule("4", 'I', '*',  1, "2")
        );
    Set<String> endStates = new HashSet();
    endStates.add("E");
    /*
    Machine m = new Machine(tape, table, "A", endStates);
    //System.out.println(tape);
    //System.out.println();
    long t = System.currentTimeMillis();
    boolean end = false;
    while (!end) {
      end = m.step();
      //System.out.println(tape);
      //System.out.println();
    }
    System.out.println(System.currentTimeMillis() - t);*/
  }
  
 //5 state beaver (benchmark)
 @Test
 public void testBeaver5() throws RuleNotFoundException {
   
   long maxBytes = Runtime.getRuntime().maxMemory();
   System.out.println("Max memory: " + maxBytes / 1024 / 1024 + "M");
   Tape tape = new Tape("0");
   // 5 state beaver, writes 4098 ones in 47'176'870 steps.
   Map<RuleTrigger,Rule> table = createTable(
         new Rule("0", '0', '1',  1, "1"),
         new Rule("0", '1', '1', -1, "2"),
         new Rule("1", '0', '1',  1, "2"),
         new Rule("1", '1', '1',  1, "1"),
         new Rule("2", '0', '1',  1, "3"),
         new Rule("2", '1', '0', -1, "4"),
         new Rule("3", '0', '1', -1, "0"),
         new Rule("3", '1', '1', -1, "3"),
         new Rule("4", '0', '1',  1, "H"),
         new Rule("4", '1', '0', -1, "0")
       );
   //table = createTable( new Rule("0", '0', '0',  0, "0") );
   Set<String> endStates = new HashSet<String>();
   endStates.add("H");
   Machine m = new Machine(tape, table, "0", new HashSet<String>(), endStates, '\0', new LimitedHistory(1));
   long t = System.currentTimeMillis();
   while (!m.inEndState()) {
     m.step();
     //System.out.println(tape);
     if (m.getHistory().steps() % 1000000 == 0) {
       System.out.println(m.getHistory().steps() / 1000000 + "M");
     }
   }
   System.out.println("time = " + (System.currentTimeMillis() - t));
 }
  
  
  private static Map<RuleTrigger,Rule> createTable(Rule... rules) {
    HashMap<RuleTrigger,Rule> table = new HashMap<RuleTrigger,Rule>();
    for (Rule r : rules) { table.put(r.trigger, r); }
    return table;
  }

}
