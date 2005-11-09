package uk.ac.starlink.fits;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RandomRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * An implementation of the StarTable interface which uses a FITS BINTABLE
 * extension.  Reading is done into the random access data (possibly
 * a mapped file); the nom.tam.fits classes are not used.
 * <p>
 * It is safe to read cells from different threads.
 *
 * @author   Mark Taylor
 */
public abstract class BintableStarTable extends AbstractStarTable {

    private final int ncol;
    private final int nrow;
    private final ColumnInfo[] colInfos;
    private final ColumnReader[] colReaders;
    private final int rowLength;
    private final int[] colOffsets;

    /* Auxiliary metadata for columns. */
    private final static ValueInfo tnullInfo = new DefaultValueInfo(
        Tables.NULL_VALUE_INFO.getName(),
        Tables.NULL_VALUE_INFO.getContentClass(),
        "Bad value indicator (TNULLn card)" );
    private final static ValueInfo tscalInfo = new DefaultValueInfo(
        "Scale",
        Double.class,
        "Multiplier for values (TSCALn card)" );
    private final static ValueInfo tzeroInfo = new DefaultValueInfo(
        "Zero",
        Double.class,
        "Offset for values (TZEROn card)" );
    private final static ValueInfo tdispInfo = new DefaultValueInfo(
        "Format",
        String.class,
        "Display format in FORTRAN notation (TDISPn card)" );
    private final static ValueInfo tbcolInfo = new DefaultValueInfo(
        "Start column",
        Integer.class,
        "Start column for data (TBCOLn card)" );
    private final static ValueInfo tformInfo = new DefaultValueInfo(
        "Format code",
        String.class,
        "Data type code (TFORMn card)" );
    private final static List auxDataInfos = Arrays.asList( new ValueInfo[] {
        tnullInfo, tscalInfo, tzeroInfo, tdispInfo, tbcolInfo, tformInfo,
    } );

    /**
     * Constructs a StarTable from a given random access stream.
     *
     * @param  hdr  FITS header descrbing the HDU
     * @param  rstream   data stream positioned at the start of the data 
     *                   section of the HDU
     */
    public static StarTable makeRandomStarTable( Header hdr, 
                                                 final RandomAccess rstream )
            throws FitsException {
        final long dataStart = rstream.getFilePointer();
        return new BintableStarTable( hdr ) {
            final long rowLength = getRowLength();
            final int[] colOffsets = getColumnOffsets();
            final int ncol = getColumnCount();
            public boolean isRandom() {
                return true;
            }
            public Object getCell( long lrow, int icol )
                    throws IOException {
                synchronized ( rstream ) {
                    long offset = dataStart + lrow * rowLength
                                            + colOffsets[ icol ];
                    rstream.seek( offset );
                    return readCell( rstream, icol );
                }
            }
            public Object[] getRow( long lrow ) throws IOException {
                Object[] row = new Object[ ncol ];
                synchronized ( rstream ) {
                    rstream.seek( dataStart + lrow * rowLength );
                    return readRow( rstream );
                }
            }
            public RowSequence getRowSequence() {
                return new RandomRowSequence( this );
            }
        };
    }

    /**
     * Constructs a sequential-only StarTable from a DataSource.
     * Reading is deferred.
     *
     * @param  hdr  FITS header descrbing the HDU
     * @param  datsrc  a data source which supplies the data stream 
     *          containing the table data
     * @param  offset  offset into the stream returned by <tt>datsrc</tt>
     *         at which the table data (not the corresponding HDU header)
     *         starts
     */
    static StarTable makeSequentialStarTable( Header hdr, 
                                              final DataSource datsrc,
                                              final long offset ) 
            throws FitsException {
        final Object[] BEFORE_START = new Object[ 0 ];
        return new BintableStarTable( hdr ) {
            public RowSequence getRowSequence() throws IOException {
                InputStream istrm = datsrc.getInputStream();
                if ( ! ( istrm instanceof BufferedInputStream ) ) {
                    istrm = new BufferedInputStream( istrm );
                }
                final DataInputStream stream = new DataInputStream( istrm );
                IOUtils.skipBytes( stream, offset );
                return new RowSequence() {
                    final long nrow = getRowCount();
                    final int ncol = getColumnCount();
                    final int rowLength = getRowLength();
                    long lrow = -1L;
                    Object[] row = BEFORE_START;

                    public boolean next() throws IOException {
                        if ( lrow < nrow - 1 ) {
                            if ( row == null ) {
                                IOUtils.skipBytes( stream, (long) rowLength );
                            }
                            row = null;
                            lrow++;
                            return true;
                        }
                        else {
                            return false;
                        }
                    }

                    public Object getCell( int icol ) throws IOException {
                        return getRow()[ icol ];
                    }

                    public Object[] getRow() throws IOException {
                        if ( row == BEFORE_START ) {
                            throw new IllegalStateException(
                                "Attempted read before start of table" );
                        }
                        if ( row == null ) {
                            row = readRow( stream );
                        }
                        return row;
                    }

                    public void close() throws IOException {
                        stream.close();
                    }
                };
            }
        };
    }

