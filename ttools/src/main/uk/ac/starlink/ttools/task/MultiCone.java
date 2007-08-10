package uk.ac.starlink.ttools.task;

import uk.ac.starlink.ttools.cone.ConeSearchConer;
import uk.ac.starlink.ttools.cone.SkyConeMatch2;

/**
 * SkyConeMatch2 implementation which uses an external Cone Search service.
 *
 * @author   Mark Taylor
 * @since    4 Jul 2006
 */
public class MultiCone extends SkyConeMatch2 {
    public MultiCone() {
        super( "Makes multiple cone search queries to the same service",
               new ConeSearchConer() );
    }
}
