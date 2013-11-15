package uk.ac.starlink.table;

/**
 * Marker interface for objects that can map input values to a particular
 * common value domain.  Abstract sub-types of this interface will define
 * a particular common value domain and abstract methods for conversions
 * to that domain.  A concrete implementation of one of those sub-types will
 * provide the actual conversion methods for a particular type of input data.
 *
 * <p>A common value domain is typically a range of numeric values with the
 * same units and zero point, giving something like a one-dimensional
 * coordinate frame, but without necessarily modelling all the
 * subtleties that can be associated with coordinates.
 * Its main job is to untangle different possible representations of
 * physical values in input data.
 *
 * <p>Table input handlers can provide typed DomainMapper instances as
 * part of the table metadata they construct, as a way to communicate
 * format-specific information about the intended semantics and
 * conversion mechanisms of columns or parameters to users of the table.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 */
public interface DomainMapper {

    /**
     * Returns a name identifying the target domain of this mapper.
     * An abstract DomainMapper sub-type should normally implement this
     * as a final method with a fixed return value.
     *
     * @return   domain identifier
     */
    String getTargetName();

    /**
     * Returns the type of values from which this mapper instance can
     * convert.
     *
     * @return  class of mapper source objects to which this mapper can be
     *          applied
     */
    Class getSourceClass();

    /**
     * Returns a short name for the type of source values which this mapper
     * can convert from.
     *
     * @return  mapper source type name
     */
    String getSourceName();

    /**
     * Returns a description of the type of source values which this mapper
     * can convert from.
     *
     * @return   mapper source type description
     */
    String getSourceDescription();
}
