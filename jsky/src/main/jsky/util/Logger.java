// Copyright 1997-2001
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: Logger.java,v 1.6 2002/08/20 09:57:58 brighton Exp $
//

package jsky.util;


import java.util.Properties;

import org.apache.log4j.*;

import java.io.IOException;

/**
 * Logger provides an interface for handling logging messages.
 * The purpose is to isolate the use of and changes to log4j as it quickly evolves.
 * <p>
 * Since log4j is primarily configured via a configuration file,
 * the interface for Logger is quite simple.  The
 * {@link #configure} routine assumes the property
 * file <code>PropertyConfigurator</code> rather than one of the
 * XML-based configuration files.
 * <p>
 * The default log4j configuration file to use may be overridden by defining
 * the system property <code>jsky.util.logger.config</code> to the name of the
 * new config file before this class is accessed. Otherwise, the
 * {@link #configure} method may be called at any time to set a new config file.
 *
 * @author		K.Gillies, A. Brighton
 **/
public class Logger {

    /** Name of the default log configuration file resource. */
    private static final String DEFAULT_LOG_CONFIG = "logConfig.prop";

    /**
     * Static initialization.
     */
    static {
        // Look for the config file resource
        String filename = System.getProperty("jsky.util.logger.config");
        if (filename != null && filename.length() != 0) {
            configure(filename);
        }
        else {
            Properties p = null;
            try {
                p = Resources.getProperties(DEFAULT_LOG_CONFIG);
            }
            catch (IOException ex) {
                System.err.println("Log config file: " + DEFAULT_LOG_CONFIG + " could not be located.");
            }
            // Fetch of properties failed so do default init
            if (p == null) {
                configure();
            }
            else {
                // Configuring with a property file
                configure(p);
            }
        }
    }

    /**
     * Configure with minimal initialization using the log4j
     * BasicConfigurator.
     */
    public static void configure() {
        BasicConfigurator.configure();
    }

    /**
     * Configure with a property file given as an argument.
     */
    public static void configure(String propertyFileName) {
        if (propertyFileName == null) {
            System.err.println("Log configuration file is null.");
            return;
        }

        PropertyConfigurator.configure(propertyFileName);
    }

    /**
     * Configure with a {@link Properties} object.
     */
    public static void configure(Properties p) {
        if (p == null) {
            System.err.println("Log configuration properties are null.");
            return;
        }

        PropertyConfigurator.configure(p);
    }

    /**
     * Check if a category is debug enabled.  Allows checking before
     * constructing complex messages.
     */
    public static boolean isDebugEnabled(String category) {
        Category c = Category.getInstance(category);
        return c.isDebugEnabled();
    }

    /**
     * Returns true if debugging is enabled for the class of the given object.
     */
    public static boolean isDebugEnabled(Object o) {
        Category c = Category.getInstance(o.getClass().getName());
        return c.isDebugEnabled();
    }

    /**
     * Post a debug message to the specified category.
     */
    public static void debug(String category, String message) {
        Category c = Category.getInstance(category);
        c.debug(message);
    }

    /**
     * Post a debug message to the specified category.  A
     * {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void debug(String category, String message, Throwable t) {
        Category c = Category.getInstance(category);
        c.debug(message, t);
    }

    /**
     * Post a debug message to the category for the class of the given object.
     */
    public static void debug(Object o, String message) {
        Category c = Category.getInstance(o.getClass().getName());
        c.debug(message);
    }

    /**
     * Post a debug message to the category for the class of the given object.
     * A {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void debug(Object o, String message, Throwable t) {
        Category c = Category.getInstance(o.getClass().getName());
        c.debug(message, t);
    }

    /**
     * Check if a category is info enabled.  Allows checking before
     * constructing complex messages.
     */
    public static boolean isInfoEnabled(String category) {
        Category c = Category.getInstance(category);
        return c.isInfoEnabled();
    }

    /**
     * Returns true if info messages are enabled for the class of the given object.
     */
    public static boolean isInfoEnabled(Object o) {
        Category c = Category.getInstance(o.getClass().getName());
        return c.isInfoEnabled();
    }

    /**
     * Post an info message to the specified category.
     */
    public static void info(String category, String message) {
        Category c = Category.getInstance(category);
        c.info(message);
    }

    /**
     * Post an info message to the specified category.  A
     * {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void info(String category, String message, Throwable t) {
        Category c = Category.getInstance(category);
        c.info(message, t);
    }

    /**
     * Post an info message to the category for the class of the given object.
     */
    public static void info(Object o, String message) {
        Category c = Category.getInstance(o.getClass().getName());
        c.info(message);
    }

    /**
     * Post an info message to the category for the class of the given object.
     * A {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void info(Object o, String message, Throwable t) {
        Category c = Category.getInstance(o.getClass().getName());
        c.info(message, t);
    }

    /**
     * Post a warning message to the specified category.
     */
    public static void warn(String category, String message) {
        Category c = Category.getInstance(category);
        c.warn(message);
    }

    /**
     * Post a warning message to the specified category.  A
     * {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void warn(String category, String message, Throwable t) {
        Category c = Category.getInstance(category);
        c.warn(message, t);
    }

    /**
     * Post a warning message to the category for the class of the given object.
     */
    public static void warn(Object o, String message) {
        Category c = Category.getInstance(o.getClass().getName());
        c.warn(message);
    }

    /**
     * Post a warning message to the category for the class of the given object.
     * A {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void warn(Object o, String message, Throwable t) {
        Category c = Category.getInstance(o.getClass().getName());
        c.warn(message, t);
    }

    /**
     * Post an error message to the specified category.
     */
    public static void error(String category, String message) {
        Category c = Category.getInstance(category);
        c.error(message);
    }

    /**
     * Post an error message to the specified category.  A
     * {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void error(String category, String message, Throwable t) {
        Category c = Category.getInstance(category);
        c.error(message, t);
    }

    /**
     * Post an error message to the category for the class of the given object.
     */
    public static void error(Object o, String message) {
        Category c = Category.getInstance(o.getClass().getName());
        c.error(message);
    }

    /**
     * Post an error message to the category for the class of the given object.
     * A {@link Throwable} can be passed and a stack trace will
     * be printed.
     */
    public static void error(Object o, String message, Throwable t) {
        Category c = Category.getInstance(o.getClass().getName());
        c.error(message, t);
    }

    /**
     * A test routine.
     */
    public static void main(String[] args) {
        if (args.length != 0)
            Logger.configure(args[0]);

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
