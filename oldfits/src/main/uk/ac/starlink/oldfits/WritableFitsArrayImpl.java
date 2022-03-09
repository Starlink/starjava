package uk.ac.starlink.oldfits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.array.AccessImpl;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.ChunkStepper;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;

/**
 * ArrayImpl implementation for writing data to a FITS stream.
 *
 * @author   Mark Taylor (Starlink)
 */
class WritableFitsArrayImpl implements ArrayImpl {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.array" );

    private final ArrayDataOutput stream;
    private final OrderedNDShape oshape;
    private final Type type;
    private final Number badValue;
    private final long npix;
    private final int nByte;
    private final boolean isRandom;
    private final TypedWriter writer;
    private final Header header;
    private long strmBase;

    public WritableFitsArrayImpl( NDShape shape, Type type, Number badValue,
                                  final ArrayDataOutput ostream,
                                  boolean primary, HeaderCard[] cards ) 
        throws IOException {

        /* Save construction variables. */
        this.stream = ostream;
        this.oshape = new OrderedNDShape( shape, Order.COLUMN_MAJOR );
        this.type = type;
        this.badValue = badValue;

        /* Store calculated values. */
        npix = oshape.getNumPixels();
        nByte = type.getNumBytes();

        /* See if random access is available. */
        // We declare that random access is not available.
        // Attempting to provide random access is prone to leaving the
        // array unpadded at the end.
        // isRandom = stream instanceof RandomAccess;
        isRandom = false;

        /* Validate input. */
        int ndim = oshape.getNumDims();
        if ( ndim > 99 ) {
            throw new IllegalArgumentException(
                "Too many dimensions " + ndim + " > " + 99 );
        }

        /* Assemble the stack of header cards to put in the FITS header. */
        List<HeaderCard> cardlist = new ArrayList<HeaderCard>();
        try {
            long[] origin = oshape.getOrigin();
            long[] dims = oshape.getDims();

            /* Set the mandatory and well-known header cards. */
            if ( primary ) {
                cardlist.add( new HeaderCard( "SIMPLE", true,
                                              "Primary FITS HDU" ) );
            }
            else {
                cardlist.add( new HeaderCard( "XTENSION", "IMAGE",
                                              "Image extension" ) );
            }
            cardlist.add( new HeaderCard( "BITPIX", 
                                          FitsConstants.typeToBitpix( type ),
                                          "Number of bits per data pixel" ) );
            cardlist.add( new HeaderCard( "NAXIS", ndim, 
                                          "Number of data axes" ) );
            for ( int i = 0; i < ndim; i++ ) {
                cardlist.add( 
                    new HeaderCard( "NAXIS" + ( i + 1 ), dims[ i ],
                                    "length of data axis " + ( i + 1 ) ) );
            }
            if ( primary ) {
                cardlist.add( new HeaderCard( "EXTEND", true, 
                                              "Extensions permitted" ) );
            }
            else {
                cardlist.add( new HeaderCard( "PCOUNT", 0,
                                              "No extra parameters" ) );
                cardlist.add( new HeaderCard( "GCOUNT", 1,
                                              "Only one group" ) );
            }
            cardlist.add( new HeaderCard( "BZERO", 0.0, 
                                          "Offset applied to value" ) );
            cardlist.add( new HeaderCard( "BSCALE", 1.0, 
                                          "Scaling applied to value" ) );

            /* Set BLANK value if requested and permitted. */
            if ( badValue != null ) {
                if ( type.isFloating() ) {
                    if ( type == Type.FLOAT && ((Float) badValue).isNaN() ||
                         type == Type.DOUBLE && ((Double) badValue).isNaN() ) {
                        // ok
                    }
                    else {
                        logger.info( "FITS does not support non-NaN bad "
                                     + "values for floating point types - "
                                     + "using NaN" );
                    }
                }
                else { // integer type
                    cardlist.add( new HeaderCard( "BLANK", 
                                  badValue.longValue(), "Bad pixel value" ) );
                }
            }

            /* Add the origin value cards if required. */
            boolean defaultOrigin = true;
            for ( int i = 0; i < ndim; i++ ) {
                if ( origin[ i ] != NDShape.DEFAULT_ORIGIN ) {
                    defaultOrigin = false;
                }
            }
            if ( ! defaultOrigin ) {
                for ( int i = 0; i < ndim; i++ ) {
                    cardlist.add( 
                        new HeaderCard( FitsConstants.originCardName( i ), 
                                        origin[ i ],
                                        "First pixel index along axis " 
                                        + ( i + 1 ) ) );
                }
            }

            /* Add user-supplied cards if there are any. */
            if ( cards != null ) {
                for ( int i = 0; i < cards.length; i++ ) {
                    cardlist.add( cards[ i ] );
                }
            }

            /* Add termination record. */
            cardlist.add( FitsConstants.END_CARD );
        }
        catch( HeaderCardException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* Write the header cards into a new Header object. */
        header = new AddableHeader();
        for ( HeaderCard card : cardlist ) {
            ((AddableHeader) header).addLine( card );
        }

        /* Construct an object for writing data. */
        if ( type == Type.BYTE ) {
            writer = new TypedWriter() {
                public void write( Object data, int start, int size )
                        throws IOException {
                    stream.write( (byte[]) data, start, size );
                }
            };
        }
        else if ( type == Type.SHORT ) {
            writer = new TypedWriter() {
                public void write( Object data, int start, int size )
                        throws IOException  {
                    stream.write( (short[]) data, start, size );
                }
            };
        }
        else if ( type == Type.INT ) {
            writer = new TypedWriter() {
                public void write( Object data, int start, int size )
                        throws IOException  {
                    stream.write( (int[]) data, start, size );
                }
            };
        }
        else if ( type == Type.FLOAT ) {
            writer = new TypedWriter() {
                public void write( Object data, int start, int size )
                        throws IOException  {
                    stream.write( (float[]) data, start, size );
                }
            };
        }
        else if ( type == Type.DOUBLE ) {
            writer = new TypedWriter() {
                public void write( Object data, int start, int size )
                        throws IOException  {
                    stream.write( (double[]) data, start, size );
                }
            };
        }
        else {
            throw new AssertionError();
        }
    }

    public OrderedNDShape getShape() {
        return oshape;
    }
    public Type getType() {
        return type;
    }
    public Number getBadValue() {
        return badValue;
    }
    public boolean isRandom() {
        return isRandom;
    }
    public boolean isReadable() {
        return false;
    }
    public boolean isWritable() {
        return true;
    }
    public boolean canMap() {
        return false;
    }
    public Object getMapped() {
        return null;
    }
    public boolean multipleAccess() {
        return false;
    }

    public void open() throws IOException {

        /* Write out the header. */
        try {
            header.write( stream );
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* Mark the start of the data. */
        strmBase = isRandom
                 ? ((RandomAccess) stream).getFilePointer()
                 : 0L;
    }

    public AccessImpl getAccess() {
        return new AccessImpl() {
            private long offset = 0L;
            private BadHandler handler = BadHandler
                                        .getHandler( type, badValue );
            public void setOffset( long off ) throws IOException {
                if ( isRandom ) {
                    ((RandomAccess) stream).seek( strmBase + ( off * nByte ) );
                }
                else if ( off != offset ) {
                    writeBlank( off - offset );
                }
                offset = off;
            }
            public void write( Object buffer, int start, int size )
                    throws IOException {
                writer.write( buffer, start, size );
                offset += size;
            }
            public void read( Object buffer, int start, int size )
                    throws IOException {
                throw new AssertionError();
            }
            public void close() throws IOException {
                setOffset( npix );
                long writtenBytes = offset * nByte;
                int partial = (int) ( writtenBytes % FitsConstants.FITS_BLOCK );
                if ( partial > 0 ) {
                    int pad = FitsConstants.FITS_BLOCK - partial;
                    stream.write( new byte[ pad ] );
                }
                stream.flush();
            }
            private void writeBlank( long num ) throws IOException {
                if ( num < 0 ) {
                    throw new AssertionError();
                }
                logger.info( "Writing " + num + " times " + " BLANK " +
                             "value to skipped pixels in FITS output" );
                ChunkStepper cIt = new ChunkStepper( num );
                Object buffer = type.newArray( cIt.getSize() );
                handler.putBad( buffer, 0, cIt.getSize() );
                for ( ; cIt.hasNext(); cIt.next() ) {
                    write( buffer, 0, cIt.getSize() );
                }
            }
        };
    }
    
    public void close() throws IOException {
        stream.close();
    }


    /**
     * Private helper type to assist in writing arrays of different types.
     */
    private static interface TypedWriter {
        void write( Object buffer, int start, int size ) throws IOException;
    }
}
