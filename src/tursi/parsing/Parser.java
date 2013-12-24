package tursi.parsing;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.BufferedReader;
import java.io.IOException;

import tursi.machine.Rule;
import tursi.machine.RuleTrigger;
import tursi.machine.Tape;


/**
 * Parser for tm-files.
 * A parser object parses ONE file and stores the read contents.
 *
 * tm-files are processed line by line and can contain comments, commands and
 * rules.
 * 
 * A line command starts with #, not followed by # or !.
 * 
 * A command starts with #!. The next word is its name, all following
 * words (separated with whitespace) are arguments to this command.
 * There are 6 commands:
 * 
 * {@code
 *   start    <state>
 *   break    <state> [...]
 *   end      <state> [...]
 *   fill     <word>
 *   write    <word>
 *   write    <pos>[<] <word> [...]
 *   wildcard <symbol>
 * }
 * 
 * If a line contains more than whitespace, a comment or a command,
 * it is interpreted as a rule. A rule is a line of the transition
 * table and has 5 fields: state, read, write, move, nextState
 * 
 * Read the <a href="ais.informatik.uni-freiburg.de/tursi/manual">manual</a>
 * for more information about tm-files.
 */
public class Parser {
  
  private static String DEFAULT_FILL_PATTERN = "*";
  
  // Attributes ----------------------------------------------------------------
  
  /** Minor parsing errors. Should be presented to the user. */
  private ArrayList<ParserException> warnings =
      new ArrayList<ParserException>();
  
  // --- following attributes are (partially) redundant because different
  //     structures are more practical in different cases. 
  /** Parsed rules, same order as in the file. */
  private ArrayList<Rule> rules = new ArrayList<Rule>();
  /** Parsed rules as a map. Redundant to {@link #rules}. */
  private HashMap<RuleTrigger,Rule> ruleTable = new HashMap<RuleTrigger,Rule>();
  /** 'States' from all rules. Partially redundant to {@link #rules}. */
  private HashSet<String> inStateSet = new HashSet<String>();
  /** 'nextStates' from all rules. Partially redundant to {@link #rules}. */
  private HashSet<String> outStateSet = new HashSet<String>();
  // --- end redundancy
    
  /**
   * Turing machine's start state. This value is vital. If it's still
   * {@code null} after parsing the whole file, it has to be guessed.
   */
  private String startState = null;
  
  /** Turing machine's break states. */
  private HashSet<String> breakStates = new HashSet<String>();

  /**
   * Turing machine's end states. It's possible to use an empty set, but not
   * recommended.
   */
  private HashSet<String> endStates = new HashSet<String>();
  
  /**
   * The command 'end' was used. If not set, end states should be determined.
   * With this field, it's possible to disable the search for end states,
   * without defining an end state.
   * @see #endStates  
   */
  private boolean endSpecified = false;
  
  /**
   * Turing machine's wildcard. Default value is {@code 0} (not {@code '0'}),
   * because it shouldn't occur, so the wildcard won't seem to be present.
   */
  private char wildcard = 0;
  
  /**
   * Tape's fill pattern. This value is vital. If it's still {@code null} after
   * parsing the whole file, a default value should be used after warning the
   * user. 
   */
  private String fill = null;
  
  /** All parsed write commands, ordered as in the file. */
  private ArrayList<WriteCmd> writeCommands = new ArrayList<WriteCmd>(); 
  
  /** A file was parsed without errors (getters are unlocked). */
  private boolean parsingSuccessful = false;
  
  // ---------------------------------------------------------------------------
  
  // Default constructor 
  
  /**
   * Parse the given file line by line. Parsed data can be obtained through
   * getters. The reader won't be closed.
   * Use this function only once for each Parser, even if parsing fails (old
   * data won't be cleared, when parsing again).
   *  
   * @param br File to be read
   * @return File was read flawless (have a look at the warnings, if not set).
   * 
   * @throws ParserException File was malformed.
   *                         Notice, that there might be minor errors, which
   *                         won't stop parsing, but should be presented. 
   * @throws IOException     An IO error occurred.
   */
  public boolean parse(BufferedReader br) throws ParserException, IOException {
    String line;
    int lineNumber = 0;
    while ((line = br.readLine()) != null) {
      ++lineNumber;
      int wn = warnings.size();
      try {
        parseLine(line);
      } catch (ParserException e) {
        e.setLine(lineNumber);
        throw e;
      } finally {
        // set line numbers of new warnings (even if an error occurred)
        for (int i = wn; i < warnings.size(); ++i) {
          warnings.get(i).setLine(lineNumber);
        }
      }
    }
    checkData();
    parsingSuccessful = true;
    return warnings.isEmpty();
  } 

