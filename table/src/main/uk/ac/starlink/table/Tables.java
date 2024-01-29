package uk.ac.starlink.table;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.formats.TextTableWriter;
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
     * ValueInfo which may be used as part of a column's auxiliary metadata
     * to indicate that the column's data represents, and can be
     * serialised as, unsigned byte values.  If so, the value is set to
     * <code>Boolean.TRUE</code> (other values are treated as if absent).
     * Data representing unsigned byte values will normally be represented
     * within STIL by Short (16-bit integer) signed values,
     * since there is no unsigned byte type in java.
     * However, this flag may be used to indicate that the
     * values can be serialised to unsigned-byte-capable output formats
     * (for instance FITS and VOTable) using an unsigned byte serialisation.
     * This annotation will normally only be honoured if the data type of
     * the column is (scalar or array) short integer (16-bit) values.
     * Some care should be exercised in applying this flag or (especially)
     * modifying values in columns it applies to, that the actual column
     * value range remains in the unsigned byte data range (0..255),
     * since otherwise problems will result if it is serialised.
     */
    public static final ValueInfo UBYTE_FLAG_INFO =
        new DefaultValueInfo( "UBYTE_FLAG", Boolean.class,
                              "If true, data represents unsigned byte values" );

    /**
     * ValueInfo that indicates result of a query.
     * Its name is QUERY_STATUS, and it is used by DALI, but non-DALI/VO
     * sources can use the same mechanism to flag non-standard status.
     * Values are normally "OK", "OVERFLOW" or "ERROR".
     */
    public static final ValueInfo QUERY_STATUS_INFO =
        new DefaultValueInfo( "QUERY_STATUS", String.class,
                              "Indicator of query status; "
                            + "anything other than OK means something wrong" );

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
     * Returns a RowSplittable object with generic characteristics
     * for a given table.
     * For a random-access table the splitting will be based on
     * {@link RowAccess} objects, and for a non-random table
     * it will not be capable of splits.
     *
     * @param  table  table
     * @return  splittable for potentially parallel iteration over rows
     */
    public static RowSplittable getDefaultRowSplittable( StarTable table )
            throws IOException {
        return table.isRandom() ? new RandomRowSplittable( table )
                                : new SequentialRowSplittable( table );
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
            RowAccess racc = table.getRowAccess();
            assertTrue( racc != null );
            racc.close();
        }

        /* Check the shape arrays look OK. */
        int[] nels = new int[ ncol ];
        Class<?>[] classes = new Class<?>[ ncol ];
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
        RowAccess racc = isRandom ? table.getRowAccess() : null;
        RowSplittable rsplit = table.getRowSplittable();
        while ( rseq.next() ) {
            assertTrue( rsplit.next() );
            Object[] row = rseq.getRow();
            Object[] rowSplit = rsplit.getRow(); 
            for ( int icol = 0; icol < ncol; icol++ ) {

                /* Check that elements got in different ways look the same. */
                Object cell = row[ icol ];
                Object val1 = cell;
                Object val2 = rseq.getCell( icol );
                Object val3 = null;
                Object val4 = null;
                Object val5 = rowSplit[ icol ];
                Object val6 = rsplit.getCell( icol );
                if ( isRandom ) {
                    val3 = table.getCell( lrow, icol );
                    racc.setRowIndex( lrow );
                    val4 = racc.getCell( icol );
                }
                boolean isNull = cell == null;
                if ( isNull ) {
                    assertTrue( colinfos[ icol ].isNullable() );
                    assertTrue( val2 == null );
                    assertTrue( val5 == null );
                    assertTrue( val6 == null );
                    if ( isRandom ) {
                        assertTrue( val3 == null );
                        assertTrue( val4 == null );
                    }
                }
                else {
                    ColumnInfo cinfo = colinfos[ icol ];
                    String s1 = cinfo.formatValue( val1, formatChars );
                    assertTrue( s1.equals( cinfo
                                          .formatValue( val2, formatChars ) ) );
                    assertTrue( s1.equals( cinfo
                                          .formatValue( val5, formatChars ) ) );
                    assertTrue( s1.equals( cinfo
                                          .formatValue( val6, formatChars ) ) );
                    if ( isRandom ) {
                        assertTrue( s1.equals( colinfos[ icol ]
                                              .formatValue( val3,
                                                            formatChars ) ) );
                        assertTrue( s1.equals( colinfos[ icol ]
                                              .formatValue( val4,
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
        assertTrue( ! rsplit.next() );
        rsplit.close();

        /* Check splittables split correctly. */
        RowSplittable split1 = table.getRowSplittable();
        RowSplittable split2 = split1.split();
        LongSupplier rowIndex1 = split1.rowIndex();
        boolean hasRowIndex = rowIndex1 != null;
        if ( split2 != null ) {
            LongSupplier rowIndex2 = split2.rowIndex();
            assertTrue( ( rowIndex2 != null ) == hasRowIndex );
            BitSet bits = hasRowIndex && nrow < Integer.MAX_VALUE
                        ? new BitSet( nrow >= 0 ? (int) nrow : 1024 )
                        : null;
            int nr = 0;
            while ( split2.next() ) {
                nr++;
                if ( bits != null ) {
                    bits.set( (int) rowIndex2.getAsLong() );
                }
            }
            while ( split1.next() ) {
                nr++;
                if ( bits != null ) {
                    bits.set( (int) rowIndex1.getAsLong() );
                }
            }
            split2.close();
            if ( nrow >= 0 ) {
                assertTrue( nrow == nr );
            }
            if ( bits != null ) {
                assertTrue( nr == bits.nextClearBit( 0 ) );
            }
        }
        split1.close();

        /* Check that the claimed number of rows is correct. */
        if ( nrow >= 0 ) {
            assertTrue( lrow == nrow );
        }
    }

    /**
     * Returns a checksum of all the cell data in a given table.
     * Currently, the Adler32 checksum is used.
     *
     * @param   table  table to checksum
     * @return   checksum value
     */
    public static int checksumData( StarTable table ) throws IOException {
        Checksum checksum = new Adler32();
        try ( RowSequence rseq = table.getRowSequence() ) {
            checksumData( rseq, checksum );
        }
        return (int) checksum.getValue();
    }

    /**
     * Feeds the data from a row sequence to a supplied checksum accumulator.
     *
     * @param  rseq  row sequence containing data
     * @param  checksum   checksum accumulator
     * @return   number of rows checksummed
     *           (note this is <em>not</em> the checksum value)
     */
    public static long checksumData( RowSequence rseq, Checksum checksum )
            throws IOException {
        long lrow = 0;
        while ( rseq.next() ) {
            Object[] row = rseq.getRow();
            for ( Object cell : row ) {
                int hash = isBlank( cell ) ? -654321 : cell.hashCode();
                checksum.update( (byte) ( hash >>> 24 ) );
                checksum.update( (byte) ( hash >>> 16 ) );
                checksum.update( (byte) ( hash >>>  8 ) );
                checksum.update( (byte) ( hash >>>  0 ) );
            }
            lrow++;
        }
        return lrow;
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
     * Collapses whitespace in a string.
     * This normalises the text in the sense of the XML Schema facet
     * <code>whitespace='collapse'</code>:
     * leading and trailing whitespace is removed,
     * and any other run of whitespace is replaced by a single space character.
     *
     * @param  txt  input string (may be null)
     * @return   string with whitespaces collapsed
     */
    public static String collapseWhitespace( String txt ) {
        return txt == null
             ? null
             : txt.trim().replaceAll( "\\s+", " " );
    }

    /**
     * Convenience method to construct a TableSequence for a single table.
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
        final Iterator<StarTable> it = Arrays.asList( tables ).iterator();
        return new TableSequence() {
            public StarTable nextTable() {
                return it.hasNext() ? it.next()
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
     * Returns the contents of a table as a string.
     * Only intended for small tables, for instance during debugging.
     *
     * @param  table   input table
     * @param  ofmt   output format specifier, or null for plain text output
     * @return  stringified table
     */
    public static String tableToString( StarTable table, String ofmt ) {
        final StarTableWriter handler;
        if ( ofmt != null ) {
            try {
                handler = new StarTableOutput().getHandler( ofmt );
            }
            catch ( TableFormatException e ) {
                throw new IllegalArgumentException( "No such format \"" + ofmt
                                                  + "\"", e );
            }
        }
        else {
            TextTableWriter textHandler = new TextTableWriter();
            textHandler.setWriteParameters( false );
            textHandler.setMaxWidth( 40 );
            handler = textHandler;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            handler.writeStarTable( table, out );
            out.close();
            return new String( out.toByteArray(), "utf-8" );
        }
        catch ( IOException e ) {
            return "Formatting error: " + e;
        }
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
     * Casts a long to an int, with an assertion that no truncation occurs.
     *
     * @param  lval  long value, asserted to be in the range
     *               Integer.MIN_VALUE..Integer.MAX_VALUE
     * @return  truncated version of <code>lval</code>
     */
    public static int assertLongToInt( long lval ) {
        int ival = (int) lval;
        assert ival == lval
             : "Long value " + lval + " unexpectedly out of int range";
        return ival;
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
        List<String> labels = new ArrayList<String>();
        for ( Iterator<int[]> it = new ShapeIterator( shape ); it.hasNext(); ) {
            int[] pos = it.next();
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 0; i < pos.length; i++ ) {
                sbuf.append( '_' )
                    .append( Integer.toString( pos[ i ] + 1 ) );
            }
            labels.add( sbuf.toString() );
        }
        return labels.toArray( new String[ 0 ] );
    }

    /**
     * Returns the Utype associated with a given metadata item.
     *
     * @deprecated  use {@link ValueInfo#getUtype()} instead
     * @param  info  metadata item
     * @return   utype string, or null if none is available
     * @see   #setUtype
     */
    @Deprecated
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
    @Deprecated
    public static void setUtype( ValueInfo info, String utype ) {
        if ( info instanceof DefaultValueInfo ) {
            ((DefaultValueInfo) info).setUtype( utype );
        }
    }

    /**
     * Returns the extended type value associated with a given metadata item.
     * The XType corresponds to the <code>xtype</code> attribute of
     * the VOTable format.  Other table formats may or may not be able
     * to represent it.
     *
     * @deprecated  use {@link ValueInfo#getXtype} instead
     * @param  info  metadata item
     * @return   xtype string, or null if none is available
     */
    @Deprecated
    public static String getXtype( ValueInfo info ) {
        return info.getXtype();
    }

    /**
     * Tries to set the Xtype for a given metadata item.
     *
     * @deprecated  use {@link DefaultValueInfo#setXtype} instead
     * @param  info  metadata item
     * @param  xtype  new xtype value
     * @see   #getXtype
     */
    @Deprecated
    public static void setXtype( ValueInfo info, String xtype ) {
        if ( info instanceof DefaultValueInfo ) {
            ((DefaultValueInfo) info).setXtype( xtype );
        }
    }

    /**
     * Utility method to update a list of DescribedValues with a new entry.
     * If an item with the same name as the new entry already exists,
     * it is removed.
     *
     * @param  dvals  list to modify
     * @param  dval  new entry to add
     */
    public static void setDescribedValue( Collection<DescribedValue> dvals,
                                          DescribedValue dval ) {
        DescribedValue old =
            getDescribedValueByName( dvals, dval.getInfo().getName() );
        if ( old != null ) {
            dvals.remove( old );
        }
        dvals.add( dval );
    }

    /**
     * Utility method to locate an element in a list of DescribedValue
     * given the name of its ValueInfo member.
     *
     * @param   dvals  list to query
     * @param   name   required value of name
     * @return  element of <code>dvals</code> for which
     *                  <code>getInfo().getName()</code>
     *                  matches <code>name</code>
     */
    public static DescribedValue
            getDescribedValueByName( Collection<DescribedValue> dvals,
                                     String name ) {
        for ( DescribedValue dval : dvals ) {
            if ( name.equals( dval.getInfo().getName() ) ) {
                return dval;
            }
        }
        return null;
    }

    /**
     * Utility method to return an auxiliary metadata item from a ValueInfo.
     *
     * @param  info   metadata item
     * @param  auxKey   info identifying aux metadata entry in <code>info</code>
     * @param  auxClazz   required result type
     * @return   typed aux metadata item requested,
     *           or null if it doesn't exist or has the wrong type
     */
    public static <T> T getAuxDatumValue( ValueInfo info, ValueInfo auxKey,
                                          Class<T> auxClazz ) {
        return getTypedValue( info.getAuxDatumByName( auxKey.getName() ),
                              auxClazz );
    }

    /**
     * Utility method to get a typed value from a, possibly null,
     * described value.
     *
     * @param  dval   described value, or null
     * @param  clazz   required return type
     * @return   typed value of <code>dval</code>, or null if dval is null,
     *           or if dval's value has the wrong type
     */
    public static <T> T getTypedValue( DescribedValue dval, Class<T> clazz ) {
        return dval == null ? null : dval.getTypedValue( clazz );
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
    public static Object getValue( Collection<DescribedValue> dvals,
                                   ValueInfo info ) {
        String iname = info.getName();
        Class<?> iclazz = info.getContentClass();
        if ( iname != null && iclazz != null ) {
            for ( DescribedValue dval : dvals ) {
                ValueInfo dinfo = dval.getInfo();
                if ( iname.equals( dinfo.getName() ) &&
                     iclazz.equals( dinfo.getContentClass() ) ) {
                    return dval.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Performs deduplication of column names for N lists of column metadata
     * objects that will be combined to form a new table.
     * The arguments are matched arrays; each list of column infos
     * has a corresponding renaming policy.
     * The ColumnInfo objects are renamed in place as required.
     *
     * @param  infoLists   array of N arrays of column metadata objects
     * @param  fixActs     array of N policies for renaming columns
     */
    public static void fixColumns( ColumnInfo[][] infoLists,
                                   JoinFixAction[] fixActs ) {

        /* Check there is a 1:1 correspondence of infoLists and fix policies. */
        int nt = infoLists.length;
        if ( fixActs.length != nt ) {
            throw new IllegalArgumentException();
        }

        /* Find the longest list of columns.  This is just used so that we
         * can get a unique numeric index for any column iCol of table iTab. */
        int maxNc = 0;
        for ( int it = 0; it < nt; it++ ) {
            maxNc = Math.max( infoLists[ it ].length, maxNc );
        } 

        /* Put all the infos into a single List. */
        String[] names = new String[ maxNc * nt ];
        for ( int it = 0; it < nt; it++ ) {
            ColumnInfo[] infos = infoLists[ it ];
            for ( int ic = 0; ic < infos.length; ic++ ) {
                int ix = it * maxNc + ic;
                names[ ix ] = infos[ ic ].getName();
            }
        }
        List<String> nameList = new ArrayList<String>( Arrays.asList( names ) );

        /* For each info, deduplicate its name as required in the context
         * of all the others. */
        for ( int it = 0; it < nt; it++ ) {
            ColumnInfo[] infos = infoLists[ it ];
            JoinFixAction fixAct = fixActs[ it ];
            for ( int ic = 0; ic < infos.length; ic++ ) {
                int ix = it * maxNc + ic;
                String origName = nameList.remove( ix );
                assert origName.equals( infos[ ic ].getName() );
                String name = fixAct.getFixedName( origName, nameList );
                nameList.add( ix, origName );
                if ( ! name.equals( origName ) ) {
                    nameList.add( name );
                    infos[ ic ].setName( name );
                }
            }
        }
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
