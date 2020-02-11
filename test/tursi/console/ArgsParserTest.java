package tursi.console;

import static org.junit.Assert.*;
import org.junit.Test;

import tursi.console.ArgsParser;

import java.util.regex.*;

public class ArgsParserTest {

  //@Test
  public void test() {
    ArgsParser p = new ArgsParser();
    if (p.parse(new String[]{"file", "-c", "eo=sttttsr", "o="})) {
      System.out.println(p.getErrMsg());
    }
  }
  
  @Test
  public void testPrintOpts() {
    
  }

}
