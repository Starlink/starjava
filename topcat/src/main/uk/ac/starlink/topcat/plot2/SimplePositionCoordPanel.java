package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.table.ValueInfo;
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
public class SimplePositionCoordPanel extends BasicCoordPanel
                                      implements PositionCoordPanel {

    private final DataGeom geom_;

    /**
     * Constructs a panel for selecting just Coords.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  geom  fixed data geom
     */
    public SimplePositionCoordPanel( Coord[] coords, DataGeom geom ) {
        this( coords, new ConfigKey<?>[ 0 ], createDefaultStack(), geom );
    }

    /**
     * Constructs a CoordPanel for selecting Coords and Config values.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  configKeys   config value keys
     * @param  stack  coord stack implementation
     * @param  geom  fixed data geom
     */
    public SimplePositionCoordPanel( Coord[] coords, ConfigKey<?>[] configKeys,
                                     CoordStack stack, DataGeom geom ) {
        super( coords, configKeys, stack );
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
    public static SimplePositionCoordPanel
            createPanel( DataGeom geom, final int npos,
                         final CoordSpotter[] spotters ) {

        /* Provide some implementation that should be able to guess
         * default settings for coordinates in some cases.
         * There are a few assumptions required for this to work,
         * for instance that all the coordinate selectors have the same
         * list of possible values in their column selectors.
         * If the overridden autoPopulate method below causes trouble
         * in some cases, nothing terrible will happen if it's just
         * not overridden, just a slightly degraded user experience. */
        Coord[] posCoords = geom.getPosCoords();
        final Coord[] coords = multiplyCoords( posCoords, npos );
        if ( spotters == null || spotters.length == 0 ||
             posCoords[ 0 ].getInputs().length > 1 ) {
            return new SimplePositionCoordPanel( coords, geom );
        }
        else {
            return new SimplePositionCoordPanel( coords, geom ) {
                @Override
                public void autoPopulate() {

                    /* Try to find coord tuples that fit the requirements
                     * for the current plot type. */
                    ValueInfo[] coordInfos =
                        CoordSpotter
                       .findCoordGroups( npos,
                                         getInfos( getColumnSelector( 0, 0 ) ),
                                         spotters );
                    if ( coordInfos != null ) {
                        int nc = coords.length;
                        assert nc == coordInfos.length;
                        for ( int ic = 0; ic < nc; ic++ ) {
                            populate( getColumnSelector( ic, 0 ),
                                      coordInfos[ ic ] );
                        }
                    }

                    /* If no success, and if it's just a single position
                     * (the most common case), just choose the first
                     * few suitable values.  This has the effect of
                     * plotting the first few numeric columns against
                     * each other. */
                    else if ( npos == 1 ) {
                        super.autoPopulate();
                    }
                }
            };
        }
    }
}
