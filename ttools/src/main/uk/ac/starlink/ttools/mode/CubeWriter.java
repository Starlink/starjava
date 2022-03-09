package uk.ac.starlink.ttools.mode;

import gnu.jel.CompilationException;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.fits.CardFactory;
import uk.ac.starlink.fits.CardImage;
import uk.ac.starlink.fits.FitsUtil;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.ttools.jel.JELTable;
import uk.ac.starlink.ttools.plot2.layer.BinList;
import uk.ac.starlink.ttools.plot2.layer.BinListCollector;
import uk.ac.starlink.ttools.plot2.layer.Combiner;
import uk.ac.starlink.util.DataBufferedOutputStream;
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
    private final String[] colExprs_;
    private final String scaleExpr_;
    private final Combiner combiner_;
    private final Class<?> outType_;
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
     * @param   colExprs   expression strings for axes
     * @param   scaleExpr  expression string for scale column (or null)
     * @param   combiner   combination mode
     * @param   dest       data output locator
     * @param   outType    primitive numeric data type for output data;
     *                     if null worked out automatically
     */
    public CubeWriter( double[] loBounds, double[] hiBounds, int[] nbins,
                       double[] binSizes, String[] colExprs, String scaleExpr,
                       Combiner combiner, Destination dest, Class<?> outType ) {
        loBounds_ = loBounds;
        hiBounds_ = hiBounds;
        nbins_ = nbins;
        binSizes_ = binSizes;
        colExprs_ = colExprs;
        scaleExpr_ = scaleExpr;
        combiner_ = combiner;
        dest_ = dest;
        outType_ = outType;
    }

    public void consume( final StarTable inTable ) throws IOException {
        int ndim = colExprs_.length;
        String[] exprs = new String[ ndim + 1 ];
        System.arraycopy( colExprs_, 0, exprs, 0, ndim );
        exprs[ ndim ] = scaleExpr_ == null ? "1" : scaleExpr_;
        StarTable asTable;
        try {
            asTable = JELTable.createJELTable( inTable, exprs );
        }
        catch ( CompilationException e ) {
            throw new IOException( "Bad expression " + e.getMessage() );
        }
        int[] icData = new int[ ndim ];
        for ( int ic = 0; ic < ndim; ic++ ) {
            icData[ ic ] = ic;
        }
        StarTable aTable = new ColumnPermutedStarTable( asTable, icData );
 
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
        double[] cube =
            calculateCube( asTable, combiner_, loBounds_, nbins_, binSizes_ );

        /* Write the cube to the output stream as FITS. */
        DataBufferedOutputStream out =
            new DataBufferedOutputStream( dest_.createStream() );
        try {
            writeFits( Tables.getColumnInfos( aTable ),
                       asTable.getColumnInfo( ndim ), cube, outType_, out );
            out.flush();
        }
        finally {
            out.close();
        }
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
     * @param   combiner  combination mode
     * @param   loBounds  N-element array of lower bounds by dimension
     * @param   nbins     N-element array of number of bins by dimension
     * @param   binSizes  N-element array of bin extents by dimension
     */
    public static double[]
            calculateCube( StarTable table, Combiner combiner,
                           double[] loBounds, int[] nbins, double[] binSizes )
            throws IOException {
        int ndim = table.getColumnCount() - 1;
        double[] hiBounds = new double[ ndim ];
        double binExtent = 1;
        for ( int idim = 0; idim < ndim; idim++ ) {
            hiBounds[ idim ] = loBounds[ idim ]
                             + nbins[ idim ] * binSizes[ idim ];
            binExtent *= binSizes[ idim ];
        }
        Combiner.Type ctype = combiner.getType();
        double binFactor = ctype.getBinFactor( binExtent );

        /* Construct a cube to hold the counts. */
        long np = 1;
        for ( int idim = 0; idim < ndim; idim++ ) {
            np *= nbins[ idim ];
        }
        int npix = Tables.checkedLongToInt( np );
        BinList binList = BinListCollector.createDefaultBinList( combiner, np );

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
                    binList.submitToBin( ipix, scale );
                }
            }
        }
        finally { 
            rseq.close();
        }

        /* Assemble bins into a cube array. */
        BinList.Result result = binList.getResult();
        double[] cube = new double[ npix ];
        if ( ! ctype.isExtensive() ) {
            Arrays.fill( cube, Double.NaN );
        }
        for ( Iterator<Long> it = result.indexIterator(); it.hasNext(); ) {
            int index = it.next().intValue();
            cube[ index ] = result.getBinValue( index ) * binFactor;
        }
        return cube;
    }

    /**
     * Writes a column-major array out as a single-HDU FITS file.
     *
     * @param   axInfos  metadata objects describing each axis
     * @param   binInfo  metadata object describing the bin values
     * @param   cube     data array, in column major order
     * @param   outType   primitive numeric type to write
     * @param   out    output stream
     */
    private void writeFits( ValueInfo[] axInfos, ValueInfo binInfo,
                            double[] cube, Class<?> outType,
                            DataBufferedOutputStream out )
            throws IOException {
        int npix = cube.length;
        int ndim = nbins_.length;

        /* Get minimum and maximum values. */
        double min = Double.NaN;
        double max = Double.NaN;
        boolean isInt = true;
        boolean hasBlank = false;
        if ( npix > 0 ) {
            for ( int i = 0; i < npix; i++ ) {
                double datum = cube[ i ];
                if ( ! ( datum >= min ) ) {
                    min = datum;
                }
                if ( ! ( datum <= max ) ) {
                    max = datum;
                }
                boolean isNaN = Double.isNaN( datum );
                hasBlank = hasBlank || isNaN;
                isInt = isInt && ( isNaN || (int) datum == datum );
            }
        }
        if ( Double.isNaN( min ) ) {
            assert max == Double.NaN;
        }

        /* Get a suitable writer for writing the numeric data to FITS. */
        Class<?> clazz = outType;
        if ( clazz == null ) {
            if ( Double.isNaN( min ) ) {
                clazz = byte.class;
            }
            else if ( isInt ) {
                if ( min >= Byte.MIN_VALUE + 1 &&
                     max <= Byte.MAX_VALUE ) {
                    clazz = byte.class;
                }
                else if ( min >= Short.MIN_VALUE + 1 &&
                          max <= Short.MAX_VALUE ) {
                    clazz = short.class;
                }
                else if ( min >= Integer.MIN_VALUE + 1 &&
                          max <= Integer.MAX_VALUE ) {
                    clazz = int.class;
                }
            }
        }
        if ( clazz == null ) {
            clazz = double.class;
        }
        NumberWriter writer = createNumberWriter( out, clazz );

        /* Assemble and write the FITS header. */
        CardFactory cf = CardFactory.STRICT;
        List<CardImage> cards = new ArrayList<>();
        cards.addAll( Arrays.asList( new CardImage[] {
            cf.createLogicalCard( "SIMPLE", true, null ),
            cf.createIntegerCard( "BITPIX", writer.getBitpix(), "Data type" ),
            cf.createIntegerCard( "NAXIS", nbins_.length, "Number of axes" ),
        } ) );
        for ( int id = 0; id < ndim; id++ ) {
            cards.add( cf.createIntegerCard( "NAXIS" + ( id + 1 ), nbins_[ id ],
                                             "Dimension " + ( id + 1 ) ) );
        }
        String isoDate =
            Times
           .mjdToIso( Times.unixMillisToMjd( System.currentTimeMillis() ) );
        cards.add( cf.createStringCard( "DATE", isoDate, "HDU creation date" ));
        cards.add( cf.createStringCard( "BUNIT", "COUNTS",
                                        "Number of points per pixel (bin)" ) );
        if ( hasBlank && writer.blank_ != 0 ) {
            cards.add( cf.createIntegerCard( "BLANK", writer.blank_,
                                             "Blank value" ) );
        }
        if ( !Double.isNaN( min ) && !Double.isNaN( max ) ) {
            cards.add( cf.createRealCard( "DATAMIN", min, "Minimum value" ) );
            cards.add( cf.createRealCard( "DATAMAX", max, "Maximum value" ) );
        }
        for ( int id = 0; id < ndim; id++ ) {
            int id1 = id + 1;
            String pd1 = " (" + id1 + ")";
            ValueInfo info = axInfos[ id ];
            String units = info.getUnitString();
            String desc = info.getDescription();
            cards.add( cf.createStringCard( "CTYPE" + id1, info.getName(),
                                            desc ) );
            if ( units != null ) {
                cards.add( cf.createStringCard( "CUNIT" + id1, // non-standard
                                                units, "Units" ) );
            }
            cards.add( cf.createRealCard( "CRVAL" + id1, 0.0,
                                          "Reference pixel position" + pd1 ) );
            cards.add( cf.createRealCard( "CDELT" + id1, binSizes_[ id ],
                                          "Reference pixel extent" + pd1 ) );
            cards.add( cf.createRealCard( "CRPIX" + id1,
                                          - loBounds_[ id ] / binSizes_[ id ],
                                          "Reference pixel index" + pd1 ) );
        }
        cards.add( cf.createStringCard( "ORIGIN", "STILTS version "
                                      + Stilts.getVersion() + " ("
                                      + getClass().getName() + ")", null ) );
        cards.add( CardFactory.END_CARD );
        FitsUtil.writeHeader( cards.toArray( new CardImage[ 0 ] ), out );

        /* Write the data. */
        for ( int ip = 0; ip < npix; ip++ ) {
            writer.writeNumber( cube[ ip ] );
        }

        /* Pad to the end of a FITS block. */
        long nbyte = ( Math.abs( writer.getBitpix() ) / 8 ) * (long) npix;
        int over = (int) ( nbyte % FitsUtil.BLOCK_LENG );
        if ( over > 0 ) {
            out.write( new byte[ FitsUtil.BLOCK_LENG - over ] );
        }
    }

    /**
     * Constructs an object which can write signed integer values to a
     * DataOutput object.
     *
     * @param   out  destination stream
     * @param   clazz  primitive numeric type for output
     */
    public static NumberWriter createNumberWriter( final DataOutput out,
                                                   Class<?> clazz ) {
        if ( clazz == byte.class ) {
            return new NumberWriter( 8, Byte.MIN_VALUE ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeByte( Double.isNaN( value ) ? (byte) blank_
                                                         : (byte) value );
                }
            };
        }
        else if ( clazz == short.class ) {
            return new NumberWriter( 16, Short.MIN_VALUE ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeShort( Double.isNaN( value ) ? (short) blank_
                                                          : (short) value );
                }
            };
        }
        else if ( clazz == int.class ) {
            return new NumberWriter( 32, Integer.MIN_VALUE ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeInt( Double.isNaN( value ) ? (int) blank_
                                                        : (int) value );
                }
            };
        }
        else if ( clazz == long.class ) {
            return new NumberWriter( 64, Long.MIN_VALUE ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeLong( Double.isNaN( value ) ? blank_
                                                         : (long) value );
                }
            };
        }
        else if ( clazz == float.class ) {
            return new NumberWriter( -32, 0 ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeFloat( (float) value );
                }
            };
        }
        else if ( clazz == double.class ) {
            return new NumberWriter( -64, 0 ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeDouble( value );
                }
            };
        }
        else {
            assert false;
            return new NumberWriter( 64, 0 ) {
                public void writeNumber( double value ) throws IOException {
                    out.writeDouble( value );
                }
            };
        }
    }

    /**
     * Defines an object which can dispose of integer values.
     */
    private abstract static class NumberWriter {

        private final int bitpix_;
        final long blank_;

        /**
         * Constructor.
         *
         * @param  bitpix  appropriate value for FITS BITPIX header card.
         * @param  blank   non-zero blank value, or 0 if there is none
         */
        protected NumberWriter( int bitpix, long blank ) {
            bitpix_ = bitpix;
            blank_ = blank;
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