  // Getters (use them after parsing) ------------------------------------------
  
  /** @return File was parsed successfully. Getters can now be used. */
  public boolean parsingSuccessful() {
    return parsingSuccessful;
  }
  
  /**
   * Returns a List of all warnings, which occurred while reading the file.
   * The warnings are sorted after the line, in which the occurred (except for
   * warnings, without line numbers).
   * @return Warnings from parsing (original, don't modify).
   */
  public ArrayList<ParserException> getWarnings() {
    return warnings;
  }
  
  /**
   * Checks, if the file was parsed successfully.
   * If not, an IllegalStateException will be thrown.
   * Call this in every getter.
   */
  private void checkParsingSuccessful() {
    if (!parsingSuccessful) {
      throw new IllegalStateException("Nothing was (successful) parsed.");
    }
  }
  
  /**
   * Creates a new tape, using {@link #fill} and {@link #writeCommands}.
   * @return New tape.
   */
  public Tape createTape() {
    checkParsingSuccessful();
    Tape tape = new Tape(fill);
    for (WriteCmd wc : writeCommands) { wc.write(tape); }
    return tape;
  }
  
  /**
   * @return All states, used in rule's trigger field {@code state}
   *         (original, don't modify).
   */
  public HashSet<String> getInStateSet() {
    checkParsingSuccessful();
    return inStateSet;
  }
  
  /**
   * @return All states, used in rule's action field {@code nextState}
   *         (original, don't modify).
   */
  public HashSet<String> getOutStateSet() {
    checkParsingSuccessful();
    return outStateSet;
  }
  
  /**
   * Creates a new set, containing all mentioned states, by uniting the sets
   * from  {@link #getInStateSet()} and {@link #getOutStateSet()}. 
   * @return Set of all states.
   */
  public HashSet<String> createStateSet() {
    checkParsingSuccessful();
    HashSet<String> s = new HashSet<String>(inStateSet);
    s.addAll(outStateSet);
    if (startState != null) {
      s.add(startState);
    }
    s.addAll(breakStates);
    s.addAll(endStates);
    return s;
  }
  
  /**
   * Creates a new array with all Rules from the file.
   * The rules have the same order as in the file.
   * @return Array with all rules.
   */
  public Rule[] getRules() {
    checkParsingSuccessful();
    return rules.toArray(new Rule[rules.size()]);
  }
  
  /**
   * Returns a map, containing all rules, linked to their Triggers.
   * @return Map of all Rules (original, don't modify).
   */
  public HashMap<RuleTrigger,Rule> getRuleTable() {
    checkParsingSuccessful();
    return ruleTable;
  }
  
  /** @return Start state, specified in file or guessed after parsing. */
  public String getStartState() {
    checkParsingSuccessful();
    return startState;
  }
  
  /** @return All break states, specified in file. */
  public HashSet<String> getBreakStates() {
    checkParsingSuccessful();
    return breakStates;
  }

  /** @return All end states, specified in file or guessed after parsing. */
  public HashSet<String> getEndStates() {
    checkParsingSuccessful();
    return endStates;
  }
  
  /**
   * @return Wildcard, specified in file,
   *         or default value (which shouldn't occur).
   */
  public char getWildcard() {
    checkParsingSuccessful();
    return wildcard;
  }
  
  /** @return Tape's fill pattern, specified in file or default value. */
  public String getFill() {
    checkParsingSuccessful();
    return fill;
  }
  
  // Private part for parsing --------------------------------------------------
  
