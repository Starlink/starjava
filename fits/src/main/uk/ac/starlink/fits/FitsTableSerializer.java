package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Class which knows how to do the various bits of serializing a StarTable
 * to FITS BINTABLE format.  This does the hard work for FitsTableWriter.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableSerializer {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.fits" );

    private final StarTable table;
    private final ColumnWriter[] colWriters;
    private final ColumnInfo[] colInfos;
    private final long rowCount;

    /**
     * Constructs a serializer which will be able to write a given StarTable.
     *
     * @param  table  the table to be written
     */
    public FitsTableSerializer( StarTable table ) throws IOException {
        this.table = table;

        /* Get table dimensions (though we may need to calculate the row
         * count directly later. */
        int ncol = table.getColumnCount();
        long nrow = table.getRowCount();

        /* Store column infos. */
        colInfos = Tables.getColumnInfos( table );

        /* Work out column shapes, and check if any are unknown (variable
         * last dimension). */
        boolean hasVarShapes = false;
        boolean hasNullableInts = false;
        int[][] shapes = new int[ ncol ][];
        int[] maxChars = new int[ ncol ];
        boolean[] useCols = new boolean[ ncol ];
        boolean[] varShapes = new boolean[ ncol ];
        boolean[] varChars = new boolean[ ncol ];
        boolean[] varElementChars = new boolean[ ncol ];
        boolean[] nullableInts = new boolean[ ncol ];
        Arrays.fill( useCols, true );
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo colinfo = colInfos[ icol ];
            Class clazz = colinfo.getContentClass();
            if ( clazz.isArray() ) {
                shapes[ icol ] = (int[]) colinfo.getShape().clone();
                int[] shape = shapes[ icol ];
                if ( shape[ shape.length - 1 ] < 0 ) {
                    varShapes[ icol ] = true;
                    hasVarShapes = true;
                }
                if ( clazz.getComponentType().equals( String.class ) ) {
                    maxChars[ icol ] = colinfo.getElementSize();
                    if ( maxChars[ icol ] <= 0 ) {
                        varElementChars[ icol ] = true;
                        hasVarShapes = true;
                    }
                }
            }
            else if ( clazz.equals( String.class ) ) {
                maxChars[ icol ] = colinfo.getElementSize();
                if ( maxChars[ icol ] <= 0 ) {
                    varChars[ icol ] = true;
                    hasVarShapes = true;
                }
            }
            else if ( colinfo.isNullable() && 
                      ( clazz == Byte.class || clazz == Short.class ||
                        clazz == Integer.class || clazz == Long.class ) ) {
                nullableInts[ icol ] = true;
                hasNullableInts = true;
            }
        }

        /* If necessary, make a first pass through the table data to
         * find out the maximum size of variable length fields and the length
         * of the table. */
        boolean[] hasNulls = new boolean[ ncol ];
        if ( hasVarShapes || hasNullableInts || nrow < 0 ) {
            int[] maxElements = new int[ ncol ];
            nrow = 0L;

            /* Get the maximum dimensions. */
            RowSequence rseq = table.getRowSequence();
            try {
                while ( rseq.next() ) {
                    nrow++;
                    for ( int icol = 0; icol < ncol; icol++ ) {
                        if ( useCols[ icol ] &&
                             ( varShapes[ icol ] || 
                               varChars[ icol ] ||
                               varElementChars[ icol ] || 
                               ( nullableInts[ icol ] && 
                                 ! hasNulls[ icol ] ) ) ) {
                            Object cell = rseq.getCell( icol );
                            if ( cell == null ) {
                                if ( nullableInts[ icol ] ) {
                                    hasNulls[ icol ] = true;
                                }
                            }
                            else {
                                if ( varChars[ icol ] ) {
                                    int leng = ((String) cell).length();
                                    maxChars[ icol ] =
                                        Math.max( maxChars[ icol ], leng );
                                }
                                else if ( varElementChars[ icol ] ) {
                                    String[] svals = (String[]) cell;
                                    for ( int i = 0; i < svals.length; i++ ) {
                                        maxChars[ icol ] =
                                            Math.max( maxChars[ icol ],
                                                      svals[ i ].length() );
                                    }
                                }
                                if ( varShapes[ icol ] ) {
                                    maxElements[ icol ] =
                                        Math.max( maxElements[ icol ],
                                                  Array.getLength( cell ) );
                                }
                            }
                        }
                    }
                }
            }
            finally {
                rseq.close();
            }

            /* In the case of variable string lengths and no non-null data
             * in any of the cells, maxChars could still be set negative.
             * Fix that here. */
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( maxChars[ icol ] < 0 ) {
                    maxChars[ icol ] = 0;
                }
            }

            /* Work out the actual shapes for columns which have variable ones,
             * based on the shapes that we encountered in the rows. */
            if ( hasVarShapes ) {
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( useCols[ icol ] ) {
                        if ( varShapes[ icol ] ) {
                            int[] shape = shapes[ icol ];
                            int ndim = shape.length;
                            assert shape[ ndim - 1 ] <= 0;
                            int nel = 1;
                            for ( int i = 0; i < ndim - 1; i++ ) {
                                nel *= shape[ i ];
                            }
                            shape[ ndim - 1 ] =
                                Math.max( 1, ( maxElements[ icol ]
                                               + nel - 1 ) / nel );
                        }
                    }
                }
            }
        }

        /* Store the row count, which we must have got by now. */
        assert nrow >= 0;
        rowCount = nrow;

        /* We now have all the information we need about the table.
         * Construct and store a custom writer for each column which 
         * knows about the characteristics of the column and how to 
         * write values to the stream.  For columns which can't be 
         * written in FITS format store a null in the writers array
         * and log a message. */
        colWriters = new ColumnWriter[ ncol ];
        int rbytes = 0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( useCols[ icol ] ) {
                ColumnInfo cinfo = colInfos[ icol ];
                ColumnWriter writer =
                    ColumnWriter
                   .makeColumnWriter( cinfo, shapes[ icol ], maxChars[ icol ],
                                      nullableInts[ icol ] 
                                      && hasNulls[ icol ] );
                if ( writer == null ) {
                    logger.warning( "Ignoring column " + cinfo.getName() +
                                    " - don't know how to write to FITS" );
                }
                colWriters[ icol ] = writer;
            }
        }
    }

    /**
     * Writes the header block for the BINTABLE extension HDU which holds
     * the table data.
     *
     * @param  strm   destination stream
     */
    public void writeHeader( DataOutput strm ) throws IOException {

        /* Work out the dimensions in columns and bytes of the table. */
        int rowLength = 0;
        int nUseCol = 0;
        int ncol = table.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnWriter writer = colWriters[ icol ];
            if ( writer != null ) {
                nUseCol++;
                rowLength += writer.getLength();
            }
        }

        /* Prepare a FITS header block. */
        Header hdr = new Header();
        try {

            /* Prepare the overall HDU metadata. */
            hdr.addValue( "XTENSION", "BINTABLE", "binary table extension" );
            hdr.addValue( "BITPIX", 8, "8-bit bytes" );
            hdr.addValue( "NAXIS", 2, "2-dimensional table" );
            hdr.addValue( "NAXIS1", rowLength, "width of table in bytes" );
            hdr.addValue( "NAXIS2", rowCount, "number of rows in table" );
            hdr.addValue( "PCOUNT", 0, "size of special data area" );
            hdr.addValue( "GCOUNT", 1, "one data group" );
            hdr.addValue( "TFIELDS", nUseCol, "number of columns" );

            /* Prepare the per-column HDU metadata. */
            int jcol = 0;
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnWriter colwriter = colWriters[ icol ];
                if ( colwriter != null ) {
                    jcol++;
                    String forcol = " for column " + jcol;
                    ColumnInfo colinfo = colInfos[ icol ];

                    /* Name. */
                    String name = colinfo.getName();
                    if ( name != null && name.trim().length() > 0 ) {
                        hdr.addValue( "TTYPE" + jcol, name, "label" + forcol );
                    }

                    /* Format. */
                    String form = colwriter.getFormat();
                    hdr.addValue( "TFORM" + jcol, form, "format" + forcol );

                    /* Units. */
                    String unit = colinfo.getUnitString();
                    if ( unit != null && unit.trim().length() > 0 ) {
                        hdr.addValue( "TUNIT" + jcol, unit, "units" + forcol );
                    }

                    /* Blank. */
                    Number bad = colwriter.getBadNumber();
                    if ( bad != null ) {
                        hdr.addValue( "TNULL" + jcol, bad.toString(),
                                      "blank value" + forcol );
                    }

                    /* Shape. */
                    int[] dims = colwriter.getDims();
                    if ( dims != null && dims.length > 1 ) {
                        StringBuffer sbuf = new StringBuffer();
                        for ( int i = 0; i < dims.length; i++ ) {
                            sbuf.append( i == 0 ? '(' : ',' );
                            sbuf.append( dims[ i ] );
                        }
                        sbuf.append( ')' );
                        hdr.addValue( "TDIM" + jcol, sbuf.toString(),
                                      "dimensions" + forcol );
                    }

                    /* Scaling. */
                    double zero = colwriter.getZero();
                    double scale = colwriter.getScale();
                    if ( zero != 0.0 ) {
                        hdr.addValue( "TZERO" + jcol, zero, "base" + forcol );
                    }
                    if ( scale != 1.0 ) {
                        hdr.addValue( "TSCALE" + jcol, scale,
                                      "factor" + forcol );
                    }

                    /* Comment (non-standard). */
                    String comm = colinfo.getDescription();
                    if ( comm != null && comm.trim().length() > 0 ) {
                        if ( comm.length() > 67 ) {
                            comm = comm.substring( 0, 68 );
                        }
                        try {
                            hdr.addValue( "TCOMM" + jcol, comm, null );
                        }
                        catch ( HeaderCardException e ) {
                            logger.warning( "Description " + comm +
                                            " too long for FITS header" );
                        }
                    }
                }
            }
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* Write the header block out. */
        FitsConstants.writeHeader( strm, hdr );
    }

    /**
     * Writes the data part of the BINTABLE extension HDU which holds
     * the table data.
     *
     * @param  strm   destination stream
     */
    public void writeData( DataOutput strm ) throws IOException {

        /* Work out the length of each row in bytes. */
        int rowBytes = 0;
        int ncol = table.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnWriter writer = colWriters[ icol ];
            if ( writer != null ) {
                rowBytes += writer.getLength();
            }
        }

        /* Write the data cells, delegating the item in each column to
         * the writer that knows how to handle it. */
        long nWritten = 0L;
        RowSequence rseq = table.getRowSequence();
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    ColumnWriter writer = colWriters[ icol ];
                    if ( writer != null ) {
                        writer.writeValue( strm, row[ icol ] );
                    }
                }
                nWritten += rowBytes;
            }
        }
        finally {
            rseq.close();
        }

        /* Write padding. */
        int extra = (int) ( nWritten % (long) 2880 );
        if ( extra > 0 ) {
            strm.write( new byte[ 2880 - extra ] );
        }
    }

    /**
     * Returns the FITS TFORM letter which describes the type of data
     * output for a given column.  This is as described by the FITS
     * standard - 'J' for 4-byte integer, 'A' for characters, etc.
     * If the column is not being output, <tt>(char)0</tt> will be 
     * returned.
     *
     * @param  icol   column to query
     * @return   format letter for data in column <tt>icol</tt>,
     *           or 0 for a column being skipped
     */
    public char getFormatChar( int icol ) {
        if ( colWriters[ icol ] == null ) {
            return (char) 0;
        }
        else {
            return colWriters[ icol ].getFormatChar();
        }
    }

    /**
     * Returns the dimensions of the items which will be output for a 
     * given column.  This will be <tt>null</tt> only if that column
     * is not being output.  Otherwise it will be a zero-element array
     * for a scalar, 1-element array for a vector, etc.
     *
     * @param  icol  column to query
     * @return   dimensions array for data in column <tt>icol</tt> 
     *           or <tt>null</tt>  for a column being skipped
     */
    public int[] getDimensions( int icol ) {
        if ( colWriters[ icol ] == null ) {
            return null;
        }
        else {
            int[] dims = colWriters[ icol ].getDims();
            return dims == null ? new int[ 0 ] : dims;
        }
    }
}
