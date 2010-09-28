package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Iterator over tables.
 * Unlike the <code>java.util.Iterator</code> interface,
 * the <code>nextTable</code> method throws a checked exception.
 *
 * <p>A suitable looping idiom is
 * <pre>
 *     TableSequence tseq = getTableSequence();
 *     for (StarTable table; (table = tseq.nextTable()) != null;) {
 *         doStuff(table);
 *     }
 * </pre>
 *
 * @author   Mark Taylor
 * @since    5 Jul 2010
 */
public interface TableSequence {

    /**
     * Returns the next table in the sequence, or null if the end of
     * the sequence is reached.
     *
     * @throws   IOException  if there is an error obtaining the table
     * @return   next table, or null if there are no more
     */
    StarTable nextTable() throws IOException;
}
