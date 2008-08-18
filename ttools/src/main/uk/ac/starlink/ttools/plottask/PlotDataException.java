package uk.ac.starlink.ttools.plottask;

import java.io.IOException;

/**
 * Exception used for exporting errors from the plot system.
 * This exception is unchecked, but can be used to contain a checked exception,
 * especially an IOException.  It is required in this package because the
 * plotting classes do not declare checked exceptions, but the table
 * data access classes declare IOExceptions.
 * Code which invokes the <code>paint</code> methods of the plotting 
 * components should therefore check for such exceptions being thrown
 * even though the compiler does not enforce this.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public class PlotDataException extends RuntimeException {

    /**
     * Constructor based on an IOException.
     *
     * @param  e  cause
     */
    public PlotDataException( IOException e ) {
        super( e.getMessage(), e );
    }

    /**
     * Constructor based on an arbitrary throwable.
     *
     * @param  msg  message
     * @param  e    cause
     */
    public PlotDataException( String msg, Throwable e ) {
        super( msg, e );
    }
}
