package uk.ac.starlink.topcat.plot2;

import java.util.Comparator;
import uk.ac.starlink.ttools.plot2.config.Specifier;

/**
 * Defines how ZoneIDs are produced for use in a multi-plotting context.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2016
 */
public interface ZoneFactory {

    /**
     * Indicates whether this factory corresponds to a single-zone plot.
     * If so, all the zone ids dispensed by this factory will be identical.
     *
     * @return   true if this factory is for use in single-zone contexts
     */
    boolean isSingleZone();

    /**
     * Returns a default ZoneId that can be used in absence of any other.
     * The same value is returned over the lifetime of this factory.
     *
     * @return  default zone
     */
    ZoneId getDefaultZone();

    /**
     * Returns a Specifier that can be used to select zoneIds.
     *
     * @return  zone id specifier
     */
    Specifier<ZoneId> createZoneSpecifier();

    /**
     * Returns a comparator that is suitable for use with the ZoneIds
     * dispensed by this factory.
     *
     * @return  comparator
     */
    Comparator<ZoneId> getComparator();

    /**
     * Returns the ZoneId corresponding to a zone name.
     *
     * @param  name  zone name
     * @return   zone identifier in this factory, or null if not known
     */
    ZoneId nameToId( String name );
}
