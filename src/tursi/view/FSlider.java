package tursi.view;

import javax.swing.*;

/** Slider with a quadratic scale.
 *  The F stands for a function, which describes the scale.
 */
public class FSlider extends JSlider {
  
  private static final long serialVersionUID = 1L;
  
  private double fMax;
  
  public FSlider(int max, int value) {
    super(0, max);
    fMax = f(getMaximum());
    setFValue(value);
  }
  
  public int getFValue() {;
    return toF(super.getValue());
  }
  
  public void setFValue(int value) {
    super.setValue(fromF(value));
  }
  
  private int toF(double x) {
    return (int) Math.round(f(x) / fMax * getMaximum());
  }
  
  private int fromF(double y) {
    return (int) Math.round(fInv(y / getMaximum() * fMax));
  }
  
  private double f(double x) {
    return x * x;
  } 

  private double fInv(double y) {
    return Math.sqrt(y);
  }
  
  @Override
  public void setMaximum(int max) {
    if (max <= 0 || max <= getMinimum()) {
      throw new IllegalArgumentException("FSlider max " + max);
    }
    fMax = f(max);
    super.setMaximum(max);
  }
  
  @Override
  public void setMinimum(int min) {
    if (min < 0 || min >= getMaximum()) {
      throw new IllegalArgumentException("FSlider min " + min);
    }
    super.setMinimum(min);
  }
  
  @Override
  public void setModel(BoundedRangeModel model) {
    int min = model.getMinimum();
    int max = model.getMaximum();
    if (min != 0 || max <= min) {
      throw new IllegalArgumentException("FSlider min " + min + " max " + max);
    }
    fMax = f(max);
    super.setModel(model);
  }
}
