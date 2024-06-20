package uk.ac.starlink.topcat.plot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.WrapperPlotData;

/**
 * Encapsulates the selection of the list of points which is to be plotted.
 * This may be composed of points from one or more than one tables.
 *
 * @author   Mark Taylor
 * @since    2 Nov 2005
 */
public class PointSelection implements PlotData {

    private final int ndim_;
    private final int nTable_;
    private final TopcatModel[] tcModels_;
    private final long[] nrows_;
    private final StarTable[] dataTables_;
    private final StarTable[] errorTables_;
    private final StarTable[] labelTables_;
    private final ErrorMode[] errorModes_;
    private final RowSubset[] subsets_;
    private final Style[] styles_;
    private final SetId[] setIds_;
    private final PointSelector mainSelector_;
    private Points points_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

    /**
     * Constructs a new selection.
     *
     * <p>As well as the point selectors themselves which hold almost all
     * the required state, an additional array, <code>subsetPointers</code>
     * is given to indicate in what order the subsets should be plotted.
     * Each element of this array is a two-element int array; the first
     * element is the index of the point selector, and the second element
     * the index of the subset within that selector.
     *
     * @param  selectors  array of PointSelector objects whose current state
     *         determines the points to be plotted
     * @param  subsetPointers  pointers to subsets
     * @param  subsetNames     labels to be used for the subsets in 
     *                         <code>subsetPointers</code>
     */
    public PointSelection( PointSelector[] selectors, int[][] subsetPointers,
                           String[] subsetNames ) {
        nTable_ = selectors.length;
        mainSelector_ = selectors[ 0 ];
        ndim_ = mainSelector_.getAxesSelector().getNdim();
        for ( int i = 0; i < nTable_; i++ ) {
            if ( selectors[ i ].getAxesSelector().getNdim() != ndim_ ) {
                throw new IllegalArgumentException();
            }
        }

        /* Store the tables and column indices representing the data. */
        errorModes_ = mainSelector_.getAxesSelector()
                                                 .getErrorModes().clone();
        tcModels_ = new TopcatModel[ nTable_ ];
        dataTables_ = new StarTable[ nTable_ ];
        errorTables_ = new StarTable[ nTable_ ];
        labelTables_ = new StarTable[ nTable_ ];
        nrows_ = new long[ nTable_ ];
        long[] offsets = new long[ nTable_ ];
        long offset = 0L;
        for ( int itab = 0; itab < nTable_; itab++ ) {
            PointSelector psel = selectors[ itab ];
            tcModels_[ itab ] = psel.getTable();
            AxesSelector axsel = psel.getAxesSelector();
            dataTables_[ itab ] = axsel.getData();
            errorTables_[ itab ] = axsel.getErrorData();
            labelTables_[ itab ] = axsel.getLabelData();
            nrows_[ itab ] = tcModels_[ itab ].getDataModel().getRowCount();
            offsets[ itab ] = offset;
            offset += nrows_[ itab ];
        }

        /* Store a list of subsets and corresponding styles which represent
         * which of the data points will be plotted and what they will 
         * look like.  This is done by flattening the (table,subsets)
         * array which is got from the point selector array.  The stored
         * subsets have to be offset by the start index of the row in 
         * their table.  This corresponds (and must correspond) to the
         * sequence in which the points are returned by the getPoints()
         * method. */
        List<RowSubset> subsetList = new ArrayList<RowSubset>();
        List<Style> styleList = new ArrayList<Style>();
        List<SetId> idList = new ArrayList<SetId>();
        for ( int isub = 0; isub < subsetPointers.length; isub++ ) {
            int[] subsetPointer = subsetPointers[ isub ];
            int itab = subsetPointer[ 0 ];
            int itsub = subsetPointer[ 1 ];
            RowSubset rset = tcModels_[ itab ].getSubsets().get( itsub );
            rset = new OffsetRowSubset( rset, offsets[ itab ], nrows_[ itab ],
                                        subsetNames[ isub ] );
            subsetList.add( rset );
            styleList.add( selectors[ itab ].getStyle( itsub ) );
            idList.add( new SetId( selectors[ itab ], itsub ) );
        }
        subsets_ = subsetList.toArray( new RowSubset[ 0 ] );
        styles_ = styleList.toArray( new Style[ 0 ] );
        setIds_ = idList.toArray( new SetId[ 0 ] );

        /* Set dummy points object. */
        points_ = getEmptyPoints();
    }

