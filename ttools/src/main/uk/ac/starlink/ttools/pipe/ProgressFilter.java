package uk.ac.starlink.ttools.pipe;

import java.io.PrintStream;
import java.util.Iterator;
import uk.ac.starlink.table.ProgressLineStarTable;
import uk.ac.starlink.table.StarTable;

/**
 * Processing step which writes table progress to the terminal.
 * More than one of these active at once using the same output stream
 * is likely to be messy.
 *
 * @author   Mark Taylor (Starlink)
 * @since    27 Apr 2005
 */
public class ProgressFilter implements ProcessingFilter, ProcessingStep {

    private final PrintStream pstrm_;

    /**
     * Constructs a new filter which writes progress to System.err.
     */
    public ProgressFilter() {
        this( System.err );
    }

    /**
     * Constructs a new filter which writes progress to a given print stream.
     *
     * @param   pstrm   destination terminal for progress characters
     */
    public ProgressFilter( PrintStream pstrm ) {
        pstrm_ = pstrm;
    }

    public String getName() {
        return "progress";
    }

    public String getFilterUsage() {
        return null;
    }

    public ProcessingStep createStep( Iterator argIt ) {
        return this;
    }

    public StarTable wrap( StarTable in ) {
        return new ProgressLineStarTable( in, pstrm_ );
    }
}
