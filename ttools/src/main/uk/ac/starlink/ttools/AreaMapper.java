package uk.ac.starlink.ttools;

import java.util.function.Function;
import uk.ac.starlink.table.DomainMapper;

/**
 * DomainMapper for AreaDomain.
 *
 * @author   Mark Taylor
 * @since    14 Apr 2020
 */
public abstract class AreaMapper implements DomainMapper {

    private final String sourceName_;
    private final String sourceDescription_;
    private final Class<?> sourceClazz_;

    /**
     * Constructor.
     *
     * @param  sourceName  source name
     * @param  sourceDescription  source description,
     *                            may include XML formatting but don't wrap
     *                            in &lt;p&gt; tags
     * @param  sourceClazz  source class
     */
    protected AreaMapper( String sourceName, String sourceDescription,
                          Class<?> sourceClazz ) {
        sourceName_ = sourceName;
        sourceDescription_ = sourceDescription;
        sourceClazz_ = sourceClazz;
    }

    /**
     * @return   {@link AreaDomain#INSTANCE}
     */
    public AreaDomain getTargetDomain() {
        return AreaDomain.INSTANCE;
    }

    public Class<?> getSourceClass() {
        return sourceClazz_;
    }

    public String getSourceName() {
        return sourceName_;
    }

    public String getSourceDescription() {
        return sourceDescription_;
    }

    /**
     * Returns a function that can map source objects of a given class
     * to corresponding Area instances.
     *
     * @param  aclazz   class of objects that is to be mapped
     * @return   function that converts typed objects to Area instances
     */
    public abstract Function<Object,Area> areaFunction( Class<?> aclazz );
}
