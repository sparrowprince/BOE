package bm.log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * The purpose of this class is to provide other objects with a global and
 * static way to log messages to a predefined logging file (or maybe a set of
 * logging files).
 * 
 * @author tobi
 * 
 */
public class Logger {

    /* Filename of the logging file. */
    private static final String FN = "bm.log";

    /* Some error messages. */
    private static final String ERROR = "ERROR";
    private static final String DEACTIVATE = "Deactivating Logger.";
    private static final String INITERR = "Unable to open " + FN
            + " for logging. " + DEACTIVATE;
    private static final String WRERR = "Unable to write to " + FN + ". "
            + DEACTIVATE;
    private static final String CLERR = "Unable to properly close connection to "
            + FN + ". " + DEACTIVATE;

    // separating symbol
    private static final String SEPERATOR = ": ";

    private static final Logger STATICINSTANCE = new Logger();

    private BufferedWriter bfWrt;
    private boolean verbose = false;
    private boolean available;

    /*
     * Creates a new object of the type Logger. There are no params as Logger
     * always uses the same file for logging. It is highly recommend to use the
     * static methods provided by this class.
     */
    private Logger() {
        try {
            bfWrt = new BufferedWriter(new FileWriter(FN));
            available = true;
        } catch (IOException e) {
            System.err.println(INITERR);
            e.printStackTrace();
            available = false;
        }
    }

    /**
     * Static method which is used to log messages to the file specified by FN.
     * The current time, the message msg and a newline symbol will be appended
     * to the log.
     * 
     * An I/O error will deactivate the Logger.
     * 
     * @param msg The message to be logged.
     */
    public static void writeln(String msg) {
        if (!STATICINSTANCE.available)
            return;
        try {
            STATICINSTANCE.bfWrt.write(new Date().toString() + SEPERATOR);
            STATICINSTANCE.bfWrt.write(msg + '\n');
            STATICINSTANCE.bfWrt.flush();
        } catch (IOException e) {
            System.err.println(WRERR);
            e.printStackTrace();
            STATICINSTANCE.available = false;
        }
    }

    /**
     * Static method which sets the Logger to be verbose. This does not in any
     * way affect the Logger internal behavior. It is rather meant as a globally
     * accessible verbose flag.
     * 
     * @param verbose New value for the verbose flag of the static Logger
     * instance.
     */
    public static void setVerbose(boolean verbose) {
        STATICINSTANCE.verbose = verbose;
    }

    /**
     * Returns true is the static Logger instance is set to be verbose. This
     * does not in any way affect the Logger internal behavior. It is rather
     * meant as a globally accessible verbose flag.
     * 
     * @return true if the static Logger instance is set to be verbose.
     */
    public static boolean verbose() {
        return STATICINSTANCE.verbose && STATICINSTANCE.available;
    }

    /**
     * Closes the OutputStream (rather BufferedWriter) used by the static Logger
     * instance. It is recommended to invoke this method when the program is
     * properly terminated.
     * 
     * An I/O error will deactivate the Logger.
     */
    public static void close() {
        try {
            STATICINSTANCE.bfWrt.close();
        } catch (IOException e) {
            System.err.println(CLERR);
            e.printStackTrace();
            STATICINSTANCE.available = false;
        }
    }

    /**
     * Static method which is used to log error messages to the file specified
     * by FN. The current time, an ERROR prefix, the message msg and a newline
     * symbol will be appended to the log.
     * 
     * An I/O error will deactivate the Logger.
     * 
     * @param msg The message to be logged.
     */
    public static void writeerrln(String msg) {
        writeln(ERROR + SEPERATOR + msg);
    }

} // end of class Logger
