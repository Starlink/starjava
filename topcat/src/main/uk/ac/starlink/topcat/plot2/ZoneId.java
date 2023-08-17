package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Ganger;

/**
 * Marker interface for identifying one zone of a multi-zone plot.
 *
 * @author   Mark Taylor
 * @since    5 Feb 2015
 */
@Equality
public interface ZoneId {

    /**
     * Returns the index into the zones managed by a given ganger of
     * a zone with this identifier.
     *
     * @param   ganger  ganger context
     * @return   index of zone; -1 may be returned if the zone does not
     *           belong to the supplied ganger
     */
    int getZoneIndex( Ganger<?,?> ganger );
}
