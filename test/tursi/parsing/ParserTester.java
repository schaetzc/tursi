package tursi.parsing;

import java.io.*;

import tursi.parsing.Parser;
import tursi.parsing.ParserException;

/**
 * Standalone version of the parser.
 * Can be used to test it manually with real files.
 */
public class ParserTester {

  /**
   * Parses given files and prints the results.
   * @param args Path to files
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("tm-Dateien als Komandozeilenparameter angeben.");
    }
    for (String path : args) {
      System.out.println("\n\n\n++++++++++ " + path + " ++++++++++\n");
      Parser p = new Parser();
      try {
        BufferedReader br = new BufferedReader(new FileReader(path));
        p.parse(br);
        System.out.println("Successfull");
        p.printWarnings();
        System.out.println();
        p.printAttributes();
      } catch (IOException ioe) {
        System.out.println("IO exception");
        System.out.println(ioe.getMessage());
        ioe.printStackTrace(System.out);
      } catch (ParserException pe) {
        System.out.println("Handled/known format exception");
        if (pe.lineAvailable()) { System.out.print(pe.getLine() + ": "); }
        System.out.println(pe.getMessage());
        p.printWarnings();
      } catch (Exception e) {
        System.err.println("Unhandled/unknown error -- PROGRAMM ERROR!");
        System.err.println(e.getMessage());
        e.printStackTrace(System.err);
      }
    }
  }

  
  
}
