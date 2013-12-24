package tursi.view.icons;

import java.net.URL;

import java.awt.Image;
import javax.swing.ImageIcon;
import java.util.ArrayList;

public class IconLoader {
  
  public static enum Id {
    OPEN                 ("open.png"),
    RELOAD               ("reload.png"),
    WRITE_TOWARDS        ("write_towards.png"),
    WRITE_FROM           ("write_from.png"),
    CLEAR_HISTORY        ("clear_history.png"),
    RESET_TAPE           ("reset_tape.png"),
    RESET_MACHINE        ("reset_machine.png"),
    RUN                  ("run.png"),
    STOP                 ("stop.png"),
    BREAKPOINTS_ON       ("breakpoints_on.png"),
    BREAKPOINTS_OFF      ("breakpoints_off.png"),
    SCROLL_NONE_OFF      ("scroll_none_off.png"),
    SCROLL_BORDERS_OFF   ("scroll_borders_off.png"),
    SCROLL_IMMEDIATE_OFF ("scroll_immediate_off.png"),
    SCROLL_NONE_ON       ("scroll_none_on.png"),
    SCROLL_BORDERS_ON    ("scroll_borders_on.png"),
    SCROLL_IMMEDIATE_ON  ("scroll_immediate_on.png"),
    STEP_BACKWARDS       ("step_backwards.png"),
    STEP_FORWARDS        ("step_forwards.png"),
    MARK_START_ON        ("mark_start_on.png"),
    MARK_BREAK_ON        ("mark_break_on.png"),
    MARK_END_ON          ("mark_end_on.png"),
    MARK_START_OFF       ("mark_start_off.png"),
    MARK_BREAK_OFF       ("mark_break_off.png"),
    MARK_END_OFF         ("mark_end_off.png"),

    LOGO_128 ("logo_128.png"),
    LOGO_64  ("logo_64.png"),
    LOGO_48  ("logo_48.png"),
    LOGO_32  ("logo_32.png"),
    LOGO_24  ("logo_24.png"),
    LOGO_16  ("logo_16.png");
    
    public final String path;
    
    private Id(String path) {
      this.path = path;
    }
    
    public ImageIcon get() {
      return IconLoader.get(this);
    }
  }
  
  // ---------------------------------------------------------------------------
  
  private static void ensureInstance() {
    if (instance == null) {
      instance = new IconLoader();
    }
  }

  private static IconLoader instance;
  
  private ImageIcon[] icons;
  private boolean loadingSuccessful;
  
  private IconLoader() {
    Id[] ids = Id.values();
    icons = new ImageIcon[ids.length];
    loadingSuccessful = true;
    Class<?> c = getClass();
    for (int i=0; i<ids.length; ++i) {
      URL url = c.getResource(ids[i].path);
      if (url == null) {
        loadingSuccessful = false;
      } else {
        icons[i] = new ImageIcon(url, ids[i].toString());        
      }
      if (icons[i] == null) {
        loadingSuccessful = false;
      }
    }
  }
  
  
  public static boolean loadingSuccessful() {
    ensureInstance();
    return instance.loadingSuccessful;
  }
  
  public static ImageIcon get(Id id) {
    ensureInstance();
    return instance.icons[id.ordinal()];
  }
  
  public static ArrayList<Image> logoList() {
    ensureInstance();
    ArrayList<Image> logoList = new ArrayList<Image>();
    int end = Id.LOGO_16.ordinal(); //inclusive
    for (int i = Id.LOGO_128.ordinal(); i <= end; ++i) {
      if (instance.icons[i] != null) {
        logoList.add(instance.icons[i].getImage());
      }        
    }
    return logoList;
  }

}

