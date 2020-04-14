package uk.ac.starlink.table;

/**
 * Common value domain.  An instance of this class represents some
 * class of externally representable values that can in some sense
 * be treated internally in the same way, but which may have
 * various different external representations.
 * It is used in conjunction with instances of the {@link DomainMapper}
 * interface, provided by the table input handler or the user,
 * to untangle different possible representations of physical values
 * in input data.
 *
 * <p>See the available instances of this interface such as
 * {@link TimeDomain} for better understanding of how it works.
 *
 * @author   Mark Taylor
 * @since    14 Apr 2020
 * @see  DomainMapper
 */
public interface Domain<M extends DomainMapper> {

    /**
     * Returns the name of this domain.
     *
     * @return  domain name
     */
    String getDomainName();

    /**
     * Returns a list of all mappers known to map values to this domain.
     * This is not necessarily exhaustive, and may in particular not include
     * mappers specific to certain input file formats,
     * but it can be offered to users as a list of options for mapping
     * to this domain where no other option is obvious.
     *
     * @return  list of generic mappers to this domain
     */
    M[] getMappers();

    /**
     * Returns a typed DomainMapper that is likely to be appropriate
     * for data described by the given metadata object.
     *
     * <p>In general, implementations of this method will first check the
     * {@link ValueInfo#getDomainMappers} method.
     * If it is doubtful how to map from the given info to this domain,
     * null should be returned.
     * 
     * @param  info  metadata describing data to be mapped
     * @return   reliable guess at a suitable mapper
     */
    M getProbableMapper( ValueInfo info );

    /**
     * Returns a typed DomainMapper that can be used
     * for data described by the given metadata object.
     * If some reasonable way to make the conversion exists,
     * an appropriate value should be returned,
     * but it's quite possible the conversion will be incorrect.
     * If there is no known possible or plausible mapper,
     * null should be returned.
     *
     * <p>If the result of {@link #getProbableMapper} is non-null,
     * then this method must also return non-null, but this method
     * is <em>not</em> required to return the same value as
     * <code>getProbableMapper</code>.
     *
     * @param  info  metadata describing data to be mapped
     * @return   best-efforts guess at a suitable mapper
     */
    M getPossibleMapper( ValueInfo info );
}
