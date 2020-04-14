package uk.ac.starlink.table;

/**
 * Marker interface for objects that can map input values to a particular
 * common value domain.  Abstract sub-types of this interface will reference
 * a particular target {@link Domain} and provide abstract methods for
 * conversions to that domain.  A concrete implementation of one of
 * those sub-types will provide the actual conversion methods for a
 * particular type of input data.
 *
 * <p>Table input handlers can provide typed DomainMapper instances as
 * part of the table metadata they construct
 * (see the {@link ValueInfo#getDomainMappers} method),
 * as a way to communicate format-specific information about the
 * intended semantics and conversion mechanisms of columns or
 * parameters to users of the table.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 * @see    Domain
 */
public interface DomainMapper {

    /**
     * Returns the target domain of this mapper.
     * An abstract DomainMapper sub-type should normally implement this
     * as a final method with a fixed return value.
     *
     * @return   domain identifier
     */
    Domain<?> getTargetDomain();

    /**
     * Returns the type of values from which this mapper instance can
     * convert.  This should be as specific as possible to cover all
     * the possible options, but in some cases that may mean it has
     * simply to return <code>Object.class</code>.
     *
     * @return  class of mappable source objects to which this mapper can be
     *          applied
     */
    Class<?> getSourceClass();

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
