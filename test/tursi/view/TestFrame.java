package tursi.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import tursi.view.FSlider;
import tursi.view.dialogs.*;

public class TestFrame extends JFrame {
  
  private static final long serialVersionUID = 1L;
  
  public TestFrame() {
    super("Tursi");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //TODO create closing method, which stores current window proportions 
    
    System.out.println(TapeSectionDialog.show(this, -10, 3));
    System.out.println(TapeSectionDialog.show(this, 0, 0));
    System.out.println(TapeSectionDialog.show(this, -8, 0));
    System.exit(0);
    
    final FSlider slide = new FSlider(1000, 500);
    
    final JLabel lbl = new JLabel("-");
    
    final JButton b = new JButton("click");
    b.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
      }
    });
    
    slide.addChangeListener(new ChangeListener() {
      @Override public void stateChanged(ChangeEvent e) {
        lbl.setText("v=" + slide.getValue());// + "   f=" + slide.getFValue());
      }
    });
    
    Container cp = this.getContentPane();
    cp.setLayout(new BorderLayout());
    cp.add(slide, BorderLayout.NORTH);
    cp.add(lbl, BorderLayout.CENTER);
    cp.add(b, BorderLayout.SOUTH);

  
    this.pack();
    this.setLocationRelativeTo(null); // center window on the screen
    this.setVisible(true);
  }
  
  // TODO move main-method to another class (for console mode handling)
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
      UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
      UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch(Exception e) {
      // stick with the default look and feel
    }
    
    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { new TestFrame(); }
    });
  }
  
}
