package tursi.view;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import tursi.console.Console;
import tursi.export.*;
import tursi.machine.*;
import tursi.machine.events.*;
import tursi.parsing.*;
import tursi.view.dialogs.*;
import tursi.view.events.*;
import tursi.view.icons.IconLoader;

public class GUI implements AliasConverter, StateTypeTester, PrefConsumer {
  
  // --------- (temporary) preferences ---------
  private final PrefDialog pref;

  private boolean autoReset; 
  private String  aliasLeft;
  private String  aliasNone;
  private String  aliasRight;
  private boolean breakpointsOn;
  
  // --------- core data ---------- 

  private final LimitedHistory history; // also histTableModel
  private Tape tape;
  private Machine machine;
  private Parser curParser; // parser from currently opened tm-file
  
  private boolean ignoreCurStateChanges = false;
  private boolean ignoreHeadPosChanges = false;
  private final   AdaptiveTimer autoRunTimer;
  
  private final TapeListener  tapeListener;
  private final StateListener stateListener;
  
  
  // --------- GUI elements  ----------
  private final int brdrGap   = 4;
  private final int tlbrGap   = 8;
  private final int gridInset = 3;
  
  private final JFrame frame;
  
  private final TapeViewer tapeViewer;

  private final JLabel lblRuleCol;
  private final JLabel lblWildcard;
  private final JLabel valWildcard;
  private final RuleTableModel ruleTableModel;
  private final TableRenderer ruleTableRenderer; 
  private final JTable ruleTable;
  private final JScrollPane ruleTableScrollPane;
  
  private final JLabel lblHistCol;
  private final JLabel histIncompMarker;
  private final JButton btnClearHist;
  private final TableRenderer histTableRenderer;
  private final JTable histTable;
  private final JScrollPane histTableScrollPane;
  
  private final JLabel lblTapeCol; // top of the ctrlCol
  private final JButton btnResetTape;
  private final JButton btnScrollNone;
  private final JButton btnScrollBorders;
  private final JButton btnScrollImmediate;
  private final JTextField writeValuesField;
  private final SpinnerNumberModel writeCellModel;
  private final JSpinner writeCellSpinner;
  private final JButton btnTapeWriteTowards;
  private final JButton btnTapeWriteFrom;
  private final JButton btnResetMachine;
  private final JButton btnStepBackwards;
  private final JButton btnStepForwards;
  private final JButton btnRunStop;
  private final JButton btnBreakpoints;
  private final FSlider speedSlider;
  private final JLabel lblCurState;
  private final StateCellRenderer curStateRenderer;
  private final JComboBox<String> curStateComboBox;
  private final JLabel lblHeadPos;
  private final SpinnerNumberModel headPosModel;
  private final JSpinner headPosSpinner;
  private final JLabel lblSteps;
  private final JLabel valSteps;
  private final JLabel lblLeftmostCell;
  private final JLabel valLeftmostCell;
  private final JLabel lblRightmostCell;
  private final JLabel valRightmostCell;
  private final JLabel lblInitialCell;
  private final JLabel valInitialCell;
  
  private final JPanel rulePanel;
  private final JPanel histPanel;
  private final JPanel ctrlPanel;
  private final JSplitPane splitLC_R;
  private final JSplitPane splitL_C;
  
  private final JMenuBar menuBar;
  private final JMenu menuFile;
  private final JMenu menuFileExport;
  private final JMenu menuView;
  private final JMenu menuHelp;
  private final JMenuItem menuFileOpen;
  private final JMenuItem menuFileReload;
  private final JMenuItem menuFileExportTape;
  private final JMenuItem menuFileExportRules;
  private final JMenuItem menuFileExportHist;
  private final JMenuItem menuFilePreferences;
  private final JMenuItem menuFileQuit;
  private final JCheckBoxMenuItem menuViewMarkCurrent;
  private final JCheckBoxMenuItem menuViewMarkStart;
  private final JCheckBoxMenuItem menuViewMarkBreak;
  private final JCheckBoxMenuItem menuViewMarkEnd;
  private final JCheckBoxMenuItem menuViewTape;
  private final JCheckBoxMenuItem menuViewRules;
  private final JCheckBoxMenuItem menuViewHist;
  private final JMenuItem menuHelpAbout;
  private final JMenuItem menuHelpWebsite;
  
  public void quit() {
    pref.exit();
    frame.dispose(); // may not be needed
    System.exit(0);
  }
  
