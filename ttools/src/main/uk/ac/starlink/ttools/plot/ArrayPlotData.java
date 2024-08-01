package uk.ac.starlink.ttools.plot;

import java.util.ArrayList;
import java.util.List;

/**
 * PlotData implementation which stores its data in an array of
 * {@link PointData} objects.
 *
 * @author   Mark Taylor
 * @since    9 Apr 2008
 */
public class ArrayPlotData implements PlotData {

    private final int nset_;
    private final String[] setNames_;
    private final Style[] setStyles_;
    private final int ndim_;
    private final int nerror_;
    private final boolean hasLabels_;
    private PointData[] points_;

    /**
     * Constructor.
     *
     * @param   nset  number of subsets 
     * @param   setNames   <code>nset</code>-element array of subset names
     * @param   setStyles  <code>nset</code>-element array of subset styles
     * @param   ndim    data point dimensionality
     * @param   nerror  number of error values for each point
     * @param   hasLabels  whether there are per-point text labels 
     * @param   points   point data array
     */
    @SuppressWarnings("this-escape")
    public ArrayPlotData( int nset, String[] setNames, Style[] setStyles, 
                          int ndim, int nerror, boolean hasLabels,
                          PointData[] points ) {
        nset_ = nset;
        setNames_ = setNames;
        setStyles_ = setStyles;
        ndim_ = ndim;
        nerror_ = nerror;
        hasLabels_ = hasLabels;
        setPoints( points );
    }

    public int getSetCount() {
        return nset_;
    }

    public String getSetName( int iset ) {
        return setNames_[ iset ];
    }

    public Style getSetStyle( int iset ) {
        return setStyles_[ iset ];
    }

    public int getNdim() {
        return ndim_;
    }

    public int getNerror() {
        return nerror_;
    }

    public boolean hasLabels() {
        return hasLabels_;
    }

    public PointSequence getPointSequence() {
        return new ArrayPointSequence();
    }

    /**
     * Sets the point array supplying the data for this object.
     *
     * @param   points  point array
     */
    public void setPoints( PointData[] points ) {
        points_ = points;
    }

    /**
     * Returns the point array supplying the data for this object.
     *
     * @return  point array
     */
    public PointData[] getPoints() {
        return points_;
    }

    /**
     * PointSequence implementation used by this object.
     */
    private class ArrayPointSequence implements PointSequence {

        private int ip_;

        /**
         * Constructor.
         */
        ArrayPointSequence() {
            ip_ = -1;
        }

        public boolean next() {
            return ++ip_ < points_.length;
        }

        public double[] getPoint() {
            return points_[ ip_ ].getPoint();
        }

        public double[][] getErrors() {
            return points_[ ip_ ].getErrors();
        }

        public String getLabel() {
            return points_[ ip_ ].getLabel();
        }

        public boolean isIncluded( int iset ) {
            return points_[ ip_ ].isIncluded( iset );
        }

        public void close() {
            ip_ = Integer.MIN_VALUE;
        }
    }

    /**
     * Factory method which constructs a new ArrayPlotData object 
     * with data which is a copy of that taken from a supplied 
     * <code>PlotData</code> object.
     *
     * @param  data  data object to copy
     * @return  new ArrayPlotData object
     */
    public static ArrayPlotData copyPlotData( PlotData data ) {
        int nset = data.getSetCount();
        String[] setNames = new String[ nset ];
        Style[] setStyles = new Style[ nset ];
        for ( int is = 0; is < nset; is++ ) {
            setNames[ is ] = data.getSetName( is );
            setStyles[ is ] = data.getSetStyle( is );
        }
        List<PointData> pointList = new ArrayList<PointData>();
        int ndim = data.getNdim();
        int nerror = data.getNerror();
        PointSequence pseq = data.getPointSequence();
        while ( pseq.next() ) {
            boolean[] isIncluded = new boolean[ nset ];
            for ( int is = 0; is < nset; is++ ) {
                isIncluded[ is ] = pseq.isIncluded( is );
            }
            double[] pointCopy = pseq.getPoint().clone();
            double[][] errors = pseq.getErrors();
            double[][] errorsCopy;
            if ( nerror > 0 && errors != null && errors.length > 0 ) {
                errorsCopy = new double[ errors.length ][];
                for ( int ierr = 0; ierr < errors.length; ierr++ ) {
                    double[] error = errors[ ierr ];
                    if ( error != null ) {
                        errorsCopy[ ierr ] = error.clone();
                    }
                }
            }
            else {
                errorsCopy = null;
            }
            pointList.add( new PointData( pointCopy, errorsCopy,
                                          pseq.getLabel(), isIncluded ) );
        }
        PointData[] points = pointList.toArray( new PointData[ 0 ] );
        return new ArrayPlotData( nset, setNames, setStyles, data.getNdim(),
                                  data.getNerror(), data.hasLabels(), points );
    }
}
