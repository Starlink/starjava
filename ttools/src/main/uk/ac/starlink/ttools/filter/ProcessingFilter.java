package uk.ac.starlink.ttools.filter;

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
     * Usage message for this filter.  This should contain any arguments
     * which apply to this item; the name itself should not be included.
     * May contain newline characters to break up a long line.
     *
     * @return  usage string
     */
    String getUsage();

    /**
     * Description for this filter.  This is currently inserted into
     * the user document, so should be in XML format.
     *
     * @return  textual description of this filter
     */
    String getDescription();

    /**
     * Creates a new ProcessingStep based on a sequence of command-line
     * arguments.  The <code>argIt</code> argument is an iterator over the
     * command-line arguments positioned just before any arguments
     * intended for this filter.  If legal, any that can be comprehended
     * by this filter should be read (iterated over) and removed,
     * and a <code>ProcessingStep</code> should accordingly be returned.
     * In the case of a successful return, it is essential
     * that no arguments other than the ones intended for this
     * filter are read from the iterator.
     *
     * <p>If the argument list is badly-formed as far as this filter is
     * concerned, an {@link ArgException} should be thrown.
     *
     * @param  argIt  iterator over command-line arguments positioned
     *         at the first one
     */
    ProcessingStep createStep( Iterator<String> argIt ) throws ArgException;
}
