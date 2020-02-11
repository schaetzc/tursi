package tursi.export;

import static org.junit.Assert.*;
import org.junit.Test;

import tursi.export.HistTSVExporter;
import tursi.export.RulesGMLExporter;
import tursi.export.TapeTXTExporter;
import tursi.machine.*;
import tursi.view.AliasConverter;

import java.io.*;
import java.util.HashSet;

public class ExporterTest {

  private final Rule[] rules = {
      new Rule("Lorem", 'a', 'b',  0, "Dolor"),
      new Rule("Lorem", 'x', 'y',  1, "Sit"),
      new Rule("Entry", '1', '0',  0, "Entry"),
      new Rule("Entry", '0', '1',  0, "Next"),
      new Rule("Sit",   'u', 'v',  2, "Last"),
      new Rule("Next",  '0', '1',  0,  "Dead End"),
      new Rule("Lorem", 'r', 'w', -1, "Dolor"),
      new Rule("Dolor", 'x', 'y',  1, "Hidden"),
      new Rule("'\"&;", '<', '>', -7, "\"&&\"  ;&\"")
  };
  
  private final AliasConverter aliasConv = new AliasConverter() {
    @Override
    public String convertToAlias(int move) {
      switch (move) {
        case -1: return "Ll";
        case  0: return "Nn";
        case  1: return "Rr";
        default: return Integer.toString(move);
      }
    }
  };
  
  @Test
  public void testTXT() {
    Tape tape = new Tape("abcdefghijklmnopqrstuvwxyz");
    TapeTXTExporter exp = new TapeTXTExporter(true, true);
    StringWriter w = new StringWriter();
    try {
      exp.export(w, tape, -5, 12);
      w.write("\n");
      exp.export(w, tape, 0, 1);
      w.write("\n");
      exp.export(w, tape, -1, 1);
      w.write("\n");
      exp.export(w, tape, -1, 3);
      w.write("\n");
      exp.export(w, tape, 7, 10);
    } catch (IOException e) { }
    System.out.println(w);
  }
  
  //@Test
  public void testTSV() {
    LimitedHistory hist = new LimitedHistory(5);
    for (Rule r : rules) { hist.push(r); }
    HistTSVExporter exp = new HistTSVExporter(true, aliasConv);
    StringWriter w = new StringWriter();
    try {
      exp.export(w, hist);
    } catch (IOException e) { }
    System.out.println(w);
  }

  //@Test
  public void testGML() {
    String start = "Entry";
    HashSet<String> breaks = new HashSet<String>();
    breaks.add("Dolor");
    breaks.add("Not in rule list");
    HashSet<String> ends = new HashSet<String>();
    ends.add("Last");
    ends.add("Dolor");
    ends.add("Sit");
    
    RulesGMLExporter exp = new RulesGMLExporter(true, true, "&", aliasConv);
    StringWriter w = new StringWriter();
    try {
      exp.export(w, rules, start, breaks, ends);
    } catch (IOException e) { }
    
    System.out.println(w);
  }
  
  

}
