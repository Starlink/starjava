package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.StorageType;

/**
 * GUI component for obtaining data position coordinates.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public abstract class PositionCoordPanel extends CoordPanel {

    /**
     * Constructor with supplied coord stack.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  configKeys  config value keys (often empty)
     * @param  stack   coord stack implementation
     */
    protected PositionCoordPanel( Coord[] coords, ConfigKey<?>[] configKeys,
                                  CoordStack stack ) {
        super( coords, configKeys, stack );
    }

    /**
     * Constructor with default coord stack.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  configKeys  config value keys (often empty)
     */
    protected PositionCoordPanel( Coord[] coords, ConfigKey<?>[] configKeys ) {
        this( coords, configKeys, createDefaultStack() );
    }

    /**
     * Returns the position geometry that defines the mapping of input
     * to data coordinates.
     *
     * @return  data geom
     */
    public abstract DataGeom getDataGeom();

    /**
     * Returns a list of coordinates which is like multiple copies of a
     * supplied group.  The returned coords have metadata which
     * distinguish them from each other, currently an integer appended
     * to their name.  The returned coords are not totally respectable,
     * but their metadata is OK.
     *
     * @param   coords  basic coordinates
     * @param  ncopy   number of copies to make
     * @return   array of ncopy copies of coords
     */
    public static Coord[] multiplyCoords( Coord[] coords, int ncopy ) {
        List<Coord> coordList = new ArrayList<Coord>();
        for ( int ig = 0; ig < ncopy; ig++ ) {
            for ( int ic = 0; ic < coords.length; ic++ ) {
                Coord coord = coords[ ic ];
                coordList.add( ncopy == 1 ? coord : relabel( coord, ig ) );
            }
        }
        return coordList.toArray( new Coord[ 0 ] );
    }

    /**
     * Returns a Coord like a given one but with modified metadata.
     *
     * <p>The returned Coord is not of the right subclass, hence does not
     * have the appropriate type-specific read*Coord method.
     * However that doesn't matter, because we're just using the results
     * from this call to represent coordinate metadata, not for reading data.
     *
     * @param  baseCoord  coord on which to base the copy
     * @param  iPoint  point index, used to label the coordinate
     * @return   new coord like the input one
     */
    private static Coord relabel( final Coord baseCoord, int iPoint ) {
        String iptxt = PlotUtil.getIndexSuffix( iPoint );
        final Input[] inputs = baseCoord.getInputs().clone();
        int ni = inputs.length;
        for ( int ii = 0; ii < ni; ii++ ) {
            Input input0 = inputs[ ii ];
            InputMeta meta0 = input0.getMeta();
            InputMeta meta =
                new InputMeta( meta0.getShortName() + iptxt,
                               meta0.getLongName() + " (" + iptxt + ")" );
            meta.setShortDescription( meta0.getShortDescription()
                                    + " for point " + iptxt );
            meta.setXmlDescription( meta0.getXmlDescription()
                                  + ( "<p>Applies to point "
                                    + iptxt + ".</p>" ) );
            inputs[ ii ] = input0.withMeta( meta );
        }
        return new Coord() {
            public Input[] getInputs() {
                return inputs;
            }
            public boolean isRequired() {
                return true;
            }
            public StorageType getStorageType() {
                return baseCoord.getStorageType();
            }
            public Function<Object[],?> inputStorage( ValueInfo[] infos,
                                                      DomainMapper[] dms ) {
                return baseCoord.inputStorage( infos, dms );
            }
        };
    }
}
