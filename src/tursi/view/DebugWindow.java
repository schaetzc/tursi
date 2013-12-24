package tursi.view;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class DebugWindow {

  //TODO not thread safe  
  public static DebugWindow dw = new DebugWindow();
  
  private JTextArea textArea;
  
  private DebugWindow() {
    JFrame frame = new JFrame();
    //TODO this exits the complete programm! Change to better behavior
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    textArea = new JTextArea();
    textArea.setEditable(false);
    frame.setLayout(new BorderLayout());
    frame.add(textArea, BorderLayout.CENTER);
    JButton btn = new JButton("clear");
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        textArea.setText("");
      }
    });
    frame.add(btn, BorderLayout.PAGE_END);
    frame.pack();
    frame.setSize(400, 600);
    frame.setVisible(true);    
  }
  
  public void println(String s) {
    textArea.setText(s + "\n" + textArea.getText());
  }
}
