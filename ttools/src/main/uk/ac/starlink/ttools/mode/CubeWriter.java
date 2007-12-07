package uk.ac.starlink.ttools.mode;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;
import uk.ac.starlink.util.Destination;

/**
 * TableConsumer implementation which constructs and outputs a histogram
 * data cube for an input table.
 *
 * @author   Mark Taylor
 * @since    9 May 2006
 */
public class CubeWriter implements TableConsumer {

    private final double[] loBounds_;
    private final double[] hiBounds_;
    private final String[] colIds_;
    private final String scaleId_;
    private final Class outType_;
    private final Destination dest_;
    private int[] nbins_;
    private double[] binSizes_;

    /**
     * Constructor.
     * One, but not both, of <code>nbins</code> and <code>binSizes</code>
     * may be null (it will be worked out from the other).
     * Elements of <code>loBounds</code> and <code>hiBounds</code> may
     * be NaN to indicate that the corresponding bound should be calculated
     * from a pass through the data.
     *
     * @param   loBounds   lower bounds for each dimension
     * @param   hiBounds   upper bounds for each dimension
     * @param   nbins      number of bins in each dimension
     * @param   binSizes   extent of bins in each dimension
     * @param   colIds     column ID strings for axes
     * @param   scaleId    column ID string for scale column (or null)
     * @param   dest       data output locator
     * @param   outType    primitive numeric data type for output data;
     *                     if null worked out automatically
     */
    public CubeWriter( double[] loBounds, double[] hiBounds, int[] nbins,
                       double[] binSizes, String[] colIds, String scaleId,
                       Destination dest, Class outType ) {
        loBounds_ = loBounds;
        hiBounds_ = hiBounds;
        nbins_ = nbins;
        binSizes_ = binSizes;
        colIds_ = colIds;
        scaleId_ = scaleId;
        dest_ = dest;
        outType_ = outType;
    }

    public void consume( final StarTable inTable ) throws IOException {

        /* Permute table columns so that only the selected ones appear in
         * the table we're working with. */
        int ndim = colIds_.length;
        StarTable aTable = getPermutedTable( inTable, colIds_ );
        StarTable asTable;
        if ( scaleId_ != null ) {
            String[] ids = new String[ ndim + 1 ];
            System.arraycopy( colIds_, 0, ids, 0, ndim );
            ids[ ndim ] = scaleId_;
            asTable = getPermutedTable( inTable, ids );
        }
        else {
            ColumnStarTable unitTable = new ColumnStarTable( inTable ) {
                public long getRowCount() {
                    return inTable.getRowCount();
                }
            };
            unitTable.addColumn( new UnitColumnData() );
            asTable =
                new JoinStarTable( new StarTable[] { aTable, unitTable } );
        }
 
        /* Read data to acquire bounds from the data if necessary. */
        fixBounds( aTable, loBounds_, hiBounds_ );

        /* Calculate bin sizes from bin counts or vice versa. */
        if ( binSizes_ == null ) {
            assert nbins_ != null;
            binSizes_ = new double[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                binSizes_[ i ] =
                    ( hiBounds_[ i ] - loBounds_[ i ] ) / nbins_[ i ];
            }
        }
        else if ( nbins_ == null ) {
            assert binSizes_ != null;
            nbins_ = new int[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                nbins_[ i ] =
                    (int) Math.ceil( ( hiBounds_[ i ] - loBounds_[ i ] )
                                     / binSizes_[ i ] );
            }
        }

        /* Populate the cube by reading the table data. */
        double[] cube = calculateCube( asTable, loBounds_, nbins_, binSizes_ );

        /* Write the cube to the output stream as FITS. */
        DataOutputStream out = new DataOutputStream(
                                   new BufferedOutputStream(
                                       dest_.createStream() ) );
        try {
            writeFits( Tables.getColumnInfos( aTable ),
                       asTable.getColumnInfo( ndim ), cube, outType_, out );
        }
        finally {
            out.close();
        }
    }

    /**
     * Returns a table which is made from the named columns selected from
     * a given input table.
     *
     * @param  baseTable   input table
     * @param  colIds     list of column IDs describing the table you want
     * @return  table consisting of only <code>colIds</code> out of 
     *          <code>baseTable</code>
     */
    private static StarTable getPermutedTable( StarTable baseTable,
                                               String[] colIds )
            throws IOException {
        ColumnIdentifier ident = new ColumnIdentifier( baseTable );
        int ndim = colIds.length;
        int[] colMap = new int[ ndim ];
        for ( int idim = 0; idim < ndim; idim++ ) {
            colMap[ idim ] = ident.getColumnIndex( colIds[ idim ] );
        }
        StarTable dimTable = new ColumnPermutedStarTable( baseTable, colMap );
        for ( int idim = 0; idim < ndim; idim++ ) {
            ColumnInfo info = dimTable.getColumnInfo( idim );
            if ( ! Number.class.isAssignableFrom( info.getContentClass() ) ) {
                throw new IOException( "Column " + info +
                                       " not numeric" );
            }
        }
        return dimTable;
    }