    /** 
     * Reads a data points list for this selection.  The data are actually
     * read from the table objects in this call, so it may be time-consuming.
     * So, don't call it if you already have the data it would return
     * (see {@link #sameData}).  If a progress bar model is supplied it
     * will be updated as the read progresses.
     *
     * <p>This method checks for interruption status on its calling thread.
     * If an interruption is made, it will cease calculating and throw
     * an InterruptedException.
     *
     * @param    progress bar model to be updated as read is done
     * @return   points list
     * @throws   InterruptedException  if the calling thread is interrupted
     */
    public Points readPoints( BoundedRangeModel progress )
            throws IOException, InterruptedException {
        int npoint = 0;
        for ( int itab = 0; itab < nTable_; itab++ ) {
            npoint += Tables.checkedLongToInt( tcModels_[ itab ]
                                              .getDataModel().getRowCount() );
        }
        PointStore pointStore =
            mainSelector_.getAxesSelector().createPointStore( npoint );
        if ( progress != null ) {
            progress.setMinimum( 0 );
            progress.setMaximum( npoint );
        }
        int step = Math.max( npoint / 100, 1000 );
        int ipoint = 0;
        for ( int itab = 0; itab < nTable_; itab++ ) {
            RowSequence datSeq = null;
            RowSequence errSeq = null;
            RowSequence labSeq = null;
            try {
                datSeq = dataTables_[ itab ].getRowSequence();
                if ( errorTables_[ itab ] != null ) {
                    errSeq = errorTables_[ itab ].getRowSequence();
                }
                if ( labelTables_[ itab ] != null ) {
                    labSeq = labelTables_[ itab ].getRowSequence();
                }
                while ( datSeq.next() ) {
                    Object[] datRow = datSeq.getRow();
                    Object[] errRow;
                    String label;
                    if ( errSeq == null ) {
                        errRow = null;
                    }
                    else {
                        boolean hasNext = errSeq.next();
                        assert hasNext;
                        errRow = errSeq.getRow();
                    }
                    if ( labSeq == null ) {
                        label = null;
                    }
                    else {
                        boolean hasNext = labSeq.next();
                        assert hasNext;
                        Object obj = labSeq.getCell( 0 );
                        label = obj == null ? null : obj.toString();
                    }
                    pointStore.storePoint( datRow, errRow, label );
                    ipoint++;
                    if ( ipoint % step == 0 ) {
                        if ( progress != null ) {
                            progress.setValue( ipoint );
                        }
                        if ( Thread.interrupted() ) {
                            throw new InterruptedException();
                        }
                    }
                }
                assert ( errSeq == null ) || ( ! errSeq.next() );
                assert ( labSeq == null ) || ( ! labSeq.next() );
            }
            finally {
                if ( datSeq != null ) {
                    assert ! datSeq.next();
                    datSeq.close();
                }
                if ( errSeq != null ) {
                    assert ! errSeq.next();
                    errSeq.close();
                }
                if ( labSeq != null ) {
                    assert ! labSeq.next();
                    labSeq.close();
                }
            }
        }
        assert ipoint == npoint;
        return pointStore;
    }

    /**
     * Returns a dummy Points object compatible with this selection.
     * It contains no data.
     *
     * @return   points object with <code>getCount()==0</code>
     */
    public Points getEmptyPoints() {
        return new EmptyPoints();
    }

    /**
     * Returns a list of subsets to be plotted.  The row indices used by these
     * subsets correspond to the row sequence returned by a call to
     * {@link #readPoints}.
     *
     * @return  subset array
     */
    public RowSubset[] getSubsets() {
        return subsets_;
    }

    /**
     * Returns a list of styles for subset plotting.
     * This corresponds to the subset list returned by 
     * {@link #getSubsets}.
     *
     * @return  style array
     */
    public Style[] getStyles() {
        return styles_;
    }

