package uk.ac.starlink.ttools.plot;

import java.util.ArrayList;
import java.util.List;

/**
 * PlotData implementation which aggregates a set of constituent PlotDatas.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public class MultiPlotData implements PlotData {

    private final PlotData[] datas_;
    private final int ndim_;
    private final int nerror_;
    private final boolean hasLabels_;
    private final SetLoc[] setLocs_;

    /**
     * Constructor.
     *
     * @param   datas   constituent plot data objects
     */
    public MultiPlotData( PlotData[] datas ) {
        datas_ = (PlotData[]) datas.clone();
        int ndata = datas_.length;
        ndim_ = datas_[ 0 ].getNdim();
        int nerror = 0;
        boolean hasLabels = false;
        List setLocList = new ArrayList();
        for ( int id = 0; id < ndata; id++ ) {
            PlotData data = datas_[ id ];
            if ( data.getNdim() != ndim_ ) {
                throw new IllegalArgumentException( "Inconsistent ndims" );
            }
            nerror = Math.max( nerror, data.getNerror() );
            hasLabels = hasLabels || data.hasLabels();
            for ( int is = 0; is < data.getSetCount(); is++ ) {
                setLocList.add( new SetLoc( id, is ) );
            }
        }
        nerror_ = nerror;
        hasLabels_ = hasLabels;
        setLocs_ = (SetLoc[]) setLocList.toArray( new SetLoc[ 0 ] );
    }

    public int getNdim() {
        return ndim_;
    }

    public int getNerror() {
        return nerror_;
    }

    public int getSetCount() {
        return setLocs_.length;
    }

    public String getSetName( int iset ) {
        SetLoc loc = setLocs_[ iset ];
        return datas_[ loc.id_ ].getSetName( loc.is_ );
    }

    public Style getSetStyle( int iset ) {
        SetLoc loc = setLocs_[ iset ];
        return datas_[ loc.id_ ].getSetStyle( loc.is_ );
    }

    public boolean hasLabels() {
        return hasLabels_;
    }

    public PointSequence getPointSequence() {
        return new MultiPointSequence();
    }

    /**
     * Implements PointSequence for MultiPlotData.
     */
    private class MultiPointSequence implements PointSequence {
        private int idata_;
        private PointSequence pseq_;
        private int loSet_;
        private int hiSet_;

        public boolean next() {
            while ( pseq_ != null || idata_ < datas_.length ) {
                if ( pseq_ == null ) {
                    loSet_ = hiSet_;
                    hiSet_ += datas_[ idata_ ].getSetCount();
                    pseq_ = datas_[ idata_ ].getPointSequence();
                    idata_++;
                }
                if ( pseq_.next() ) {
                    return true;
                }
                else {
                    pseq_.close();
                    pseq_ = null;
                }
            }
            return false;
        }

        public double[] getPoint() {
            return pseq_.getPoint();
        }

        public double[][] getErrors() {
            return pseq_.getErrors();
        }

        public String getLabel() {
            return pseq_.getLabel();
        }

        public boolean isIncluded( int iset ) {
            return iset >= loSet_ && iset < hiSet_
                 ? pseq_.isIncluded( iset - loSet_ )
                 : false;
        }

        public void close() {
            if ( pseq_ != null ) {
                pseq_.close();
            }
        }
    }

    /**
     * Utility class which stores information about a constituent 
     * subset within this object.
     */
    private static class SetLoc {
        final int id_;
        final int is_;

        /**
         * Constructor.
         *
         * @param  id  index of constituent PlotData
         * @param  is  index of subset within the constituent PlotData
         */
        SetLoc( int id, int is ) {
            id_ = id;
            is_ = is;
        }
    }
}
