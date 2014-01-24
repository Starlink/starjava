package uk.ac.starlink.topcat.plot2;

import javax.swing.BoundedRangeModel;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Collects a set of SubClouds together to provide a description of a
 * collection of positions in a plot.
 *
 * @author   Mark Taylor
 * @since    24 Jan 2014
 */
public class GuiPointCloud {

    private final SubCloud[] subClouds_;
    private final DataStore baseDataStore_;
    private final BoundedRangeModel progModel_;
    private final long nrow_;

    /**
     * Constructor.
     *
     * @param  subClouds   per-layer position collections
     * @param  baseDataStore  data store supplying the position data
     * @param  progModel   progress bar model; if non-null, iteration over
     *                     the points will update it
     */
    public GuiPointCloud( SubCloud[] subClouds, DataStore baseDataStore,
                          BoundedRangeModel progModel ) {
        subClouds_ = subClouds;
        baseDataStore_ = baseDataStore;
        progModel_ = progModel;

        /* If we will be displaying progress, it is necessary to find out
         * how many points there are in the data set. */
        if ( progModel != null ) {
            long nr = 0;
            for ( int ic = 0; ic < subClouds.length; ic++ ) {
                nr += ((GuiDataSpec) subClouds[ ic ].getDataSpec())
                     .getRowCount();
            }
            nrow_ = nr;
        }
        else {
            nrow_ = -1;
        }
    }

    /**
     * Returns the subclouds aggregated by this point cloud.
     *
     * @return  subcloud array
     */
    public SubCloud[] getSubClouds() {
        return subClouds_;
    }

    /**
     * Returns a data store that can be used for iterating over
     * this point cloud.
     * This data store adds value to the supplied base data store:
     * it checks for thread interruptions and reports progress as appropriaate.
     *
     * @return  data store
     */
    public GuiDataStore createGuiDataStore() {
        return new GuiDataStore( baseDataStore_, progModel_, nrow_ );
    }

    /**
     * Returns an iterable over the point cloud.
     * This uses the GUI data store.
     *
     * @return   iterable over data positions
     */
    public Iterable<double[]> createDataPosIterable() {
        return new PointCloud( subClouds_ )
              .createDataPosIterable( createGuiDataStore() );
    }
}
