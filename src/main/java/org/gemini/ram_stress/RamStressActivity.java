package org.gemini.ram_stress;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.TextView;
import java.util.Date;

public class RamStressActivity extends Activity {
  private final static int POINTER_WIDTH = getPointerSize();
  private final static int POINTER_SIZE = POINTER_WIDTH / 8;
  private Thread thread = null;

  private static int getPointerSize() {
    String arch = System.getProperty("os.arch");
    if (arch == null) {
      return 32;
    }
    return arch.contains("64") ? 64 : 32;
  }

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.main);
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (thread != null) {
      Thread c = thread;
      thread = null;
      while (c.getState() == Thread.State.RUNNABLE) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException ex) {}
      }
    } else {
      final RamStressActivity me = this;
      thread = new Thread() {
        @Override
        public void run() {
          me.clear();
          me.writeln("Start at ", new Date());
          me.writeln("You may press here again to stop.");
          me.writeln("Detected pointer size is ", POINTER_SIZE);
          final int SIZE = 256 * 1024;
          final int SLICE_SIZE = 64 * 1024;
          final int M_SIZE = SIZE * POINTER_SIZE;
          byte[][] m;
          try {
            m = new byte[SIZE][];
          } catch (OutOfMemoryError err) {
            me.writeln("Failed to allocate m");
            return;
          }
          me.writeAllocInfo(M_SIZE);

          int i = 0;
          for (; i < SIZE && me.thread != null; i++) {
            try {
              m[i] = new byte[SLICE_SIZE];
              if (i % 1000 == 999) {
                me.writeAllocInfo((long)(i + 1) * SLICE_SIZE + M_SIZE);
              }
            } catch (OutOfMemoryError err) {
              m = null;
              break;
            }
          }
          final long size = (long)(i) * SLICE_SIZE + M_SIZE;
          me.writeln("Maximum memory allocatable is ",
                     size,
                     " in bytes, ~",
                     size / 1024 / 1024,
                     "MB");
          me.writeln("Finish at ", (new Date()).toString());
          me.thread = null;
        }
      };
      thread.start();
    }
    return true;
  }

  private void write(final Object... args) {
    if (args != null && args.length > 0) {
      if (Looper.myLooper() != Looper.getMainLooper()) {
        final RamStressActivity me = this;
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
          @Override
          public void run() {
            me.write(args);
          }
        });
        return;
      }

      StringBuilder builder = new StringBuilder();
      for (Object arg : args) {
        builder.append(arg);
      }

      TextView view = (TextView) findViewById(R.id.text);
      view.append(builder.toString());
    }
  }

  private void clear() {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      final RamStressActivity me = this;
      (new Handler(Looper.getMainLooper())).post(new Runnable() {
        @Override
        public void run() {
          me.clear();
        }
      });
      return;
    }
    TextView view = (TextView) findViewById(R.id.text);
    view.setText("");
  }

  private void writeln(final Object... args) {
    Object[] newArgs = new Object[args == null ? 1 : args.length + 1];
    System.arraycopy(args, 0, newArgs, 0, newArgs.length - 1);
    newArgs[newArgs.length - 1] = "\n";
    write(newArgs);
  }

  private void writeAllocInfo(final long size) {
    writeln("Allocated ", size, " bytes memory, ~", size / 1024 / 1024, "MB");
  }
}
