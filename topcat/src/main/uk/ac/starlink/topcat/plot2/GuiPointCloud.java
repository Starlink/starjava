package uk.ac.starlink.topcat.plot2;

import java.awt.Point;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.BoundedRangeModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.SubCloud;
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
    public Iterable<double[]>
            createDataPosIterable( final DataStore dataStore ) {
        return new Iterable<double[]>() {
            public Iterator<double[]> iterator() {
                return new DataPosIterator( dataStore );
            }
        };
    }

    /**
     * Iterator over data positions in this cloud.
     *
     * <p>This implementation is mostly copied from PointCloud, it would
     * be quite fiddly to subclass from the same code.
     */
    private class DataPosIterator implements Iterator<double[]> {
        private final DataStore dataStore_;
        private final Iterator<TableCloud> cloudIt_;
        private final double[] dpos_;
        private final double[] dpos1_;
        private final Point gp_;
        private DataGeom geom_;
        private int iPosCoord_;
        private TupleSequence tseq_;
        private boolean hasNext_;

        /**
         * Constructor.
         *
         * @param  dataStore  data storage object
         */
        DataPosIterator( DataStore dataStore ) {
            dataStore_ = dataStore;
            cloudIt_ = Arrays.asList( tclouds_ ).iterator();
            dpos_ = new double[ ndim_ ];
            dpos1_ = new double[ ndim_ ];
            gp_ = new Point();
            tseq_ = PlotUtil.EMPTY_TUPLE_SEQUENCE;
            hasNext_ = advance();
        }

        public boolean hasNext() {
            return hasNext_;
        }

        public double[] next() {
            if ( hasNext_ ) {
                System.arraycopy( dpos_, 0, dpos1_, 0, ndim_ );
                hasNext_ = advance();
                return dpos1_;
            }
            else {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Does work for the next iteration.
         */
        private boolean advance() {
            while ( tseq_.next() ) {
                if ( geom_.readDataPos( tseq_, iPosCoord_, dpos_ ) ) {
                    return true;
                }
            }
            while ( cloudIt_.hasNext() ) {
                TableCloud tcloud = cloudIt_.next();
                geom_ = tcloud.getDataGeom();
                iPosCoord_ = tcloud.getPosCoordIndex();
                tseq_ = tcloud.createTupleSequence( dataStore_ );
                return advance();
            }
            return false;
        }
    }
}
