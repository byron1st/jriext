package byron1st.jriext.run;

import byron1st.jriext.JRiExt;
import byron1st.jriext.util.JRiExtException;

import java.util.ArrayList;

/**
 * Created by byron1st on 2016. 5. 8..
 */
public class ProcessDeathDetector extends Thread {
    public static class SubProcessRunException extends JRiExtException {
        SubProcessRunException(String message) {
            super(message);
        }
        SubProcessRunException(String message, Exception e) {
            super(message, e);
        }
    }

    private Process process;
    private ArrayList<ProcessListener> listeners = new ArrayList<>();

    public ProcessDeathDetector(Process process) throws SubProcessRunException {
        if (process.isAlive()) this.process = process;
        else throw new SubProcessRunException("A process is unexpectedly terminated beforehand.");
    }

    @Override
    public void run() {
        try {
            process.waitFor();
            for (ProcessListener listener : listeners) {
                listener.listenProcessDeath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addListener(ProcessListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ProcessListener listener) {
        listeners.remove(listener);
    }
}
