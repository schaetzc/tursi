package tursi.view.events;

import javax.swing.SwingUtilities;
import java.util.concurrent.Semaphore;

//         I M P O R T A N T  ! ! !
// TODO point out, that stop must also be called from the thread EDT  
//         I M P O R T A N T  ! ! ! 
/**
 * This timer calls a task in given intervals.
 * It allows very short intervals. If the given task needs more time to
 * execute, than the intervals are long, the timer adapts to this situation
 * instead of flooding the thread queue with more and more calls.
 * It's guaranteed, that the next task is only scheduled, if the last has
 * terminated.
 * 
 * <b>Very important!</b>
 * Stop must also be called from the thread EDT
 * (used by SwingUtilities.invokeLater()).
 */
public class AdaptiveTimer {

  private volatile int       interruptInfo = -1;
  private final    Semaphore mutex         = new Semaphore(1);
  private          Thread    timerThread   = null;
  private final    Runnable  taskWrapper;
  
  private int defaultPauseTime;
  
  public AdaptiveTimer(final Runnable task) {
    if (task == null) {
      throw new IllegalArgumentException("Timer task was null.");
    }
    taskWrapper = new Runnable() {
      @Override
      public void run() {
        try {
          if (isRunning()) { task.run(); }
        } finally {
          mutex.release();
        }
      }
    };
  }
  
  //calculated times with currentTimeMillis may be not accurate, especially
  //on 1 core cpu (calculation not finished, cpu changes thread).
  //However, it should be close enough.
  private Thread makeTimerThread() {
    final Object monitor = this;
    final int defaultPauseTime = this.defaultPauseTime; //volatile spared
    return new Thread("timer thread") {
      @Override
      public void run() {
        long lastStep  = 0; // start immediately
        long pauseTime = defaultPauseTime;
        boolean run = true; 
        while (run) {
          long curPauseTime = lastStep + pauseTime - System.currentTimeMillis();
          try {
            if (curPauseTime > 0) { sleep(curPauseTime); }
            mutex.acquire();        
          } catch(InterruptedException e) {
            synchronized (monitor) {
              // clear flag (maybe, it was set after try and before synchronize) 
              interrupted();
              if (interruptInfo < 0) {
                run = false;
              } else {
                pauseTime = interruptInfo;
                interruptInfo = -1;
              }
            }
            continue;
          }
          lastStep = System.currentTimeMillis();
          SwingUtilities.invokeLater(taskWrapper);
        }
      }
    };
  }
  
  public void start() {
    if (isRunning()) {
      throw new IllegalStateException("Timer is already running.");
    }
    timerThread = makeTimerThread();
    timerThread.start();
  }
  
  public void stop() {
    if (isRunning()) {
      synchronized (this) {
        interruptInfo = -1;
        timerThread.interrupt();
      }
      timerThread = null;
    }
  }

  public void changeSpeed(int newPauseTime) {
    if (newPauseTime < 0) {
      throw new IllegalArgumentException("Negativ pause time.");
    }
    defaultPauseTime = newPauseTime;
    if (isRunning()) {
      synchronized (this) {
        interruptInfo = newPauseTime;
        timerThread.interrupt();
      }
    }
  }
  
  public boolean isRunning() {
    return timerThread != null;
  }

}
