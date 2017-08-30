package simulation.util;

import java.util.Locale;
import java.util.logging.*;

import static java.util.logging.Level.*;

/**
 * Class that provides a static accessible logger
 */
public final class Log {

    /** Don't let anyone instantiate this class */
    private Log() {}

    // Make sure LoggingModule constructor is called exists
    static {
        new LoggingModule();
    }

    /**
     * Log a message
     * @param logLevel Level, i.e., severeness, of the message
     * @param msg Content of the message
     */
    public static void log(Level logLevel, String msg) {
        if (isLogEnabled())
            LoggingModule.logger.log(logLevel, msg);
    }

    /**
     * Internally used logger
     */
    private static class LoggingModule {

        /** Instance of used logger */
        private static Logger logger = null;

        /** True iff logging is enabled by user */
        private static boolean logEnabled = true;

        /** True iff the log messages should be written to a file */
        private static boolean writeToDiskEnabled = false;

        /** File handler for storing the log messages to disk */
        private static FileHandler fileHandler;


        /**
         * Constructor for a logging module according to the configurations for
         * writing to disk and enabling logs
         */
        protected LoggingModule() {
            if (logger == null && logEnabled) {

                // Change locale to always get English messages
                Locale.setDefault(Locale.US);

                // Create logger
                logger = Logger.getLogger("Simulation");
                logger.setLevel(FINEST);
                logger.setUseParentHandlers(false);

                // Setup formatter directly in code, avoid ugly hacks with project settings or Maven
                System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %4$s: %5$s%n");

                // Setup console logger
                Handler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(INFO);
                consoleHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(consoleHandler);

                if (writeToDiskEnabled) {
                    // Setup file logger
                    try {
                        fileHandler = new FileHandler("simulation.log", false);
                        fileHandler.setLevel(INFO);
                        fileHandler.setFormatter(new SimpleFormatter());
                        logger.addHandler(fileHandler);
                    } catch (Exception e) {
                        logger.log(SEVERE, e.toString());
                    }
                }
            }
        }

    }

    /**
     * Log a message of level "severe"
     * @param msg Content of the message
     */
    public static void severe(String msg) {
        log(SEVERE, msg);
    }

    /**
     * Log a message of level "warning"
     * @param msg Content of the message
     */
    public static void warning(String msg) { log(WARNING, msg); }

    /**
     * Log a message of level "info"
     * @param msg Content of the message
     */
    public static void info(String msg) { log(INFO, msg); }

    /**
     * Log a message of level "finest"
     * @param msg Content of the message
     */
    public static void finest(String msg) { log(FINEST, msg); }

    /**
     * Returns whether or not the logging is enabled
     * @return True iff logging is enabled
     */
    public static boolean isLogEnabled() {
        return LoggingModule.logEnabled;
    }

    /**
     * Enable or disable logging
     * @param enabled True iff logging should enabled
     */
    public static void setLogEnabled(boolean enabled) {
        LoggingModule.logEnabled = enabled;
    }

    /**
     * Returns whether or not writing logs to disk is enabled
     * @return True iff logging to disk is enabled
     */
    public static boolean isWriteToDiskEnabled() {
        return LoggingModule.writeToDiskEnabled;
    }

    /**
     * Enable or disable logging to disk
     * @param enabled True iff logging to disk should enabled
     */
    public static void setWriteToDiskEnabled(boolean enabled) {
        //File write should be disabled
        if (!enabled && LoggingModule.writeToDiskEnabled) {
            // Remove file logger
            try {
                LoggingModule.logger.removeHandler(LoggingModule.fileHandler);
            } catch (Exception e) {
                LoggingModule.logger.log(SEVERE, e.toString());
            }
        }

        //File write should be enabled
        if (!enabled && LoggingModule.writeToDiskEnabled) {
            // Setup file logger
            try {
                LoggingModule.fileHandler = new FileHandler("simulation.log", false);
                LoggingModule.fileHandler.setLevel(INFO);
                LoggingModule.fileHandler.setFormatter(new SimpleFormatter());
                LoggingModule.logger.addHandler(LoggingModule.fileHandler);
            } catch (Exception e) {
                LoggingModule.logger.log(SEVERE, e.toString());
            }
        }

        LoggingModule.writeToDiskEnabled = enabled;
    }
}
