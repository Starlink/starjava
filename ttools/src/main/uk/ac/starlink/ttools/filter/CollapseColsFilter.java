package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.RowData;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Table filter for collapsing multiple scalar columns into an array column.
 *
 * @author   Mark Taylor
 * @since    7 Sep 2018
 */
public class CollapseColsFilter extends BasicFilter {

    /**
     * Constructor.
     */
    public CollapseColsFilter() {
        super( "collapsecols",
               "[-[no]keepscalars] <array-colname> <col-id0> <ncol>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Adds a new array-valued column",
            "by using the values from a specified range of scalar columns",
            "as array elements.",
            "The new column is named <code>&lt;array-colname&gt;</code>,",
            "and produced from the sequence of <code>&lt;ncol&gt;</code>",
            "scalar columns starting with <code>&lt;col-id0&gt;</code>.",
            "</p>",
            "<p>The array type of the output column is determined by the type",
            "of the first input column (<code>&lt;col-id0&gt;</code>).",
            "If it is of type <code>Double</code>,",
            "the output array column will be a <code>double[]</code> array,",
            "and similarly for types <code>Long</code>, <code>Integer</code>,",
            "<code>Float</code> and <code>Boolean</code>.",
            "Other integer types are currently mapped to <code>int[]</code>,",
            "and object types, e.g. <code>String</code>,",
            "to the corresponding array type.",
            "Array elements for null or mistyped input values",
            "are mapped to NaN for floating point types, but",
            "<strong><em>note</em></strong> that they currently",
            "just turn into zeros for integer array types",
            "and <code>false</code> for boolean.",
            "</p>",
            "<p>By default the scalar columns that have been used are removed",
            "from the output table and the new column replaces them",
            "at the same position.",
            "However, if you supply the <code>-keepscalars</code> flag",
            "they will be retained alongside the new array column",
            "(the new column will appear just after the run of scalar",
            "columns).",
            "</p>",
            "<p>This filter does the opposite of",
            "<ref id='explodecols'><code>explodecols</code></ref>.",
            "</p>",
            explainSyntax( new String[] { "col-id0", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        String colName = null;
        String colId0 = null;
        int ncol = -1;
        boolean keepScalars = false;
        while ( argIt.hasNext() &&
                ( colName == null || colId0 == null || ncol < 0 ) ) {
            String arg = argIt.next();
            if ( arg.toLowerCase().startsWith( "-keepscalar" ) ) {
                argIt.remove();
                keepScalars = true;
            }
            else if ( arg.toLowerCase().startsWith( "-nokeepscalar" ) ) {
                argIt.remove();
                keepScalars = false;
            }
            else if ( colName == null ) {
                argIt.remove();
                colName = arg;
            }
            else if ( colId0 == null ) {
                argIt.remove();
                colId0 = arg;
            }
            else if ( ncol < 0 ) {
                argIt.remove();
                try {
                    ncol = Integer.parseInt( arg );
                }
                catch ( NumberFormatException e ) {
                    throw new ArgException( "Non-numeric <ncol>: \""
                                          + arg + "\"" );
                }
                if ( ncol < 0 ) {
                    throw new ArgException( "Negative <ncol>: " + ncol );
                }
            }
        }
        if ( ncol < 0 ) {
            throw new ArgException( "Bad " + getName() + " specification" );
        }
        return new CollapseStep( colName, colId0, ncol, keepScalars );
    }

    /**
     * ProcessingStep implementation for collapsecols.
     */
    private static class CollapseStep implements ProcessingStep {

        private final String colName_;
        private final String colId0_;
        private final int ncol_;
        private final boolean keepScalars_;

        /**
         * Constructor.
         *
         * @param  colName  name of array column to create
         * @param  colId0   column ID for first scalar input column
         * @param  ncol     number of scalar input columns
         * @param  keepScalars  if true reatain scalar columns,
         *                      otherwise remove them
         */
        CollapseStep( String colName, String colId0, int ncol,
                      boolean keepScalars ) {
            colName_ = colName;
            colId0_ = colId0;
            ncol_ = ncol;
            keepScalars_ = keepScalars;
        }

        public StarTable wrap( StarTable base ) throws IOException {

            /* Create a table with the new array column appended at the end. */
            int icol0 = new ColumnIdentifier( base )
                       .getColumnIndex( colId0_ );
            ColumnSupplement arraySup =
                createArrayColumnSupplement( base, colName_, icol0, ncol_ );
            int nc0 = base.getColumnCount();
            StarTable extTable = new AddColumnsTable( base, arraySup, nc0 );

            /* Prepare and return an output table with the required
             * columns selected from the appended table. */
            int[] colMap = new int[ nc0 + 1 - ( keepScalars_ ? 0 : ncol_ ) ];
            int jc = 0;
            for ( int ic = 0; ic < icol0; ic++ ) {
                colMap[ jc++ ] = ic;
            }
            if ( keepScalars_ ) {
                for ( int ic = 0; ic < ncol_; ic++ ) {
                    colMap[ jc++ ] = icol0 + ic;
                }
            }
            int icArray = jc;
            colMap[ jc++ ] = nc0;
            for ( int ic = icol0 + ncol_; ic < nc0; ic++ ) {
                colMap[ jc++ ] = ic;
            }
            StarTable out = new ColumnPermutedStarTable( extTable, colMap );
            AddColumnFilter.checkDuplicatedName( out, icArray );
            return out;
        }
    }

    /**
     * Returns an object containing the data and metadata for an
     * array column representing a sequence of input columns from
     * a base table.
     *
     * @param  table    input table
     * @param  colName  name of array column to create
     * @param  colId0   column ID for first scalar input column
     * @param  ncol     number of scalar input columns
     */
    private static ColumnSupplement
            createArrayColumnSupplement( StarTable table, String colName,
                                         int icol0, int ncol ) {
        Class<?> eclazz = table.getColumnInfo( icol0 ).getContentClass();

        /* Long (to long[]) and then other integer types (to int[]).
         * Note 0 is used for missing or mistyped values. */
        if ( Long.class.equals( eclazz ) ) {
            return new ArrayColumnSupplement<long[]>( table, colName, icol0,
                                                      ncol, long[].class ) {
                protected void setElement( long[] array, int index,
                                           Object value ) {
                    array[ index ] = value instanceof Number
                                   ? ((Number) value).longValue()
                                   : 0;
                }
            };
        }
        else if ( Integer.class.equals( eclazz ) ||
                  Short.class.equals( eclazz ) ||
                  Byte.class.equals( eclazz ) ) {
            return new ArrayColumnSupplement<int[]>( table, colName, icol0,
                                                     ncol, int[].class ) {
                protected void setElement( int[] array, int index,
                                           Object value ) {
                    array[ index ] = value instanceof Number
                                   ? ((Number) value).intValue()
                                   : 0;
                }
            };
        }

        /* Floating point types. */
        else if ( Float.class.equals( eclazz ) ) {
            return new ArrayColumnSupplement<float[]>( table, colName, icol0,
                                                       ncol, float[].class ) {
                protected void setElement( float[] array, int index,
                                           Object value ) {
                    array[ index ] = value instanceof Number
                                   ? ((Number) value).floatValue()
                                   : Float.NaN;
                }
            };
        }
        else if ( Number.class.isAssignableFrom( eclazz ) ) {
            return new ArrayColumnSupplement<double[]>( table, colName, icol0,
                                                        ncol, double[].class ) {
                protected void setElement( double[] array, int index,
                                           Object value ) {
                    array[ index ] = value instanceof Number
                                   ? ((Number) value).doubleValue()
                                   : Double.NaN;
                }
            };
        }

        /* Boolean type. */
        else if ( Boolean.class.equals( eclazz ) ) {
            return new ArrayColumnSupplement<boolean[]>( table, colName, icol0,
                                                         ncol,
                                                         boolean[].class ) {
                protected void setElement( boolean[] array, int index,
                                           Object value ) {
                    array[ index ] = Boolean.TRUE.equals( value );
                }
            };
        }

        /**
         * Other (non-primitive) types, including string.
         */
        else {
            Class<?> aclazz = Array.newInstance( eclazz, 0 ).getClass();
            return createGenericArrayColumnSupplement( table, colName, icol0,
                                                       ncol, aclazz );
        }
    }

    /**
     * Returns an array column object for a non-primitive type.
     */
    private static <A> ColumnSupplement
            createGenericArrayColumnSupplement( StarTable table,
                                                String colName, int icol0,
                                                int ncol, Class<A> aclazz ) {
        final Class<?> eclazz = aclazz.getComponentType();
        return new ArrayColumnSupplement<A>( table, colName, icol0, ncol,
                                             aclazz ) {
            protected void setElement( A array, int index, Object value ) {
                Array.set( array, index,
                           eclazz.isInstance( value ) ? value : null );
            }
        };
    }

    /**
     * Partial implementation of column data and metadata for an array
     * column based on a sequence of scalar columns.
     * Subclasses just need to specify the array type and provide an
     * implementation of {@link #setElement}.
     */
    private static abstract class ArrayColumnSupplement<A>
            implements ColumnSupplement {

        private final StarTable table_;
        private final int icol0_;
        private final int ncol_;
        private final Class<A> aclazz_;
        private final Class<?> eclazz_;
        private final ColumnInfo info_;

        /**
         * Constructor.
         *
         * @param  table  input table
         * @param  colName  name of output array column
         * @param  icol0    index of first scalar input column
         * @param  ncol     number of scalar input columns
         * @param  aclazz   array class of output column
         */
        ArrayColumnSupplement( StarTable table, String colName, int icol0,
                               int ncol, Class<A> aclazz ) {
            table_ = table;
            icol0_ = icol0;
            ncol_ = ncol;
            aclazz_ = aclazz;
            info_ = new ColumnInfo( colName, aclazz, null );
            info_.setShape( new int[] { ncol } );
            eclazz_ = aclazz_.getComponentType();
        }

        /**
         * Inserts an input scalar value at a given position in an array
         * that forms the output from this column.
         *
         * @param  array  target array
         * @param  index  index into array which should be modified
         * @param  value  scalar value to be entered as array element
         */
        protected abstract void setElement( A array, int index, Object value );

        public int getColumnCount() {
            return 1;
        }

        public ColumnInfo getColumnInfo( int icol ) {
            if ( icol == 0 ) {
                return info_;
            }
            else {
                throw new IndexOutOfBoundsException();
            }
        }

        public Object getCell( long irow, int icol ) throws IOException {
            if ( icol == 0 ) {
                A array = createArray();
                for ( int i = 0; i < ncol_; i++ ) {
                    setElement( array, i, table_.getCell( irow, icol0_ + i ) );
                }
                return array;
            }
            else {
                throw new IndexOutOfBoundsException();
            }
        }

        public Object[] getRow( long irow ) throws IOException {
            return new Object[] { getCell( irow, 0 ) };
        }

        public SupplementData createSupplementData( final RowData rdata ) {
            return new SupplementData() {
                public Object getCell( long irow, int icol )
                        throws IOException {
                    if ( icol == 0 ) {
                        A array = createArray();
                        for ( int i = 0; i < ncol_; i++ ) {
                            setElement( array, i, rdata.getCell( icol0_ + i ) );
                        }
                        return array;
                    }
                    else {
                        throw new IndexOutOfBoundsException();
                    }
                }
                public Object[] getRow( long irow ) throws IOException {
                    return new Object[] { getCell( irow, 0 ) };
                }
            };
        }

        /**
         * Returns an empty array of the type and length returned
         * by this column, ready for populating with values.
         *
         * @return  new array
         */
        private A createArray() {
            @SuppressWarnings("unchecked")
            A array = (A) Array.newInstance( eclazz_, ncol_ );
            return array;
        }
    }
}
