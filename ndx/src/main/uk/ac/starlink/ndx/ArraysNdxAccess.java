package uk.ac.starlink.ndx;

import java.io.IOException;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.ChunkIterator;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;


/**
 * Provides access to an Ndx based on the underlying NDArrays.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ArraysNdxAccess implements NdxAccess {

    private final boolean imageOut;
    private final boolean varianceOut;
    private final boolean qualityOut;
    private final ArrayAccess imageAccess;
    private final ArrayAccess varianceAccess;
    private final ArrayAccess qualityAccess;
    private final OrderedNDShape oshape;
    private final Type type;
    private final BadHandler badHandler;
    private final boolean isReadable;
    private final boolean isWritable;
    private final boolean isRandom;
    private final boolean isMapped;
    private final byte badbits;

    private final static int BUFSIZ = ChunkIterator.defaultChunkSize;
    private byte[] qualBuffer;
    private long offset = 0L;

    public ArraysNdxAccess( NDArray image, NDArray variance, 
                            NDArray quality,
                            Requirements req,
                            boolean wantImage, boolean wantVariance,
                            boolean wantQuality, byte badbits ) 
            throws IOException {

        /* Prepare a requirements object for getting the NDArrays we will use.
         * These follow the requirements specified by the req argument, 
         * but where req has no preference take the values of the things
         * that need to be consistent from the supplied image NDArray. */
        req = ( req == null ) ? new Requirements() : (Requirements) req.clone();
        if ( req.getWindow() == null ) {
            req.setWindow( image.getShape() );
        }
        if ( req.getOrder() == null ) {
            req.setOrder( image.getShape().getOrder() );
        }
        if ( req.getType() == null ) {
            req.setType( image.getType() );
        }
        if ( req.getBadHandler() == null ) {
            req.setBadHandler( image.getBadHandler() );
        }

        /* Set attributes of this object accordingly. */
        oshape = new OrderedNDShape( req.getWindow(), req.getOrder() );
        type = req.getType();
        badHandler = req.getBadHandler();

        /* Get the image NDArray we will use, if one is required. */
        if ( wantImage ) {
            image = NDArrays.toRequiredArray( image, req );
            imageAccess = image.getAccess();
            imageOut = true;
        }
        else {
            imageAccess = null;
            imageOut = false;
        }

        /* Get the variance NDArray we will use, if one is required and
         * available. */
        if ( wantVariance && variance != null ) {
            variance = NDArrays.toRequiredArray( variance, req );
            varianceAccess = variance.getAccess();
            varianceOut = true;
        }
        else {
            varianceAccess = null;
            varianceOut = false;
        }

        /* Get the quality NDArray we will use, if one is required and 
         * available.  Note that we need access to quality to do implicit
         * masking on image and variance arrays if (1) direct access to
         * quality is not required by the client, (2) a quality array exists,
         * and (3) the badbits value indicates that the quality array could
         * result in some bad values (badbits!=0), (4) we have read access.  */
        boolean willBeReadable = 
               ( imageAccess == null || imageAccess.isReadable() )
            && ( varianceAccess == null || varianceAccess.isReadable() )
            && ( quality == null || quality.isReadable() );
        if ( ( quality != null ) && 
             ( wantQuality || willBeReadable && badbits != 0 ) ) {
            if ( quality.getType() != Type.BYTE ) {
                throw new IllegalArgumentException( 
                    "Quality array " + quality + 
                    " does not have type BYTE" );
            }
            Requirements qreq = ((Requirements) req.clone())
                               .setType( null )
                               .setBadHandler( null );
            quality = NDArrays.toRequiredArray( quality, qreq );
            qualityAccess = quality.getAccess();
            qualityOut = wantQuality;
        }
        else {
            qualityAccess = null;
            qualityOut = false;
        }

        this.badbits = badbits;
        isReadable = ( imageAccess == null || imageAccess.isReadable() )
                  && ( varianceAccess == null || varianceAccess.isReadable() )
                  && ( qualityAccess == null || qualityAccess.isReadable() );
        isWritable = ( imageAccess == null || imageAccess.isWritable() )
                  && ( varianceAccess == null || varianceAccess.isWritable() )
                  && ( qualityAccess == null || qualityAccess.isWritable() );
        isRandom = ( imageAccess == null || imageAccess.isRandom() )
                && ( varianceAccess == null || varianceAccess.isRandom() )
                && ( qualityAccess == null || qualityAccess.isRandom() );
        isMapped = ( imageAccess == null || imageAccess.isMapped() )
                && ( varianceAccess == null || varianceAccess.isMapped() )
                && ( qualityAccess == null || qualityOut 
                                           && qualityAccess.isMapped() );
    }

    // ArrayDescription interface methods.

    public Type getType() {
        return type;
    }
    public OrderedNDShape getShape() {
        return oshape;
    }
    public BadHandler getBadHandler() {
        return badHandler;
    }
    public boolean isReadable() {
        return isReadable;
    }
    public boolean isWritable() {
        return isWritable;
    }
    public boolean isRandom() {
        return isRandom;
    }
    public boolean isMapped() {
        return isMapped;
    }

    public boolean hasImage() {
        return imageOut;
    }
    public boolean hasVariance() {
        return varianceOut;
    }
    public boolean hasQuality() {
        return qualityOut;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset( long off ) throws IOException {
        if ( imageAccess != null ) {
            imageAccess.setOffset( off );
        }
        if ( varianceAccess != null ) {
            varianceAccess.setOffset( off );
        }
        if ( qualityAccess != null ) {
            qualityAccess.setOffset( off );
        }
        offset = off;
    }

    public long[] getPosition() {
        return oshape.indexToPosition( offset );
    }

    public void setPosition( long[] pos ) throws IOException {
        setOffset( oshape.positionToIndex( pos ) );
    }

    public Object getMappedImage() {
        if ( ! isMapped ) {
            throw new UnsupportedOperationException( "No mapped access" );
        }
        if ( ! imageOut ) {
            throw new UnsupportedOperationException( "Image not available" );
        }
        return imageAccess.getMapped();
    }
            
    public Object getMappedVariance() {
        if ( ! isMapped ) {
            throw new UnsupportedOperationException( "No mapped access" );
        }
        if ( ! varianceOut ) {
            throw new UnsupportedOperationException( "Variance not available" );
        }
        return varianceAccess.getMapped();
    }

    public byte[] getMappedQuality() {
        if ( ! isMapped ) {
            throw new UnsupportedOperationException( "No mapped access" );
        }
        if ( ! qualityOut ) {
            throw new UnsupportedOperationException( "Quality not available" );
        }
        // assert qualityAccess.getType() == Type.BYTE;
        return (byte[]) qualityAccess.getMapped();
    }

    public void read( Object ibuf, Object vbuf, byte[] qbuf, int start, 
                      int size ) throws IOException {
        checkBuffers( ibuf, vbuf, qbuf );
        try {
            if ( imageOut ) {
                imageAccess.read( ibuf, start, size );
            }
            if ( varianceOut ) {
                varianceAccess.read( vbuf, start, size );
            }
            if ( qualityOut ) {
                qualityAccess.read( qbuf, start, size );
            }
            else if ( qualityAccess != null ) {
                if ( size <= BUFSIZ ) {  // optimisation
                    qualityAccess.read( qualBuffer, 0, size );
                    accountForQuality( qualBuffer, ibuf, vbuf, start, size );
                }
                else {
                    for ( ChunkIterator cIt = 
                              new ChunkIterator( (long) size, BUFSIZ );
                          cIt.hasNext(); cIt.next() ) {
                        int leng = cIt.getSize();
                        int base = (int) cIt.getBase();
                        qualityAccess.read( qualBuffer, 0, leng );
                        accountForQuality( qualBuffer, ibuf, vbuf, base, leng );
                    }
                }
            }
            offset += size;
        }
        catch ( IOException e ) {
            close();
            throw e;
        }
    }
 
    public void readTile( Object ibuf, Object vbuf, byte[] qbuf,
                          NDShape tile ) throws IOException {
        checkBuffers( ibuf, vbuf, qbuf );
        int npix = (int) tile.getNumPixels();
        try {
            if ( imageOut ) {
                imageAccess.readTile( ibuf, tile );
            }
            if ( varianceOut ) {
                varianceAccess.readTile( vbuf, tile );
            }
            if ( qualityOut ) {
                qualityAccess.readTile( qbuf, tile );
            }
            else if ( qualityAccess != null ) {
                byte[] qb = new byte[ npix ];
                qualityAccess.readTile( qb, tile );
                accountForQuality( qb, ibuf, vbuf, 0, npix );
            }
            offset += npix;
        }
        catch ( IOException e ) {
            close();
            throw e;
        }
    }

    public void write( Object ibuf, Object vbuf, byte[] qbuf, int start,
                       int size ) throws IOException {
        checkBuffers( ibuf, vbuf, qbuf );
        try {
            if ( imageOut ) {
                imageAccess.write( ibuf, start, size );
            }
            if ( varianceOut ) {
                varianceAccess.write( vbuf, start, size );
            }
            if ( qualityOut ) {
                qualityAccess.write( qbuf, start, size );
            }
            offset += size;
        }
        catch ( IOException e ) {
            close();
            throw e;
        }
    }
 
    public void writeTile( Object ibuf, Object vbuf, byte[] qbuf,
                           NDShape tile ) throws IOException {
        checkBuffers( ibuf, vbuf, qbuf );
        int npix = (int) tile.getNumPixels();
        try {
            if ( imageOut ) {
                imageAccess.writeTile( ibuf, tile );
            }
            if ( varianceOut ) {
                varianceAccess.writeTile( vbuf, tile );
            }
            if ( qualityOut ) {
                qualityAccess.writeTile( qbuf, tile );
            }
            offset += npix;
         }
         catch ( IOException e ) {
            close();
            throw e;
         }
    }

    public void close() throws IOException {
        offset = -1L;
        if ( imageAccess != null ) {
            imageAccess.close();
        }
        if ( varianceAccess != null ) {
            varianceAccess.close();
        }
        if ( qualityAccess != null ) {
            qualityAccess.close();
        }
    }
        
    private void accountForQuality( byte[] qbuf, Object ibuf, Object vbuf,
                                    int start, int size ) {
        for ( int i = 0; i < size; i++ ) {
            if ( ( qbuf[ i ] & badbits ) != (byte) 0 ) {
                if ( imageOut ) {
                    badHandler.putBad( ibuf, start );
                }
                if ( varianceOut ) {
                    badHandler.putBad( vbuf, start );
                }
            }
            start++;
        }
    }

    private void checkBuffers( Object ibuf, Object vbuf, byte[] qbuf ) {
        if ( ! imageOut && ibuf != null ) {
            throw new IllegalArgumentException(
                "Image buffer not null, but no image access" );
        }
        if ( ! varianceOut && vbuf != null ) {
            throw new IllegalArgumentException(
                "Variance buffer not null, but no variance access" );
        }
        if ( ! qualityOut && qbuf != null ) {
            throw new IllegalArgumentException(
                "Quality buffer not null, but no quality access" );
        }
    }


}
