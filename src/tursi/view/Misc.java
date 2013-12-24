package tursi.view;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public abstract class Misc {

  public static Color opaque(Color c) {
    return new Color(c.getRGB(), false);
  }
  
  public static void setMaxHeight(Component c, int height) {
    c.setMaximumSize(new Dimension(c.getMaximumSize().width, height));
  }
  
  public static void setPrefWidth(Component c, double width) {
    c.setPreferredSize(new Dimension((int) width, c.getPreferredSize().height));
  }

  public static void setMinWidth(Component c, double width) {
    c.setMinimumSize(new Dimension((int) width, c.getMinimumSize().height));
  }
  
  public static void setMaxToPrefSize(Component c) {
    c.setMaximumSize(c.getPreferredSize());
  }
  
  public static void setMaxToPrefWidth(Component c) {
    c.setMaximumSize(new Dimension(c.getPreferredSize().width,
                                   c.getMaximumSize().height));
  }
  
  public static void setMaxToPrefHeight(Component c) {
    c.setMaximumSize(new Dimension(c.getMaximumSize().width,
                                   c.getPreferredSize().height));
  }
  
  public static void setMinToPrefWidth(Component c) {
    c.setMinimumSize(new Dimension(c.getPreferredSize().width,
                                   c.getMinimumSize().height));
  }
  
  public static void setMinToPrefHeight(Component c) {
    c.setMinimumSize(new Dimension(c.getMinimumSize().width,
                                   c.getPreferredSize().height));
  }
  
  public static JPanel makeBoxPanel(int alignment) {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, alignment));
    return p;
  }
  
  public static void setPlainNumEditor(JSpinner spinner) {
    spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
  }
  
  public static void adjustColumnWidth(JTable table) {
    for (int col = 0; col < table.getColumnCount(); ++col) {
      adjustColumnWidth(table, col);
    }
  }
  
  public static void adjustColumnWidth(JTable table, int column) {
    TableColumn tabCol = table.getColumnModel().getColumn(column);
    TableCellRenderer renderer = tabCol.getHeaderRenderer();
    if (renderer == null) {
        renderer = table.getTableHeader().getDefaultRenderer();
    }
    Component comp = renderer.getTableCellRendererComponent(
        table, tabCol.getHeaderValue(), false, false, 0, 0);
    int width = comp.getPreferredSize().width;
    for (int row = 0; row < table.getRowCount(); row++) {
        renderer = table.getCellRenderer(row, column);
        comp = renderer.getTableCellRendererComponent(
            table, table.getValueAt(row, column), false, false, row, column);
        width = Math.max(width, comp.getPreferredSize().width);
    }
    width += 4; // columns should be a little wider than the minimum
    tabCol.setPreferredWidth(width);
    tabCol.setWidth(width);
  }
  
}