  /**
   * Checks the attributes, needed to create a machine.
   * If something is missing, it will be guessed, set to a default value, or
   * an exception will be thrown.
   * @throws ParserException An attribute was not set and couldn't be
   *                         initialized automatically.
   */
  private void checkData() throws ParserException {
    if (rules.isEmpty()) { // could also be a stateSet or the ruleTable
      warnings.add(new ParserException("File had no rules."));
    }
    // ----- start state -----
    if (startState == null) {
      startState = guessStartState();
      String msg = "No start state specified.";
      if (startState == null) {
        msg += " Couldn't guess start state.";
        msg += " Use command 'start' to specify one.";
        startState = "n/a";
      } else {
        msg += " Guessed '" + startState + "'.";
        msg += " Use command 'start' to specify another start state.";
      }
      warnings.add(new ParserException(msg));
    }
    // ----- end state -----
    if (!endSpecified) {
      endStates = findEndStates();
      String msg = "No end states specified. ";
      if (endStates.isEmpty()) {
        msg += "Couldn't guess end state.";
        msg += " Use command 'end' to specify one.";
      } else {
        Iterator<String> iter = endStates.iterator();
        msg += "Found '" + iter.next() + "'";
        while (iter.hasNext()) {
          msg += ", '" + iter.next() + "'";
        }
        msg += ". Use command 'end' to specify other or no end states.";
      }
      warnings.add(new ParserException(msg));
    }
    // ----- fill -----
    if (fill == null) {
      fill = DEFAULT_FILL_PATTERN;
      warnings.add(new ParserException("Default fill pattern '" + fill
                              + "' used. Use command 'fill' to specify one."));
    }
    if (fill.isEmpty()) { //no "else if", default could also be empty
      throw new ParserException("No default value for tape specified."
      		                    + " Use command 'fill' to specify one.");
    }
  }
  
  /**
   * Tries to guess the start state from all rules.
   * If the graph of this turing machine has one ore more sources, the first
   * one will be guessed to be the start state. Otherwise, the first state from
   * the file will be used.
   * @return Guessed start state or {@code null}, if there are no rules.
   */
  private String guessStartState() {
    if (rules.isEmpty()) { return null; }
    for (String s : inStateSet) {
      if (!outStateSet.contains(s)) { return s; } // source in graph
    }
    return rules.get(0).trigger.state; // first state read from file
  }

  /**
   * Finds all end states from all rules.
   * Every sink of the turing machine's graph is expected to be an end state. 
   * @return Set of all end states.
   */
  private HashSet<String> findEndStates() {
    HashSet<String> ends = new HashSet<String>();
    for (String s : outStateSet) {
      if (!inStateSet.contains(s)) { ends.add(s); }
    }
    return ends;
  }  
  
  /**
   * Parses a line.
   * @param line Line to be parsed.
   * @throws ParserException Line was malformed.
   */
  private void parseLine(String line) throws ParserException {
    String[] parts = escAndSplit(line);
    // Rule ----------
    if (!parts[0].isEmpty()) { // was trimmed before
      String[] rule = patSpace.split(parts[0]);
      if (rule.length != 5) {
        throw new ParserException("Expected rule to have 5 fields, but found "
                                  + rule.length + ".");
      }
      addRule(parseRule(rule[0], rule[1], rule[2], rule[3], rule[4]));
    }
    // Commands ----------
    for (int i = 1; i < parts.length; ++i) {
      if (parts[i].isEmpty()) { // was trimmed before
        warnings.add(new ParserException("Ignored empty command."));
      } else {
        String[] args = patSpace.split(parts[i]);
        Command cmd = commands.get(args[0]);
        if (cmd == null) {
          throw new ParserException("Unknown command '" + args[0] + "'.");
        } else {
          cmd.exec(args, this);
        }
      }
    }
  }
  
  /**
   * Adds a rule to all (redundant) rule fields. Avoids inconsistency.
   * @param r Rule to be added
   * @throws ParserException A rule with equal trigger already existed.
   */
  private void addRule(Rule r) throws ParserException {
    if (ruleTable.put(r.trigger, r) != null) {
      throw new ParserException("Rule for state '"
          + r.trigger.state + "' and symbol '"
          + r.trigger.read + "' was already defined.");
    }
    inStateSet.add(r.trigger.state);
    outStateSet.add(r.action.nextState);
    rules.add(r);
  }

  // static part ---------------------------------------------------------------

  /** Pattern for escaped comment symbols. */
  private final static Pattern patEscCmnt = Pattern.compile("##");
  /** Pattern for comments (after escaped ones were removed). */
  private final static Pattern patCmnt = Pattern.compile("#(?!!)"); 
  /** Pattern for command esc sequence (after escaped ones were removed). */
  private final static Pattern patCmd = Pattern.compile("#!");
  /** One ore more whitespace. Use this to split. */
  private final static Pattern patSpace = Pattern.compile("\\s+");
  /** Aliases for value -1 in rule's field 'move'. */
  private final static Pattern patMvLeft = Pattern.compile("[lL<]");
  /** Aliases for value 0 in rule's field 'move'. */
  private final static Pattern patMvNot = Pattern.compile("[nN=sS]");
  /** Aliases for value 1 in rule's field 'move'. */
  private final static Pattern patMvRight = Pattern.compile("[rR>]");

