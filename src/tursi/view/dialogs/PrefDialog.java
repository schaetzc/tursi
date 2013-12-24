package tursi.view.dialogs;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.prefs.*;

import tursi.view.Misc;
import tursi.view.icons.IconLoader;

// Preferences
public class PrefDialog {

  private static final String  PREF_AUTO_RESET      = "auto_reset";
  private static final String  PREF_HIST_SIZE       = "hist_size";
  private static final String  PREF_ALIAS_LEFT      = "alias_left";
  private static final String  PREF_ALIAS_NONE      = "alias_none";
  private static final String  PREF_ALIAS_RIGHT     = "alias_right";
  private static final String  PREF_TV_FRAME_LENGTH = "tv_frame_length";
  private static final String  PREF_TV_STRIPE_SIZE  = "tv_stripe_size";
  
  private static final boolean DEF_AUTO_RESET       = true;
  private static final int     DEF_HIST_SIZE        = 1000;
  private static final String  DEF_ALIAS_LEFT       = "L";
  private static final String  DEF_ALIAS_NONE       = "N";
  private static final String  DEF_ALIAS_RIGHT      = "R";
  private static final int     DEF_TV_FRAME_LENGTH  = 25;
  private static final int     DEF_TV_STRIPE_SIZE   = 10;
  
  private static final String  PREF_LAST_TM_FILE = "last_tm_file";
  private static final String  PREF_LAST_DIR = "last_dir";

  private static final String  DEF_LAST_TM_FILE = "no last file";
  private static final String  DEF_LAST_DIR = System.getProperty("user.home");
  
  // ---------------------------------------------------------------------------
  
  private final Preferences pref;
  
  private final int vGap   = 2;
  private final int hGap   = 10;
  private final int btnGap = 2;
  private final int secGap = 24;
  private final int border = 10;
  
  private final JDialog   dialog;
  private final JCheckBox autoReset;
  private final SpinnerNumberModel histSizeModel;
  private final JTextField aliasLeft;
  private final JTextField aliasNone;
  private final JTextField aliasRight;
  private final SpinnerNumberModel tvFrameLengthModel;
  private final SpinnerNumberModel tvStripeSizeModel;
  
  private final JButton btnDefs;

  private final JButton btnCancel;
  private final JButton btnOK;
  private final JButton btnApply;

  private final PrefConsumer consumer;
  