    /**
     * Ensure that the elements of lower and upper bounds arrays contain
     * usable (non-NaN) limits which can define a cube extent.
     * If any of the elements are NaN, then a read is made of all the
     * table rows to determine the limits defined by the extrema of
     * the data and the bounds arrays updated.  Thus on exit, all the
     * elements of <code>loBounds</code> and <code>hiBounds</code>
     * will be non-NaN.
     *
     * @param   table  table containing only numeric columns
     * @param   loBounds   lower bounds array
     * @param   hiBounds   upper bounds array
     */
    private static void fixBounds( StarTable table, double[] loBounds,
                                   double[] hiBounds )
            throws IOException {

        /* See if we need to perform any automatic bounds assessment. */
        int ndim = table.getColumnCount();
        boolean autobound = false;
        for ( int idim = 0; idim < ndim; idim++ ) {
            autobound = autobound || Double.isNaN( loBounds[ idim ] )
                                  || Double.isNaN( hiBounds[ idim ] );
        }

        /* If so, read all the data accumulating extrema and update the
         * bounds arrays accordingly. */
        if ( autobound ) {
            double[][] dataBounds = getDataBounds( table );
            for ( int idim = 0 ; idim < ndim; idim++ ) {
                if ( Double.isNaN( loBounds[ idim ] ) ) {
                    loBounds[ idim ] = dataBounds[ idim ][ 0 ];
                }
                if ( Double.isNaN( hiBounds[ idim ] ) ) {
                    hiBounds[ idim ] = dataBounds[ idim ][ 1 ];
                }
                if ( Double.isNaN( loBounds[ idim ] ) ||
                     Double.isNaN( hiBounds[ idim ] ) ) {
                    throw new IOException( "Can't get bounds for "
                                         + table.getColumnInfo( idim ).getName()
                                         + " - no data?" );
                }
            }
        }
    }

    /**
     * Reads the data from a table and determines extrema.
     *
     * @param   table  input table containing only numeric columns
     * @return  array of 2-element values giving lower,higher bounds for
     *          the data in each column of <code>table</code>
     */
    private static double[][] getDataBounds( StarTable table )
            throws IOException {
        int ndim = table.getColumnCount();
        double[][] bounds = new double[ ndim ][ 2 ];
        for ( int idim = 0; idim < ndim; idim++ ) {
            bounds[ idim ] = new double[] { Double.NaN, Double.NaN };
        }
        RowSequence rseq = table.getRowSequence();
        try {
            while ( rseq.next() ) {
                 Object[] row = rseq.getRow();
                 for ( int idim = 0; idim < ndim; idim++ ) {
                     Object cell = row[ idim ];
                     if ( cell instanceof Number ) {
                         double dval = ((Number) cell).doubleValue();
                         if ( ! Double.isInfinite( dval ) &&
                              ! Double.isNaN( dval ) ) {
                             if ( ! ( dval >= bounds[ idim ][ 0 ] ) ) {
                                 bounds[ idim ][ 0 ] = dval;
                             }
                             if ( ! ( dval <= bounds[ idim ][ 1 ] ) ) {
                                 bounds[ idim ][ 1 ] = dval;
                             }
                         }
                     }
                 }
            }
        }
        finally {
            rseq.close();
        }
        return bounds;
    }

