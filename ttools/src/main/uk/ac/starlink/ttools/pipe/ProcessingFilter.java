package uk.ac.starlink.ttools.pipe;

import java.util.Iterator;

/**
 * Defines a type of filter-like processing which can be done on a StarTable.
 * An object in this class serves as a factory for 
 * {@link ProcessingStep} instances, based on a list of command-line
 * arguments.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public interface ProcessingFilter {

    /**
     * Returns the name of this filter.  Will be turned into a command-line
     * flag (by prepending a '-') so it should be short.
     *
     * @return  short name
     */
    String getName();

    /**
     * Returns a usage string for this filter.
     * Describes in standard format any required or optional command-line
     * arguments required for it to work; this should match the 
     * arguments that are judged as legal by {@link #createStep}.
     *
     * @return   usage string
     */
    String getFilterUsage();

    /**
     * Creates a new ProcessingStep based on a sequence of command-line
     * arguments.  The <tt>argIt</tt> argument is an iterator over the
     * command-line arguments positioned just before any arguments 
     * intended for this filter.  If legal, any that can be comprehended
     * by this filter should be read (iterated over) and removed,
     * and a <tt>ProcessingStep</tt> should accordingly be returned.
     * If they are illegal, <tt>null</tt> should be returned.
     * In the case of a successful (non-null) return, it is essential
     * that no arguments other than the ones intended for this
     * filter are read from the iterator.
     *
     * @param  argIt  iterator over command-line arguments positioned
     *         just after the -getName() flag
     */
    ProcessingStep createStep( Iterator argIt );
}
