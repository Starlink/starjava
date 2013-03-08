package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.table.ValueInfo;

/**
 * Defines a coordinate quantity in terms of both the user's view of it
 * and its representation for use in plotting.
 * The {@link #userToStorage} method translates between these
 * two representations.
 *
 * <p>An implementation of this class defines an additional
 * <code>read*Coord(TupleSequence,int)</code> method which is able to
 * read appropriate coordinate values from a suitable column of a
 * {@link TupleSequence}.  That behaviour is not enforced or defined in this
 * interface using generic types, partly in order to allow use of
 * primitive types and eliminate unnecessary use of wrapper classes.
 *
 * <p>In many cases, both the user and the plotting views will be a scalar.
 * One notable case for which this is not true is for {@link SkyCoord},
 * which has two user coordinates (lat + long) and three storage coordinates
 * (x, y, z vector).
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public interface Coord {

    /**
     * Returns the user-directed metadata for acqusition of the relevant
     * value(s).  This serves to document what is required in terms of
     * geometry or other general characteristics.
     *
     * @return   one or more metadata items describing the required coordinate
     *           quantity
     */
    ValueInfo[] getUserInfos();

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
     * @param   userValues  array of objects corresponding to the result of
     *                      {@link #getUserInfos} 
     * @return  object of the type corresponding to the result of
     *          {@link #getStorageType}; not null
     */
    Object userToStorage( Object[] userValues );
}
