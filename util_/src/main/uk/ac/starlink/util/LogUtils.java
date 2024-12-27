package uk.ac.starlink.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Utilities for working with logging.
 *
 * @author   Mark Taylor
 * @since    20 Jul 2022
 */
public class LogUtils {

    private static final Map<String,Logger> loggerMap_ =
        new ConcurrentHashMap<>();

    /**
     * Private sole constructor prevents instantiation.
     */
    private LogUtils() {
    }

    /**
     * Returns the logger for a given name, retaining a reference to
     * that logger.
     *
     * <p>The output is the same as
     * {@link java.util.logging.Logger#getLogger(java.lang.String)},
     * for which it is a drop-in replacement,
     * but because a reference is retained by this class,
     * the returned object will not be subsequently garbage collected;
     * as noted in the {@link java.util.logging.Logger} javadocs:
     * <blockquote>
     *   "It is important to note that the Logger returned by one of the
     *   <code>getLogger</code> factory methods may be garbage collected
     *   at any time if a strong reference to the Logger is not kept."
     * </blockquote>
     *
     * <p>So if you want to modify one of the loggers in the logging hierarchy,
     * you can do it like:
     * <pre>
     *    LogUtils.getLogger("a.b.c").setLevel(Level.WARNING)
     * </pre>
     * If you make the corresponding call using <code>Logger.getLogger</code>,
     * the logger may have been garbage collected and recreated without
     * the desired configuration by the time it's actually used to log
     * something.
     *
     * @param  name  logger name
     * @return   logger
     * @see    java.util.logging.Logger#getLogger
     */
    public static Logger getLogger( String name ) {
        return loggerMap_.computeIfAbsent( name, Logger::getLogger );
    }
}