  public GUI(final String filePath) {  

    Thread.setDefaultUncaughtExceptionHandler(
      new Thread.UncaughtExceptionHandler() {
        @Override public void uncaughtException(Thread t, Throwable e) {
          new UnexpectedErrorDialog(t, e);
        }
      }
    );
    
    // ----- core variables ----------------------------------------------------
    pref = new PrefDialog(this);
    autoReset  = pref.getAutoReset();
    aliasLeft  = pref.getAliasLeft();
    aliasNone  = pref.getAliasNone();
    aliasRight = pref.getAliasRight();
    
    curParser = new Parser();
    try {
      String tm = "#! fill * #! start n/a #! end n/a";
      curParser.parse(new BufferedReader(new StringReader(tm)));
    } catch (Exception e) {
      dispError("Parser must be corrupted!\n" + e.getMessage(), "Error");
      System.exit(Console.ERR_UNKNOWN);
    }
    tape = curParser.createTape();
    history = new LimitedHistory(pref.getHistSize());
    machine = new Machine(tape, curParser.getRuleTable(),
        curParser.getStartState(), curParser.getBreakStates(),
        curParser.getEndStates(), curParser.getWildcard(), history);
    autoRunTimer = new AdaptiveTimer(new Runnable() {
      @Override public void run() {
        stepForwardsAutoRun();
      }
    });
    
    // listeners that need to be relinked when loading a new machine
    stateListener = new StateListener() {
      @Override public void stateChanged() {
        if (menuViewMarkCurrent.isEnabled()) {
          ruleTable.repaint();        
        }
        curStateComboBox.setSelectedItem(machine.getState());
      }
    };
    tapeListener = new TapeListener() {
      @Override public void tapeChanged() {
        headPosModel.setValue(tape.getPos());
        valLeftmostCell.setText(Integer.toString(tape.getLeftmost()));
        valRightmostCell.setText(Integer.toString(tape.getRightmost()));
      }
    };
    
    // ----- view variables ----------------------------------------------------
    
    if (!IconLoader.loadingSuccessful()) {
      dispError("Could not load one or more icons.", "Missing Icons");
      //continue anyway (some buttons may be empty)
    }
    frame = new JFrame("Tursi");
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent e) {
        quit();
      }
    });

    tapeViewer = new TapeViewer(tape, pref.getTVFrameLength(), pref.getTVStripeSize());
    
    lblRuleCol  = new JLabel("Rules");
    lblWildcard = new JLabel("Wildcard: ");
    valWildcard = new JLabel("none");
    updateValWildcard();
    ruleTableModel = new RuleTableModel(curParser.getRules());
    ruleTableRenderer = TableRenderer.makeRuleRenderer(this, this);
    ruleTable = new JTable(ruleTableModel);
    ruleTable.setDefaultRenderer(Object.class, ruleTableRenderer);
    ruleTableScrollPane = new JScrollPane(ruleTable);
    
    lblHistCol        = new JLabel("History");
    histIncompMarker  = new JLabel(" (incomplete)");
    histIncompMarker.setVisible(false);
    btnClearHist      = makeToolbarBtn(IconLoader.Id.CLEAR_HISTORY.get());
    histTableRenderer = TableRenderer.makeHistRenderer(this, this);
    histTable         = new JTable(history);
    histTable.setDefaultRenderer(Object.class, histTableRenderer);
    histTableScrollPane = new JScrollPane(histTable);
    
    lblTapeCol          = new JLabel("Tape");
    btnResetTape        = makeToolbarBtn(IconLoader.Id.RESET_TAPE.get());
    btnScrollNone       = makeToolbarBtn(IconLoader.Id.SCROLL_NONE_OFF.get());
    btnScrollBorders    = makeToolbarBtn(IconLoader.Id.SCROLL_BORDERS_OFF.get());
    btnScrollImmediate  = makeToolbarBtn(IconLoader.Id.SCROLL_IMMEDIATE_OFF.get());
    writeValuesField    = new JTextField();
    writeCellModel      = new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
    writeCellSpinner    = new JSpinner(writeCellModel);
    Misc.setPlainNumEditor(writeCellSpinner);
    btnTapeWriteTowards = makeToolbarBtn(IconLoader.Id.WRITE_TOWARDS.get());
    btnTapeWriteFrom    = makeToolbarBtn(IconLoader.Id.WRITE_FROM.get());
    btnResetMachine     = makeToolbarBtn(IconLoader.Id.RESET_MACHINE.get());
    btnStepBackwards    = makeToolbarBtn(IconLoader.Id.STEP_BACKWARDS.get());
    btnStepForwards     = makeToolbarBtn(IconLoader.Id.STEP_FORWARDS.get());
    btnRunStop          = makeToolbarBtn(IconLoader.Id.RUN.get());
    btnBreakpoints      = makeToolbarBtn(IconLoader.Id.BREAKPOINTS_ON.get());
    setBreakpoints(true);
    speedSlider         = new FSlider(1000, 250);
    speedSlider.setInverted(true);
    lblCurState         = new JLabel("Current State");
    curStateComboBox    = new JComboBox<>();
    curStateRenderer    = new StateCellRenderer(curStateComboBox, this);
    curStateComboBox.setRenderer(curStateRenderer);
    updateCurStateModel();
    lblHeadPos        = new JLabel("Head Position");
    headPosModel      = new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
    headPosSpinner    = new JSpinner(headPosModel);
    Misc.setPlainNumEditor(headPosSpinner);
    lblSteps            = new JLabel("Steps");
    valSteps            = new JLabel(Long.toString(history.steps()));
    lblLeftmostCell     = new JLabel("Leftmost Cell");
    valLeftmostCell     = new JLabel(Integer.toString(tape.getLeftmost()));
    lblRightmostCell    = new JLabel("Rightmost Cell");
    valRightmostCell    = new JLabel(Integer.toString(tape.getRightmost()));
    lblInitialCell      = new JLabel("Initial Cell");
    valInitialCell      = new JLabel("0");
    
    rulePanel = new JPanel(new BorderLayout(brdrGap, brdrGap));
    histPanel = new JPanel(new BorderLayout(brdrGap, brdrGap));
    ctrlPanel = new JPanel(new BorderLayout(brdrGap, brdrGap));
    splitLC_R = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitL_C  = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    menuBar  = new JMenuBar();
    menuFile = new JMenu("File");
    menuView = new JMenu("View");
    menuHelp = new JMenu("Help");
    menuFileOpen        = new JMenuItem("Open...", IconLoader.Id.OPEN.get());
    menuFileReload      = new JMenuItem("Reload", IconLoader.Id.RELOAD.get());
    menuFileExport      = new JMenu("Export");
    menuFileExportTape  = new JMenuItem("Tape (txt)...");
    menuFileExportRules = new JMenuItem("State diagram (gml)...");
    menuFileExportHist  = new JMenuItem("History (tsv)...");
    menuFilePreferences = new JMenuItem("Preferences");
    menuFileQuit        = new JMenuItem("Quit");
    menuViewMarkCurrent = new JCheckBoxMenuItem("Mark current state", true);
    menuViewMarkStart   = new JCheckBoxMenuItem("Mark start states",  true);
    menuViewMarkBreak   = new JCheckBoxMenuItem("Mark break states",  false);
    menuViewMarkEnd     = new JCheckBoxMenuItem("Mark end states",    true);
    menuViewTape        = new JCheckBoxMenuItem("show tape",    true);
    menuViewRules       = new JCheckBoxMenuItem("show rules",   true);
    menuViewHist        = new JCheckBoxMenuItem("show history", true);
    menuHelpAbout       = new JMenuItem("About");
    menuHelpWebsite     = new JMenuItem("Website");
    
    addListeners();
    setMenuAccelerators();
    setKeyboardShortcuts();
    setToolTips();
    assembleGUI();
    
    if (filePath != null) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override public void run() {
          openTMFile(new File(filePath));
        }
      });      
    }
  }
  
  private void addListeners() {
    // ----- menu bar ----------------------------------------------------------
    menuFileOpen.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        File f = openFileDialog();
        if (f != null) { openTMFile(f); }
      }
    });
    menuFileReload.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        openTMFile(new File(pref.getLastTMFile()));
      }
    });
    menuFileExportTape.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        String extDetail = "Text File (*.txt)";
        File f = saveFileDialog("tape.txt", extDetail, "txt");
        if (f != null) { exportTape(f); }
      }
    });
    menuFileExportRules.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        String extDetail = "Graph Modelling Language (*.gml)";
        File f = saveFileDialog("stateDiagram.gml", extDetail, "gml");
        if (f != null) { exportRules(f); }
      }
    });
    menuFileExportHist.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        String extDetail = "Tab-Separated values (*.tsv)";
        File f = saveFileDialog("history.tsv", extDetail, "tsv");
        if (f != null) { exportHist(f); }
      }
    });
    menuFilePreferences.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        pref.show();
      }
    });
    menuFileQuit.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        quit();
      }
    });
    
    menuHelpAbout.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        
        JLabel lbl1 = new JLabel("Tursi - Turing Machine Simulator");
        Font fnt = lbl1.getFont(); 
        lbl1.setFont(fnt.deriveFont(Font.BOLD, fnt.getSize()*1.15f));
        JLabel lbl2 = new JLabel("Version 1.1");
        JLabel lbl3 = new JLabel("2014-12-09");
        JPanel msg = Misc.makeBoxPanel(BoxLayout.Y_AXIS);
        msg.add(lbl1);
        msg.add(Box.createVerticalGlue());
        msg.add(lbl2);
        msg.add(lbl3);
        msg.add(Box.createVerticalGlue());
        JOptionPane.showMessageDialog(
            frame, msg, "About", JOptionPane.INFORMATION_MESSAGE,
            IconLoader.get(IconLoader.Id.LOGO_64));
      }
    });

    menuHelpWebsite.addActionListener(new ActionListener() {
      private final String url =
          "http://ais.informatik.uni-freiburg.de/tursi/";
      @Override public void actionPerformed(ActionEvent e) {
        try {
          Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (UnsupportedOperationException uoe) {
          dispError("Opening a browser is not supported by your system.",
              "Cannot open browser");
        } catch (IOException ioe) {
          dispError("Your default browser couldn't open the website.",
              "Cannot open website");
        } catch (URISyntaxException use) { // Should not occur
          throw new IllegalArgumentException("Not a URI: " + use.getReason(),
              use); // will be catched by "unexpected error" handler
        }
      }
    });
    
    ActionListener tableViewListener = new ActionListener() {
      private int locL_C;
      @Override public void actionPerformed(ActionEvent e) {
        if (splitLC_R.getLeftComponent() == splitL_C) { // if both are visible
          locL_C = splitL_C.getDividerLocation();
        }
        boolean r = menuViewRules.isSelected();
        boolean h = menuViewHist.isSelected();
        final int locLC_R = splitLC_R.getDividerLocation();
        if (r && h) {
          splitL_C.setLeftComponent(rulePanel);
          splitL_C.setRightComponent(histPanel);
          splitLC_R.setLeftComponent(splitL_C);
        } else if (r) {
          splitLC_R.setLeftComponent(rulePanel);
        } else if (h) {
          splitLC_R.setLeftComponent(histPanel);
        } else {
          splitLC_R.setLeftComponent(new JPanel()); // empty
        }
        splitLC_R.setDividerLocation(locLC_R);
        if (r && h) {
          // works only after splitLC_R was repainted, so invoke it later
          final int locL_C = this.locL_C;
          SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
              if (locL_C >= locLC_R + splitL_C.getDividerSize()) {
                splitL_C.setDividerLocation(
                    locLC_R / 2 - splitL_C.getDividerSize() / 2);
              } else {
                splitL_C.setDividerLocation(locL_C);
              }
            }
          });          
        }
      }
    };    
    menuViewTape.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        tapeViewer.setVisible(menuViewTape.isSelected());
      }
    });
    menuViewRules.addActionListener(tableViewListener);
    menuViewHist.addActionListener(tableViewListener);
    
    ActionListener markListener = new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        boolean b0 = menuViewMarkCurrent.isSelected();
        boolean b1 = menuViewMarkStart.isSelected(); 
        boolean b2 = menuViewMarkBreak.isSelected();
        boolean b3 = menuViewMarkEnd.isSelected();
        if (ruleTableRenderer.setMarks(b0, b1, b2, b3)) {
          ruleTable.repaint();
        }
        if (curStateRenderer.setMarks(b0, b1, b2, b3)) {
          curStateComboBox.repaint();          
        }
      }
    };
    menuViewMarkCurrent.addActionListener(markListener);
    menuViewMarkStart.addActionListener(markListener);
    menuViewMarkBreak.addActionListener(markListener);
    menuViewMarkEnd.addActionListener(markListener);
    markListener.actionPerformed(null); // update values
    
    // ----- reset buttons for history and tape --------------------------------  
    btnClearHist.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        setAutoRun(false);
        history.clear();
      }
    });
    btnResetTape.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        resetTape();
      }
    });
    
    // ----- scroll mode buttons -----------------------------------------------
    btnScrollNone.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        tapeViewer.setScrollMode(TapeViewer.SCROLL_NONE);
      }
    });
    btnScrollBorders.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        tapeViewer.setScrollMode(TapeViewer.SCROLL_BORDERS);
      }
    });
    btnScrollImmediate.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        tapeViewer.setScrollMode(TapeViewer.SCROLL_IMMEDIATE);
      }
    });
    tapeViewer.addScrollModeListener(new ScrollModeListener() {
      final ImageIcon on1  = IconLoader.Id.SCROLL_NONE_ON.get();
      final ImageIcon on2  = IconLoader.Id.SCROLL_BORDERS_ON.get();
      final ImageIcon on3  = IconLoader.Id.SCROLL_IMMEDIATE_ON.get();
      final ImageIcon off1 = IconLoader.Id.SCROLL_NONE_OFF.get();
      final ImageIcon off2 = IconLoader.Id.SCROLL_BORDERS_OFF.get();
      final ImageIcon off3 = IconLoader.Id.SCROLL_IMMEDIATE_OFF.get();
      @Override
      public void scrollModeChanged(ScrollModeEvent e) {
        btnScrollNone.setIcon(
            e.scrollMode == TapeViewer.SCROLL_NONE ? on1 : off1);
        btnScrollBorders.setIcon(
            e.scrollMode == TapeViewer.SCROLL_BORDERS ? on2 : off2);
        btnScrollImmediate.setIcon(
            e.scrollMode == TapeViewer.SCROLL_IMMEDIATE ? on3 : off3);
      }
    });
    tapeViewer.fireScrollModeChanged();
    
    // ----- tape write toolbar ------------------------------------------------
    writeValuesField.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        btnTapeWriteFrom.doClick();
      }
    });
    btnTapeWriteTowards.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        setAutoRun(false);
        history.clear();
        tape.writeTw(writeCellModel.getNumber().intValue(),
            writeValuesField.getText());
      }
    });
    btnTapeWriteFrom.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        setAutoRun(false);
        history.clear();
        tape.write(writeCellModel.getNumber().intValue(),
            writeValuesField.getText());
      }
    });
    
    // ----- machine control toolbar -------------------------------------------
    btnResetMachine.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        resetMachine();
      }
    });
    btnStepBackwards.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        stepBackwards();
      }
    });
    btnStepForwards.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        stepForwards();
      }
    });
    btnRunStop.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        setAutoRun(!autoRunTimer.isRunning());
      }
    });
    btnBreakpoints.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        setBreakpoints(!breakpointsOn);
      }
    });
    speedSlider.addChangeListener(new ChangeListener() {
      @Override public void stateChanged(ChangeEvent e) {
        autoRunTimer.changeSpeed(speedSlider.getFValue());
      }
    });
    
    // ----- machine state / position ------------------------------------------  
    history.setHistoryListener(new HistoryListener() {
      @Override public void historyContentChanged() {
        histIncompMarker.setVisible(!history.isComplete());
        valSteps.setText(Long.toString(history.steps()));
      }
    });
    tape.addTapeListener(tapeListener);
    machine.setStateListener(stateListener);
    curStateComboBox.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        processCurStateChange();
      }
    });
    headPosModel.addChangeListener(new ChangeListener() {
      @Override public void stateChanged(ChangeEvent e) {
        processHeadPosChange();
      }
    });
    
    // ----- jump to leftmost and rightmost cell -------------------------------
    MouseListener jumpLeftmost = new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
          tapeViewer.scrollTo(tape.getLeftmost());          
        }
      }
    };
    MouseListener jumpRightmost = new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
          tapeViewer.scrollTo(tape.getRightmost());          
        }
      }
    };
    MouseListener jumpInitial = new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
          tapeViewer.scrollTo(0);          
        }
      }
    };
    MouseListener jumpHead = new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
          tapeViewer.scrollTo(tape.getPos());          
        }
      }
    };
    lblLeftmostCell.addMouseListener(jumpLeftmost);
    valLeftmostCell.addMouseListener(jumpLeftmost);
    lblRightmostCell.addMouseListener(jumpRightmost);
    valRightmostCell.addMouseListener(jumpRightmost);
    lblInitialCell.addMouseListener(jumpInitial);
    valInitialCell.addMouseListener(jumpInitial);
    lblHeadPos.addMouseListener(jumpHead);
  }
  
  private void setMenuAccelerators() {
    int k = Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask();
    menuFileOpen.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_O, k));
    menuFileReload.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_R, k));
    menuFilePreferences.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, k));
    menuFileQuit.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, k));
    menuViewTape.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_0, k | KeyEvent.SHIFT_DOWN_MASK));
    menuViewRules.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_1, k | KeyEvent.SHIFT_DOWN_MASK));
    menuViewHist.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_2, k | KeyEvent.SHIFT_DOWN_MASK));
    menuViewMarkStart.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_1, k));
    menuViewMarkBreak.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_2, k));
    menuViewMarkEnd.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_3, k));
    menuViewMarkCurrent.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_0, k));
  }
  
  private void setKeyboardShortcuts() {
    tapeViewer.addKeyListener(new KeyListener() {
      @Override public void keyTyped(KeyEvent e) { }
      @Override public void keyReleased(KeyEvent e) { }
      @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT) {
          stepBackwards();
        } else if (k == KeyEvent.VK_RIGHT) {
          stepForwards();
        } else if (k == KeyEvent.VK_SPACE) {
          setAutoRun(!autoRunTimer.isRunning());
        } else if (k == KeyEvent.VK_BACK_SPACE) {
          resetMachine();
        }
      }
    });
  }
  
  private void assembleGUI() {
    // ----- initialize three columns ------------------------------------------ 
    JPanel ruleTitlePanel = makeBoxPanel(BoxLayout.X_AXIS);
    ruleTitlePanel.add(lblRuleCol);
    ruleTitlePanel.add(Box.createHorizontalStrut(tlbrGap));
    ruleTitlePanel.add(Box.createHorizontalGlue());
    ruleTitlePanel.add(lblWildcard);
    ruleTitlePanel.add(valWildcard);
    
    JPanel histTitlePanel = makeBoxPanel(BoxLayout.X_AXIS);
    histTitlePanel.add(lblHistCol);
    histTitlePanel.add(histIncompMarker);
    histTitlePanel.add(Box.createHorizontalStrut(tlbrGap));
    histTitlePanel.add(Box.createHorizontalGlue());
    histTitlePanel.add(btnClearHist);
    
    JPanel ctrlTitlePanel = makeBoxPanel(BoxLayout.X_AXIS);
    ctrlTitlePanel.add(lblTapeCol);
    ctrlTitlePanel.add(Box.createHorizontalStrut(tlbrGap));
    ctrlTitlePanel.add(Box.createHorizontalGlue());
    ctrlTitlePanel.add(btnScrollNone);
    ctrlTitlePanel.add(btnScrollBorders);
    ctrlTitlePanel.add(btnScrollImmediate);
    ctrlTitlePanel.add(Box.createHorizontalStrut(tlbrGap));
    ctrlTitlePanel.add(Box.createHorizontalGlue());
    ctrlTitlePanel.add(btnResetTape);
    
    //make title panels the same height
    int h1 = ruleTitlePanel.getPreferredSize().height;
    int h2 = histTitlePanel.getPreferredSize().height;
    int h3 = ctrlTitlePanel.getPreferredSize().height;
    Dimension strut = new Dimension(0, Math.max(h1, Math.max(h2, h3)));
    ruleTitlePanel.add(Box.createRigidArea(strut));
    histTitlePanel.add(Box.createRigidArea(strut));
    ctrlTitlePanel.add(Box.createRigidArea(strut));
    
    rulePanel.add(ruleTitlePanel, BorderLayout.NORTH);
    histPanel.add(histTitlePanel, BorderLayout.NORTH);
    ctrlPanel.add(ctrlTitlePanel, BorderLayout.NORTH);
    
    disableTableInteractivity(histTable);
    disableTableInteractivity(ruleTable);
    
    ruleTableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    ruleTableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    histTableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    histTableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    rulePanel.add(ruleTableScrollPane, BorderLayout.CENTER);
    histPanel.add(histTableScrollPane, BorderLayout.CENTER);
    ctrlPanel.add(makeCtrlColBody(), BorderLayout.CENTER);
    
    Border b = BorderFactory.createEmptyBorder(brdrGap, brdrGap, brdrGap, brdrGap);
    rulePanel.setBorder(b);
    histPanel.setBorder(b);
    ctrlPanel.setBorder(b);

    Misc.setMinWidth(rulePanel, 0);
    Misc.setMinWidth(histPanel, 0);
    
    // ----- assemble menu bar -------------------------------------------------  
    menuBar.add(menuFile);
    menuBar.add(menuView);
    menuBar.add(menuHelp);
    menuFile.add(menuFileOpen);
    menuFile.add(menuFileReload);
    menuFile.add(menuFileExport);
    menuFileExport.add(menuFileExportTape);
    menuFileExport.add(menuFileExportRules);
    menuFileExport.add(menuFileExportHist);
    menuFile.addSeparator();
    menuFile.add(menuFilePreferences);
    menuFile.addSeparator();
    menuFile.add(menuFileQuit);
    menuView.add(menuViewMarkCurrent);
    menuView.add(menuViewMarkStart);
    menuView.add(menuViewMarkBreak);
    menuView.add(menuViewMarkEnd);
    menuView.addSeparator();
    menuView.add(menuViewTape);
    menuView.add(menuViewRules);
    menuView.add(menuViewHist);
    menuHelp.add(menuHelpAbout);
    menuHelp.add(menuHelpWebsite);
    
    // ----- assemble frame-----------------------------------------------------
    frame.setJMenuBar(menuBar);
    frame.setIconImages(IconLoader.logoList());
    Container cp = frame.getContentPane();
    cp.setLayout(new BorderLayout());
    cp.add(tapeViewer, BorderLayout.NORTH);
    cp.add(splitLC_R, BorderLayout.CENTER);
    
    // limit size of ctrlPanel (more space will be empty);
    JPanel wrapCtrlPanel = makeBoxPanel(BoxLayout.X_AXIS);
    ctrlPanel.setAlignmentY(0);
    Dimension d = ctrlPanel.getPreferredSize();
    d.width *= 1.3;
    ctrlPanel.setPreferredSize(d);
    ctrlPanel.setMaximumSize(d);
    wrapCtrlPanel.add(Box.createGlue());
    wrapCtrlPanel.add(ctrlPanel);
    wrapCtrlPanel.add(Box.createGlue());
    Misc.setPrefWidth(wrapCtrlPanel, ctrlPanel.getPreferredSize().width * 0.5);
    
    splitLC_R.setBorder(null);
    splitL_C.setBorder(null);
    splitLC_R.setLeftComponent(splitL_C);
    splitLC_R.setRightComponent(wrapCtrlPanel);
    
    int ctrlPanelMinHeight = cp.getPreferredSize().height; 

    splitL_C.setRightComponent(histPanel);
    splitL_C.setLeftComponent(rulePanel);
    
    
    frame.pack();
    Insets in = frame.getInsets();
    int frameMinWidth  = ctrlPanel.getMinimumSize().width + in.left + in.right
                       + splitL_C.getDividerSize() + splitLC_R.getDividerSize();
    int frameMinHeight = ctrlPanelMinHeight + menuBar.getPreferredSize().height
                       + in.top + in.bottom;
    frame.setMinimumSize(new Dimension(frameMinWidth, frameMinHeight));
    frame.setSize(3*frameMinWidth, frameMinHeight);
    splitLC_R.setDividerLocation(
        2*frameMinWidth - splitLC_R.getDividerSize()/2);
    splitL_C.setDividerLocation(
        frameMinWidth - splitL_C.getDividerSize()/2);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    
  }

  private JPanel makeCtrlColBody() {
    Misc.setMaxToPrefHeight(writeCellSpinner);
    Misc.setMinWidth(writeCellSpinner, 0);
    JPanel writeLn2 = makeBoxPanel(BoxLayout.X_AXIS);
    writeLn2.add(btnTapeWriteTowards);
    writeLn2.add(writeCellSpinner);
    writeLn2.add(btnTapeWriteFrom);
    btnTapeWriteTowards.setFocusable(true);
    btnTapeWriteFrom.setFocusable(true);
    
    JPanel toolbar = makeBoxPanel(BoxLayout.X_AXIS);
    toolbar.add(btnResetMachine);
    toolbar.add(btnStepBackwards);
    toolbar.add(btnStepForwards);
    toolbar.add(btnRunStop);
    toolbar.add(btnBreakpoints);  
    
    JPanel p = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.WEST;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = new Insets(gridInset, gridInset, gridInset, gridInset);
    
    // ----- all components, spanning 2 cols ----- 
    c.gridwidth = 2;
    c.gridx = 0;
    c.gridy = 0;
    p.add(writeValuesField, c);
    c.gridy = 1;
    p.add(writeLn2, c);
    c.gridy = 2;
    p.add(makeHorSeparator(), c);
    c.gridwidth = 2;
    c.gridy = 6;
    p.add(makeHorSeparator(), c);
    
    // ----- left col -----
    c.gridwidth = 1;
    c.gridy = 3;
    p.add(toolbar, c);
    c.gridy = 4;
    p.add(lblCurState, c);
    c.gridy = 5;
    p.add(lblHeadPos, c);
    c.gridy = 7;
    p.add(lblSteps, c);
    c.gridy = 8;
    p.add(lblLeftmostCell, c);
    c.gridy = 9;
    p.add(lblRightmostCell, c);
    c.gridy = 10;
    p.add(lblInitialCell, c);
    
    // ----- right col -----
    c.weightx = 1;
    c.gridx = 1;
    c.gridy = 3;
    p.add(speedSlider, c);
    c.gridy = 4;
    p.add(curStateComboBox, c);
    c.gridy = 5;
    p.add(headPosSpinner, c);
    c.gridy = 7;
    p.add(valSteps, c);
    c.gridy = 8;
    p.add(valLeftmostCell, c);
    c.gridy = 9;
    p.add(valRightmostCell, c);
    c.gridy = 10;
    p.add(valInitialCell, c);
    
    // ------ filler at the end keeps previous elements at the top -----
    c.weighty = 1;
    c.gridwidth = 2;
    c.gridx = 0;
    c.gridy = 11;
    p.add(new JPanel(), c);

    Misc.setMinWidth(p, toolbar.getPreferredSize().width * 1.5);
    return p;
  }
  
  private void setToolTips() {
    histIncompMarker.setToolTipText("Some of the oldest steps were discarded");
    btnClearHist.setToolTipText("Clear history");
    btnScrollNone.setToolTipText("Don't scroll with head");
    btnScrollBorders.setToolTipText("Scroll with head when at border");
    btnScrollImmediate.setToolTipText("Immediately scroll with head");
    btnResetTape.setToolTipText("Reset tape");
    writeValuesField.setToolTipText("Word to be written onto the tape");
    btnTapeWriteTowards.setToolTipText(
        "Write towards this cell (word ends here)");
    writeCellSpinner.setToolTipText("Destination cell of write commands");
    btnTapeWriteFrom.setToolTipText(
        "Write from this cell (word starts here)");
    btnResetMachine.setToolTipText("Reset machine to the start state");
    btnStepBackwards.setToolTipText("Undo last step from history");
    btnStepForwards.setToolTipText("Find and excute next rule");
    btnRunStop.setToolTipText("Enable or disable automatic execution");
    btnBreakpoints.setToolTipText("Enable or disable breaking on breakpoints");
    ChangeListener speedChangeListener = new ChangeListener() {
      private final String speedTT = "Pause time between steps: ";
      @Override public void stateChanged(ChangeEvent e) {
        speedSlider.setToolTipText(speedTT + speedSlider.getFValue() + "ms");
      }
    };
    speedChangeListener.stateChanged(null);
    speedSlider.addChangeListener(speedChangeListener);
    curStateComboBox.setToolTipText("Current state");
    headPosSpinner.setToolTipText("Current head position");
    String headTT = "Current head position - click to view";
    lblHeadPos.setToolTipText(headTT);
    String stepsTT = "Taken steps";
    lblSteps.setToolTipText(stepsTT);
    valSteps.setToolTipText(stepsTT);
    String leftTT = "Leftmost cell accessed by head - click to view";
    lblLeftmostCell.setToolTipText(leftTT);
    valLeftmostCell.setToolTipText(leftTT);
    String rightTT = "Rightmost cell accessed by head - click to view";
    lblRightmostCell.setToolTipText(rightTT);
    valRightmostCell.setToolTipText(rightTT);
    String initTT = "Cell where the head started - click to view";
    lblInitialCell.setToolTipText(initTT);
    valInitialCell.setToolTipText(initTT);
  }
  
  private static JPanel makeBoxPanel(int alignment) {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, alignment));
    return p;
  }
  
  private static JSeparator makeHorSeparator() {
    JSeparator s = new JSeparator(JSeparator.HORIZONTAL);
    s.setMinimumSize(new Dimension(0, s.getPreferredSize().height));
    return s;
  }
  
  private static void disableTableInteractivity(JTable table) {
    table.getTableHeader().setReorderingAllowed(false);
    table.setEnabled(false);
    /*
    table.setFocusable(false);
    table.setCellSelectionEnabled(false);
    table.setRowSelectionAllowed(false);
    table.setColumnSelectionAllowed(false);
    table.clearSelection();
    */
  }

  private static JButton makeToolbarBtn(ImageIcon icn) {
    JButton b = new JButton(icn);
    b.setMargin(new Insets(0,0,0,0));
    b.setFocusable(false);
    return b;
  }

  // ----- dialogs  ------------------------------------------------------------
  
  private void dispRuleNotFound(RuleTrigger t) {
    boolean atEnd = machine.inEndState();
    String msg;
    if (machine.getTable().isEmpty()) {
      msg = "Cannot run on empty rule table.\n";
    } else {
      msg = "Missing rule for\n";
      if (atEnd) {  msg += "end "; }
      msg += "state '" + t.state + "'\n";
      msg += "read '" + t.read + "'";
      if (atEnd) {
        msg += "\nYou may want to reset the machine";
        msg += "\nor enable automatic reseting.";
      }
    }
    dispWarning(msg, "Missing rule");
  }
  
  private void dispNothingToUndo() {
    String msg;
    if (history.isComplete()) {
      msg = "No steps were taken since the last reset.";
    } else {
      msg = "Steps were discarded and cannot be undone.";
    }
    dispWarning(msg, "Cannot undo");
  }
  
  private void dispWarning(String msg, String title) {
    JOptionPane.showMessageDialog(
        frame, msg, title, JOptionPane.WARNING_MESSAGE);
  }
  
  private void dispError(String msg, String title) {
    JOptionPane.showMessageDialog(
        frame, msg, title, JOptionPane.ERROR_MESSAGE);
  }
  
  // ----- functionality -------------------------------------------------------
  
  public void checkAutoReset() {
    if (autoReset && machine.inEndState()) {
      ignoreCurStateChanges = true;
      resetMachine();
      ignoreCurStateChanges = false;
    }    
  }
  
  public void resetMachine() {
    setAutoRun(false);
    machine.reset();
  }
  
  public void stepBackwards() {
    setAutoRun(false);
    if (stepBackwardsIntern()) {
      dispNothingToUndo();
    }
  }
  
  public void stepForwards() {
    setAutoRun(false);
    checkAutoReset();
    stepForwardsIntern();
  }
  
  private void stepForwardsAutoRun() {
    boolean stop = stepForwardsIntern();
    stop = stop || breakpointsOn && machine.inBreakState()
                || machine.inEndState();
    if (stop) { setAutoRun(false); }
  }

  private boolean stepBackwardsIntern() {
    if (history.isEmpty()) {
      return true;
    } else {
      ignoreCurStateChanges = true;
      ignoreHeadPosChanges = true;
      machine.undo();
      ignoreHeadPosChanges = false;
      ignoreCurStateChanges = false;
      return false;
    }
  }
  
  private boolean stepForwardsIntern() {
    try {
      ignoreCurStateChanges = true;
      ignoreHeadPosChanges = true;
      machine.step();
    } catch (RuleNotFoundException e) {
      dispRuleNotFound(e.getTrigger());
      return true;
    } finally {
      ignoreHeadPosChanges = false;
      ignoreCurStateChanges = false; 
    }
    return false;
  }
  
  public void setAutoRun(boolean run) {
    curStateComboBox.setEnabled(!run);
    headPosSpinner.setEnabled(!run);
    if (autoRunTimer.isRunning() == run) {
      if (run) {
        String msg = "Auto run thread has already started!"; 
        dispError(msg, "Already started");        
      }
    } else if (run) {
      btnRunStop.setIcon(IconLoader.Id.STOP.get());
      checkAutoReset();
      autoRunTimer.changeSpeed(speedSlider.getFValue());
      autoRunTimer.start();
    } else {
      btnRunStop.setIcon(IconLoader.Id.RUN.get());
      autoRunTimer.stop();
    }
  }
  
  public void setBreakpoints(boolean breakpointsOn) {
    this.breakpointsOn = breakpointsOn;
    ImageIcon icon = breakpointsOn ? IconLoader.Id.BREAKPOINTS_ON.get()
                                   : IconLoader.Id.BREAKPOINTS_OFF.get();
    btnBreakpoints.setIcon(icon);
  }

  
  private void processCurStateChange() {
    if (ignoreCurStateChanges ||
        machine.getState().equals(curStateComboBox.getSelectedItem())) {
      return; // Change was not from user, or not relevant
    }
    setAutoRun(false);
    history.clear();
    ignoreCurStateChanges = true;
    machine.setState(curStateComboBox.getSelectedItem().toString());
    ignoreCurStateChanges = false;      
  }

  private void processHeadPosChange() {
    if (ignoreHeadPosChanges) { return; }
    setAutoRun(false);
    history.clear();
    ignoreHeadPosChanges = true;
    tape.setPos(headPosModel.getNumber().intValue());
    ignoreHeadPosChanges = false;
  }
  
  //----- menu functionality ---------------------------------------------------
  
  public File openFileDialog() {
    File f = nextExistingDir(pref.getLastDir());
    JFileChooser fc = new JFileChooser(f);
    javax.swing.filechooser.FileFilter defaultFilter = fc.getFileFilter();
    fc.addChoosableFileFilter(
        new FileNameExtensionFilter("Turing Machines (*.tm)", "tm"));
    fc.setFileFilter(defaultFilter);
    int ret = fc.showOpenDialog(frame);
    if (ret == JFileChooser.APPROVE_OPTION) {
      pref.setLastDir(fc.getSelectedFile().getParent());
      return fc.getSelectedFile();
    } else {
      return null;
    }
  }
  
  public void openTMFile(File f) {
    setAutoRun(false);
    pref.setLastTMFile(f.getPath());
    if (testFileRead(f)) { return; }
    
    // Parse file and show ParserExceptions
    Parser p = new Parser();
    BufferedReader br = null;
    ParserException error = null;
    try {
      br = new BufferedReader(new FileReader(f));
      p.parse(br);
    } catch (IOException e) {
      dispError(e.getMessage(), "IO Exception");
      return;
    } catch (ParserException e)  {
      error = e;
    } finally { 
      try { br.close(); } catch (Exception e) { }
    }
    List<ParserException> warnings = p.getWarnings();
    if (error != null || !warnings.isEmpty()) {
      new ParserResultDialog(frame, f.getName(), error, warnings);
      if (error != null) { return; }
    }
    // parsing successful, set core variables ----------
    curParser = p;
    history.clear();
    tape.removeTapeListener(tapeListener); // unlink listener from old tape
    tape = curParser.createTape();
    tape.addTapeListener(tapeListener);
    tapeViewer.setTape(tape);
    machine.setStateListener(null); // unlink listener from old machine
    machine = new Machine(tape, p.getRuleTable(), p.getStartState(),
        p.getBreakStates(), p.getEndStates(), p.getWildcard(), history);
    machine.setStateListener(stateListener);    
    
    // update gui ---------
    updateValWildcard();
    ruleTableModel.setValues(p.getRules());
    ignoreCurStateChanges = true;
    ignoreHeadPosChanges = true;
    updateCurStateModel();
    machine.fireStateEvent(); 
    tape.fireTapeEvent(); // updates tapeViewer and headPosSpinner 
    ignoreHeadPosChanges = false;
    ignoreCurStateChanges = false;
  }

  private void updateCurStateModel() {
    Set<String> stateSet = curParser.createStateSet();
    String[] states = new String[stateSet.size()];
    int i = 0;
    for (String st : stateSet) {
      states[i++] = st;
    }
    Arrays.sort(states);
    curStateComboBox.setModel(new DefaultComboBoxModel<String>(states)); 
    curStateComboBox.setSelectedItem(machine.getState());
  }
  
  // Test file before reading, for detailed error message
  private boolean testFileRead(File f) {
    boolean fileMissing      = !f.exists();
    boolean permissionDenied = !f.canRead();
    boolean notAFile         = !f.isFile();
    if (fileMissing || permissionDenied || notAFile) {
      String title = null;
      String msg = "Cannot read '" + f.getPath() + "'.\n";
      if (fileMissing) {
        title = "File not found";
        msg += "File does not exist.";
      } else if (permissionDenied) {
        title = "Permission denied";
        msg += "Permission denied.";
      } else if (notAFile) {
        title = "Not a file";
        msg += "Isn't a (normal) file";
      }
      dispError(msg, title);
      return true;
    }
    return false;
  }
  
  private void updateValWildcard() {
    char wc = curParser.getWildcard();
    lblWildcard.setVisible(wc != 0);
    valWildcard.setVisible(wc != 0);
    valWildcard.setText("" + wc);
  }
  
  public void resetTape() {
    setAutoRun(false);
    history.clear();
    tape.removeTapeListener(tapeListener); // unlink listener from old tape 
    tape = curParser.createTape();
    tape.addTapeListener(tapeListener);
    tapeViewer.setTape(tape);
    machine.setTape(tape);
    
    // update GUI
    ignoreHeadPosChanges = true;
    tape.fireTapeEvent();
    ignoreHeadPosChanges = false;
  }
  
  public File saveFileDialog(String suggestedFileName, String extDetail, String ext) {
    File f = nextExistingDir(pref.getLastDir());
    JFileChooser fc = new SaveFileDialog(f, extDetail, ext);
    if (suggestedFileName != null) {
      fc.setSelectedFile(new File(f, suggestedFileName));
    }
    int ret = fc.showSaveDialog(frame);
    if (ret == JFileChooser.APPROVE_OPTION) {
      pref.setLastDir(fc.getSelectedFile().getParent());
      return fc.getSelectedFile();
    } else {
      return null;
    }
  }
  
  public void exportTape(File f) {
    // TODO let user choose export options
    TapeTXTExporter exp = new TapeTXTExporter(true, true);
    TapeSectionDialog.Section section =
        TapeSectionDialog.show(frame, tape.getLeftmost(), tape.getRightmost());
    if (section == null) { return; }
    if (testSaveFile(f)) { return; }
    BufferedWriter bw = null;
    try {
      bw = new BufferedWriter(new FileWriter(f));
      exp.export(bw, tape, section.start, section.length);
    } catch (IOException e) {
      dispError(e.getMessage(), "IO Exception");
    } finally {
      try { bw.close(); } catch (Exception e) { }
    }
  }
  
  public void exportRules(File f) {
    // TODO let user choose export options
    RulesGMLExporter exp = new RulesGMLExporter(true, true, " ", this);
    if (testSaveFile(f)) { return; }
    BufferedWriter bw = null;
    try {
      bw = new BufferedWriter(new FileWriter(f));
      exp.export(bw, curParser.getRules(), curParser.getStartState(),
          curParser.getBreakStates(), curParser.getEndStates());
    } catch (IOException e) {
      dispError(e.getMessage(), "IO Exception");
    } finally {
      try { bw.close(); } catch (Exception e) { }
    }
  }
  
  public void exportHist(File f) {
    // TODO let user choose export options
    HistTSVExporter exp = new HistTSVExporter(true, this);
    if (testSaveFile(f)) { return; }
    BufferedWriter bw = null;
    try {
      bw = new BufferedWriter(new FileWriter(f));
      exp.export(bw, history);
    } catch (IOException e) {
      dispError(e.getMessage(), "IO Exception");
    } finally {
      try { bw.close(); } catch (Exception e) { }
    }
  }
  
  public boolean testSaveFile(File f) {
    try {
      f.createNewFile();
    } catch (IOException e) {
      String title = "File not created";
      String msg = "Couldn't create file \"" + f.getName() + "\".\n"
                 + e.getMessage();
      dispError(msg, title);
      return true;
    }
    if (!f.canWrite()) {
      String title = "Permission denied";
      String msg = "Couldn't write to file \"" + f.getName() + "\".\n"
                 + "Permission denied";
      dispError(msg, title);
      return true;
    }
    return false;
  }
  
  public File nextExistingDir(String path) {
    File f = new File(path);
    while (f != null && (!f.exists() || f.isFile())) {
      f = f.getParentFile();
    }
    return f;
  }
  
  // ----- Interface MoveAliasConverter ----------------------------------------
  
  @Override
  public String convertToAlias(int move) {
    switch (move) {
      case -1: return aliasLeft;
      case  0: return aliasNone;
      case  1: return aliasRight;
      default: return Integer.toString(move);
    }
  }
 
  @Override
  public boolean isStartState(String s) {
    return curParser.getStartState().equals(s);
  }
  
  @Override
  public boolean isBreakState(String s) {
    return curParser.getBreakStates().contains(s);
  }
  
  @Override
  public boolean isEndState(String s) {
    return curParser.getEndStates().contains(s);
  }
  
  @Override
  public boolean isCurrentState(String s) {
    return machine.getState().equals(s);
  }

  // ----- Interface PrefConsumer ----------------------------------------------
  
  @Override
  public void consumePrefChange() {
    autoReset  = pref.getAutoReset();
    aliasLeft  = pref.getAliasLeft();
    aliasNone  = pref.getAliasNone();
    aliasRight = pref.getAliasRight();
    ruleTable.repaint();
    history.setMaxSize(pref.getHistSize());
    tapeViewer.setFrameLength(pref.getTVFrameLength());
    tapeViewer.setStripeSize(pref.getTVStripeSize());
  }
  
}
