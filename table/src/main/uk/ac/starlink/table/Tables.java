package uk.ac.starlink.table;

import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.jdbc.JDBCStarTable;

/**
 * Utility class for miscellaneous table-related functionality.
 */
public class Tables {

    /**
     * ValueInfo which may be used as part of a column's metadata to indicate
     * a special value (preferably a {@link java.lang.Number} 
     * that should be interpreted as a null (blank).
     * This should only be used on nullable columns, and really only
     * on ones with a contentClass which is an integer (or possibly boolean)
     * type; for other types, there is usually a value which can 
     * conventionally be understood to mean blank.
     * Note this is here as a standard key to use when software components
     * wish to communicate this information; the table system does not
     * guarantee to honour instances of this value in a column's 
     * auxiliary data.  It is the job of a StarTable instance to ensure
     * that a <tt>null</tt> is returned from the table interrogation methods
     * if that is what is meant.
     */
    public static final ValueInfo NULL_VALUE_INFO =
        new DefaultValueInfo( "NULL_VALUE", Object.class,
                              "Integer value which represents a null" );

    /**
     * ValueInfo representing Right Ascension. 
     * The units are radians and it is non-nullable.
     */
    public static final DefaultValueInfo RA_INFO =
        new DefaultValueInfo( "RA", Number.class, "Right Ascension" );

    /**
     * ValueInfo representing Declination.
     * The units are radians and it is non-nullable.
     */
    public static final DefaultValueInfo DEC_INFO =
        new DefaultValueInfo( "Dec", Number.class, "Declination" );

    static {
        RA_INFO.setUnitString( "radians" );
        DEC_INFO.setUnitString( "radians" );
        RA_INFO.setNullable( false );
        DEC_INFO.setNullable( false );
        RA_INFO.setUCD( "POS_EQ_RA" );
        DEC_INFO.setUCD( "POS_EQ_DEC" );
    }

    /**
     * Returns a table based on a given table and guaranteed to have 
     * random access.  If the original table <tt>stab</tt> has random
     * access then it is returned, otherwise a new random access table
     * is built using its data.
     *
     * <p>This convenience method is equivalent to calling
     * <tt>StoragePolicy.getDefaultPolicy().randomTable(startab)</tt>.
     *
     * @param  startab  original table
     * @return  a table with the same data as <tt>startab</tt> and with 
     *          <tt>isRandom()==true</tt>
     */
    public static StarTable randomTable( StarTable startab )
            throws IOException {
        return StoragePolicy.getDefaultPolicy().randomTable( startab );
    }

    /**
     * Convenience method to return an array of all the column headers
     * in a given table.  Modifying this array will not affect the table.
     *
     * @param  startab  the table being enquired about
     * @return an array of all the column headers
     */
    public static ColumnInfo[] getColumnInfos( StarTable startab ) {
        int ncol = startab.getColumnCount();
        ColumnInfo[] infos = new ColumnInfo[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            infos[ i ] = startab.getColumnInfo( i );
        }
        return infos;
    }

    /**
     * Returns a table equivalent to the original but with a given column
     * deleted.  The result may or may not be the same object as the
     * input table.
     *
     * @param  startab  the table from which to delete a column
     * @param  icol     the index of the column to be deleted
     * @throws  IndexOutOfBoundsException if <tt>startab</tt> has no column
     *          at <tt>icol</tt>
     */
    public static StarTable deleteColumn( StarTable startab, int icol ) {
        int ncol = startab.getColumnCount();
        if ( icol < 0 || icol >= ncol ) {
            throw new IndexOutOfBoundsException( 
                "Deleted column " + icol + " out of range 0.." + ( ncol - 1 ) );
        }
        int[] colmap = new int[ ncol - 1 ];
        int j = 0;
        for ( int i = 0; i < ncol; i++ ) {
            if ( i != icol ) {
                colmap[ j++ ] = i;
            }
        }
        assert j == ncol - 1;
        return new ColumnPermutedStarTable( startab, colmap );
    }