    /**
     * Reads a BINTABLE extension from a stream and writes the result to
     * a table sink.
     *
     * @param   hdr  FITS header object describing the BINTABLE extension
     * @param   stream  input stream positioned at the start of the 
     *          data part of the BINTABLE extension
     * @param   sink   destination for the table
     */
    public static void streamStarTable( Header hdr, DataInput stream, 
                                        TableSink sink )
            throws FitsException, IOException {
        BintableStarTable meta = new BintableStarTable( hdr ) {
            public RowSequence getRowSequence() {
                throw new UnsupportedOperationException( "Metadata only" );
            }
        };
        sink.acceptMetadata( meta );
        long nrow = meta.getRowCount();
        for ( long i = 0; i < nrow; i++ ) {
            Object[] row = meta.readRow( stream );
            sink.acceptRow( row );
        }
        sink.endRows();
        long datasize = nrow * meta.getRowLength();
        int over = (int) ( datasize % (long) 2880 );
        if ( over > 0 ) {
            IOUtils.skipBytes( stream, (long) ( 2880 - over ) );
        }
    }

    /**
     * Constructs a table.
     *
     * @param  hdr  FITS header cards describing this HDU
     */
    BintableStarTable( Header hdr ) throws FitsException {

        /* Check we have a BINTABLE header. */
        if ( ! hdr.getStringValue( "XTENSION" ).equals( "BINTABLE" ) ) {
            throw new IllegalArgumentException( "Not a binary table header" );
        }

        /* Get Table characteristics. */
        ncol = hdr.getIntValue( "TFIELDS" );
        nrow = hdr.getIntValue( "NAXIS2" );

        /* Get column characteristics. */
        colInfos = new ColumnInfo[ ncol ];
        colReaders = new ColumnReader[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            int jcol = icol + 1;
            ColumnInfo cinfo = new ColumnInfo( "col" + jcol );
            List auxdata = cinfo.getAuxData();
            colInfos[ icol ] = cinfo;

            /* Name. */
            String ttype = hdr.getStringValue( "TTYPE" + jcol );
            if ( ttype != null ) {
                cinfo.setName( ttype );
            }

            /* Units. */
            String tunit = hdr.getStringValue( "TUNIT" + jcol );
            if ( tunit != null ) {
                cinfo.setUnitString( tunit );
            }

            /* Format string. */
            String tdisp = hdr.getStringValue( "TDISP" + jcol );
            if ( tdisp != null ) {
                auxdata.add( new DescribedValue( tdispInfo, tdisp ) );
            }

            /* Blank value. */
            String blankKey = "TNULL" + jcol;
            long blank;
            boolean hasBlank;
            if ( hdr.containsKey( blankKey ) ) {
                blank = hdr.getLongValue( blankKey );
                hasBlank = true;
                auxdata.add( new DescribedValue( tnullInfo,
                                                 new Long( blank ) ) );
            }
            else {
                cinfo.setNullable( false );
                blank = 0L;
                hasBlank = false;
            }

            /* Shape. */
            int[] dims = null;
            String tdim = hdr.getStringValue( "TDIM" + jcol );
            if ( tdim != null ) {
                tdim = tdim.trim();
                if ( tdim.charAt( 0 ) == '(' &&
                     tdim.charAt( tdim.length() - 1 ) == ')' ) {
                    tdim = tdim.substring( 1, tdim.length() - 1 ).trim();
                    String[] sdims = tdim.split( "," );
                    if ( sdims.length > 0 ) {
                        try {
                            dims = new int[ sdims.length ];
                            for ( int i = 0; i < sdims.length; i++ ) {
                                dims[ i ] = Integer.parseInt( sdims[ i ] );
                            }
                        }
                        catch ( NumberFormatException e ) {
                            // can't set shape
                        }
                    }
                }
            }

            /* Scaling. */
            double scale;
            double zero;
            if ( hdr.containsKey( "TSCAL" + jcol ) ) {
                scale = hdr.getDoubleValue( "TSCAL" + jcol );
                auxdata.add( new DescribedValue( tscalInfo,
                                                 new Double( scale ) ) );
            }
            else {
                scale = 1.0;
            }
            if ( hdr.containsKey( "TZERO" + jcol ) ) {
                zero = hdr.getDoubleValue( "TZERO" + jcol );
                auxdata.add( new DescribedValue( tzeroInfo,
                                                 new Double( zero ) ) );
            }
            else {
                zero = 0.0;
            }

            /* Format code (recorded but otherwise ignored). */
            String tbcol = hdr.getStringValue( "TBCOL" + jcol );
            if ( tbcol != null ) {
                int bcolval = Integer.parseInt( tbcol );
                auxdata.add( new DescribedValue( tbcolInfo,
                                                 new Integer( bcolval ) ) );
            }

            /* Data type. */
            String tform = hdr.getStringValue( "TFORM" + jcol );
            if ( tform != null ) {
                auxdata.add( new DescribedValue( tformInfo, tform ) );
            }

            /* Comment (non-standard). */
            String tcomm = hdr.getStringValue( "TCOMM" + jcol );
            if ( tcomm != null ) {
                cinfo.setDescription( tcomm );
            }

            /* Construct a data reader for this column. */
            Matcher fmatch = Pattern.compile( "([0-9]*)([LXBIJKAEDCMP])(.*)" )
                                    .matcher( tform );
            if ( fmatch.lookingAt() ) {
                String scount = fmatch.group( 1 );
                int count = scount.length() == 0 
                          ? 1 
                          : Integer.parseInt( scount );
                char type = fmatch.group( 2 ).charAt( 0 );
                String matchA = fmatch.group( 3 ).trim();

                /* Make sure the dims array is sensible. */
                if ( count == 1 ) {
                    dims = null;
                }
                else if ( dims == null ) {
                    dims = new int[] { count };
                }
                else {
                    int nel = 1;
                    for ( int i = 0; i < dims.length; i++ ) {
                        nel *= dims[ i ];
                    }
                    if ( nel != count ) {
                        dims = new int[] { count };
                    }
                }

                /* Obtain and store a reader suitable for this column. */
                ColumnReader reader = 
                    makeColumnReader( type, count, scale, zero,
                                      hasBlank, blank, dims );
                colReaders[ icol ] = reader;

                /* Adjust nullability of strings - they can always be 
                 * null, since an empty string (all spaces) is interpreted
                 * as null. */
                if ( reader.getContentClass().equals( String.class ) ) {
                    cinfo.setNullable( true );
                }

                /* Do additional column info configuration as directed 
                 * by the reader. */
                cinfo.setContentClass( reader.getContentClass() );
                cinfo.setShape( reader.getShape() );
                cinfo.setElementSize( reader.getElementSize() );
            }
            else {
                throw new FitsException( "Error parsing header line TFORM"
                                         + jcol + " = " + tform );
            }
        }

        /* Calculate offsets so we know where to look for each cell. */
        int leng = 0;
        colOffsets = new int[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            colOffsets[ icol ] = leng;
            leng += colReaders[ icol ].getLength();
        }
        rowLength = leng;
        int nax1 = hdr.getIntValue( "NAXIS1" );
        if ( rowLength != nax1 ) {
            throw new FitsException( "Got wrong row length: " + nax1 + 
                                     " != " + rowLength );
        }
    }

