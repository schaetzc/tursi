package tursi.machine;

import static org.junit.Assert.*;
import org.junit.Test;

import tursi.machine.Tape;

/** Unit tests for {@link Tape}. */
public class TapeTest {

  /** Small test for basic functions. */
  @Test
  public void testBasics() {
    try {
      new Tape("");
      fail("Tape without fill allowed");
    } catch(Exception e) { }
    
    Tape t = new Tape(7, 3, 1.2, "abcd");
    assertEquals(0, t.getPos());
    assertEquals(0, t.getLeftmost());
    assertEquals(0, t.getRightmost());
    assertEquals("bcdabcd" + "abc", new String(t.read(-7, 10)));
    assertEquals('a', t.read());
    assertEquals('a', t.read(0));
    
    t.move(-3);
    t.move(2);
    assertEquals(-1, t.getPos());
    assertEquals(-3, t.getLeftmost());
    assertEquals(0, t.getRightmost());
    t.move(10);
    assertEquals(9, t.getPos());
    assertEquals(-3, t.getLeftmost());
    assertEquals(9, t.getRightmost());
    
    t.write('-'); // expands right part 1st time
    assertEquals('-', t.read());
    assertEquals('-', t.read(9));
    assertEquals("-", new String(t.read(9, 1)));
    
    assertEquals('a', t.read(-8)); // expands left part 1st time
    t.write(-9, '='); // expands left part 2nd time (factor was small)
    
    String s = new String(t.read(-15, 30)); // expands both sides
    assertEquals("bcdabc=abcdabcd" + "abcdabcda-cdabc", s);
  }
  
  /** Test the higher write functions. */
  @Test
  public void testStringWrite() {
    Tape t = new Tape(2, 3, 1.1, ".");
    t.write(-3, "12"); // begin before existing tape
    t.write(1, "ABC"); // end after existing tape
    assertEquals("12." + ".ABC", new String(t.read(-3, 7)));
    t.write(-5, "lorem ipsum"); // begin before and end after existing tape
    assertEquals("lorem ipsum", new String(t.read(-5, 11)));
    
    // small test for writeTw (uses write, which was tested before)
    t = new Tape(2, 3, 1.1, ".");
    t.writeTw(0, "dolor");
    t.writeTw(-2, "sit");
    t.writeTw(5, "amet");
    assertEquals("sito" + "r.amet", new String(t.read(-4, 10)));
  }
}
