package uk.ac.starlink.ttools.plot2.data;

/**
 * Coord implementation for String values.
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public class StringCoord extends SingleCoord {

    /**
     * Constructor.
     *
     * @param   name   user-directed coordinate name
     * @param   description  user-directed coordinate description
     * @param   isRequired  true if this coordinate is required for plotting
     */
    public StringCoord( String name, String description, boolean isRequired ) {
        super( name, description, isRequired,
               Object.class, StorageType.STRING );
    }

    public Object userToStorage( Object[] userCoords ) {
        Object c = userCoords[ 0 ];
        return c == null ? null : c.toString();
    }

    /**
     * Reads a String value from an appropriate column in the current row
     * of a given TupleSequence.
     *
     * @param  tseq  sequence positioned at a row
     * @param  icol  index of column in sequence corresponding to this Coord
     * @return  value of string column at the current sequence row
     */
    public String readStringCoord( TupleSequence tseq, int icol ) {
        Object o = tseq.getObjectValue( icol );
        return o == null ? null : o.toString();
    }
}
