package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.DataGeom;
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
     * Constructor.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  autoPopulate  if true, some attempt will be made to
     *                       fill in the fields with non-blank values
     *                       when a table is selected
     * @param  geom  fixed data geom
     */
    public SimplePositionCoordPanel( Coord[] coords, boolean autoPopulate,
                                     DataGeom geom ) {
        super( coords, autoPopulate );
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
     * @param  autoPopulate  if true, some attempt may be made to
     *                       fill in the fields with non-blank values
     *                       when a table is selected
     */
    public static SimplePositionCoordPanel createPanel( DataGeom geom, int npos,
                                                        boolean autoPopulate ) {
        Coord[] coords = multiplyCoords( geom.getPosCoords(), npos );
        return new SimplePositionCoordPanel( coords, autoPopulate, geom );
    }
}
