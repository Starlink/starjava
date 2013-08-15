package uk.ac.starlink.ttools.plot2.data;

import java.util.List;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.DomainMapper;

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
     * Indicates the target common value domain(s) in which the relevant
     * value(s) will be used.
     * The return value is a list (one for each user value) of DomainMapper
     * abstract sub-types.  Each of these sub-types effectively defines
     * a target value domain.  Null entries for this list are the norm,
     * indicating that the user values will just be interpreted as numeric
     * values, but non-null domains values can be used if a particular
     * interpretation (for instance time) is going to be imposed.
     *
     * @return   list (same length as <code>getUserInfos</code>) of
     *           domain mapper subtypes; elements may be null
     */
    List<Class<? extends DomainMapper>> getUserDomains();

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
     * <p>The <code>userMappers</code> contains entries as specified
     * by the result of the {@link #getUserDomains} method.
     * The array must be the same length as the user domains array,
     * and each non-null element must be an instance of the class
     * in the corresponding element of the user domains array.
     * In many cases however, coordinates are not sensitive to domains,
     * and for those cases implementations will ignore
     * <code>userMappers</code>.
     *
     * @param   userValues  array of objects corresponding to the result of
     *                      {@link #getUserInfos} 
     * @param   userMappers  domains mappers associated with submitted values
     *                       if available and appropriate;
     * @return  object of the type corresponding to the result of
     *          {@link #getStorageType}; not null
     */
    Object userToStorage( Object[] userValues, DomainMapper[] userMappers );
}
