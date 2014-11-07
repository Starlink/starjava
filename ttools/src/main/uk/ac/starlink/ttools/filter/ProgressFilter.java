package uk.ac.starlink.ttools.filter;

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
public class ProgressFilter extends BasicFilter implements ProcessingStep {

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
        super( "progress", null );
        pstrm_ = pstrm;
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Monitors progress by displaying the number of rows processed",
            "so far on the terminal (standard error).",
            "This number is updated every second or thereabouts;",
            "if all the processing is done in under a second you may not",
            "see any output.",
            "If the total number of rows in the table is known,",
            "an ASCII-art progress bar is updated, otherwise just the",
            "number of rows seen so far is written.",
            "</p>",
            "<p>Note under some circumstances progress may appear to",
            "complete before the actual work of the task is done",
            "since part of the processing involves slurping up the whole",
            "table to provide random access on it.",
            "In this case, applying the",
            "<ref id='cache'><code>cache</code></ref> upstream may help.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) {
        return this;
    }

    public StarTable wrap( StarTable in ) {
        return new ProgressLineStarTable( in, pstrm_ );
    }
}
