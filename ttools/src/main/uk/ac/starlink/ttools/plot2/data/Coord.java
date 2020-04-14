package uk.ac.starlink.ttools.plot2.data;

import java.util.function.Function;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;

/**
 * Defines a coordinate quantity in terms of both the user's view of it
 * and its representation for use in plotting.
 * The {@link #inputStorage} method provides translation between these
 * two representations.
 *
 * <p>An implementation of this class defines an additional
 * <code>read*Coord(Tuple,int)</code> method which is able to
 * read appropriate coordinate values from a suitable field of a
 * {@link Tuple}.  That behaviour is not enforced or defined in this
 * interface using generic types, partly in order to allow use of
 * primitive types and eliminate unnecessary use of wrapper classes.
 *
 * <p>In many cases, both the input and the plotting views will be a scalar,
 * in which case there will be only one Input.
 * One notable case for which this is not true is for {@link SkyCoord},
 * which has two input coordinates (lat + long) and three storage coordinates
 * (x, y, z vector).
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public interface Coord {

    /**
     * Returns specifications of the one or more input values the user
     * supplies to provide the data values for this coord.
     *
     * @return   one or more items describing the user input values
     *           for this quantity
     */
    Input[] getInputs();

    /**
     * Indicates whether this item must have a non-blank value in order
     * for a plot to be possible.
     *
     * @return   if true, values must be supplied to make a plot
     */
    boolean isRequired();

    /**
     * Returns a code indicating how the quantity defined by this
     * object is stored internally and presented to the plotting classes.
     *
     * @return  storage type enum instance
     */
    StorageType getStorageType();

    /**
     * Provides a function to turn a quantity in the user view to
     * a plotting view object.
     *
     * <p>The supplied <code>infos</code> and <code>domainMappers</code>
     * arrays correspond to
     * (have the same length as) this object's Inputs array,
     * and may influence the return values.  However, Coord instances
     * that always behave the same way (for instance whose Input Domains
     * have fixed DomainMappers) are free to ignore these arguments.
     *
     * <p>The returned function converts an array of per-input user values
     * to a storable object of the type corresponding to the result of
     * {@link #getStorageType}; the return value of the returned function
     * is never null.
     *
     * @param   infos  per-input array of column input metadata
     * @param   domainMappers  per-input array of input value-&gt;domain value
     *                         mappers
     * @return   input values to storage object conversion function,
     *           or null if such conversions will never be possible
     */
    Function<Object[],?> inputStorage( ValueInfo[] infos,
                                       DomainMapper[] domainMappers );
}
