package tursi.parsing;

import static org.junit.Assert.*;
import org.junit.Test;

import tursi.parsing.Parser;
import tursi.parsing.ParserException;

import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ParserTest {
  
  private final PrintStream o = System.out;
  
  // TODO remove or rewite this test, due to absolut paths and changing file content
  @Test
  public void testParsing() throws IOException, ParserException {
    Reader r;
    //r = new StringReader("a * c N e#! fill arg1 #! wildcard ### !cmd a  \tb   c");
    r = new FileReader("");//"/Users/Claus/Documents/Uni/Projekte/Turingsimulator/Tursi/test/parsing/test.tm");
    BufferedReader br = new BufferedReader(r);
    Parser p = new Parser();
    p.parse(br);
    p.printAttributes();
    //o.println(p.warnings);
    //o.println(p.table);
  }
  
  @Test
  public void testComment() {
    //o.println(Parser.errMsgRuleFields(-1));
    //o.println(Arrays.toString(s));
  }

  @Test
  public void testSplit() {
    testSplit("state r w -1 nextState #! command arg1 arg2 #! CMD # comment",
        "state r w -1 nextState", "command arg1 arg2", "CMD");
    testSplit("  lorem\t  \tipsum\t \t#!dolor \t sit amet#comment",
        "lorem\t  \tipsum", "dolor \t sit amet");
    testSplit("", "");
    testSplit(" ", "");
    testSplit("\t \t   \t\t\t   \t", "");
    testSplit("#! cmd arg", "", "cmd arg");
    testSplit("  #! \tcmd\t arg  ", "", "cmd\t arg");
    testSplit("  #!#! #! ##! ##### comment", "", "", "", "#! ##");
    testSplit(("####" + "#!" + "##" + "#!" + "##!"), "##", "#", "#!");
    testSplit("abc#xyz", "abc");
    testSplit("#xyz", "");
    testSplit("abc#", "abc");
    testSplit("#", "");
    testSplit("###", "#");
    testSplit("# ##", "");
    testSplit("# ##", "");
  }
  
  private void testSplit(String ln, String... parts) {
    assertArrayEquals(parts, Parser.escAndSplit(ln));
  } 
  
}