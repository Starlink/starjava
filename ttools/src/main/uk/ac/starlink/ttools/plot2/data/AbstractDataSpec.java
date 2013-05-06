package uk.ac.starlink.ttools.plot2.data;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Abstract superclass for DataSpec implementations.
 * This handles object identity, so that <code>equals</code>
 * and <code>hashCode</code> are implemented correctly.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2013
 */
public abstract class AbstractDataSpec implements DataSpec {

    @Override
    public int hashCode() {
        return getId( this ).hashCode();
    }

    @Override
    public boolean equals( Object other ) {
        return other instanceof DataSpec
            && getId( (DataSpec) other ).equals( getId( this ) );
    }

    /**
     * Returns an identity object for a given DataSpec.
     * The equals and hashCode methods of the returned object behave
     * sensibly.
     *
     * @param  spec  data spec
     * @return  equal-able object
     */
    @Equality
    private static Object getId( DataSpec spec ) {
        if ( spec == null ) {
            return null;
        }
        int nc = spec.getCoordCount();
        int ni = nc + 2;
        List<Object> list = new ArrayList<Object>( ni );
        list.add( spec.getSourceTable() );
        list.add( spec.getMaskId() );
        for ( int ic = 0; ic < nc; ic++ ) {
            list.add( spec.getCoordId( ic ) );
        }
        assert list.size() == ni;
        return list;
    }
}