    public int getSetCount() {
        return subsets_.length;
    }

    public String getSetName( int iset ) {
        return subsets_[ iset ].getName();
    }

    public Style getSetStyle( int iset ) {
        return styles_[ iset ];
    }

    public int getNdim() {
        return points_.getNdim();
    }

    public int getNerror() {
        return points_.getNerror();
    }

    public boolean hasLabels() {
        return points_.hasLabels();
    }

    /**
     * Returns a PlotData object based on this point selection but with a 
     * given points object.  Since PointSelection implements PlotData in
     * any case, this is not always necessary, but what this method provides
     * is a PlotData whose data will not change if the points object owned
     * by this PointSelection is replaced.
     *
     * @param  points  fixed points data
     * @return   plot data closure
     */
    public PlotData createPlotData( final Points points ) {
        return new WrapperPlotData( this ) {
            public int getNdim() {
                return points.getNdim();
            }
            public int getNerror() {
                return points.getNerror();
            }
            public boolean hasLabels() {
                return points.hasLabels();
            }
            public PointSequence getPointSequence() {
                return new SelectionPointSequence( points );
            }
        };
    }

    public PointSequence getPointSequence() {
        return new SelectionPointSequence( points_ );
    }

    public void setPoints( Points points ) {
        points_ = points;
    }

    public Points getPoints() {
        return points_;
    }

    /**
     * Returns the list of set ID labels which identify where each set 
     * comes from.  This corresponds to the subset list returned by
     * {@link #getSubsets}.
     *
     * @return   set ID array
     */
    public SetId[] getSetIds() {
        return setIds_;
    }

    /**
     * Given a point index from this selection, returns the table
     * that it comes from.
     *
     * @param  ipoint  point index
     * @return   topcat model that the point is from
     * @see   #getPointRow
     */
    public TopcatModel getPointTable( long ipoint ) {
        for ( int itab = 0; itab < nTable_; itab++ ) {
            if ( ipoint >= 0 && ipoint < nrows_[ itab ] ) {
                return tcModels_[ itab ];
            }
            ipoint -= nrows_[ itab ];
        }
        return null;
    }

    /**
     * Given a point index from this selection, returns the row number
     * in its table (see {@link #getPointTable} that it represents.
     *
     * @param  ipoint  point index
     * @return  row number of point index in its table
     */
    public long getPointRow( long ipoint ) {
        for ( int itab = 0; itab < nTable_; itab++ ) {
            if ( ipoint >= 0 && ipoint < nrows_[ itab ] ) {
                return ipoint;
            }
            ipoint -= nrows_[ itab ];
        }
        return -1L;
    }

    /**
     * Given a table and a row index into that table, returns the point
     * indices of any points in this selection which correspond to that row.
     *
     * @param   tcModel  table 
     * @param   lrow   row index in <code>tcModel</code>
     * @return   array of point indices for that row
     */
    public long[] getPointsForRow( TopcatModel tcModel, long lrow ) {
        List<Long> ipList = new ArrayList<Long>();
        long offset = 0;
        for ( int itab = 0; itab < nTable_; itab++ ) {
            if ( tcModels_[ itab ] == tcModel ) {
                ipList.add( Long.valueOf( offset + lrow ) );
            }
            offset += nrows_[ itab ];
        }
        long[] ips = new long[ ipList.size() ];
        for ( int i = 0; i < ips.length; i++ ) {
            ips[ i ] = ipList.get( i ).longValue();
        }
        return ips;
    }