    public long getRowCount() {
        return (long) nrow;
    }

    public int getColumnCount() {
        return ncol;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos[ icol ];
    }

    public List getColumnAuxDataInfos() {
        return auxDataInfos;
    }

    /**
     * Reads a cell from a given column from the current position in 
     * a stream.
     *
     * @param  icol  the column index corresponding to the cell to be read
     * @param  stream  a stream containing the byte data, positioned to
     *                 the right place
     */
    public Object readCell( DataInput stream, int icol ) throws IOException {
        return colReaders[ icol ].readValue( stream );
    }

    /**
     * Reads a whole row of the table from the current position in a stream,
     * returning a new Object[] array.
     *
     * @param  stream a stream containing the byte data, positioned to
     *                the right place
     * @return  <tt>ncol</tt>-element array of cells for this row
     */
    public Object[] readRow( DataInput stream ) throws IOException {
        Object[] row = new Object[ ncol ];
        readRow( stream, row );
        return row;
    }

    /**
     * Reads a whole row of the table into an existing array.
     *
     * @param  stream a stream containing the byte data, positioned to
     *                the right place
     * @param  row  <tt>ncol</tt>-element array, filled with row data
     *              on completion
     */
    public void readRow( DataInput stream, Object[] row ) throws IOException {
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = colReaders[ icol ].readValue( stream );
        }
    }

    /**
     * Returns the number of bytes occupied in the data stream by a single
     * row of the table.  This is equal to the sum of the column offsets array.
     *
     * @return  row length in bytes
     */
    protected int getRowLength() {
        return rowLength;
    }

    /**
     * Returns the array of byte offsets from the start of the row at 
     * which each column starts.
     * 
     * @return  <tt>ncol</tt>-element array of byte offsets
     */
    protected int[] getColumnOffsets() {
        return colOffsets;
    }

    /**
     * Constructs a ColumnReader object suitable for reading a given column
     * of a table.
     *
     * @param   type  character defining the FITS type of the data
     * @param   count  number of items per cell
     * @param   scale  factor to scale numerical values by
     * @param   zero   offset to add to numerical values
     * @param   hasBlank  true if a magic value is regarded as blank
     * @param   blank   value represnting magic null value
     *                  (only used if hasBlank is true)
     * @return  a reader suitable for reading this type of column
     */
    private static ColumnReader makeColumnReader( char type, final int count,
                                                  final double scale,
                                                  final double zero,
                                                  final boolean hasBlank, 
                                                  final long blank,
                                                  int[] dims ) {
        boolean single = ( count == 1 );
        final boolean isScaled = ( scale != 1.0 || zero != 0.0 );
        final boolean isOffset = ( scale == 1.0 && zero != 0.0 );
        final boolean intOffset = isOffset &&
                                  (double) Math.round( zero ) == zero;
        ColumnReader reader;
        switch ( type ) {

            /* Logical. */
            case 'L':
                if ( single ) {
                    reader = new ColumnReader( Boolean.class, 1 ) {
                        Object readValue( DataInput stream ) 
                                throws IOException {
                            switch ( stream.readByte() ) {
                                case (byte) 'T':
                                    return Boolean.TRUE;
                                case (byte) 'F':
                                    return Boolean.FALSE;
                                default:
                                    return null;
                            }
                        }
                    };
                }
                else {
                    reader = new ColumnReader( boolean[].class, dims, count ) {
                        Object readValue( DataInput stream ) 
                                throws IOException {
                            boolean[] value = new boolean[ count ];
                            for ( int i = 0; i < count; i++ ) {
                                value[ i ] = stream.readByte() == (byte) 'T';
                            }
                            return value;
                        }
                    };
                }
                return reader;

            /* Bits. */
            case 'X':
                int nbyte = ( count + 7 ) / 8;
                reader = new ColumnReader( boolean[].class, dims, nbyte ) {
                    Object readValue( DataInput stream ) 
                            throws IOException {
                        boolean[] value = new boolean[ count ];
                        int ibit = 0;
                        int b = 0;
                        for ( int i = 0; i < count; i++ ) {
                            if ( ibit == 0 ) {
                                ibit = 8;
                                b = stream.readByte();
                            }
                            value[ i ] = ( b & 0x01 ) != 0;
                            b = b >>> 1;
                            ibit--;
                        }
                        return value;
                    }
                };
                return reader;

            /* Unsigned byte - this is a bit fiddly, since a java byte is
             * signed and a FITS byte is unsigned.  We cope with this in
             * general by reading the byte as a short.  However, in the
             * special case that the scaling is exactly right to store
             * a signed byte as an unsigned one (scale=1, zero=-128) we
             * can transform a FITS byte directly into a java one. */
            case 'B':
                final short mask = (short) 0x00ff;
                boolean shortable = intOffset && zero >= Short.MIN_VALUE
                                              && zero < Short.MAX_VALUE - 256;
                final short sZero = (short) zero;
                if ( single ) {
                //  if ( zero == -128.0 && scale == 1.0 ) {
                //      reader = new ColumnReader( Byte.class, 1 ) {
                //          Object readValue( DataInput stream )
                //                  throws IOException {
                //              byte val = stream.readByte();
                //              return ( hasBlank && val == (byte) blank )
                //                          ? null
                //                          : new Byte( (byte) 
                //                                      ( val ^ (byte) 0x80 ) );
                //          }
                //      };
                //  }
                    if ( shortable ) {
                        reader = new ColumnReader( Short.class, 1 ) {
                            Object readValue( DataInput stream )
                                    throws IOException {
                                byte val = stream.readByte();
                                return ( hasBlank && val == (byte) blank )
                                            ? null
                                            : new Short( (short) 
                                                         ( ( val & mask ) +
                                                             sZero ) );
                            }
                        };
                    }
                    else if ( isScaled ) {
                        reader = new ColumnReader( Float.class, 1 ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                byte val = stream.readByte();
                                return ( hasBlank && val == (byte) blank ) 
                                            ? null
                                            : new Float( ( val & mask )
                                                         * scale + zero );
                            }
                        };
                    }
                    else {
                        reader = new ColumnReader( Short.class, 1 ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                byte val = stream.readByte();
                                return ( hasBlank && val == (byte) blank )
                                            ? null
                                            : new Short( (short) 
                                                         ( val & mask ) );
                            }
                        };
                    }
                }
                else {
                //  if ( zero == -128.0 && scale == 1.0 ) {
                //      reader = new ColumnReader( byte[].class, dims,
                //                                 1 * count ) {
                //          Object readValue( DataInput stream )
                //                  throws IOException {
                //              byte[] value = new byte[ count ];
                //              for ( int i = 0; i < count; i++ ) {
                //                  byte val = stream.readByte();
                //                  value[ i ] = (byte) ( val ^ (byte) 0x80 );
                //              }
                //              return value;
                //          }
                //      };
                //  }
                    if ( shortable ) {
                        reader = new ColumnReader( short[].class, dims,
                                                   1 * count ) {
                            Object readValue( DataInput stream )
                                    throws IOException {
                                short[] value = new short[ count ];
                                for ( int i = 0; i < count; i++ ) {
                                    byte val = stream.readByte();
                                    value[ i ] = (short) ( ( val & mask ) +
                                                           sZero );
                                }
                                return value;
                            }
                        };
                    }
                    else if ( isScaled ) {
                        reader = new ColumnReader( float[].class, dims,
                                                   1 * count ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                double[] value = new double[ count ];
                                for ( int i = 0; i < count; i++ ) {
                                    byte val = stream.readByte(); 
                                    value[ i ] = 
                                        ( hasBlank && val == (byte) blank )
                                             ? Float.NaN
                                             : (float) ( ( val & mask ) 
                                                         * scale + zero );
                                }
                                return value;
                            }
                        };
                    }
                    else {
                        reader = new ColumnReader( short[].class, dims,
                                                   1 * count ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                short[] value = new short[ count ];
                                for ( int i = 0; i < count; i++ ) {
                                    byte val = stream.readByte();
                                    value[ i ] = (short) ( val & mask );
                                }
                                return value;
                            }
                        };
                    }
                }
                return reader;

            /* Short. */
            case 'I':
                if ( single ) {
                    if ( isScaled ) {
                        reader = new ColumnReader( Float.class, 2 ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                short val = stream.readShort();
                                return ( hasBlank && val == (short) blank ) 
                                            ? null
                                            : new Float( (float) 
                                                       ( val * scale + zero ) );
                            }
                        };
                    }
                    else {
                        reader = new ColumnReader( Short.class, 2 ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                short val = stream.readShort();
                                return ( hasBlank && val == (short) blank )
                                            ? null
                                            : new Short( val );
                            }
                        };
                    }
                }
                else {
                    if ( isScaled ) {
                        reader = new ColumnReader( float[].class, dims, 
                                                   2 * count ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                double[] value = new double[ count ];
                                for ( int i = 0; i < count; i++ ) {
                                    short val = stream.readShort();
                                    value[ i ] =
                                        ( hasBlank && val == (short) blank )
                                             ? Float.NaN
                                             : (float) ( val * scale + zero );
                                }
                                return value;
                            }
                        };
                    }
                    else {
                        reader = new ColumnReader( short[].class, dims,
                                                   2 * count ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                short[] value = new short[ count ];
                                for ( int i = 0; i < count; i++ ) {
                                    short val = stream.readShort();
                                    value[ i ] = val;
                                }
                                return value;
                            }
                        };
                    }
                }
                return reader;

            /* Integer. */
            case 'J':
                if ( single ) {
                    if ( isScaled ) {
                        reader = new ColumnReader( Double.class, 4 ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                int val = stream.readInt();
                                return ( hasBlank && val == (int) blank ) 
                                            ? null
                                            : new Double( val * scale + zero );
                            }
                        };
                    }
                    else {
                        reader = new ColumnReader( Integer.class, 4 ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                int val = stream.readInt();
                                return ( hasBlank && val == (int) blank )
                                            ? null
                                            : new Integer( val );
                            }
                        };
                    }
                }
                else {
                    if ( isScaled ) {
                        reader = new ColumnReader( double[].class, dims,
                                                   4 * count ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                double[] value = new double[ count ];
                                for ( int i = 0; i < count; i++ ) {
                                    int val = stream.readInt();
                                    value[ i ] = 
                                        ( hasBlank && val == (int) blank )
                                             ? Double.NaN
                                             : val * scale + zero;
                                }
                                return value;
                            }
                        };
                    }
                    else {
                        reader = new ColumnReader( int[].class, dims, 
                                                   4 * count ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                int[] value = new int[ count ];
                                for ( int i = 0; i < count; i++ ) {
                                    int val = stream.readInt();
                                    // can't do anything with a blank
                                    value[ i ] = val;
                                }
                                return value;
                            }
                        };
                    }
                }
                return reader;

            /* Long. */
            case 'K':
                if ( single ) {
                    if ( isScaled ) {
                        reader = new ColumnReader( Double.class, 8 ) {
                            Object readValue( DataInput stream )
                                    throws IOException {
                                long val = stream.readLong();
                                return ( hasBlank && val == (long) blank )
                                            ? null
                                            : new Double( val * scale + zero );
                            }
                        };
                    }
                    else {
                        reader = new ColumnReader( Long.class, 8 ) {
                            Object readValue( DataInput stream )
                                    throws IOException {
                                long val = stream.readLong();
                                return ( hasBlank && val == (long) blank )
                                            ? null
                                            : new Long( val );
                            }
                        };
                    }
                }
                else {
                    if ( isScaled ) {
                        reader = new ColumnReader( double[].class, dims,
                                                   8 * count ) {
                            Object readValue( DataInput stream )
                                    throws IOException {
                                double[] value = new double[ count ];
                                for ( int i = 0; i < count; i++ ) {
                                    long val = stream.readLong();
                                    value[ i ] =
                                        ( hasBlank && val == (long) blank )
                                             ? Double.NaN
                                             : val * scale + zero;
                                }
                                return value;
                            }
                        };
                    }
                    else {
                        reader = new ColumnReader( long[].class, dims,
                                                   8 * count ) {
                            Object readValue( DataInput stream )
                                    throws IOException {
                                long[] value = new long[ count ];
                                for ( int i = 0; i < count; i++ ) {
                                    long val = stream.readLong();
                                    // can't do anything with a blank
                                    value[ i ] = val;
                                }
                                return value;
                            }
                        };
                    }
                }
                return reader;

            /* Characters. */
            case 'A':
                if ( single ) {
                    reader = new ColumnReader( Character.class, 1 ) {
                        Object readValue( DataInput stream ) 
                                throws IOException {
                            char c = (char) ( stream.readByte() & 0xff );
                            return new Character( c );
                        }
                    };
                }
                else if ( dims.length == 1 ) {
                    reader = new ColumnReader( String.class, count ) {
                        Object readValue( DataInput stream ) 
                                throws IOException {
                            return readString( stream, count );
                        }
                        int getElementSize() {
                            return count;
                        }
                    };
                }
                else {
                    int nel = 1;
                    for ( int i = 1; i < dims.length; i++ ) {
                        nel *= dims[ i ];
                    }
                    final int stringLength = dims[ 0 ];
                    final int nString = nel;
                    int[] shape = new int[ dims.length - 1 ];
                    System.arraycopy( dims, 1, shape, 0, dims.length - 1 );
                    reader = new ColumnReader( String[].class, shape, count ) {
                        Object readValue( DataInput stream ) 
                                throws IOException {
                            String[] value = new String[ nString ];
                            for ( int i = 0; i < nString; i++ ) {
                                value[ i ] = readString( stream, stringLength );
                            }
                            return value;
                        }
                        int getElementSize() {
                            return stringLength;
                        }
                    };
                }
                return reader;

            /* Floating point. */
            case 'E':
                if ( single ) {
                    if ( isScaled ) {
                        reader = new ColumnReader( Float.class, 4 ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                float val = stream.readFloat();
                                return new Float( val * scale + zero );
                            }
                        };
                    }
                    else {
                        reader = new ColumnReader( Float.class, 4 ) {
                            Object readValue( DataInput stream )
                                    throws IOException {
                                float val = stream.readFloat();
                                return new Float( val );
                            }
                        };
                    }
                }
                else {
                    reader = makeFloatsColumnReader( count, dims, scale, zero );
                }
                return reader;

            /* Double precision. */
            case 'D':
                if ( single ) {
                    if ( isScaled ) {
                        reader = new ColumnReader( Double.class, 8 ) {
                            Object readValue( DataInput stream ) 
                                    throws IOException {
                                double val = stream.readDouble();
                                return new Double( val );
                            }
                        };
                    }
                    else {
                        reader = new ColumnReader( Double.class, 8 ) {
                            Object readValue( DataInput stream )
                                    throws IOException {
                                double val = stream.readDouble();
                                return new Double( val * scale + zero );
                            }
                        };
                    }
                }
                else {
                    reader = makeDoublesColumnReader( count, dims, 
                                                      scale, zero );
                }
                return reader;

            /* Single precision complex. */
            case 'C':
                return makeFloatsColumnReader( count * 2, complexShape( dims ),
                                               scale, zero );

            /* Double precision comples. */
            case 'M':
                return makeDoublesColumnReader( count * 2, complexShape( dims ),
                                                scale, zero );

            case 'P':
                return new ColumnReader( String.class, count * 8 ) {
                    Object readValue( DataInput stream ) 
                            throws IOException {
                        for ( int i = 0; i < count * 8; i++ ) {
                            stream.readByte();
                        }
                        return "(Variable-length arrays not supported)";
                    }
                };

            default:
                throw new AssertionError( "Unknown TFORM type " + type );
        }
    }

    /**
     * Returns a reader for a column containing arrays of floats.
     */
    private static ColumnReader makeFloatsColumnReader( final int count, 
                                                        int[] shape,
                                                        final double scale,
                                                        final double zero ) {
        final boolean isScaled = scale != 1.0 || zero != 0.0;
        if ( isScaled ) {
            return new ColumnReader( float[].class, shape, 4 * count ) {
                Object readValue( DataInput stream ) 
                        throws IOException {
                    float[] value = new float[ count ];
                    for ( int i = 0; i < count; i++ ) {
                        float val = stream.readFloat();
                        value[ i ] = (float) ( val * scale + zero );
                    }
                    return value;
                }
            };
        }
        else {
            return new ColumnReader( float[].class, shape, 4 * count ) {
                Object readValue( DataInput stream )
                        throws IOException {
                    float[] value = new float[ count ];
                    for ( int i = 0; i < count; i++ ) {
                        value[ i ] = stream.readFloat();
                    }
                    return value;
                }
            };
        }
    }

    /**
     * Returns a reader for a column containing arrays of doubles.
     */
    private static ColumnReader makeDoublesColumnReader( final int count,
                                                         int[] shape,
                                                         final double scale,
                                                         final double zero ) {
        final boolean isScaled = scale != 1.0 || zero != 0.0;
        if ( isScaled ) {
            return new ColumnReader( double[].class, shape, 8 * count ) {
                Object readValue( DataInput stream ) 
                        throws IOException {
                    double[] value = new double[ count ];
                    for ( int i = 0; i < count; i++ ) {
                        double val = stream.readDouble();
                        value[ i ] = val * scale + zero;
                    }
                    return value;
                }
            };
        }
        else {
            return new ColumnReader( double[].class, shape, 8 * count ) {
                Object readValue( DataInput stream )
                        throws IOException {
                    double[] value = new double[ count ];
                    for ( int i = 0; i < count; i++ ) {
                        value[ i ] = stream.readDouble();
                    }
                    return value;
                }
            };
        }
    }

    /**
     * Returns a dimensions array based on a given one, but with an extra
     * dimension of extent 2 prepended to the list.
     *
     * @param  dims  intial dimensions array (<tt>null</tt> is interpreted
     *               as a zero-dimensional array
     * @return  like <tt>dims</tt> but with a 2 at the start
     */
    private static int[] complexShape( int[] dims ) {
        if ( dims == null ) {
            return new int[] { 2 };
        }
        else {
            int[] shape = new int[ dims.length + 1 ];
            shape[ 0 ] = 2;
            System.arraycopy( dims, 0, shape, 1, dims.length );
            return shape;
        }
    }

    /**
     * Reads a string from a data stream.
     * A fixed number of bytes are read from the stream, but the returned
     * object is a variable-length string with trailing spaces omitted.
     * If it's all spaces, <tt>null</tt> is returned.
     *
     * @param  stream  the stream to read from
     * @param  count  number of bytes to read from the stream
     * @return  string read
     */
    private static String readString( DataInput stream, int count )
            throws IOException {
        char[] letters = new char[ count ];
        int last = -1;
        boolean end = false;
        for ( int i = 0; i < count; i++ ) {
            char letter = (char) ( stream.readByte() & 0xff );
            if ( letter == 0 ) {
                end = true;
            }
            if ( ! end ) {
                letters[ i ] = letter;
                if ( letter != ' ' ) {
                    last = i;
                }
            }
        }
        int leng = last + 1;
        return leng == 0 ? null
                         : new String( letters, 0, leng );
    }

    /**
     * Abstract class defining what needs to be done to read from a 
     * stream and return an object representing a value in a given 
     * table column.
     */
    private static abstract class ColumnReader {

        private final Class clazz;
        private final int[] shape;
        private final int length;
 
        /**
         * Constructs a new reader with a given content class, shape and length.
         *
         * @param clazz  the class which <tt>readValue</tt> will return
         * @param shape  the shape to be imposed on the array returned by
         *               <tt>readValue</tt>, or <tt>null</tt> if that
         *               returns a scalar
         * @param length  the number of bytes <tt>readValue</tt> reads from
         *                the stream
         */
        ColumnReader( Class clazz, int[] shape, int length ) {
            this.clazz = clazz;
            this.length = length;
            this.shape = shape;
        }

        /**
         * Constructs a scalar reader with a given content class and length.
         *
         * @param clazz  the class which <tt>readValue</tt> will return
         *               (shouldn't be an array)
         * @param length  the number of bytes <tt>readValue</tt> reads from
         *                the stream
         */
        ColumnReader( Class clazz, int length ) {
            this( clazz, null, length );
        }

        /**
         * Reads bytes from a stream to return an object.
         *
         * @param  stream containing bytes to turn into an object
         * @return  an object read from the stream of type 
         *          <tt>getContentClass</tt> (or <tt>null</tt>)
         */
        abstract Object readValue( DataInput stream ) throws IOException;

        /**
         * Returns the class which objects returned by <tt>readValue</tt>
         * will belong to.
         *
         * @return  value class
         */
        Class getContentClass() {
            return clazz;
        }

        /**
         * Returns the shape imposed on array elements.
         *
         * @param  shape, or null for scalars
         */
        int[] getShape() {
            return shape;
        }

        /**
         * Returns string size etc.
         * 
         * @return element size, or -1 if not applicable
         */
        int getElementSize() {
            return -1;
        }

        /**
         * Returns the number of bytes each call to <tt>readValue</tt> reads.
         *
         * @return  byte count
         */
        int getLength() {
            return length;
        }
    }
}
