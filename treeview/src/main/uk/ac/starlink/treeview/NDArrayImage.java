package uk.ac.starlink.treeview;

import java.io.IOException;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferDouble;
import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import java.awt.Point;
import com.sun.media.jai.codec.ImageCodec;
import javax.media.jai.RasterFactory; 
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.jaiutil.SimpleRenderedImage;

/* The implementation of this class is largely pinched from HDXImage in
 * uk.ac.starlink.hdx.jai.  However it doesn't do any of the XML
 * mucking about that that class does. */

public class NDArrayImage extends SimpleRenderedImage {

    private ArrayAccess acc;
    private int defaultTileWidth = 200;
    private int defaultTileHeight = 200;
    private static final int MAX_TILE_BYTES = 64*1024*1024;
    private long[] origin;

    public final static TileCache tileCache = 
        JAI.getDefaultInstance().getTileCache();

    public NDArrayImage( NDArray nda ) throws IOException {

        /* Check we are planar. */
        NDShape shape = nda.getShape();
        if ( shape.getNumDims() != 2 ) {
            throw new IllegalArgumentException( "NDArray " + nda + " not 2-d" );
        }

        /* Store an accessor. */
        acc = nda.getAccess();

        /* Store NDArray geometry. */
        this.origin = shape.getOrigin();
        long[] dims = shape.getDims();
        width = (int) dims[ 0 ];
        height = (int) dims[ 1 ];

        /* Get a reasonable tile size. */
        tileWidth = defaultTileWidth;
        if ( width / tileWidth <=  1 ) {
            tileWidth = width;
        }

        tileHeight = defaultTileHeight;
        if ( height / tileHeight <=  1 ) {
            tileHeight = height;
        }

        /* Make sample model and colour model. */
        sampleModel = makeSampleModel( getDataType( nda.getType() ), 
                                       tileWidth, tileHeight );
        colorModel = ImageCodec.createComponentColorModel( sampleModel );
    }


    public synchronized Raster getTile( int tileX, int tileY ) {
        Raster tile = tileCache.getTile( this, tileX, tileY );
        if ( tile == null ) {
            Point origin = new Point( tileXToX( tileX ), tileYToY( tileY ) );
            tile = RasterFactory.createWritableRaster( sampleModel, origin );
            fillTile( tile );
            tileCache.add( this, tileX, tileY, tile );
        }
        return tile;
    }

    private void fillTile( Raster tile ) {
        long[] tOrigin = new long[] { (long) tile.getMinX() + origin[ 0 ], 
                                      (long) tile.getMinY() + origin[ 1 ] };
        int[] tDims = new int[] { tile.getWidth(), tile.getHeight() };
        NDShape tileShape = new NDShape( tOrigin, tDims );
        Object destArray = getArrayData( tile.getDataBuffer() );
        try {
            synchronized ( this ) {
                acc.readTile( destArray, tileShape );
            }
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }


    private static SampleModel makeSampleModel( int dataType, int tileWidth,
                                                int tileHeight ) {
        int[] bandOffsets = new int[1];
        bandOffsets[0] = 0;
        int pixelStride = 1;
        int scanlineStride = tileWidth;         
        return RasterFactory
              .createPixelInterleavedSampleModel( dataType, tileWidth,
                                                  tileHeight, pixelStride,
                                                  scanlineStride, bandOffsets );
    }

    private Object getArrayData( DataBuffer dbuf ) {
        Type type = acc.getType();
        if ( type == Type.BYTE ) {
            return ((DataBufferByte) dbuf).getData();
        }
        else if ( type == Type.SHORT ) {
            return ((DataBufferShort) dbuf).getData();
        }
        else if ( type == Type.INT ) {
            return ((DataBufferInt) dbuf).getData();
        }
        else if ( type == Type.FLOAT ) {
            if ( dbuf instanceof DataBufferFloat ) {
                return ((DataBufferFloat) dbuf).getData();
            }

            /* In early versions of JAI, a custom class was used
             * (since the corresponding one didn't exist in the J2SE). */
            else {
                return ((javax.media.jai.DataBufferFloat) dbuf).getData();
            }
        }
        else if ( type == Type.DOUBLE ) {
            if ( dbuf instanceof DataBufferDouble ) {
                return ((DataBufferDouble) dbuf).getData();
            }

            /* In early versions of JAI, a custom class was used
             * (since the corresponding one didn't exist in the J2SE). */
            else {
                return ((javax.media.jai.DataBufferDouble) dbuf).getData();
            }
        }
        else {
            // assert false;
            return null;
        }
    }

    private static int getDataType( Type type ) {
        if ( type == Type.BYTE ) {
            return DataBuffer.TYPE_BYTE;
        }
        else if ( type == Type.SHORT ) {
            return DataBuffer.TYPE_SHORT;
        }
        else if ( type == Type.INT ) {
            return DataBuffer.TYPE_INT;
        }
        else if ( type == Type.FLOAT ) {
            return DataBuffer.TYPE_FLOAT;
        }
        else if ( type == Type.DOUBLE ) {
            return DataBuffer.TYPE_DOUBLE;
        }
        else {
            // assert false;
            throw new AssertionError();
        }
    }

    public void finalize() throws Throwable {
        try {
            tileCache.removeTiles( this );
        }
        finally {
            super.finalize();
        }
    }

}