  /**
   * Escapes double {\@code #} and splits the line into rule, commands and
   * comment (which will be removed). Escaped chars will be returned without
   * their escape sequences. Non escaped special chars will be used for
   * splitting, and therefore not be included in the result.
   * The parts will be trimmed, but must be split and trimmed again for further
   * use (because a command and all it's parameters will be in one string).
   * @param line Line to be processed
   * @return Trimmed parts of the line.
   *         First element will always be a (possible empty) string for a rule.
   *         All following elements are commands. Comments aren't included.
   */
  protected static String[] escAndSplit(String line) {   
    String escLine = patEscCmnt.matcher(line).replaceAll("__");
    // delete comment
    Matcher m = patCmnt.matcher(escLine);
    if (m.find()) {
      escLine = escLine.substring(0, m.start());
      line = line.substring(0, m.start());
    }
    // split rule from commands
    m = patCmd.matcher(escLine);
    ArrayList<String> a = new ArrayList<String>();
    int start = 0;
    int end = 0;
    while (m.find()) {
      start = end;
      end = m.start();
      a.add(patEscCmnt.matcher(line.substring(start, end).trim())
          .replaceAll("#")); // replace all ## with #
      end += 2; // length of "#!" (we don't want it)
    }
    a.add(patEscCmnt.matcher(line.substring(end).trim()).replaceAll("#"));
    return a.toArray(new String[a.size()]);
  }
  
  /**
   * Validates input form and creates a rule, if possible.
   * All arguments must not be null and should be trimmed.
   * @param state     Name of the current state
   * @param read      Read symbol (only one char)
   * @param write     Symbol to be written (only one char)
   * @param move      Number of cells to be moved along (number or alias)
   * @param nextState Name of the next state
   * @return new Rule
   * @throws ParserException An argument was malformed
   */
  protected static Rule parseRule(String state, String read,
      String write, String move, String nextState) throws ParserException {
    if (read.length() != 1) {
      throw new ParserException("Rule's field 'read' (2) must be one char.");
    } else if (write.length() != 1) {
      throw new ParserException("Rule's field 'write' (3) must be one char.");
    } else if (state.isEmpty()) {
      throw new ParserException("Rule's field 'state' (1) is empty.");
    } else if (nextState.isEmpty()) {
      throw new ParserException("Rule's field 'nextState' (5) is empty.");
    }
    char r = read.charAt(0);
    char w = write.charAt(0);
    int mv = 0;
    try {
      mv = Integer.parseInt(move);
    } catch (NumberFormatException nfe) {
      if (patMvRight.matcher(move).matches()) {
        mv = 1;
      } else if (patMvLeft.matcher(move).matches()) {
        mv = -1;
      } else if (patMvNot.matcher(move).matches()) {
        mv = 0;
      } else {
        throw new ParserException("Rule's field 'move' (4) must be a number or"
                                + " alias, but was '" + move + "'.");
      }
    }
    return new Rule(state, r, w, mv, nextState);
  } 
  
  // Commands ------------------------------------------------------------------
  
  /**
   * Preparsed version of the write command.
   * This is used to queue them. Because they can only be executed, if the tape
   * exists, and the tape can only be created, if the whole file was parsed.
   */
  static class WriteCmd {
    /** First or last cell to be written to. @see #towards */
    int cell;
    /** Values to be written. */
    String values;
    /** Write the values towards the specified cell, instead of from it. */
    boolean towards;
    
    /**
     * Create a new write command.
     * @param cell    First or last cell to be written to 
     * @param values  Values to be written
     * @param towards Write the values towards the specified cell,
     *                instead of from it.
     */
    WriteCmd(int cell, String values, boolean towards) {
      this.cell = cell;
      this.values = values;
      this.towards = towards;
    }
    
    /**
     * Execute this write command on a given tape.
     * @param tape Tape to be written to
     */
    void write(Tape tape) {
      if (towards) {
        tape.writeTw(cell, values);
      } else {
        tape.write(cell, values);
      }
    }
  }
  
  /**
   * Map of all allowed commands in tm-files.
   * Commands (values) are linked to their name (key).
   */
  private final static HashMap<String,Command> commands = buildCommands();  
  