    /**
     * Copies the data and metadata from a <tt>StarTable</tt> into a 
     * table sink.  
     * This method is supplied for convenience; its implementation is
     * very straightforward.
     *
     * @param   source  table to be copied
     * @param   sink    table destination
     */
    public static void streamStarTable( StarTable source, TableSink sink ) 
            throws IOException {
        sink.acceptMetadata( source );
        RowSequence rseq = source.getRowSequence();
        try {
            while ( rseq.next() ) {
                sink.acceptRow( rseq.getRow() );
            }
        }
        finally {
            rseq.close();
        }
        sink.endRows();
    }

    /**
     * Diagnostic method which tests the invariants of a StarTable.
     * This method returns no value, and throws an exception if a table
     * is illegal in some way.
     * If this method throws an exception when called on a given StarTable,
     * that table is deemed to be bad in some way (buggy implementation
     * or I/O trouble during access).
     * This method is provided for convenience and diagnostics 
     * because there are a number of ways, some of them subtle, in which
     * a StarTable can fail to fulfil its general contract.
     * <p>
     * That a table passes this test does not guarantee that the table has
     * no bugs.  This method should not generally be used in production
     * code, since it may be expensive in time and/or memory.
     *
     * @param  table  table to test
     * @throws  AssertionError  if an invariant is violated
     * @throws  IOException   if there is an I/O error
     */
    public static void checkTable( StarTable table ) throws IOException {
        int formatChars = 100;
        int ncol = table.getColumnCount();
        long nrow = table.getRowCount();
        boolean isRandom = table.isRandom();

        /* Check a random-access table knows how many rows it has. */
        if ( isRandom ) {
            assertTrue( nrow >= 0 );
        }

        /* Check the shape arrays look OK. */
        int[] nels = new int[ ncol ];
        Class[] classes = new Class[ ncol ];
        ColumnInfo[] colinfos = new ColumnInfo[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo cinfo = table.getColumnInfo( icol );
            colinfos[ icol ] = cinfo;
            classes[ icol ] = cinfo.getContentClass();
            int[] dims = cinfo.getShape();
            if ( dims != null ) {
                int ndim = dims.length;
                assertTrue( ndim > 0 );
                assertTrue( cinfo.getContentClass().isArray() );
                int nel = 1;
                for ( int i = 0; i < ndim; i++ ) {
                    nel *= dims[ i ];
                    assertTrue( dims[ i ] != 0 );
                    if ( i < ndim - 1 ) {
                        assertTrue( dims[ i ] > 0 );
                    }
                }
                nels[ icol ] = nel;
            }
            assertTrue( cinfo.getContentClass().isArray() == cinfo.isArray() );
        }

        /* Get a RowSequence object and check it can't be read before a next. */
        RowSequence rseq = table.getRowSequence();
        try {
            rseq.getRow();
            assertTrue( false );
        }
        catch ( IllegalStateException e ) {
            // ok
        }

        /* Read all cells. */
        long lrow = 0L;
        while ( rseq.next() ) {
            Object[] row = rseq.getRow();
            for ( int icol = 0; icol < ncol; icol++ ) {

                /* Check that elements got in different ways look the same. */
                Object cell = row[ icol ];
                Object val1 = cell;
                Object val2 = rseq.getCell( icol );
                Object val3 = null;
                if ( isRandom ) {
                    val3 = table.getCell( lrow, icol );
                }
                boolean isNull = cell == null;
                if ( isNull ) {
                    assertTrue( colinfos[ icol ].isNullable() );
                    assertTrue( val2 == null );
                    if ( isRandom ) {
                        assertTrue( val3 == null);
                    }
                }
                else {
                    String s1 = colinfos[ icol ]
                               .formatValue( val1, formatChars );
                    assertTrue( s1.equals( colinfos[ icol ]
                                          .formatValue( val2, formatChars ) ) );
                    if ( isRandom ) {
                        assertTrue( s1.equals( colinfos[ icol ]
                                              .formatValue( val3,
                                                            formatChars ) ) );
                    }
                }

                /* If the cell is an array, check it's the right shape. */
                if ( cell != null && cell.getClass().isArray() ) {
                    int nel = Array.getLength( cell );
                    if ( nels[ icol ] < 0 ) {
                        assertTrue( nel % nels[ icol ] == 0 );
                    }
                    else {
                        assertTrue( nels[ icol ] == nel );
                    }
                }

                /* Check the cell is of the declared type. */
                if ( cell != null ) {
                    if ( ! classes[ icol ]
                          .isAssignableFrom( cell.getClass() ) ) {
                        throw new AssertionError( "Column " + ( icol + 1 ) +
                            ": " + cell + " is a " + cell.getClass().getName() +
                            " not a " + classes[ icol ].getName() );
                    }
                }
            }
            lrow++;
        }
        rseq.close();

        /* Check that the claimed number of rows is correct. */
        if ( nrow >= 0 ) {
            assertTrue( lrow == nrow );
        }
    }

