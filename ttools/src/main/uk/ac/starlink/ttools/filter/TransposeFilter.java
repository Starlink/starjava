package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import uk.ac.starlink.table.AccessRowSequence;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.table.formats.RowEvaluator;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Processing filter which transposes a table.
 *
 * @author   Mark Taylor
 * @since    30 Jun 2006
 */
public class TransposeFilter extends BasicFilter {

    /** Metadata for column representing original column names. */
    public static final ColumnInfo HEADING_INFO =
        new ColumnInfo( "Heading", String.class,
                        "Name of the column of which this "
                      + "row is the transpose" );

    /**
     * Constructor.
     */
    public TransposeFilter() {
        super( "transpose", "[-namecol <col-id>]" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Transposes the input table so that columns become rows",
            "and vice versa.",
            "The <code>-namecol</code> flag can be used to specify a column",
            "in the input table which will provide the column names for",
            "the output table.",
            "The first column of the output table will contain the",
            "column names of the input table.",
            "</p>",
            explainSyntax( new String[] { "col-id", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        String namcol = null;
        while ( argIt.hasNext() ) {
            String arg = argIt.next();
            if ( arg.equals( "-namecol" ) && argIt.hasNext() ) {
                argIt.remove();
                namcol = argIt.next();
                argIt.remove();
            }
        }
        final String nameCol = namcol;
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                int iNameCol = nameCol == null
                            ? -1
                            : new ColumnIdentifier( base )
                             .getColumnIndex( nameCol );
                base = StoragePolicy.getDefaultPolicy().randomTable( base );
                return new TransposeTable( base, iNameCol );
            }
        };
    }

    /**
     * StarTable implementation for a transposed table.
     */
    private static class TransposeTable extends WrapperStarTable {

        private final StarTable base_;
        private final int jNameCol_;
        private final int nBaseCol_;
        private final int nBaseRow_;
        private final ColumnInfo[] colInfos_;
        private final RowEvaluator.Decoder<?>[] decoders_;

        /**
         * Constructor.
         *
         * @param   base  base table
         * @param   jNameCol  index of column in base table which will 
         *          provide column names of new table
         */
        TransposeTable( StarTable base, int jNameCol )
                throws IOException {
            super( base );
            base_ = base;
            jNameCol_ = jNameCol;
            ColumnInfo nameInfo = jNameCol >= 0
                                ? base.getColumnInfo( jNameCol )
                                : null;
            nBaseCol_ = base.getColumnCount();
            nBaseRow_ = checkedLongToInt( base.getRowCount() );

            /* Examine all the cells in the base table to find out the
             * metadata (column types) for the columns in the new, 
             * transposed table. */
            RowEvaluator rowEval = new RowEvaluator( nBaseRow_ );
            for ( int iBaseCol = 0; iBaseCol < nBaseCol_; iBaseCol++ ) {
                if ( iBaseCol != jNameCol ) {
                    String[] baseCol = new String[ nBaseRow_ ];
                    for ( int iBaseRow = 0; iBaseRow < nBaseRow_; iBaseRow++ ) {
                        baseCol[ iBaseRow ] =
                            asString( base_.getCell( iBaseRow, iBaseCol ) );
                    }
                    rowEval.submitRow( Arrays.asList( baseCol ) );
                }
            }
            RowEvaluator.Metadata meta = rowEval.getMetadata();
            assert meta.nrow_ == getRowCount();
            decoders_ = meta.decoders_;
            colInfos_ = meta.colInfos_;

            /* Set the column names from a selected column in the base table
             * if so requested. */
            for ( int iBaseRow = 0; iBaseRow < nBaseRow_; iBaseRow++ ) {
                String colName = null;
                if ( jNameCol_ >= 0 ) {
                    Object colNameObj = base_.getCell( iBaseRow, jNameCol_ );
                    colName = nameInfo.formatValue( colNameObj, 32 );
                }
                if ( Tables.isBlank( colName ) ) {
                    colName = "col" + ( iBaseRow + 1 );
                }
                colInfos_[ iBaseRow ].setName( colName );
            }
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return icol == 0 ? HEADING_INFO
                             : colInfos_[ icol - 1 ];
        }

        public boolean isRandom() {
            return true;
        }

        public int getColumnCount() {
            return 1 + nBaseRow_;
        }

        public long getRowCount() {
            return (long) ( nBaseCol_ - ( jNameCol_ >= 0 ? 1 : 0 ) );
        }

        public Object getCell( long lrow, int icol ) throws IOException {
            int iBaseCol = getBaseColumnForRow( lrow );
            if ( icol == 0 ) {
                return base_.getColumnInfo( iBaseCol ).getName();
            }
            else {
                Object baseCell = base_.getCell( icol - 1, iBaseCol );
                return Tables.isBlank( baseCell ) 
                     ? null
                     : decoders_[ icol - 1 ].decode( asString( baseCell ) );
            }
        }

        public Object[] getRow( long lrow ) throws IOException {
            int ncol = 1 + nBaseRow_;
            Object[] row = new Object[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                row[ icol ] = getCell( lrow, icol );
            }
            return row;
        }

        public RowAccess getRowAccess() throws IOException {
            final RowAccess baseAcc = base_.getRowAccess();
            final int ncol = getColumnCount();
            return new RowAccess() {
                long irow_ = -1;
                int iBaseCol_ = -1;
                final Object[] row_ = new Object[ ncol ];
                public void setRowIndex( long irow ) {
                    if ( irow != irow_ ) {
                        irow_ = irow;
                        iBaseCol_ = getBaseColumnForRow( irow );
                    }
                }
                public Object getCell( int icol ) throws IOException {
                    if ( icol == 0 ) {
                        return base_.getColumnInfo( iBaseCol_ ).getName();
                    }
                    else {
                        baseAcc.setRowIndex( icol - 1 );
                        Object baseCell = baseAcc.getCell( iBaseCol_ );
                        return Tables.isBlank( baseCell ) 
                             ? null
                             : decoders_[ icol - 1 ]
                              .decode( asString( baseCell ) );
                    }
                }
                public Object[] getRow() throws IOException {
                    for ( int icol = 0; icol < ncol; icol++ ) {
                        row_[ icol ] = getCell( icol );
                    }
                    return row_;
                }
                public void close() throws IOException {
                    baseAcc.close();
                }
            };
        }

        public RowSequence getRowSequence() throws IOException {
            return AccessRowSequence.createInstance( this );
        }

        public RowSplittable getRowSplittable() throws IOException {
            return Tables.getDefaultRowSplittable( this );
        }

        /**
         * Returns the column in the base table corresponding to a given
         * row in this table.
         *
         * @param   lrow  row index in this table
         * @return  column index in base table
         */
        private int getBaseColumnForRow( long lrow ) {
            int irow = checkedLongToInt( lrow );
            if ( irow < jNameCol_ || jNameCol_ < 0 ) {
                return irow;
            }
            else {
                return irow + 1;
            }
        }

        /**
         * Converts an object to a string, converting nulls to empty strings.
         *
         * @param   obj  object
         * @return  stringified <code>obj</code>
         */
        private static String asString( Object obj ) {
            return Tables.isBlank( obj ) ? null : obj.toString();
        }
    }
}
