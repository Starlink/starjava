package uk.ac.starlink.array;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pixel ordering identifier.  Objects in this class are used to identify
 * the ordering of pixels when they are presented as a vectorised array.
 * <p>
 * This class exemplifies the <i>typesafe enum</i> pattern -- the only
 * possible instances are supplied as static final fields of the class, and
 * these instances are immutable.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class Order {

    private static List allOrders = new ArrayList( 2 );

    /**
     * Object representing column-major (first-index-fastest) ordering.
     * This is how FITS data is organised, and is natural to Fortran.
     * The pixels of an array with origin=(1,1) and dims=(2,2) with this
     * ordering would be vectorised in the order (1,1), (2,1), (1,2), (2,2).
     */
    public static final Order COLUMN_MAJOR = new Order( "column-major", true );

    /**
     * Object representing row-major (last-index-fastest) ordering.
     * Row-major order, in which the last index varies fastest.
     * it is natural to C-like languages (though such languages generally lack
     * true multi-dimensional rectangular arrays).
     */
    public static final Order ROW_MAJOR = new Order( "row-major", false );

    private final boolean fitsLike;
    private final String name;

    /*
     * Private sole constructor.
     */
    private Order( String name, boolean fitsLike ) {
        this.name = name;
        this.fitsLike = fitsLike;
        allOrders.add( this );
    }

    /**
     * Convenience method which returns true for ordering which is 
     * FITS-like and Fortran-like (that is for COLUMN_MAJOR), otherwise false.
     *
     * @return  true for COLUMN_MAJOR, false otherwise
     */
    public boolean isFitsLike() {
        return fitsLike;
    }

    public String toString() {
        return name;
    }

    /**
     * Returns a list of all the known ordering schemes.
     *
     * @return  an unmodifiable List containing all the existing Order objects.
     */
    public static List allOrders() {
        return Collections.unmodifiableList( allOrders );
    }

}
