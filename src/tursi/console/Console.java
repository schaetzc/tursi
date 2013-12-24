//  _____               _ 
// |_   _|   _ _ __ ___(_)
//   | || | | | '__/ __| |
//   | || |_| | |  \__ \ |
//   |_| \__,_|_|  |___/_|
//
// A Turing-Machine-Simulator
//
// AIS, University of Freiburg
// Claus Schaetzle

package tursi.console;

import java.io.*;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import tursi.machine.*;
import tursi.parsing.*;
import tursi.view.AliasConverter;
import tursi.view.GUI;

/**
 * Starts the application with GUI or in command line mode
 * (depending on given parameters).
 */
public class Console {
  
  public static int ERR_UNKNOWN      = 1;
  public static int ERR_ARGUMENTS    = 2;
  public static int ERR_FILE_IO      = 3; 
  public static int ERR_FILE_PARSING = 4;
  public static int ERR_RULE         = 5;
  
  public static void main(String args[]) {
    ArgsParser p = new ArgsParser();
    if (p.parse(args)) {
      System.err.println(p.getErrMsg());
      System.exit(ERR_ARGUMENTS);
    }
    if (p.isPrintHelp()) {
      System.out.println(p.HELP);
    } else if (p.isConsoleMode()) {
      Console c = new Console(p);
      c.run();
    } else {
      openGUI(p.getFile());
    }
  }
    
  private static void openGUI(final String filePath) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override public void run() {
        try {
          /*
          // Show Dialog for choosing look and feel
          UIManager.LookAndFeelInfo[] lafs =
              UIManager.getInstalledLookAndFeels();
          String[] lafNames = new String[lafs.length];
          for (int i=0; i < lafs.length; ++i) {
            lafNames[i] = lafs[i].getClassName();
          }
          String laf = (String) javax.swing.JOptionPane.showInputDialog(
              null, "Choose the look and feel", "L&F",
              javax.swing.JOptionPane.PLAIN_MESSAGE, null, lafNames,"");
          UIManager.setLookAndFeel(laf);
          */
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());          
        } catch (Exception e) {
          try {
        	String fallback = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
			UIManager.setLookAndFeel(fallback);
		  } catch (Exception e2) {/*Stick with default*/}
        }
        new GUI(filePath);
      }
    });
  }
  
  // ---------------------------------------------------------------------------
  
  private ArgsParser argsParser;
  
  private Tape tape;
  private MinHistory history;
  private Machine machine;
  
  private PrintStream out = new PrintStream(System.out); // buffer the output
  private PrintStream err = System.err;
  
  private AliasConverter aliasConverter = new AliasConverter() {
    @Override
    public String convertToAlias(int move) {
      switch (move) {
        case -1: return "L";
        case  0: return "N";
        case  1: return "R";
        default: return Integer.toString(move);
      }
    }
  };
  
  public Console(ArgsParser argsParser) {
    this.argsParser = argsParser;
    if (argsParser.getFile() == null) { // shouldn't occur
      err.println("No file specified.");
      System.exit(ERR_ARGUMENTS);
    }
    
    Parser p = new Parser();
    FileReader r = null;
    try {
      r = new FileReader(argsParser.getFile());
      p.parse(new BufferedReader(r));
    } catch (IOException e) {
      err.println("Couldn't read file: " + e.getMessage());
      System.exit(ERR_FILE_IO);
    } catch (ParserException e) {
      printParserErr(e, p.getWarnings());
      System.exit(ERR_FILE_PARSING);
    } finally {
      try { r.close(); } catch (Exception e) { }
    }
    printParserErr(null, p.getWarnings());
    
    tape = p.createTape();
    history = new MinHistory();
    machine = new Machine(tape, p.getRuleTable(), p.getStartState(),
        p.getBreakStates(), p.getEndStates(), p.getWildcard(), history);
  }

  private void printParserErr(ParserException error,
      List<ParserException> warnings) {
    if (error == null && warnings.size() == 0) { return; }
    
    String msg;
    if (error == null) {
      msg = "File parsed with ";
    } else {
      msg = "Couldn't parse file due to 1 error and ";      
    }
    msg += warnings.size() + " warning";
    if (warnings.size() != 1) { msg += "s"; }
    msg += ".";
    err.println(msg);
    
    if (error != null) {
      printParserErr(true, error);
    }
    for (ParserException w : warnings) {
      printParserErr(false, w);
    }
  }

  private void printParserErr(boolean error, ParserException e) {
    if (error) {
      err.print("error   ");
    } else {
      err.print("warning ");
    }
    if (e.lineAvailable()) {
      err.format("(line %4d): ", e.getLine());
    } else {
      err.print("           : ");
    }
    err.println(e.getMessage());
  }
  
  public void run() {
    while (!machine.inEndState()) {
      PrintOptions opts = argsParser.checkPrinting(
          false, machine.inBreakState(), history.steps()); 
      if (opts.s) { printS(); }
      if (opts.t) { printT(); }
      try {
        machine.step();
      } catch (RuleNotFoundException e) {
        RuleTrigger t = e.getTrigger();
        err.println("Couldn't find rule for state '" +
                   t.state + "', read '" + t.read + "'.");
        System.exit(ERR_RULE);
      }
      if (opts.r) { printR(); }
      out.flush();
    }
    PrintOptions opts = argsParser.checkPrinting(true, false, history.steps());
    if (opts.s) { printS(); }
    if (opts.t) { printT(); }
    out.flush();
  }

  private void printS() {
    out.print(history.steps());
    out.print('\t');
    out.print(machine.getState());
    out.println();
  }
  
  private void printT() {
    int left = tape.getLeftmost();
    int right = tape.getRightmost();
    out.print("\ttape (");
    out.print(left);
    out.print(", ");
    out.print(right - left + 1);
    out.print(", ");
    out.print(right);
    out.print("), header ");
    out.println(tape.getPos());
    out.print('\t');
    out.print(tape.read(tape.getLeftmost(), -tape.getLeftmost()));
    out.println();
    out.print('\t');
    out.print(tape.read(0, tape.getRightmost() + 1));
    out.println();
  }
  
  private void printR() {
    Rule r = history.get();
    out.print('\t');
    out.print(r.trigger.state);
    out.print('\t');
    out.print(r.trigger.read);
    out.print('\t');
    out.print(r.action.write);
    out.print('\t');
    out.print(aliasConverter.convertToAlias(r.action.move));
    out.print('\t');
    out.print(r.action.nextState);
    out.println();
  }
  
}