  public PrefDialog(PrefConsumer consumer) {
    this.consumer = consumer;
    pref = Preferences.userNodeForPackage(this.getClass());

    dialog = new JDialog((JFrame) null);    
    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent e) {
        hide(false);
      }
    });
    dialog.setIconImages(IconLoader.logoList());
    
    autoReset = new JCheckBox("Reset when starting from end state");
    histSizeModel = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1);
    aliasLeft  = new JTextField(DEF_ALIAS_LEFT);
    aliasNone  = new JTextField(DEF_ALIAS_NONE);
    aliasRight = new JTextField(DEF_ALIAS_RIGHT);
    tvFrameLengthModel = new SpinnerNumberModel(0, 0, 1000, 1);
    tvStripeSizeModel  = new SpinnerNumberModel(0, -10000, 10000, 1);
    loadPrefs();
    
    btnDefs   = new JButton("Restore Defaults");
    btnCancel = new JButton("Cancel");
    btnOK     = new JButton("OK");
    btnApply  = new JButton("Apply");
    
    final PrefDialog thisDialog = this;
    btnDefs.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        loadDefs();
      }
    });
    btnCancel.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        hide(false);
      }
    });
    btnOK.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        dialog.setVisible(false);
        storePrefs();
        thisDialog.consumer.consumePrefChange();
      }
    });
    btnApply.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        storePrefs();
        thisDialog.consumer.consumePrefChange();
      }
    });

    assemble();
  }
  
  private void assemble() {
    JPanel p = new JPanel();
    p.setBorder(BorderFactory.createEmptyBorder(border, border, border, border));
    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
    
    JLabel titleMachine = new JLabel("Machine");
    JLabel titleHist    = new JLabel("History");
    JLabel titleAlias   = new JLabel("Aliases for field 'move'");
    JLabel titleTape    = new JLabel("Tape visualization");
    Font titleFont      =
        titleHist.getFont().deriveFont(titleHist.getFont().getSize()*1.15f);
    titleMachine.setFont(titleFont);
    titleHist.setFont(titleFont);
    titleHist.setFont(titleFont);
    titleAlias.setFont(titleFont);
    titleTape.setFont(titleFont);
    
    // ---------- Machine Section ----------------------------------------------
    p.add(titleMachine);
    p.add(Box.createVerticalStrut(vGap));
    p.add(autoReset);
    setAlignAndMaxHeight(titleMachine);
    setAlignAndMaxHeight(autoReset);
    
    p.add(Box.createVerticalStrut(secGap));
    // ---------- History Section ----------------------------------------------
    JPanel histPanel = Misc.makeBoxPanel(BoxLayout.X_AXIS);
    histPanel.add(new JLabel("Max. size"));
    histPanel.add(Box.createHorizontalStrut(hGap));
    JSpinner histSize = new JSpinner(histSizeModel);
    Misc.setPlainNumEditor(histSize);
    histPanel.add(histSize);
    p.add(titleHist);
    p.add(Box.createVerticalStrut(vGap));
    p.add(histPanel);
    setAlignAndMaxHeight(titleHist);
    setAlignAndMaxHeight(histPanel);
    
    p.add(Box.createVerticalStrut(secGap));
    // ---------- Alias Section ------------------------------------------------
    JPanel aliasPanel = new JPanel(new GridLayout(2, 3, hGap, vGap));
    aliasPanel.add(new JLabel("Left (-1)"));
    aliasPanel.add(new JLabel("None (0)"));
    aliasPanel.add(new JLabel("Right (1)"));
    aliasPanel.add(aliasLeft);
    aliasPanel.add(aliasNone);
    aliasPanel.add(aliasRight);
    p.add(titleAlias);
    p.add(Box.createVerticalStrut(vGap));
    p.add(aliasPanel);
    setAlignAndMaxHeight(titleAlias);
    setAlignAndMaxHeight(aliasPanel);
    
    p.add(Box.createVerticalStrut(secGap));
    // ---------- Tape Section -------------------------------------------------
    JPanel tapePanel = new JPanel(new GridLayout(2, 2, hGap, vGap));
    JLabel lblFrameLength = new JLabel("Frame length");
    tapePanel.add(lblFrameLength);
    tapePanel.add(new JLabel("Stripe size"));
    JSpinner tvFrameLength = new JSpinner(tvFrameLengthModel);
    JSpinner tvStripeSize  = new JSpinner(tvStripeSizeModel);
    Misc.setPlainNumEditor(tvFrameLength);
    Misc.setPlainNumEditor(tvStripeSize);
    tapePanel.add(tvFrameLength);
    tapePanel.add(tvStripeSize);
    p.add(titleTape);
    p.add(Box.createVerticalStrut(vGap));
    p.add(tapePanel);
    setAlignAndMaxHeight(titleTape);
    setAlignAndMaxHeight(tapePanel);

    String ttFrameLength = "Time in ms between two repaints (max. 1000 ms)";
    tvFrameLength.setToolTipText(ttFrameLength);
    lblFrameLength.setToolTipText(ttFrameLength);
    
    p.add(Box.createVerticalStrut(secGap));
    p.add(Box.createVerticalGlue());
    // ---------- Button Section -----------------------------------------------
    Misc.setMaxToPrefSize(btnDefs);
    btnDefs.setAlignmentX(0);
    p.add(btnDefs);
    p.add(Box.createVerticalStrut(vGap));
    JPanel buttonPanel = Misc.makeBoxPanel(BoxLayout.X_AXIS);
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.add(btnCancel);
    buttonPanel.add(Box.createHorizontalStrut(btnGap));
    buttonPanel.add(btnOK);
    buttonPanel.add(Box.createHorizontalStrut(btnGap));
    buttonPanel.add(btnApply);
    p.add(buttonPanel);
    setAlignAndMaxHeight(buttonPanel);
    
    dialog.getContentPane().add(p);
    dialog.pack();
    dialog.setMinimumSize(dialog.getSize());
    dialog.setLocationRelativeTo(null);
  }

  private void setAlignAndMaxHeight(JComponent c) {


    c.setAlignmentX(0);
    Misc.setMaxToPrefHeight(c);
  }
  
  private void loadPrefs() {
    autoReset.setSelected(getAutoReset());
    histSizeModel.setValue(getHistSize());
    aliasLeft.setText(getAliasLeft());
    aliasNone.setText(getAliasNone());
    aliasRight.setText(getAliasRight());
    tvFrameLengthModel.setValue(getTVFrameLength());
    tvStripeSizeModel.setValue(getTVStripeSize());
  }
  
  private void loadDefs() {
    autoReset.setSelected(DEF_AUTO_RESET);
    histSizeModel.setValue(DEF_HIST_SIZE);
    aliasLeft.setText(DEF_ALIAS_LEFT);
    aliasNone.setText(DEF_ALIAS_NONE);
    aliasRight.setText(DEF_ALIAS_RIGHT);
    tvFrameLengthModel.setValue(DEF_TV_FRAME_LENGTH);
    tvStripeSizeModel.setValue(DEF_TV_STRIPE_SIZE);
  }
  
  private void storePrefs() {
    pref.putBoolean(PREF_AUTO_RESET, autoReset.isSelected());
    pref.putInt(PREF_HIST_SIZE, histSizeModel.getNumber().intValue());
    pref.put(PREF_ALIAS_LEFT, aliasLeft.getText());
    pref.put(PREF_ALIAS_NONE, aliasNone.getText());
    pref.put(PREF_ALIAS_RIGHT, aliasRight.getText());
    pref.putInt(PREF_TV_FRAME_LENGTH, tvFrameLengthModel.getNumber().intValue());
    pref.putInt(PREF_TV_STRIPE_SIZE, tvStripeSizeModel.getNumber().intValue());
  }
  
  // ---------------------------------------------------------------------------
  
  public boolean isVisible() {
    return dialog.isVisible();
  }
  
  public void show() {
    dialog.setVisible(true);
    dialog.toFront();
  }
  
  public void hide(boolean apply) {
    dialog.setVisible(false);
    if (apply) {
      storePrefs();
    } else {
      loadPrefs();
    }
  }

  public void exit() {
    dialog.setVisible(false);
    try {
      pref.flush();      
    } catch (BackingStoreException e) {
      String msg = "Couldn't save one ore more preferences.\n" + e.getMessage();
      JOptionPane.showMessageDialog(dialog.getOwner(),
          msg, "Preferences not saved", JOptionPane.ERROR_MESSAGE);
    }
    dialog.dispose();
  }
  
  // ---------------------------------------------------------------------------
  
  public boolean getAutoReset() {
    return pref.getBoolean(PREF_AUTO_RESET, DEF_AUTO_RESET);
  }

  public int getHistSize() {
    return pref.getInt(PREF_HIST_SIZE, DEF_HIST_SIZE);
  }
  
  public String getAliasLeft() {
    return pref.get(PREF_ALIAS_LEFT, DEF_ALIAS_LEFT);
  }

  public String getAliasNone() {
    return pref.get(PREF_ALIAS_NONE, DEF_ALIAS_NONE);
  }
  
  public String getAliasRight() {
    return pref.get(PREF_ALIAS_RIGHT, DEF_ALIAS_RIGHT);
  }
 
  public int getTVFrameLength() {
    return pref.getInt(PREF_TV_FRAME_LENGTH, DEF_TV_FRAME_LENGTH);
  }
  
  public int getTVStripeSize() {
    return pref.getInt(PREF_TV_STRIPE_SIZE, DEF_TV_STRIPE_SIZE);
  } 
 
  // -----------------
  
  public String getLastTMFile() {
    return pref.get(PREF_LAST_TM_FILE, DEF_LAST_TM_FILE);
  }
  
  public String getLastDir() {
    return pref.get(PREF_LAST_DIR, DEF_LAST_DIR);
  }

  public void setLastTMFile(String path) {
    pref.put(PREF_LAST_TM_FILE, path);
  }
  
  public void setLastDir(String path) {
    pref.put(PREF_LAST_DIR, path);
  }
  
}
