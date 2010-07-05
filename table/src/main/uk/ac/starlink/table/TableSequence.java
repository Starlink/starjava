package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Iterator over tables.
 * Unlike the <code>java.util.Iterator</code> interface,
 * the <code>nextTable</code> method throws a checked exception.
 *
 * @author   Mark Taylor
 * @since    5 Jul 2010
 */
public interface TableSequence {

    /**
     * Whether there are thought to be more tables to read from this sequence.
     *
     * @return   true iff there are more tables
     */
    boolean hasNextTable();

    /**
     * Returns the next table in the sequence.
     *
     * @throws   IOException  if there is an error obtaining the table
     * @throws   NoSuchElementException   if there are no more elements
     */
    StarTable nextTable() throws IOException;
}
