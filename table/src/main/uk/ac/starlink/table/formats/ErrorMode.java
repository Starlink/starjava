package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.IOConsumer;

/**
 * Defines how error messages are reported.
 *
 * @author   Mark Taylor
 * @since    30 Apr 2021
 */
public abstract class ErrorMode {

    private final String name_;
    private final boolean isReport_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.formats" );

    /** Messages are ignored. */
    public static final ErrorMode IGNORE;

    /** Messages are written as WARNINGs through the logging system. */
    public static final ErrorMode WARN;

    /** Messages are thrown as TableFormatExceptions,
     *  probably causing read failure. */
    public static final ErrorMode FAIL;

    /** Known values. */
    public static final ErrorMode[] OPTIONS = {
        IGNORE = createMode( "ignore", false, txt -> {} ),
        WARN = createMode( "warn", true, logger_::warning ),
        FAIL = createMode( "fail", true,
                           txt -> { throw new TableFormatException( txt ); } ),
    };

    /**
     * Constructor.
     *
     * @param  name  mode name
     * @param  isReport  true if text reports are ever used for anything
     */
    protected ErrorMode( String name, boolean isReport ) {
        isReport_ = isReport;
        name_ = name;
    }

    /**
     * Returns true if text reports submitted may ever be used for any
     * purpose.  If this returns false, there's no point submitting reports.
     *
     * @return   true if reports may be used
     */
    public boolean isReport() {
        return isReport_;
    }

    /**
     * Returns this option's name.
     */
    @Override
    public String toString() {
        return name_;
    }

    /**
     * Consumes a report string in a way appropriate for this mode.
     *
     * @param  msg  message to report
     */
    public abstract void report( String msg ) throws IOException;

    /**
     * Convenience method to create an ErrorMode instance.
     *
     * @param  name  mode name
     * @param  isReport  true if text reports are ever used for anything
     * @param  reporter  implementation of {@link #report} method
     */
    private static ErrorMode createMode( String name, boolean isReport,
                                         final IOConsumer<String> reporter ) {
        return new ErrorMode( name, isReport ) {
            public void report( String msg ) throws IOException {
                reporter.accept( msg );
            }
        };
    }
}