    /**
     * Accumulates the contents of an N-dimensional histogram representing
     * data from an N+1-columned table.  The final column is a scaling
     * value.
     *
     * @param   table  table with N+1 columns
     * @param   loBounds  N-element array of lower bounds by dimension
     * @param   nbins     N-element array of number of bins by dimension
     * @param   binSizes  N-element array of bin extents by dimension
     */
    public static double[] calculateCube( StarTable table, double[] loBounds, 
                                          int[] nbins, double[] binSizes )
            throws IOException {
        int ndim = table.getColumnCount() - 1;
        double[] hiBounds = new double[ ndim ];
        for ( int idim = 0; idim < ndim; idim++ ) {
            hiBounds[ idim ] = loBounds[ idim ]
                             + nbins[ idim ] * binSizes[ idim ];
        }

        /* Construct a cube to hold the counts. */
        long np = 1;
        for ( int idim = 0; idim < ndim; idim++ ) {
            np *= nbins[ idim ];
        }
        int npix = Tables.checkedLongToInt( np );
        double[] cube = new double[ npix ];

        /* Populate it. */
        RowSequence rseq = table.getRowSequence();
        try {
            int[] coords = new int[ ndim ];
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();

                /* Get the scaling value. */
                Object scaleObj = row[ ndim ];
                double scale = scaleObj instanceof Number 
                             ? ((Number) scaleObj).doubleValue()
                             : Double.NaN;
                boolean okRow = scale != 0.0 && ! Double.isNaN( scale );

                for ( int idim = 0; okRow && idim < ndim; idim++ ) {
                    boolean okCell = false;
                    Object cell = row[ idim ];
                    if ( cell instanceof Number ) {
                        double dval = ((Number) cell).doubleValue();

                        /* This criterion is questionable - it should really
                         * be exclusive at the upper bound (dval < hiBounds).
                         * However, if the bounds have been calculated
                         * automatically you'd expect every point to be 
                         * included.  For integer columns the answer would
                         * possibly be to shift everything by half a pixel.
                         * Hmm. */
                        if ( dval >= loBounds[ idim ] &&  
                             dval <= hiBounds[ idim ] ) {
                            int ibin = (int) ( ( dval - loBounds[ idim ] )
                                               / binSizes[ idim ] );
                            if ( ibin == nbins[ idim ] ) {
                                ibin--;
                            }
                            assert ibin >= 0 && ibin <= nbins[ idim ];
                            coords[ idim ] = ibin;
                            okCell = true;
                        }
                    }
                    okRow = okRow && okCell;
                }
                if ( okRow ) {
                    int ipix = 0;
                    for ( int idim = 0, step = 1; idim < ndim; idim++ ) {
                        ipix += step * coords[ idim ];
                        step *= nbins[ idim ];
                    }
                    cube[ ipix ] += scale;
                }
            }
        }
        finally { 
            rseq.close();
        }
        return cube;
    }

    /**
     * Writes a column-major array out as a single-HDU FITS file.
     *
     * @param   axInfos  metadata objects describing each axis
     * @param   binInfo  metadata object describing the bin values
     * @param   cube   data array, in column major order
     * @param   outType   primitive numeric type to write
     * @param   out    output stream
     */
    private void writeFits( ValueInfo[] axInfos, ValueInfo binInfo,
                            double[] cube, Class outType, DataOutputStream out )
            throws IOException {
        int npix = cube.length;
        int ndim = nbins_.length;

        /* Get minimum and maximum values. */
        double min = Double.NaN;
        double max = Double.NaN;
        if ( npix > 0 ) {
            for ( int i = 0; i < npix; i++ ) {
                double datum = cube[ i ];
                if ( ! ( datum >= min ) ) {
                    min = datum;
                }
                if ( ! ( datum <= max ) ) {
                    max = datum;
                }
            }
        }
        if ( min == Double.NaN ) {
            assert max == Double.NaN;
        }

        /* Get a suitable writer for writing the numeric data to FITS. */
        Class clazz = outType;
        if ( clazz == null ) {
            if ( min == Double.NaN ) {
                clazz = byte.class;
            }
            else {
                Class binType = binInfo.getContentClass();
                if ( binType == Byte.class ||
                     binType == Short.class ||
                     binType == Integer.class ||
                     binType == Long.class ) {
                    if ( min >= Byte.MIN_VALUE && max <= Byte.MAX_VALUE ) {
                        clazz = byte.class;
                    }
                    else if ( min >= Short.MIN_VALUE &&
                              max <= Short.MAX_VALUE ) {
                        clazz = short.class;
                    }
                    else if ( min >= Integer.MIN_VALUE &&
                              max <= Integer.MAX_VALUE ) {
                        clazz = int.class;
                    }
                }
            }
            if ( clazz == null ) {
                clazz = double.class;
            }
        }
        NumberWriter writer = createNumberWriter( out, clazz );

        /* Assemble and write the FITS header. */
        Header hdr = new Header();
        try {
            hdr.addValue( "SIMPLE", true, "" );
            hdr.addValue( "BITPIX", writer.getBitpix(), "Data type" );
            hdr.addValue( "NAXIS", nbins_.length, "Number of axes" );
            for ( int id = 0; id < ndim; id++ ) {
                hdr.addValue( "NAXIS" + ( id + 1 ), nbins_[ id ],
                              "Dimension " + ( id + 1 ) );
            }
            hdr.addValue( "DATE", Times.mjdToIso( Times.unixMillisToMjd(
                                                System.currentTimeMillis() ) ),
                          "HDU creation date" );
            hdr.addValue( "BUNIT", "COUNTS",
                          "Number of points per pixel (bin)" );
            hdr.addValue( "DATAMIN", (double) min, "Minimum value" );
            hdr.addValue( "DATAMAX", (double) max, "Maximum value" );
            for ( int id = 0; id < ndim; id++ ) {
                ValueInfo info = axInfos[ id ];
                String units = info.getUnitString();
                String desc = info.getDescription();
                hdr.addValue( "CTYPE" + ( id + 1 ), info.getName(),
                              desc != null ? desc : "" );
                if ( units != null ) {
                    hdr.addValue( "CUNIT" + ( id + 1 ),  // unofficial card name
                                  info.getUnitString(), "Units" );
                }
                hdr.addValue( "CRVAL" + ( id + 1 ), 0.0,
                              "Reference pixel position (" + ( id + 1 ) + ")" );
                hdr.addValue( "CDELT" + ( id + 1 ), binSizes_[ id ],
                              "Reference pixel extent (" + ( id + 1 ) + ")" );
                hdr.addValue( "CRPIX" + ( id + 1 ),
                              - loBounds_[ id ] / binSizes_[ id ],
                              "Reference pixel index (" + ( id + 1 ) + ")" );
            }
            hdr.addValue( "ORIGIN", "STILTS version " + Stilts.getVersion()
                                  + " (" + getClass().getName() + ")", null );
            FitsConstants.writeHeader( out, hdr );
        }
        catch ( HeaderCardException e ) {
            throw (IOException) new IOException( "Trouble with FITS headers: "
                                               + e.getMessage() )
                               .initCause( e );
        }

        /* Write the data. */
        for ( int ip = 0; ip < npix; ip++ ) {
            writer.writeNumber( cube[ ip ] );
        }

        /* Pad to the end of a FITS block. */
        long nbyte = ( Math.abs( writer.getBitpix() ) / 8 ) * (long) npix;
        int over = (int) ( nbyte % FitsConstants.FITS_BLOCK );
        if ( over > 0 ) {
            out.write( new byte[ FitsConstants.FITS_BLOCK - over ] );
        }
        out.flush();
    }

    /**
     * Constructs an object which can write signed integer values to a
     * DataOutput object.
     *
     * @param   out  destination stream
     * @param   clazz  primitive numeric type for output
     */
    public static NumberWriter createNumberWriter( final DataOutput out,
                                                   Class clazz ) {
        if ( clazz == byte.class ) {
            return new NumberWriter( 8 ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeByte( (byte) value );
                }
            };
        }
        else if ( clazz == short.class ) {
            return new NumberWriter( 16 ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeShort( (short) value );
                }
            };
        }
        else if ( clazz == int.class ) {
            return new NumberWriter( 32 ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeInt( (int) value );
                }
            };
        }
        else if ( clazz == long.class ) {
            return new NumberWriter( 64 ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeLong( (long) value );
                }
            };
        }
        else if ( clazz == float.class ) {
            return new NumberWriter( -32 ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeFloat( (float) value );
                }
            };
        }
        else if ( clazz == double.class ) {
            return new NumberWriter( -64 ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeDouble( (double) value );
                }
            };
        }
        else {
            assert false;
            return new NumberWriter( 64 ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeDouble( (double) value );
                }
            };
        }
    }

    /**
     * Defines an object which can dispose of integer values.
     */
    private abstract static class NumberWriter {

        private final int bitpix_;

        /**
         * Constructor.
         *
         * @param  bitpix  appropriate value for FITS BITPIX header card.
         */
        protected NumberWriter( int bitpix ) {
            bitpix_ = bitpix;
        }

        /**
         * Accepts a double precision value.
         *
         * @param   value  value to use
         */
        public abstract void writeNumber( double value ) throws IOException;

        /**
         * Return the appropriate value for the FITS BITPIX header card.
         */
        public int getBitpix() {
            return bitpix_;
        }
    }

    /**
     * ColumnData implementation that returns 1.
     */
    private static class UnitColumnData extends ColumnData {
        private final Integer unit_ = new Integer( 1 );
        UnitColumnData() {
            super( new DefaultValueInfo( "Counts", Integer.class ) );
        }
        public Object readValue( long irow ) {
            return unit_;
        }
    }
}