    /**
     * Indicates whether a given value is conventionally regarded as a 
     * blank value.  For most objects this is equivalent to testing
     * whether it is equall to <tt>null</tt>, but some classes have
     * additional non-<tt>null</tt> values which count as blanks,
     * for instance zero-length Strings and floating Not-A-Number values.
     *
     * @param  value  value to test
     * @return  true iff <tt>value</tt> counts as a blank value
     */
    public static boolean isBlank( Object value ) {
        return ( value == null ) 
            || ( value instanceof Float && ((Float) value).isNaN() )
            || ( value instanceof Double && ((Double) value).isNaN() )
            || ( value instanceof String && ((String) value).length() == 0 )
            || ( value.getClass().isArray() && Array.getLength( value ) == 0 )
            || false;
    }

    /**
     * Convenience method to consruct a TableSequence for a single table.
     *
     * @param   table  table
     * @return  table sequence with just one element
     */
    public static TableSequence singleTableSequence( StarTable table ) {
        return arrayTableSequence( new StarTable[] { table } );
    }

    /**
     * Convenience method to construct a TableSequence for a supplied array
     * of tables.
     *
     * @param   tables  table array
     * @return  table sequence containing input tables
     */
    public static TableSequence arrayTableSequence( StarTable[] tables ) {
        final Iterator it = Arrays.asList( tables ).iterator();
        return new TableSequence() {
            public StarTable nextTable() {
                return it.hasNext() ? (StarTable) it.next()
                                    : null;
            }
        };
    }

    /**
     * Convenience method to construct an array of StarTables from a
     * TableSequence.
     *
     * @param  tseq   table sequence
     * @return   array containing tables from sequence
     */
    public static StarTable[] tableArray( TableSequence tseq )
            throws IOException {
        List<StarTable> list = new ArrayList<StarTable>();
        for ( StarTable table; ( table = tseq.nextTable() ) != null; ) {
            list.add( table );
        }
        return list.toArray( new StarTable[ 0 ] );
    }

    /**
     * Returns a sorted version of a table.  The sorting is done on the
     * values of one or more columns, as specified by the <tt>colIndices</tt>
     * argument; the first element is the primary sort key, but in case
     * of a tie the second element is used, and so on.  The original table
     * is not affected.  The natural comparison order of the values in
     * the table is used, and blank values may be ranked at the start or
     * end of the collation order.
     *
     * @param   table  table to sort - must be random access
     * @param   colIndices  indices of the columns which are to act as sort
     *          keys; first element is primary key etc
     * @param   up  true for sorting into ascending order, false for 
     *          descending order
     * @param   nullsLast  true if blank values should be considered 
     *          last in the collation order, false if they should 
     *          be considered first
     * @return  a table with the same rows as <tt>table</tt> but in an 
     *          order determined by the other arguments
     * @throws  IOException if <tt>table.isRandom</tt> is not true
     */
    public static StarTable sortTable( StarTable table, int[] colIndices,
                                       boolean up, boolean nullsLast )
            throws IOException {
        long[] rowMap =
            TableSorter.getSortedOrder( table, colIndices, up, nullsLast );
        return new RowPermutedStarTable( table, rowMap );
    }

