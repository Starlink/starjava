package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.table.DomainMapper;

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
     * @param   meta   input value metadata
     * @param   isRequired  true if this coordinate is required for plotting
     */
    public StringCoord( InputMeta meta, boolean isRequired ) {
        super( meta, isRequired, Object.class, StorageType.STRING, null );
    }

    public Object inputToStorage( Object[] values, DomainMapper[] mappers ) {
        Object c = values[ 0 ];
        return c == null ? "" : c.toString();
    }

    /**
     * Reads a String value from an appropriate field of a given Tuple.
     *
     * @param  tuple  tuple
     * @param  icol  index of field in tuple corresponding to this Coord
     * @return  value of string field
     */
    public String readStringCoord( Tuple tuple, int icol ) {
        Object o = tuple.getObjectValue( icol );
        return o == null ? null : o.toString();
    }
}
