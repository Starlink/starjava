package uk.ac.starlink.ttools.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Represents an unordered set of data positions forming part of a plot.
 * An iterator over the points is provided.
 * Instances of this class may also be compared for equality:
 * if instances compare equal, the iterators will dispense the same positions,
 * though not necessarily in the same order.
 *
 * <p>The unordered semantics of this class is imposed by the equality
 * requirement.  Most of the ordering could be restored by rewording the
 * contract a bit differently if that becomes useful, but at time of
 * writing it's not needed.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
@Equality
public class PointCloud {

    private final int ndim_;
    private final SubCloud[] subClouds_;

    /**
     * Constructs a point cloud from an array of subclouds.
     * The order of the items in the array may determine the sequence
     * of point iteration, but does not affect equality with respect
     * to other instances.
     *
     * @param  subClouds  array of cloud components
     */
    public PointCloud( SubCloud[] subClouds ) {
        subClouds_ = subClouds;
        ndim_ = subClouds.length > 0
              ? subClouds[ 0 ].getDataGeom().getDataDimCount()
              : 0;
    }

    /**
     * Constructs a point cloud from a single subcloud.
     *
     * @param  subCloud  cloud component
     */
    public PointCloud( SubCloud subCloud ) {
        this( new SubCloud[] { subCloud } );
    }

    /**
     * Returns an iterable over data positions.
     * Iteration is over <code>dataDimCount</code>-element coordinate arrays
     * giving position in data space.
     *
     * @param  dataStore  data storage object
     * @return   iterable over usable data positions
     */
    public Supplier<CoordSequence>
            createDataPosSupplier( DataStore dataStore ) {
        int nc = subClouds_.length;
        DataPosSequence.PositionCloud[] pclouds =
            new DataPosSequence.PositionCloud[ nc ];
        for ( int ic = 0; ic < nc; ic++ ) {
            final SubCloud subCloud = subClouds_[ ic ];
            final DataSpec dataSpec = subCloud.getDataSpec();
            pclouds[ ic ] = new DataPosSequence.PositionCloud() {
                public int getPosCoordIndex() {
                    return subCloud.getPosCoordIndex();
                }
                public DataGeom getDataGeom() {
                    return subCloud.getDataGeom();
                }
                public TupleSequence createTupleSequence( DataStore dstore ) {
                    return dstore.getTupleSequence( dataSpec );
                }
                public long getTupleCount() {
                    return dataSpec.getSourceTable().getRowCount();
                }
            };
        }
        return () -> new DataPosSequence( ndim_, pclouds, dataStore );
    }

    /**
     * Returns an array of the subclouds in this point cloud.
     *
     * @return  subcloud array
     */
    public SubCloud[] getSubClouds() {
        return subClouds_;
    }

    @Override
    public int hashCode() {
        int code = 74433;
        code = 23 * code + unorderedHashCode( subClouds_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof PointCloud ) {
            PointCloud other = (PointCloud) o;
            assert unorderedEquals( this.subClouds_, other.subClouds_ )
                == unorderedEquals( other.subClouds_, this.subClouds_ );
            return unorderedEquals( this.subClouds_, other.subClouds_ );
        }
        else {
            return false;
        }
    }

    /**
     * Returns a hashcode for an array which depends on the elements
     * it contains, but not on their order.
     *
     * @param   array  array
     * @return  hash code
     */
    private static int unorderedHashCode( Object[] array ) {
        int code = 0;
        for ( int i = 0; i < array.length; i++ ) {
            code += array[ i ].hashCode();
        }
        return code;
    }

    /**
     * Equality determination for two arrays based on the elements
     * they contain, but not on their order.
     *
     * @param  array1  one array
     * @param  array2  other array
     * @return  true iff array1 and array2 have the same elements
     */
    private static boolean unorderedEquals( Object[] array1, Object[] array2 ) {
        if ( Arrays.equals( array1, array2 ) ) {
            return true;
        }
        else if ( array1.length != array2.length ) {
            return false;
        }
        else {
            List<Object> l2 = new ArrayList<Object>( Arrays.asList( array2 ) );
            for ( Object item : array1 ) {
                if ( ! l2.remove( item ) ) {
                    return false;
                }
            }
            assert l2.isEmpty();
            assert unorderedHashCode( array1 ) == unorderedHashCode( array2 );
            return true;
        }
    }
}
