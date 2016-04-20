package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;

/**
 * Simple implementation of a PositionCoordPanel.
 * It only deals with a single, fixed, DataGeom.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class SimplePositionCoordPanel extends PositionCoordPanel {

    private final DataGeom geom_;

    /**
     * Constructs a panel for selecting just Coords.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  geom  fixed data geom
     */
    public SimplePositionCoordPanel( Coord[] coords, DataGeom geom ) {
        this( coords, new ConfigKey[ 0 ], geom );
    }

    /**
     * Constructs a CoordPanel for selecting Coords and Config values.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  configKeys   config value keys
     * @param  geom  fixed data geom
     */
    public SimplePositionCoordPanel( Coord[] coords, ConfigKey[] configKeys,
                                     DataGeom geom ) {
        super( coords, configKeys );
        geom_ = geom;
    }

    public DataGeom getDataGeom() {
        return geom_;
    }

    /**
     * Constructs a position coord panel based on a given DataGeom.
     * A given number of groups of the positional coordinates defined
     * by the DataGeom are shown.
     *
     * @param  geom   provides description of positional coordinates
     * @param  npos   number of positional groups to include
     */
    public static SimplePositionCoordPanel createPanel( DataGeom geom,
                                                        int npos ) {
        Coord[] coords = multiplyCoords( geom.getPosCoords(), npos );
        return new SimplePositionCoordPanel( coords, geom );
    }
}
