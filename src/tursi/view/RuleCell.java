package tursi.view;

import java.awt.*;
import javax.swing.*;

import tursi.view.icons.IconLoader;

public class RuleCell extends JPanel {

  private static final long serialVersionUID = 1L;
  
  public static final int START = 0;
  public static final int BREAK = 1;
  public static final int END   = 2;
  
  private final JLabel[]    iconLabels;
  private final ImageIcon[] iconsOn;
  private final ImageIcon[] iconsOff;
  private final JLabel      text;
  
  public RuleCell() {
    final int length = 3;
    iconsOn         = new ImageIcon[length];
    iconsOn[START]  = IconLoader.Id.MARK_START_ON.get();
    iconsOn[BREAK]  = IconLoader.Id.MARK_BREAK_ON.get();
    iconsOn[END]    = IconLoader.Id.MARK_END_ON.get();
    iconsOff        = new ImageIcon[length];
    iconsOff[START] = IconLoader.Id.MARK_START_OFF.get();
    iconsOff[BREAK] = IconLoader.Id.MARK_BREAK_OFF.get();
    iconsOff[END]   = IconLoader.Id.MARK_END_OFF.get();
    
    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    iconLabels = new JLabel[length];
    for (int i = 0; i < length; ++i) {
      iconLabels[i] = new JLabel();
      iconLabels[i].setAlignmentY(0.5f);
      this.add(iconLabels[i]);
    }
    this.add(Box.createHorizontalStrut(2));
    text = new JLabel();
    this.add(text);
    this.setOpaque(true);
  }
  
  public void setText(String t) {
    text.setText(t);
  }

  @Override
  public void setForeground(Color c) {
    if (text != null) {
      text.setForeground(c);      
    }
  }

  @Override
  public Color getForeground() {
    return text == null ? null : text.getForeground();
  }
  
  @Override
  public void setFont(Font f) {
    if (text != null) {
      text.setFont(f);
    }
  }
  
  @Override
  public Font getFont() {
    return text == null ? null : text.getFont();
  }
  
  
  public void hideIcon(int icon) {
    iconLabels[icon].setIcon(null);
  }
  
  public void showIcon(int icon, boolean on) {
    iconLabels[icon].setIcon(on ? iconsOn[icon] : iconsOff[icon]);
  }

  // Faster implementations for validate, invalidate, ... would be desirable.
  // See original implementation of DefaultTableCellRenderer or
  // DefaultListCellRenderer.
}
