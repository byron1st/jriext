package byron1st.jriext.util;

/**
 * Created by byron1st on 2016. 5. 23..
 */
public class JRiExtException extends Exception {
    private Exception originalException;
    public JRiExtException(String message) { super(message); }
    public JRiExtException(String message, Exception e) { super(message); this.originalException = e;}

    public Exception getOriginalException() {
        return originalException;
    }
    public void printStackTraceAndQuit() {
        if (originalException != null) originalException.printStackTrace();
        else System.out.println("An original exception is null.");
    }
}
