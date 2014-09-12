package uk.ac.starlink.ttools.plot2.data;

import java.util.List;
import uk.ac.starlink.table.DomainMapper;

/**
 * Defines a coordinate quantity in terms of both the user's view of it
 * and its representation for use in plotting.
 * The {@link #inputToStorage} method translates between these
 * two representations.
 *
 * <p>An implementation of this class defines an additional
 * <code>read*Coord(TupleSequence,int)</code> method which is able to
 * read appropriate coordinate values from a suitable column of a
 * {@link TupleSequence}.  That behaviour is not enforced or defined in this
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
     * Turns a quantity in the user view to a plotting view object.
     * The return value is never null.
     *
     * <p>The supplied parameters both correspond (have the same length as)
     * this object's Inputs array.
     * For each Input, the corresponding element of the
     * <code>inputValues</code> array gives the value obtained from
     * the user-supplied data (matching {@link Input#getValueClass}),
     * and the corresponding element of the <code>inputMappers</code>
     * array gives a DomainMapper object
     * (consistent with {@link Input#getDomain}).
     * InputMappers may be null however, and in many cases,
     * coordinates are not sensitive to domains,
     * and for those cases implementations will ignore
     * <code>inputMappers</code>.
     *
     * @param   inputValues  per-input values
     * @param   inputMappers  per-input domain mappers, each may be null
     * @return  object of the type corresponding to the result of
     *          {@link #getStorageType}; not null
     */
    Object inputToStorage( Object[] inputValues, DomainMapper[] inputMappers );
}
