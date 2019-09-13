package uk.ac.starlink.topcat.plot2;

import java.util.function.Supplier;
import javax.swing.BoundedRangeModel;
import uk.ac.starlink.ttools.plot2.CoordSequence;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.DataPosSequence;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Collects a set of TableClouds together to provide a description of a
 * collection of positions in a plot.
 *
 * @author   Mark Taylor
 * @since    24 Jan 2014
 */
public class GuiPointCloud {

    private final TableCloud[] tclouds_;
    private final int ndim_;
    private final DataStore baseDataStore_;
    private final BoundedRangeModel progModel_;
    private final long nrow_;

    /**
     * Constructor.
     *
     * @param  tclouds   per-table position collections
     * @param  baseDataStore  data store supplying the position data
     * @param  progModel   progress bar model; if non-null, iteration over
     *                     the points will update it
     */
    public GuiPointCloud( TableCloud[] tclouds, DataStore baseDataStore,
                          BoundedRangeModel progModel ) {
        tclouds_ = tclouds;
        ndim_ = tclouds.length > 0
              ? tclouds[ 0 ].getDataGeom().getDataDimCount()
              : 0;
        baseDataStore_ = baseDataStore;
        progModel_ = progModel;

        /* If we will be displaying progress, it is necessary to find out
         * how many points will be read to iterate over the data set. */
        if ( progModel != null ) {
            long nr = 0;
            for ( int ic = 0; ic < tclouds.length; ic++ ) {
                nr += tclouds[ ic ].getReadRowCount();
            }
            nrow_ = nr;
        }
        else {
            nrow_ = -1;
        }
    }

    /**
     * Returns the TableClouds aggregated by this point cloud.
     *
     * @return  table cloud array
     */
    public TableCloud[] getTableClouds() {
        return tclouds_;
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
     *
     * @param  dataStore  data store
     * @return   iterable over data positions
     */
    public Supplier<CoordSequence>
            createDataPosSupplier( DataStore dataStore ) {
        int nc = tclouds_.length;
        DataPosSequence.PositionCloud[] clouds =
            new DataPosSequence.PositionCloud[ nc ];
        for ( int i = 0; i < nc; i++ ) {
            final TableCloud tcloud = tclouds_[ i ];
            clouds[ i ] = new DataPosSequence.PositionCloud() {
                public int getPosCoordIndex() {
                    return tcloud.getPosCoordIndex();
                }
                public DataGeom getDataGeom() {
                    return tcloud.getDataGeom();
                }
                public TupleSequence createTupleSequence( DataStore dstore ) {
                    return tcloud.createTupleSequence( dstore );
                }
                public long getTupleCount() {
                    return tcloud.getReadRowCount();
                }
            };
        }
        return () -> new DataPosSequence( ndim_, clouds, dataStore );
    }
}
