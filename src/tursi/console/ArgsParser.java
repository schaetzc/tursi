package tursi.console;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.*;


public class ArgsParser {

  public final String HELP =
      "Tursi [<file> [-c [<grp>=<opt> ...]]]\n" +
      "   -c   console mode\n" +
      "   grp  print group\n" +
      "           e    end states\n" +
      "           b    break states\n" +
      "           o    other states\n" +
      "           <n>  every nth step\n" +
      "   opt  print options\n" +
      "           s    print step count and state name\n" +
      "           t    print tape\n" +
      "           r    print rule on next step";
  
  private String errMsg;
  
  private String  file = null;
  private boolean printHelp = false;
  private boolean consoleMode = false;
  
  
  private PrintOptions pOptsEnd;
  private PrintOptions pOptsBreak;
  private PrintOptions pOptsOther;
  private HashMap<Long, PrintOptions> pOpts;
  
  public ArgsParser() {
    pOpts = new HashMap<Long, PrintOptions>();
    pOptsEnd   = new PrintOptions(true,  true,  false);
    pOptsBreak = new PrintOptions(true,  false, false);
    pOptsOther = new PrintOptions(false, false, false);
  }
  
  public boolean isPrintHelp() {
    return printHelp;
  }
  
  public String getFile() {
    return file;
  }

  public boolean isConsoleMode() {
    return consoleMode;
  }
  
  public boolean parse(String args[]) {
    if (args.length == 0) { return false; }
    if (args.length >= 1) {
      if (args[0].equals("-?")) {
        printHelp = true;
        return false;
      }
      file = args[0];
    }
    if (args.length >= 2) {
      if (args[1].equals("-c")) {
        consoleMode = true;
        if (parsePrintOptions(args, 2)) { return true; }
      } else {
        errMsg = "Illegal option or place for option: " + args[1]; 
        return true;
      }
    }
    return false;
  }
  
  private boolean parsePrintOptions(String args[], int start) {
    if (args.length <= start) { return false; }

    final Pattern pat = Pattern.compile("((([ebo])|([1-9][0-9]*))+)=([str]*)");
    final int patGroupLeft  = 1;
    final int patGroupRight = 5;
    final Pattern patState = Pattern.compile("[ebo]");
    final Pattern patStep  = Pattern.compile("[0-9]+");
    
    for (int i = start; i < args.length; ++i) {
      String arg = args[i];
      Matcher m = pat.matcher(arg);
      if (!m.matches()) {
        errMsg = "Illegal option or place for option: " + args[i];
        return true;
      }  
      String left  = m.group(patGroupLeft);
      String right = m.group(patGroupRight);
      
      m = patState.matcher(left);
      boolean e = false, b = false, o = false;
      while (m.find()) {
        String gr = m.group();
        if ("e".equals(gr)) { e = true; }
        if ("b".equals(gr)) { b = true; }
        if ("o".equals(gr)) { o = true; }
      }
      m = patStep.matcher(left);
      ArrayList<Long> steps = new ArrayList<Long>();
      while (m.find()) {
        steps.add(Long.parseLong(m.group()));
      }
      boolean s = false, t = false, r = false;
      for (char c : right.toCharArray()) {
        switch (c) {
          case 's': s = true; break;
          case 't': t = true; break;
          case 'r': r = true; break;
        }
      }
      PrintOptions opts = new PrintOptions(s, t, r);
      if (e) { pOptsEnd = opts; }
      if (b) { pOptsBreak = opts; }
      if (o) { pOptsOther = opts; }
      for (long step : steps) {
        pOpts.put(step, opts);
      }
    }
    return false;
  }

  public PrintOptions checkPrinting(
      boolean endState, boolean breakState, long step) {
    PrintOptions options;
    if (endState) {
      options = pOptsEnd;
    } else if (breakState) {
      options = pOptsBreak;
    } else {
      options = pOptsOther;
    }
    for (Map.Entry<Long, PrintOptions> e : pOpts.entrySet()) {
      if (step % e.getKey() == 0) { options = options.or(e.getValue()); }
    }
    return options;
  } 
  
  public String getErrMsg() {
    return errMsg + "\nUse \'tursi -?\' for help.";
  }
  
  public String printOptionsToString() {
    StringBuilder sb = new StringBuilder();
    sb.append( "e=" + pOptsEnd);
    sb.append(" b=" + pOptsBreak);
    sb.append(" o=" + pOptsOther);
    for (Map.Entry<Long, PrintOptions> e : pOpts.entrySet()) {
      sb.append(" " + e.getKey() + "=" + e.getValue());
    }
    return sb.toString();
  }
  
}