  /**
   * Creates the Map for {@link #commands}.
   * @return Map of all allowed commands in tm-files.
   */
  private static HashMap<String,Command> buildCommands() {
    HashMap<String,Command> cmd = new HashMap<String,Command>();
    cmd.put("start", new Command() {
      @Override
      public void exec(String[] args, Parser p) throws ParserException {
        if (args.length != 2) {
          throw new ParserException("Command 'start' expected 1 argument,"
                                  + " but found " + (args.length - 1) + ".");
        } else if (p.startState != null) {
          p.warnings.add(new ParserException("Start state '" + p.startState
                             + "' was overwritten with '" + args[1] + "'."));
        }
        p.startState = args[1];
      }
    });
    cmd.put("break", new Command() {
      @Override
      public void exec(String[] args, Parser p) throws ParserException {
        if (args.length < 2) {
          String msg = "Useless call of command 'break'.";
          p.warnings.add(new ParserException(msg));
        }
        for (int i = 1; i < args.length; ++i) {
          if (!p.breakStates.add(args[i])) {
            p.warnings.add(new ParserException("State '" + args[i]
                + "' was already defined as a break point."));
          }
        }
      }
    });
    cmd.put("end", new Command() {
      @Override
      public void exec(String[] args, Parser p) throws ParserException {
        if (p.endSpecified && args.length < 2) {
          String msg = "Useless call of command 'end'.";
          p.warnings.add(new ParserException(msg));
        }
        p.endSpecified = true;
        for (int i = 1; i < args.length; ++i) {
          if (!p.endStates.add(args[i])) {
            p.warnings.add(new ParserException("State '" + args[i]
                                + "' was already defined as an end state."));
          }
        }
      }
    });
    cmd.put("fill", new Command() {
      @Override
      public void exec(String[] args, Parser p) throws ParserException {
        if (args.length != 2) {
          throw new ParserException("Command 'fill' expected 1 argument,"
                                  + " but found " + (args.length - 1) + ".");
        } else if (p.fill != null) {
          p.warnings.add(new ParserException("Fill pattern '" + p.fill
                             + "' was overwritten with '" + args[1] + "'."));
        }
        p.fill = args[1];
      }
    });
    cmd.put("wildcard", new Command() {
      @Override
      public void exec(String[] args, Parser p) throws ParserException {
        if (args.length != 2) {
          throw new ParserException("Command 'wildcard' expected 1 argument, "
              + "but found " + (args.length - 1) + ".");
        } else if (args[1].length() != 1) {
          throw new ParserException("Wildcard must be a single symbol.");
        } else if (p.wildcard != 0) {
            p.warnings.add(new ParserException("Wildcard '" + p.wildcard
                + "' was overwritten with '" + args[1] + "'."));
        }
        p.wildcard = args[1].charAt(0);
      }
    });
    cmd.put("write", new Command() {
      @Override
      public void exec(String[] args, Parser p) throws ParserException {
        if (args.length < 2) {
          throw new ParserException("Command 'write' expected at least" +
          		                      " 1 argument.");
        } 
        boolean isPos = args.length > 2; // current argument is a position
        int pos = 0; // default writing position
        boolean tw = false; // write towards
        for (int i = 1; i < args.length; ++i) {
          if (isPos) {
            String posStr = args[i]; 
            tw = posStr.endsWith("<"); // write towards
            if (tw) {
              // remove last char '<' to parse number 
              posStr = posStr.substring(0, posStr.length() - 1);
            }
            try {
              pos = Integer.parseInt(posStr);
            } catch (NumberFormatException nfe) {
              String msg = "Command 'write' expected a number as argument "
                         + i + ", but found ";
              if (posStr.isEmpty()) {
                msg += "nothing.";
              } else {
                msg += "'" + posStr + "'.";
              }
              throw new ParserException(msg);
            }
            isPos = false;
          } else {
            p.writeCommands.add(new WriteCmd(pos, args[i], tw));
            isPos = true;
          }
        }
        if (!isPos) {
          p.warnings.add(new ParserException("Last argument of command 'write'"
              + " is a position. Add another argument, to write to it."));
        }
      }
    });
    return cmd;
  }

} // +++ End of class Parser +++





/** Skeleton of a command, used by users in the tm-files. */
interface Command {

  /**
   * Execute this command. The implementation don't needs to check, if the
   * command name is the right one. 
   * @param args Arguments for this command. {@code args[0]} contains the
   *             name of this command.
   * @param p    Parser to be modified.
   * 
   * @throws ParserException Couldn't parse or execute command properly.
   *             Minor warnings will be added to {@code p}'s waring list. 
   */
  public void exec(String[] args, Parser p) throws ParserException;
}
