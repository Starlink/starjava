package uk.ac.starlink.ttools.plot2.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * DataGeom implementation that reads positions corresponding to some,
 * but not all, orthogonal data position coordinates.
 *
 * @author   Mark Taylor
 * @since    16 Jul 2013
 */
public class SliceDataGeom implements DataGeom {

    private final int dataDimCount_;
    private final FloatingCoord[] sliceCoords_;
    private final boolean[] sliceFlags_;
    private final Coord[] posCoords_;
    private final String variantName_;

    /**
     * Constructor.
     * The supplied array of coordinates should contain an element for
     * each of the data position coordinates, but some of those elements
     * may be null, to indicate that no positioning is done in that
     * dimension.  Coordinates read into a data position array in
     * the corresponding dimensions will be given as NaNs.
     *
     * @param   sliceCoords  per-data dimension array of coords,
     *                       some elements may be null
     * @param   variantName  variant name
     */
    public SliceDataGeom( FloatingCoord[] sliceCoords, String variantName ) {
        sliceCoords_ = sliceCoords;
        dataDimCount_ = sliceCoords.length;
        sliceFlags_ = new boolean[ dataDimCount_ ];
        List<Coord> posCoordList = new ArrayList<Coord>();
        for ( int i = 0; i < dataDimCount_; i++ ) {
            Coord coord = sliceCoords[ i ];
            if ( coord != null ) {
                posCoordList.add( coord );
                sliceFlags_[ i ] = true;
            }
        }
        posCoords_ = posCoordList.toArray( new Coord[ 0 ] );
        variantName_ = variantName;
    }

    public int getDataDimCount() {
        return dataDimCount_;
    }

    public String getVariantName() {
        return variantName_;
    }

    /**
     * Returns an array of the non-null coords.
     */
    public Coord[] getPosCoords() {
        return posCoords_;
    }

    public boolean readDataPos( Tuple tuple, int ic, double[] dpos ) {
        for ( int i = 0; i < dataDimCount_; i++ ) {
            if ( sliceFlags_[ i ] ) {
                double d = sliceCoords_[ i ].readDoubleCoord( tuple, ic++ );
                if ( Double.isNaN( d ) ) {
                    return false;
                }
                else {
                    dpos[ i ] = d;
                }
            }
            else {
                dpos[ i ] = Double.NaN;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int code = 771;
        code = 23 * code + Arrays.hashCode( sliceCoords_ );
        code = 23 * code + variantName_.hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof SliceDataGeom ) {
            SliceDataGeom other = (SliceDataGeom) o;
            return Arrays.equals( this.sliceCoords_, other.sliceCoords_ )
                && this.variantName_.equals( other.variantName_ );
        }
        else {
            return false;
        }
    }
}
