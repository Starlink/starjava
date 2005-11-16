package uk.ac.starlink.topcat.plot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * Encapsulates the selection of the list of points which is to be plotted.
 * This may be composed of points from one or more than one tables.
 *
 * @author   Mark Taylor
 * @since    2 Nov 2005
 */
public class PointSelection {

    private final int ndim_;
    private final int nTable_;
    private final TopcatModel[] tcModels_;
    private final long[] nrows_;
    private final int[][] icols_;
    private final RowSubset[] subsets_;
    private final Style[] styles_;

    private static final double MILLISECONDS_PER_YEAR =
        365.25 * 24 * 60 * 60 * 1000;
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
     * @param  ndim  dimensionality of the points
     * @param  selectors  array of PointSelector objects whose current state
     *         determines the points to be plotted
     * @param  names   labels corresponding to the elements of the 
     *                 <code>selectors</code> array
     * @param  subsetPointers  pointers to subsets
     */
    public PointSelection( int ndim, PointSelector[] selectors, String[] names,
                           int[][] subsetPointers ) {
        ndim_ = ndim;
        nTable_ = selectors.length;
        for ( int i = 0; i < nTable_; i++ ) {
            if ( selectors[ i ].getNdim() != ndim_ ) {
                throw new IllegalArgumentException();
            }
        }

        /* Store the tables and column indices representing the data. */
        tcModels_ = new TopcatModel[ nTable_ ];
        nrows_ = new long[ nTable_ ];
        long[] offsets = new long[ nTable_ ];
        icols_ = new int[ nTable_ ][];
        long offset = 0L;
        for ( int itab = 0; itab < nTable_; itab++ ) {
            PointSelector psel = selectors[ itab ];
            tcModels_[ itab ] = psel.getTable();
            nrows_[ itab ] = tcModels_[ itab ].getDataModel().getRowCount();
            StarTableColumn[] cols = psel.getColumns();
            icols_[ itab ] = new int[ ndim_ ];
            for ( int idim = 0; idim < ndim_; idim++ ) {
                icols_[ itab ][ idim ] = cols[ idim ].getModelIndex();
            }
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
        List subsetList = new ArrayList();
        List styleList = new ArrayList();
        for ( int isub = 0; isub < subsetPointers.length; isub++ ) {
            int[] subsetPointer = subsetPointers[ isub ];
            int itab = subsetPointer[ 0 ];
            int itsub = subsetPointer[ 1 ];
            RowSubset rset = (RowSubset)
                             tcModels_[ itab ].getSubsets().get( itsub );
            rset = new OffsetRowSubset( rset, offsets[ itab ], nrows_[ itab ] );
            if ( names[ itab ] != null ) {
                ((OffsetRowSubset) rset).setName( names[ itab ] + "." 
                                                + rset.getName() );
            }
            subsetList.add( rset );
            styleList.add( selectors[ itab ].getStyle( itsub ) );
        }
        subsets_ = (RowSubset[]) subsetList.toArray( new RowSubset[ 0 ] );
        styles_ = (Style[]) styleList.toArray( new Style[ 0 ] );
    }

    /** 
     * Reads a data points list for this selection.  The data are actually
     * read from the table objects in this call, so it may be time-consuming.
     * So, don't call it if you already have the data it would return
     * (see {@link #sameData}).
     *
     * @return   points list
     */
    public Points readPoints() {
        int npoint = 0;
        for ( int itab = 0; itab < nTable_; itab++ ) {
            npoint += Tables.checkedLongToInt( tcModels_[ itab ]
                                              .getDataModel().getRowCount() );
        }
        ValueStorePoints points = new ValueStorePoints( ndim_, npoint );
        int ipoint = 0;
        double[] coords = new double[ ndim_ ];
        try {
            for ( int itab = 0; itab < nTable_; itab++ ) {
                int[] icols = icols_[ itab ];
                RowSequence rseq =
                    tcModels_[ itab ].getDataModel().getRowSequence();
                while ( rseq.next() ) {
                    for ( int idim = 0; idim < ndim_; idim++ ) {
                        coords[ idim ] = 
                            doubleValue( rseq.getCell( icols[ idim ] ) );
                    }
                    points.putCoords( ipoint++, coords );
                }
                rseq.close();
            }
        }
        catch ( IOException e ) {
            logger_.log( Level.SEVERE, "Data read error", e );
            return new EmptyPoints();
        }
        assert ipoint == npoint;
        return points;
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
        List ipList = new ArrayList();
        long offset = 0;
        for ( int itab = 0; itab < nTable_; itab++ ) {
            if ( tcModels_[ itab ] == tcModel ) {
                ipList.add( new Long( offset + lrow ) );
            }
            offset += nrows_[ itab ];
        }
        long[] ips = new long[ ipList.size() ];
        for ( int i = 0; i < ips.length; i++ ) {
            ips[ i ] = ((Long) ipList.get( i )).longValue();
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
        List tableList = new ArrayList();
        List indexList = new ArrayList();
        for ( int itab = 0; itab < nTable_; itab++ ) {
            TopcatModel tcModel = tcModels_[ itab ];
            if ( tcModel != null ) {
                int igrp = tableList.indexOf( tcModel );
                if ( igrp < 0 ) {
                    igrp = tableList.size();
                    tableList.add( tcModel );
                    indexList.add( new ArrayList() );
                }
                ((List) indexList.get( igrp )).add( new Integer( itab ) );
            }
        }
        int ngrp = tableList.size();
        assert ngrp == indexList.size();

        /* Now for each table, amalgamate any parts of the mask which
         * apply to it and if the result is a non-empty set of points
         * in that table, record the table/mask pair. */
        List tableMaskList = new ArrayList();
        for ( int igrp = 0; igrp < ngrp; igrp++ ) {
            TopcatModel tcModel = (TopcatModel) tableList.get( igrp );
            BitSet tMask = new BitSet();
            Integer[] ixs = (Integer[]) ((List) indexList.get( igrp ))
                                       .toArray( new Integer[ 0 ] );
            for ( int iix = 0; iix < ixs.length; iix++ ) {
                int itab = ixs[ iix ].intValue();
                tMask.or( pointMask.get( starts[ itab ], ends[ itab ] ) );
            }
            if ( tMask.cardinality() > 0 ) {
                tableMaskList.add( new TableMask( tcModel, tMask ) );
            }
        }
        return (TableMask[]) tableMaskList.toArray( new TableMask[ 0 ] );
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
        if ( other == null ) {
            return false;
        }
        if ( ! Arrays.equals( tcModels_, other.tcModels_ ) ) {
            return false;
        }
        for ( int i = 0; i < nTable_; i++ ) {
            if ( ! Arrays.equals( icols_[ i ], other.icols_[ i ] ) ) {
                return false;
            }
        }
        return true;
    }

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
            code = 23 * code + tcModels_[ itab ].hashCode();
            for ( int idim = 0; idim < ndim_; idim++ ) {
                code = 23 * icols_[ itab ][ idim ];
            }
        }
        for ( int i = 0; i < subsets_.length; i++ ) {
            code = 23 * code + subsets_[ i ].hashCode();
            code = 23 * code + styles_[ i ].hashCode();
        }
        return code;
    }

