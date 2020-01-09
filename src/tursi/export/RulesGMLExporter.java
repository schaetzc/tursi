package tursi.export;

import java.io.*;

import tursi.machine.*;
import tursi.view.AliasConverter;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class RulesGMLExporter {
  
  private final String ln = String.format("%n");
  
  private final boolean extAttr;
  private final boolean extEdgeAttr;
  private final String delimiter;
  private final AliasConverter aliasConv;
  
  private int indent = 0;
  private Writer w = null;
  
  public RulesGMLExporter(boolean extAttr, boolean extEdgeAttr,
      String delimiter, AliasConverter aliasConv) {
    this.extAttr     = extAttr;
    this.extEdgeAttr = extEdgeAttr;
    this.delimiter   = delimiter;
    this.aliasConv   = aliasConv;
  }
  
  public void export(Writer w, Rule[] rules, String startState,
      Set<String> breakStates, Set<String> endStates) throws IOException {
    this.w = w;
    indent = 0;   
    HashMap<String, Integer> nodeIds = createNodeIds(rules);

    addAttr("Creator", "Tursi");
    openSec("graph");
    addAttr("directed", 1);

    // ----- nodes (states) ----------------------------------------------------
    for (Map.Entry<String, Integer> n : nodeIds.entrySet()) {
      openSec("node");
      addAttr("id", n.getValue());
      addAttr("label", n.getKey());
      closeSec();
    }
    
    // ----- extended attributes (start, break, end states) --------------------
    if (extAttr) {
      openSec("Tursi");
      Integer id = nodeIds.get(startState);
      if (id != null) {
        addAttr("start", id);
      }
      openSec("break");
      for (String s : breakStates) {
        id = nodeIds.get(s);
        if (id != null) {
          addAttr("id", id);
        }
      }
      closeSec();
      openSec("end");
      for (String s : endStates) {
        id = nodeIds.get(s);
        if (id != null) {
          addAttr("id", id);
        }
      }
      closeSec();
      closeSec(); //Tursi
    }
    
    // ----- edges (rules) -----------------------------------------------------
    for (Rule r : rules) {
      String read  = r.trigger.read + "";
      String write = r.action.write + "";
      String moveAlias = aliasConv.convertToAlias(r.action.move);
      
      openSec("edge");
      addAttr("source", nodeIds.get(r.trigger.state));
      addAttr("target", nodeIds.get(r.action.nextState));
      addAttr("label",  read + delimiter + write + delimiter + moveAlias);
      if (extEdgeAttr) {
        openSec("Tursi");
        addAttr("read", read);
        addAttr("write", write);
        addAttr("move", r.action.move);
        closeSec();
      }
      closeSec(); //edge
    }
    
    closeSec(); //graph
    w.flush();
    this.w = null;
    this.indent = 0;
  }
  
  private void openSec(String name) throws IOException {
    indent();
    w.write(name);
    w.write(" [");
    w.write(ln);
    ++indent;
  }
  
  private void addAttr(String name, String value) throws IOException {
    indent();
    w.write(name);
    w.write(" \"");
    w.write(escStr(value));
    w.write("\"");
    w.write(ln);
  }
  
  private void addAttr(String name, Integer value) throws IOException {
    indent();
    w.write(name);
    w.write(" ");
    w.write(value.toString());
    w.write(ln);
  }
  
  private void closeSec() throws IOException {
    --indent;
    indent();
    w.write("]");
    w.write(ln);
  }
  
  private void indent() throws IOException {
    for (int i = 0; i < indent; ++i) {
      w.write("\t");
    }
  }
  
  // assign a unique id to every node/state
  private static HashMap<String, Integer> createNodeIds(Rule[] rules) {
    HashMap<String, Integer> nodes;
    nodes = new HashMap<String, Integer>(2 * rules.length);  
    for (Rule r : rules) {
      if (!nodes.containsKey(r.trigger.state)) {
        nodes.put(r.trigger.state, nodes.size());
      }
      if (!nodes.containsKey(r.action.nextState)) {
        nodes.put(r.action.nextState, nodes.size());
      }
    }
    return nodes;
  }
  
  private static String escStr(String s) {
    // + space for 2 ", which will be replaced by &quot;
    StringBuilder sb = new StringBuilder(s.length() + 10);
    for (int i = 0; i < s.length(); ++i) {
      char c = s.charAt(i);
      switch (c) {
        case '"':  sb.append("&quot;"); break;
        case '\'': sb.append("&apos;"); break;
        case '&':  sb.append("&amp;");  break;
        case '<':  sb.append("&lt;"); break;
        case '>':  sb.append("&gt;"); break;
        default:   sb.append(c);
      }
    }
    return sb.toString();
  }  

}