    /**
     * Given a bit vector which represents a selection of the points
     * in this object, returns an array of TableMask objects which
     * represent selections of rows within any of the tables this
     * object knows about.
     * 
     * @param  pointMask  bit vector reprsenting a subset of the points
     *         in this object
     * @return  array of table/mask pairs representing non-empty row subsets
     */
    public TableMask[] getTableMasks( BitSet pointMask ) {

        /* Fill some lookup tables. */
        int[] starts = new int[ nTable_ ];
        int[] ends = new int[ nTable_ ];
        long offset = 0;
        for ( int itab = 0; itab < nTable_; itab++ ) {
            starts[ itab ] = (int) Math.min( offset, Integer.MAX_VALUE );
            offset += nrows_[ itab ];
            ends[ itab ] = (int) Math.min( offset, Integer.MAX_VALUE );
        }

        /* Arrange the selection elements by table.  This gives us parallel
         * arrays of tables and indices; each element of the indices list
         * is a list of the table indices which correspond to that table.
         * The point of this is so that we can tackle all the selection 
         * elements corresponding to a single table at once. */
        List<TopcatModel> tableList = new ArrayList<TopcatModel>();
        List<List<Integer>> indexList = new ArrayList<List<Integer>>();
        for ( int itab = 0; itab < nTable_; itab++ ) {
            TopcatModel tcModel = tcModels_[ itab ];
            if ( tcModel != null ) {
                int igrp = tableList.indexOf( tcModel );
                if ( igrp < 0 ) {
                    igrp = tableList.size();
                    tableList.add( tcModel );
                    indexList.add( new ArrayList<Integer>() );
                }
                indexList.get( igrp ).add( Integer.valueOf( itab ) );
            }
        }
        int ngrp = tableList.size();
        assert ngrp == indexList.size();

        /* Now for each table, amalgamate any parts of the mask which
         * apply to it and if the result is a non-empty set of points
         * in that table, record the table/mask pair. */
        List<TableMask> tableMaskList = new ArrayList<TableMask>();
        for ( int igrp = 0; igrp < ngrp; igrp++ ) {
            TopcatModel tcModel = tableList.get( igrp );
            BitSet tMask = new BitSet();
            for ( Integer itObj : indexList.get( igrp ) ) {
                int itab = itObj.intValue();
                tMask.or( pointMask.get( starts[ itab ], ends[ itab ] ) );
            }
            if ( tMask.cardinality() > 0 ) {
                tableMaskList.add( new TableMask( tcModel, tMask ) );
            }
        }
        return tableMaskList.toArray( new TableMask[ 0 ] );
    }

    /**
     * Determines if the axes defining this point selection are the same
     * as those for another one.  This resembles {@link #sameData},
     * except that PointSelections with different error or label information
     * can be considered to have the same axes.
     *
     * @param  other  comparison object
     * @return  true  iff the data axes are the same
     */
    public boolean sameAxes( PointSelection other ) {
        return other != null
            && Arrays.equals( this.dataTables_, other.dataTables_ );
    }

    /**
     * Determines if the data required to plot this point selection 
     * is the same as the data required to plot another one.
     * More exactly, it returns true only if {@link #readPoints} will
     * return the same result for this object and <code>other</code>.
     *
     * @param  other  comparison object
     * @return  true  iff the data is the same
     */
    public boolean sameData( PointSelection other ) {
        return other != null 
            && Arrays.equals( this.dataTables_, other.dataTables_ )
            && Arrays.equals( this.errorTables_, other.errorTables_ )
            && Arrays.equals( this.errorModes_, other.errorModes_ )
            && Arrays.equals( this.labelTables_, other.labelTables_ );
    }

    /**
     * Equals is implemented efficiently to identify two PointSelection 
     * objects which will behave in the same way.
     */
    public boolean equals( Object otherObject ) {
        if ( ! ( otherObject instanceof PointSelection ) ) {
            return false;
        }
        PointSelection other = (PointSelection) otherObject;
        return sameData( other )
            && Arrays.equals( subsets_, other.subsets_ )
            && Arrays.equals( styles_, other.styles_ );
    }

    public int hashCode() {
        int code = 555;
        for ( int itab = 0; itab < nTable_; itab++ ) {
            code = 23 * code + dataTables_[ itab ].hashCode();
            code = 23 * code + ( errorTables_[ itab ] == null
                                 ? 99 : errorTables_[ itab ].hashCode() );
            code = 23 * code + ( labelTables_[ itab ] == null
                                 ? 199 : labelTables_[ itab ].hashCode() );
        }
        for ( int ie = 0; ie < errorModes_.length; ie++ ) {
            code = 23 * code + errorModes_[ ie ].hashCode();
        }
        for ( int is = 0; is < subsets_.length; is++ ) {
            code = 23 * code + subsets_[ is ].hashCode();
            code = 23 * code + styles_[ is ].hashCode();
        }
        return code;
    }