    /**
     * Convenience method to get an <tt>int</tt> value from a <tt>long</tt>.
     * If the supplied long integer <tt>lval</tt> is out of the range
     * which can be represented in an <tt>int</tt>, then unlike a
     * typecast, this method will throw an <tt>IllegalArgumentException</tt>.
     *
     * @param  lval the <tt>long</tt> value to convert
     * @return an <tt>int</tt> value which has the same value as <tt>lval</tt>
     * @throws IllegalArgumentException  if the conversion cannot be done
     */
    public static int checkedLongToInt( long lval ) {
        int ival = (int) lval;
        if ( (long) ival == lval ) {
            return ival;
        }
        else {
            if ( ival < Integer.MIN_VALUE ) { 
                throw new IllegalArgumentException( "Out of supported range: "
                    + ival + " < Integer.MIN_VALUE" );
            }
            else if ( ival > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException( "Out of supported range: "
                    + ival + " > Integer.MAX_VALUE" );
            }
            else {
                throw new AssertionError();
            }
        }
    }

    /**
     * Returns an array of strings suitable as labels or label suffixes
     * for elements of an array as returned by {@link ValueInfo#getShape}.
     * If the given <tt>shape</tt> cannot be decomposed into a fixed
     * size array, returns <tt>null</tt>.
     *
     * @param   shape  vector giving dimensions of an array value
     * @return   array with one element for each element of the array values
     *           (in their natural order), or null for non-array shapes
     */
    public static String[] getElementLabels( int[] shape ) {
        if ( shape == null || shape.length == 0 ) {
            return null;
        }
        for ( int i = 0; i < shape.length; i++ ) {
            if ( shape[ i ] <= 0 ) {
                return null;
            }
        }
        List labels = new ArrayList();
        for ( Iterator it = new ShapeIterator( shape ); it.hasNext(); ) {
            int[] pos = (int[]) it.next();
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 0; i < pos.length; i++ ) {
                sbuf.append( '_' )
                    .append( Integer.toString( pos[ i ] + 1 ) );
            }
            labels.add( sbuf.toString() );
        }
        return (String[]) labels.toArray( new String[ 0 ] );
    }

    /**
     * Returns the Utype associated with a given metadata item.
     *
     * @deprecated  use {@link ValueInfo#getUtype()} instead
     * @param  info  metadata item
     * @return   utype string, or null if none is available
     * @see   #setUtype
     */
    public static String getUtype( ValueInfo info ) {
        return info.getUtype();
    }

    /**
     * Tries to set the Utype for a given metadata item.
     *
     * @deprecated  use {@link DefaultValueInfo#setUtype} instead
     * @param  info  metadata item
     * @param  utype  new utype value
     * @see   #getUtype
     */
    public static void setUtype( ValueInfo info, String utype ) {
        if ( info instanceof DefaultValueInfo ) {
            ((DefaultValueInfo) info).setUtype( utype );
        }
    }

    /**
     * Returns the value from a list of {@link DescribedValue} objects
     * which corresponds to a given info key.
     * If the key is not represented in the list, or if its value is null,
     * then null is returned.
     *
     * @param   dvals  list of DescribedValue objects
     * @param   info   key giving the value you want
     * @return  matching value  
     */
    public static Object getValue( Collection dvals, ValueInfo info ) {
        String iname = info.getName();
        Class iclazz = info.getContentClass();
        if ( iname != null && iclazz != null ) {
            for ( Iterator it = dvals.iterator(); it.hasNext(); ) {
                Object obj = it.next();
                if ( obj instanceof DescribedValue ) {
                    DescribedValue dval = (DescribedValue) obj;
                    ValueInfo dinfo = dval.getInfo();
                    if ( iname.equals( dinfo.getName() ) &&
                         iclazz.equals( dinfo.getContentClass() ) ) {
                        return dval.getValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Implements assertion semantics.  This differs from the <tt>assert</tt>
     * Java 1.4 language element in that the assertion is always done,
     * it doesn't depend on the JVM running in assertions-enabled mode.
     *
     * @param  ok  the thing that should be true
     * @throws  AssertionError  if <tt>ok</tt> is <tt>false</tt>
     */
    private static void assertTrue( boolean ok ) {
        if ( ! ok ) {
            throw new AssertionError();
        }
    }
}
