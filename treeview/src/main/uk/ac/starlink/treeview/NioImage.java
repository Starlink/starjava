package uk.ac.starlink.treeview;

import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferInt;
import javax.media.jai.DataBufferFloat;    //   !
import javax.media.jai.DataBufferDouble;   //   !
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import javax.media.jai.RasterFactory;
import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import uk.ac.starlink.hdx.jai.SimpleRenderedImage;
import jsky.image.gui.ImageDisplay;
import com.sun.media.jai.codec.ImageCodec;

class NioImage extends SimpleRenderedImage {

    private Buffer niobuf;
    private int baseOffset;
    private int defaultTileWidth = 512;
    private int defaultTileHeight = 512;
    private static TileCache tcache = JAI.createTileCache();

    public NioImage( Buffer niobuf, int[] dims ) {
        this( niobuf, dims, 0, niobuf.limit() );
    }
     
    public NioImage( Buffer niobuf, int[] dims, int start, int size ) {

        /* Store and validate buffer. */
        this.niobuf = niobuf;
        this.baseOffset = start;
        if ( dims.length != 2 || dims[ 0 ] * dims[ 1 ] != size ) {
            throw new IllegalArgumentException( "Dimensions wrong" );
        }

        /* Store geometry. */
        this.width = dims[ 0 ];
        this.height = dims[ 1 ];

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
        int dataType;
        if ( niobuf instanceof ByteBuffer ) {
            dataType = DataBuffer.TYPE_BYTE;
        }
        else if ( niobuf instanceof ShortBuffer ) {
            dataType = DataBuffer.TYPE_SHORT;
        }
        else if ( niobuf instanceof IntBuffer ) {
            dataType = DataBuffer.TYPE_INT;
        }
        else if ( niobuf instanceof FloatBuffer ) {
            dataType = DataBuffer.TYPE_FLOAT;
        }
        else if ( niobuf instanceof DoubleBuffer ) {
            dataType = DataBuffer.TYPE_DOUBLE;
        }
        else {
            // assert false;
            throw new AssertionError();
        }
        sampleModel = makeSampleModel( dataType, tileWidth, tileHeight );

        /* Make colour model. */
        colorModel = ImageCodec.createComponentColorModel( sampleModel );
    }

    public synchronized Raster getTile( int tileX, int tileY ) {
        Raster tile = tcache.getTile( this, tileX, tileY );
        if ( tile == null ) {
            Point origin = new Point( tileXToX( tileX ), tileYToY( tileY ) );
            tile = RasterFactory.createWritableRaster( sampleModel, origin );
            fillTile( tile );
            tcache.add( this, tileX, tileY, tile );
        }
        return tile;
    }

    private void fillTile( Raster tile ) {
        int[] tOrigin = new int[] { tile.getMinX(), tile.getMinY() };
        int[] tDims = new int[] { tile.getWidth(), tile.getHeight() };
        DataBuffer dbuf = tile.getDataBuffer();
        int startpos = baseOffset + tOrigin[ 1 ] * width + tOrigin[ 0 ];
        int t0 = Math.min( tDims[ 0 ], width - tOrigin[ 0 ] );
        int t1 = Math.min( tDims[ 1 ], height - tOrigin[ 1 ] );
        if ( niobuf instanceof ByteBuffer ) {
            ByteBuffer buf = (ByteBuffer) niobuf;
            byte[] destArray = ((DataBufferByte) dbuf).getData();
            for ( int i = 0; i < t1; i++ ) {
                buf.position( startpos + i * width );
                buf.get( destArray, tDims[ 0 ] * i, t0 );
            }
        }
        else if ( niobuf instanceof ShortBuffer ) {
            ShortBuffer buf = (ShortBuffer) niobuf;
            short[] destArray = ((DataBufferShort) dbuf).getData();
            for ( int i = 0; i < t1; i++ ) {
                buf.position( startpos + i * width );
                buf.get( destArray, tDims[ 0 ] * i, t0 );
            }
        }
        else if ( niobuf instanceof IntBuffer ) {
            IntBuffer buf = (IntBuffer) niobuf;
            int[] destArray = ((DataBufferInt) dbuf).getData();
            for ( int i = 0; i < t1; i++ ) {
                buf.position( startpos + i * width );
                buf.get( destArray, tDims[ 0 ] * i, t0 );
            }
        }
        else if ( niobuf instanceof FloatBuffer ) {
            FloatBuffer buf = (FloatBuffer) niobuf;
            float[] destArray = ((DataBufferFloat) dbuf).getData();
            for ( int i = 0; i < t1; i++ ) {
                buf.position( startpos + i * width );
                buf.get( destArray, tDims[ 0 ] * i, t0 );
            }
        }
        else if ( niobuf instanceof DoubleBuffer ) {
            DoubleBuffer buf = (DoubleBuffer) niobuf;
            double[] destArray = ((DataBufferDouble) dbuf).getData();
            for ( int i = 0; i < t1; i++ ) {
                buf.position( startpos + i * width );
                buf.get( destArray, tDims[ 0 ] * i, t0 );
            }
        }
    }

    private static SampleModel makeSampleModel( int dataType, int tileWidth,
                                                int tileHeight ) {
        int[] bandOffsets = new int[1];
        bandOffsets[0] = 0;
        int pixelStride = 1;
        int scanlineStride = tileWidth;
        return RasterFactory.createPixelInterleavedSampleModel( dataType,
                                                                tileWidth,
                                                                tileHeight,
                                                                pixelStride,
                                                                scanlineStride,
                                                                bandOffsets );
    }
}
