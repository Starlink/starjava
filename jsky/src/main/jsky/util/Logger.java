package jsky.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Replacement logger that uses the Jarkarta commons logging system.
 * Note this only provides the used parts of the original JSKY Logger class.
 * This replacement is so that we can remove log4j and only use the Java 1.4
 * logger system (which is used by STARJAVA).
 *
 * @author Peter W. Draper
 **/
public class Logger
{
    /**
     * Check if debug logging is enabled.
     */
    public static boolean isDebugEnabled(String category) {
        Log log = LogFactory.getLog( category );
        return log.isDebugEnabled();
    }

    /**
     * Check if debug logging is enabled.
     */
    public static boolean isDebugEnabled(Object o) {
        Log log = LogFactory.getLog( o.getClass().getName() );
        return log.isDebugEnabled();
    }

    /**
     * Post a debug message to the specified category.
     */
    public static void debug(String category, String message) {
        Log log = LogFactory.getLog( category );
        log.debug(message);
    }

    /**
     * Post a debug message to the specified category.  A
     * {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void debug(String category, String message, Throwable t) {
        Log log = LogFactory.getLog( category );
        log.debug(message, t);
    }

    /**
     * Post a debug message to the category for the class of the given object.
     */
    public static void debug(Object o, String message) {
        Log log = LogFactory.getLog( o.getClass().getName() );
        log.debug(message);
    }

    /**
     * Post a debug message to the category for the class of the given object.
     * A {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void debug(Object o, String message, Throwable t) {
        Log log = LogFactory.getLog( o.getClass().getName() );
        log.debug(message, t);
    }

    /**
     * Post an info message to the specified category.
     */
    public static void info(String category, String message) {
        Log log = LogFactory.getLog( category );
        log.info(message);
    }

    /**
     * Post an info message to the specified category.  A
     * {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void info(String category, String message, Throwable t) {
        Log log = LogFactory.getLog( category );
        log.info(message, t);
    }

    /**
     * Post an info message to the category for the class of the given object.
     */
    public static void info(Object o, String message) {
        Log log = LogFactory.getLog( o.getClass().getName() );
        log.info(message);
    }

    /**
     * Post an info message to the category for the class of the given object.
     * A {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void info(Object o, String message, Throwable t) {
        Log log = LogFactory.getLog( o.getClass().getName() );
        log.info(message, t);
    }

    /**
     * Post a warning message to the specified category.
     */
    public static void warn(String category, String message) {
        Log log = LogFactory.getLog( category );
        log.warn(message);
    }

    /**
     * Post a warning message to the specified category.  A
     * {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void warn(String category, String message, Throwable t) {
        Log log = LogFactory.getLog( category );
        log.warn(message, t);
    }

    /**
     * Post a warning message to the category for the class of the given object.
     */
    public static void warn(Object o, String message) {
        Log log = LogFactory.getLog( o.getClass().getName() );
        log.warn(message);
    }

    /**
     * Post a warning message to the category for the class of the given object.
     * A {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void warn(Object o, String message, Throwable t) {
        Log log = LogFactory.getLog( o.getClass().getName() );
        log.warn(message, t);
    }

    /**
     * Post an error message to the specified category.
     */
    public static void error(String category, String message) {
        Log log = LogFactory.getLog( category );
        log.error(message);
    }

    /**
     * Post an error message to the specified category.  A
     * {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void error(String category, String message, Throwable t) {
        Log log = LogFactory.getLog( category );
        log.error(message, t);
    }

    /**
     * Post an error message to the category for the class of the given object.
     */
    public static void error(Object o, String message) {
        Log log = LogFactory.getLog( o.getClass().getName() );
        log.error(message);
    }

    /**
     * Post an error message to the category for the class of the given object.
     * A {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void error(Object o, String message, Throwable t) {
        Log log = LogFactory.getLog( o.getClass().getName() );
        log.error(message, t);
    }

    /**
     * A test routine.
     */
    public static void main(String[] args) {

        String cname = "SomeClass";
        Logger.debug(cname, "This is a debug message.");
        Logger.info(cname, "This is an info message.");
        Logger.warn(cname, "This is a warning message.");
        Logger.error(cname, "This is an error message.");

        // A different category
        String dname = "SomeOtherClass";
        Logger.debug(dname, "This is a debug message.");
        Logger.info(cname, "This is an info message.");
        Logger.warn(cname, "This is a warning message.");
        Logger.error(dname, "This is an error message.");

        // Try an exception
        Exception e = new IllegalArgumentException();
        Logger.error(dname, "This is an illegal exception.", e);
    }
}
