package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.table.Domain;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Minimal Domain implementation with only one mapper.
 *
 * @author   Mark Taylor
 * @since    14 Apr 2020
 */
public class SimpleDomain<T> implements Domain<SimpleDomain.Mapper<T>> {

    private final String domainName_;
    private final String sourceName_;
    private final String sourceDescription_;
    private final Class<T> clazz_;
    private final Mapper<T> mapper_;

    /** SimpleDomain instance for numeric values. */
    public static final SimpleDomain<Number> NUMERIC_DOMAIN =
        new SimpleDomain<Number>( Number.class, "Number", "number",
                                  "Numeric value" );

    /** Sole DomainMapper for {@link #NUMERIC_DOMAIN}. */
    public static final SimpleDomain.Mapper<Number> NUMERIC_MAPPER =
        NUMERIC_DOMAIN.getMapper();

    /**
     * Constructor.
     *
     * @param   clazz   required source class
     * @param   domainName   target domain name
     * @param   sourceName   source value name
     * @param   sourceDescription  source value description
     */
    protected SimpleDomain( Class<T> clazz, String domainName,
                            String sourceName, String sourceDescription ) {
        clazz_ = clazz;
        domainName_ = domainName;
        sourceName_ = sourceName;
        sourceDescription_ = sourceDescription;
        mapper_ = new Mapper<T>( this );
    }

    public String getDomainName() {
        return domainName_;
    }

    public Mapper<T>[] getMappers() {
        return PlotUtil.singletonArray( mapper_ );
    }

    public Mapper<T> getProbableMapper( ValueInfo info ) {
        return clazz_.isAssignableFrom( info.getContentClass() )
             ? mapper_
             : null;
    }

    public Mapper<T> getPossibleMapper( ValueInfo info ) {
        return clazz_.isAssignableFrom( info.getContentClass() )
             ? mapper_
             : null;
    }

    /**
     * Returns sole default mapper instance for this domain.
     *
     * @return  mapper
     */
    public Mapper<T> getMapper() {
        return mapper_;
    }

    /**
     * Simple mapper implementation.
     */
    public static class Mapper<T> implements DomainMapper {
        private final SimpleDomain<T> domain_;
        private final Class<T> clazz_;

        /**
         * Constructor.
         *
         * @param   domain  target domain
         */
        public Mapper( SimpleDomain<T> domain ) {
            domain_ = domain;
            clazz_ = domain.clazz_;
        }
        public SimpleDomain<T> getTargetDomain() {
            return domain_;
        }
        public Class<?> getSourceClass() {
            return clazz_;
        }
        public String getSourceName() {
            return domain_.sourceName_;
        }
        public String getSourceDescription() {
            return domain_.sourceDescription_;
        }
    }
}
