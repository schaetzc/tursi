package tursi.machine;

import static org.junit.Assert.*;
import org.junit.Test;

import tursi.machine.LimitedHistory;
import tursi.machine.Rule;

public class LimitedHistoryTest {

  @Test
  public void test() {
    LimitedHistory h = new LimitedHistory(4);
    assertTrue(h.isEmpty());
    assertTrue(h.pop() == null);
    
    Rule r1 = new Rule("r1", '1', '1', 1, "r1");
    Rule r2 = new Rule("r2", '2', '2', 2, "r2");
    Rule r3 = new Rule("r3", '3', '3', 3, "r3");
    Rule r4 = new Rule("r4", '4', '4', 4, "r4");
    Rule r5 = new Rule("r5", '5', '5', 5, "r5");
    
    h.push(r1);
    h.push(r2);
    h.push(r3);
    assertEquals(h.size(), 3);
    assertEquals(h.steps(), 3);
    h.push(r4);
    h.push(r5);
    assertEquals(h.size(), 4);
    assertEquals(h.steps(), 5);
    
    assertEquals(h.get(0), r2);
    assertEquals(h.get(1), r3);
    assertEquals(h.get(2), r4);
    assertEquals(h.get(3), r5);
    
    assertEquals(h.pop(), r5);
    assertEquals(h.pop(), r4);
    assertEquals(h.pop(), r3);
    assertEquals(h.pop(), r2);
    assertEquals(h.steps(), 1);
    
    assertTrue(h.pop() == null);
    assertEquals(h.steps(), 1); // can't go back steps, that aren't stored anymore
    assertTrue(h.isEmpty());
    
    h.clear();
    assertEquals(h.steps(), 0);
    
    h.push(r1);
    h.push(r2);
    h.push(r2); // two times the same entry
    assertEquals(h.size(), 3);
    h.push(r3);
    h.push(r4);
    h.push(r5);
    assertEquals(h.steps(), 6);
    assertEquals(h.size(), 4);
    h.setMaxSize(4);
    assertEquals(h.size(), 4);
    h.setMaxSize(10);
    assertEquals(h.size(), 4);
    h.setMaxSize(2);
    assertEquals(h.size(), 2);
    assertEquals(h.steps(), 6);
    assertEquals(h.get(0), r4);
    assertEquals(h.get(1), r5);
    assertEquals(h.pop(), r5);
    assertEquals(h.pop(), r4);
    assertEquals(h.steps(), 4);
    assertTrue(h.pop() == null);
    assertTrue(h.pop() == null);
    assertTrue(h.pop() == null);
    assertEquals(h.steps(), 4);
    assertTrue(h.isEmpty());
    
    h.setMaxSize(1);
    h.push(r1);
    h.push(r2);
    h.push(r3);
    assertEquals(h.pop(), r3);
    assertTrue(h.pop() == null);
  }

  @Test
  public void testIllegalArguments() {
    try {
      new LimitedHistory(0);
      fail("no exception for maxSize < 1");
    } catch(Exception e) { }
    try {
      new LimitedHistory(-4);
      fail("no exception for maxSize < 1");
    } catch(Exception e) { }
    try {
      new LimitedHistory(4).setMaxSize(0);
      fail("no exception for maxSize < 1");
    } catch(Exception e) { }
  }
}