    /**
     * PointSequence implementation used by a PointSelection.
     */
    private final class SelectionPointSequence implements PointSequence {

        private final Points psPoints_;
        private final int npoint_;
        private int ip_ = -1;

        /**
         * Constructor.
         */
        SelectionPointSequence( Points points ) {
            psPoints_ = points == null ? new EmptyPoints() : points;
            npoint_ = psPoints_.getCount();
        }

        public boolean next() {
            return ++ip_ < npoint_;
        }

        public double[] getPoint() {
            return psPoints_.getPoint( ip_ );
        }

        public double[][] getErrors() {
            return psPoints_.getErrors( ip_ );
        }

        public String getLabel() {
            return psPoints_.getLabel( ip_ );
        }

        public boolean isIncluded( int iset ) {
            return subsets_[ iset ].isIncluded( ip_ );
        }

        public void close() {
            ip_ = Integer.MIN_VALUE;
            return;
        }
    }

    /**
     * Struct-type class which defines an association of a TopcatModel
     * and a BitSet.
     */
    public static class TableMask {
        private final TopcatModel tcModel_;
        private final BitSet mask_;
 
        /**
         * Constructor.
         *
         * @param  tcModel   table model
         * @param  mask   bit vector
         */
        private TableMask( TopcatModel tcModel, BitSet mask ) {
            tcModel_ = tcModel;
            mask_ = mask;
        }

        /**
         * Returns the table.
         *
         * @return  topcat model
         */
        public TopcatModel getTable() {
            return tcModel_;
        }

        /**
         * Returns the bit mask.
         *
         * @return  bit set
         */
        public BitSet getMask() {
            return mask_;
        }
    }

    /**
     * RowSubset implementation which takes the behaviour of an existing
     * RowSubset and offsets its indices by a fixed value.
     * It also applies a mask, so that any queries outside its original
     * region of applicability are returned false.
     */
    private static class OffsetRowSubset extends RowSubset {

        private final RowSubset base_;
        private final long lolim_;
        private final long hilim_;

        /**
         * Constructs a new OffsetRowSubset.
         *
         * @param  base  base row subset
         * @param  offset  offset value for the first row
         * @param  nrow   number of rows in the base subset
         * @param  name  subset name
         */
        OffsetRowSubset( RowSubset base, long offset, long nrow, String name ) {
            super( name );
            base_ = base;
            lolim_ = offset;
            hilim_ = offset + nrow - 1;
        }

        public boolean isIncluded( long lrow ) {
            return lrow >= lolim_
                && lrow <= hilim_
                && base_.isIncluded( lrow - lolim_ );
        }

        public boolean equals( Object o ) {
            if ( o instanceof OffsetRowSubset ) {
                OffsetRowSubset other = (OffsetRowSubset) o;
                return this.base_.equals( other.base_ )
                    && this.getName().equals( other.getName() )
                    && this.lolim_ == other.lolim_
                    && this.hilim_ == other.hilim_;
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = 555;
            code = 23 * code + base_.hashCode();
            code = 23 * code + getName().hashCode();
            code = 23 * code + (int) lolim_;
            code = 23 * code + (int) hilim_;
            return code;
        }
    }

    /**
     * Points implementation with no data.
     */
    private class EmptyPoints implements Points {
        public int getNdim() {
            return ndim_;
        }
        public int getNerror() {
            return 0;
        }
        public int getCount() {
            return 0;
        }
        public double[] getPoint( int ipoint ) {
            throw new IllegalArgumentException( "no data" );
        }
        public double[][] getErrors( int ipoint ) {
            throw new IllegalArgumentException( "no data" );
        }
        public boolean hasLabels() {
            return false;
        }
        public String getLabel( int ipoint ) {
            return null;
        }
    }
}
