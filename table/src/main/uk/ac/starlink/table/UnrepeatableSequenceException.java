package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Exception thrown by {@link StarTable#getRowSequence} calls after the first 
 * for tables which can only provide a single <code>RowSequence</code>.
 * The general contract of <code>StarTable</code> is that 
 * <code>getRowSequence</code> should be callable multiple times,
 * so in general this exception ought not to be thrown, but for certain
 * special circumstances where a one-shot table is known to be usable,
 * this is the exception which should be used.
 *
 * @author   Mark Taylor
 * @since    23 Jul 2007
 */
public class UnrepeatableSequenceException extends IOException {

    /**
     * Constructs an exception with a default message.
     */
    public UnrepeatableSequenceException() {
        this( "Table does not permit row sequence re-reads" );
    }

    /**
     * Constructs an exception with a given message.
     *
     * @param   msg  message
     */
    public UnrepeatableSequenceException( String msg ) {
        super( msg );
    }
}