    /**
     * Returns a numeric (double) value for the given object where it
     * can be done.
     *
     * @param  value  value to decode
     * @return   double precision equivalent
     */
    private static double doubleValue( Object value ) {
        if ( value instanceof Number ) {
            return ((Number) value).doubleValue();
        }
        else if ( value instanceof Date ) {
            long milliseconds = ((Date) value).getTime();
            return 1970.0 + milliseconds / MILLISECONDS_PER_YEAR;
        }
        else {
            return Double.NaN;
        }
    }

    /**
     * Struct-type class which defines an association of a TopcatModel
     * and a BitSet.
     */
    public static class TableMask {
        private final TopcatModel tcModel_;
        private final BitSet mask_;
 
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
    private static class OffsetRowSubset implements RowSubset {

        private final RowSubset base_;
        private final long lolim_;
        private final long hilim_;
        private String name_;

        /**
         * Constructs a new OffsetRowSubset.
         *
         * @param  base  base row subset
         * @param  offset  offset value for the first row
         * @param  nrow   number of rows in the base subset
         */
        OffsetRowSubset( RowSubset base, long offset, long nrow ) {
            base_ = base;
            lolim_ = offset;
            hilim_ = offset + nrow - 1;
            setName( base_.getName() );
        }

        /**
         * Resets this subset's name.
         *
         * @param  name  new name
         */
        public void setName( String name ) {
            name_ = name;
        }

        public String getName() {
            return name_;
        }

        public boolean isIncluded( long lrow ) {
            return lrow >= lolim_
                && lrow <= hilim_
                && base_.isIncluded( lrow - lolim_ );
        }
    }

    /**
     * Points implementation with no data.
     */
    private class EmptyPoints implements Points {
        public int getNdim() {
            return ndim_;
        }
        public int getCount() {
            return 0;
        }
        public void getCoords( int ipoint, double[] coords ) {
            throw new IllegalArgumentException( "no data" );
        }
    }
}
