/*
 * ESO Archive
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 * $Id$
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 * Peter W. Draper 2002/05/10  Converted for HDX
 *                 2002/06/18  Converted for HDX mark II
 *                 2002/09/24  Converted for NDX only (HDX mark III in
 *                             preparation).
 *                 2002/10/07  Converted to use the JAI default TileCache.
 */
package uk.ac.starlink.jaiutil;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.SeekableStream;

import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

import javax.media.jai.JAI;
import javax.media.jai.RasterFactory;
import javax.media.jai.TileCache;
import javax.media.jai.TiledImage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;

import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.ndx.Ndxs;
import uk.ac.starlink.ndx.XMLNdxHandler;

/**
 * This is the core class for JAI HDX/NDX support. It handles the
 * conversion between the HDX->NDX->NDArrays and the display data.
 * The data array is displayed by default.
 * <p>
 * This class defines a number of properties that can be accessed by
 * applications via the getProperty method.
 * <p>
 * The value of the property "#hdx_image" returns the HDXImage object
 * managing the image data.
 * <p>
 * The value of the property "#num_pages" returns an Integer with the
 * number of NDXs in the HDX structure (always 1 at present).
 * <p>
 * The "#preview_image" property returns a preshrunk preview image
 * suitable for use in a pan window. The size of the preview image may
 * be set by calling the static method HDXImage.setPreviewSize(int).
 *
 * @version $Id$
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public class HDXImage
    extends SimpleRenderedImage
{
    /**
     * HdxContainer managing the HDX structure
     */
    ////protected HdxContainer hdxContainer = null;

    /**
     * List of the NDXs (current one).
     */
    protected List ndxs = new ArrayList();

    /**
     * Current NDArray, this is either the data, variance or quality
     * of an NDX.
     */
    protected NDArray ndArray = null;

    /**
     * Object used to manage data type specific operations
     */
    protected NDArrayData ndArrayData;

    /**
     * The axes.
     */
    protected long[] axes = null;

    /**
     * Index of the current NDX.
     */
    protected int ndxIndex = -1;

    /**
     * Current metadata.
     */
    //protected MetaData ndxMetaData = null;

    /**
     * The type of the data buffer (DataBuffer.TYPE_BYTE, ...)
     */
    protected int dataType;

    /**
     * Default tile width
     */
    protected static int defaultTileWidth = 128;

    /**
     * Default tile height
     */
    protected static int defaultTileHeight = 128;

    /**
     * Object used to cache tiles
     */
    protected TileCache tileCache = JAI.getDefaultInstance().getTileCache();

    /**
     * Contains caller parameters (TODO: could specify component?).
     */
    protected HDXDecodeParam param;

    /**
     * Requested size of the preview image (width, height - actual
     * size will vary)
     */
    protected static int previewSize = 152;

    /**
     * True if the image is empty?
     */
    protected boolean empty = false;

    /**
     * Construct a HDXImage.
     *
     * @param input the SeekableStream for the HDX XML description.
     * @param param the parameter passed to the JAI create method
     * @param page specifies the desired NDX, these are just
     *             discovered in their natural order starting at 0.
     */
    public HDXImage( SeekableStream input, HDXDecodeParam param,
                     int page )
        throws IOException
    {
        this.param = param;
        XMLNdxHandler xmlHandler = XMLNdxHandler.getInstance();
        StreamSource xmlSource = new StreamSource( input );
        Ndx ndx = xmlHandler.makeNdx( xmlSource, AccessMode.READ );
        ndxs.add( ndx );
        setNDX( page );
    }

    /**
     * Construct a HDXImage.
     *
     * @param source a Source containing an HDX document.
     */
    public HDXImage( Source source )
        throws IOException
    {
        XMLNdxHandler xmlHandler = XMLNdxHandler.getInstance();
        Ndx ndx = xmlHandler.makeNdx( source, AccessMode.READ );
        ndxs.add( ndx );
        setNDX( 0 );
    }

    /**
     * Construct a HDXImage from a file or URL.
     *
     * @param fileOrURL the file name or URL
     */
    public HDXImage( String fileOrUrl )
        throws IOException
    {
        NdxIO ndxIO = new NdxIO();
        Ndx ndx = ndxIO.makeNdx( fileOrUrl, AccessMode.READ );
        ndxs.add( ndx );
        setNDX( 0 );
    }

    /**
     * Construct a HDXImage from a NDX.
     *
     * @param ndx the Ndx reference.
     */
    public HDXImage( Ndx ndx )
        throws IOException
    {
        ndxs.add( ndx );
        setNDX( 0 );
    }

    /**
     * Create a HDXImage from a w3c Document.
     *
     * @param Document HDX structure
     * @param param the parameter passed to the JAI create method
     * @param page specifies the desired NDX (default: 0)
     */
    public HDXImage( Document document, HDXDecodeParam param,
                     int page )
        throws IOException
    {
        this( document.getDocumentElement(), page );
        this.param = param;
    }

    /**
     * Create a HDXImage from a w3c Element
     *
     * @param Document HDX DOM structure containing an NDX.
     * @param page specifies the desired NDX (default: 0)
     */
    public HDXImage( Element element, int page )
        throws IOException
    {
        XMLNdxHandler xmlHandler = XMLNdxHandler.getInstance();
        DOMSource xmlSource = new DOMSource( element );
        Ndx ndx = xmlHandler.makeNdx( xmlSource, AccessMode.READ );
        ndxs.add( ndx );
        setNDX( page );
    }

    /**
     * Return the index of the current NDX
     */
    public int getCurrentNDXIndex()
    {
        return ndxIndex;
    }

    /**
     * Return a BufferedInputStream for the given file or URL.
     */
    protected BufferedInputStream getStream( String fileOrUrl )
        throws IOException
    {
        URL url = getURL( fileOrUrl );
        InputStream stream = url.openStream();
        if (! ( stream instanceof BufferedInputStream ) ) {
            stream = new BufferedInputStream( stream );
        }
        return (BufferedInputStream) stream;
    }

    /**
     * Return a URL for the given file or URL string.
     */
    protected URL getURL( String fileOrUrl )
        throws MalformedURLException
    {
        URL url = null;
        if ( fileOrUrl.startsWith( "http:" ) ||
             fileOrUrl.startsWith( "file:" ) ||
             fileOrUrl.startsWith( "ftp:" ) ) {
            url = new URL(fileOrUrl);
        }
        else {
            File file = new File( fileOrUrl );
            url = file.getAbsoluteFile().toURL();
        }
        return url;
    }

    /**
     * Return the number of NDXs in the HDX structure.
     */
    public int getNumNDXs()
    {
        if ( ndxs != null ) {
            return ndxs.size();
        }
        return 0;
    }

    /**
     * Return the HdxContainer
     */
    ////public HdxContainer getHdxContainer()
    ////{
    ////    return hdxContainer;
    ////}

    /**
     * Set the current NDX from those available.
     *
     * @param num The NDX number (starts at 0).
     */
    public void setNDX( int num )
        throws IOException
    {
        if ( ndxs == null || num >= ndxs.size() ) {
            throw new IOException( "Cannot select NDX (" + num + ")" );
        }
        if ( num == ndxIndex ) {
            return;   // Nothing to do.
        }

        //  If an NDArray is open already, should close it before
        //  proceeding.
        close();

        // Access the NDX and realize one of its components (TODO:
        // support more than data) as an NDArray. Use a MaskedImage to
        // apply any quality bad pixels.
        ndxIndex = num;
        Ndx ndx = (Ndx) ndxs.get( ndxIndex );
        Requirements req = new Requirements();
        ndArray = Ndxs.getMaskedImage( ndx, req );

        // Check basics of array.
        axes = ndArray.getShape().getDims();
        if ( axes.length <= 1 ) {
            width = 0;
            height = 0;
            empty = true;
            throw new IOException( "Dimensionality of NDX should be 2" );
        }

        // Initialize an object to do data type specific operations
        scale = 1.0F;
        subsample = 1;
        initData();
    }

    /**
     * Close the current "HDX".
     */
    public void close() 
        throws IOException
    {
        if ( ndArray != null ) {
            ndArray.getAccess().close();
        }
    }

    /**
     * Return the Bad pixel value for the current NDArray.
     * Returns Double.NaN if none is defined.
     */
    public double getBadValue()
    {
        if ( ndArray != null ) {
            BadHandler handler = ndArray.getBadHandler();
            if ( handler != null ) {
                Number value = handler.getBadValue();
                if ( value != null ) {
                    return value.doubleValue();
                }
            }
        }
        return Double.NaN;
    }

    public static void setDefaultTileWidth( int w )
    {
        defaultTileWidth = w;
    }
    public static int getDefaultTileWidth()
    {
        return defaultTileWidth;
    }
    public static void setDefaultTileHeight(int h)
    {
        defaultTileHeight = h;
    }
    public static int getDefaultTileHeight()
    {
        return defaultTileHeight;
    }

    /**
     * Try to save memory by clearing out the tile cache for this
     * image.
     */
    public void clearTileCache()
    {
        tileCache.removeTiles( this );
    }

    /**
     * Gets a property from the property set of this HDX structure.
     *
     * @param name the name of the property to get, as a String.
     * @return a reference to the property value or null if not found.
     */
    public Object getProperty( String name )
    {
        if ( name.equals( "#num_pages" ) ) {
            return Integer.toString( getNumNDXs() );
        }
        if ( name.equals( "#preview_image" ) ) {
            return getPreviewImage( previewSize );
        }
        if (name.equals( "#ndx_image" ) ) {
            return this;
        }
        return null;
    }

    /**
     * Returns a list of property names that are recognized by this image.
     *
     * @return an array of Strings containing valid property names.
     */
    public String[] getPropertyNames()
    {
        String[] names = new String[] {
            "#num_pages",
            "#preview_image",
            "#ndx_image"
        };
        return names;
    }

    /**
     * Return a prescaled PlanarImage that fits entirely in a window
     * of the given size, or null if there are any errors.
     */
    protected TiledImage getPreviewImage( int size )
    {
        if ( size == 0 || empty ) {
            return null;
        }

        int factor = Math.max((width-1)/size + 1, (height-1)/size + 1);
        if ( factor <= 1 ) {
            return null;
        }

        int tileWidth = width / factor;
        int tileHeight = height / factor;

        SampleModel sampleModel = initSampleModel( tileWidth, tileHeight );
        ColorModel colorModel = initColorModel();
        TiledImage tiledImage = new TiledImage( 0, 0, tileWidth,
                                                tileHeight, 0, 0,
                                                sampleModel,
                                                colorModel );
        Point origin = new Point( 0, 0 );
        Raster raster = RasterFactory.createWritableRaster( sampleModel,
                                                            origin );
        try {
            raster = ndArrayData.getPreviewImage( raster, factor );
        }
        catch( EOFException e ) {
            //System.out.println("XXX FITSImage.getTile(): warning: " + e.toString());
        }
        catch( IndexOutOfBoundsException e ) {
            //System.out.println("XXX FITSImage.getTile(): warning: " + e.toString());
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
        if ( raster == null ) {
            return null;
        }
        tiledImage.setData( raster );
        return tiledImage;
    }


    /**
     * Set the colormodel to use to display FITS images.
     */
    public ColorModel initColorModel()
    {
        return ImageCodec.createComponentColorModel( sampleModel );
    }

    /**
     * Return a SampleModel for this image with the given tile width
     * and height.
     */
    public SampleModel initSampleModel( int tileWidth, int tileHeight )
    {
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

    /**
     * Create an object to manage the data based on the type and set
     * the value of the dataType member variable to the correct
     * DataBuffer constant to use for the sample model.
     */
    public void initData()
        throws IOException
    {
        if ( empty ) {
            // dummy empty image (JAI doesn't like 0x0 images)
            ndArrayData = new NDArrayDataDouble( ndArray, new int[]{2, 2} );
            dataType = DataBuffer.TYPE_DOUBLE;
        }
        else {
            Type nativeType = ndArray.getType();
            if ( nativeType ==  Type.SHORT ) {
                ndArrayData = new NDArrayDataShort( ndArray );
                dataType = DataBuffer.TYPE_SHORT;
            }
            else if ( nativeType == Type.SHORT ) {
                ndArrayData = new NDArrayDataByte( ndArray );
                dataType = DataBuffer.TYPE_BYTE;
            }
            else if ( nativeType == Type.INT ) {
                ndArrayData = new NDArrayDataInt( ndArray );
                dataType = DataBuffer.TYPE_INT;
            }
            else if ( nativeType == Type.FLOAT ) {
                ndArrayData = new NDArrayDataFloat( ndArray );
                dataType = DataBuffer.TYPE_FLOAT;
            }
            else if ( nativeType == Type.DOUBLE ) {
                ndArrayData = new NDArrayDataDouble( ndArray );
                dataType = DataBuffer.TYPE_DOUBLE;
            }
            else {
                   throw new RuntimeException( "Invalid data type" +
                                               ndArray.getType() );
            }
        }
        initImage();
    }

    // Initialize the image dimensions, colormodel, samplemodel, and
    // tile size.
    private void initImage()
        throws IOException
    {
        width = (int) axes[axes.length-2];
        height = (int) axes[axes.length-1];

        if ( width != 0 && height != 0 ) {

            // handle zoom out here for performance reasons
            if ( subsample != 1 ) {
                width /= subsample;
                height /= subsample;
            }

            // try to choose a reasonable tile size
            tileWidth = defaultTileWidth;
            if ( width / tileWidth <=  1 ) {
                tileWidth = width;
            }

            tileHeight = defaultTileHeight;
            if ( height / tileHeight <=  1 ) {
                tileHeight = height;
            }

            //  Choose appropriate sample and color models.
            sampleModel = initSampleModel( tileWidth, tileHeight );
            colorModel = initColorModel();
        }
    }
    
    /**
     * Generate and return the given tile (required by the
     * RenderedImage interface). Note that tileX and tileY are indices
     * into the tile array, not pixel locations.
     *
     * @param tileX the X index of the requested tile in the tile array.
     * @param tileY the Y index of the requested tile in the tile array.
     * @return the tile given by (tileX, tileY).
     */
    public synchronized Raster getTile( int tileX, int tileY )
    {
        if ( empty ) {
            return RasterFactory.createWritableRaster( sampleModel,
                                                       new Point( 0, 0 ) );
        }

        Raster tile = tileCache.getTile( this, tileX, tileY );
        if ( tile == null ) {
            Point origin = new Point( tileXToX( tileX ), tileYToY( tileY ) );
            tile = RasterFactory.createWritableRaster( sampleModel, origin );
            fillTile( tile );
            tileCache.add( this, tileX, tileY, tile );
        }
        else {
            //System.out.println("XXX use old tile(" + tileX + ", " + tileY + ")");
        }
        return tile;
    }

    /**
     * This method fills the given tile with the appropriate image data.
     */
    protected Raster fillTile( Raster tile )
    {
        try {
            ndArrayData.getTile( tile, subsample, width, height );
        }
        catch( Exception e ) {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
        return tile;
    }


    /**
     * Set the requested size for the preview image
     */
    public static void setPreviewSize( int i )
    {
        previewSize = i;
    }

    /**
     * Return the requested size for the preview image
     */
    public static int getPreviewSize()
    {
        return previewSize;
    }

    /**
     * Get a reference to the current NDX. This allows access to other
     * goodies such as WCS information and the size of the image etc.
     * Notes that you should use this reference and then throw it away
     * as currently this HDXImage may change the NDX that it is using.
     */
    public Ndx getCurrentNDX()
    {
        return (Ndx) ndxs.get( ndxIndex );
    }

    /** 
     * Returns the real width of the image.
     * This may be different than the value returned by getWidth() if
     * the image is zoomed out.
     */
    public int getRealWidth() {
        return (int) axes[axes.length-2];
    }

    /** 
     * Returns the real height of the image. 
     * This may be different than the value returned by getHeight() if
     * the image is zoomed out.
     */
    public int getRealHeight() {
        return (int) axes[axes.length-1];
    }

    //
    // Optimization of zooming out. This follows same methods in
    // FITSImage, and may indicate the need for a common
    // class/framework to underpin this.
    //

    // The current scale factor (zooming out is handled here for
    // performance reasons). Zooming in still needs to be done by the
    // image widget.
    private float scale = 1.0F;

    // The increment to use when accessing the image data if scale is
    // less than 1. A value of 1 means no scaling is done.
    private int subsample = 1;

    /**
     * Set the scale (zoom factor) for the image and return true if a
     * new image was generated.
     * <p>
     * Note that <em>zooming out</em> is handled here for performance
     * reasons, to avoid having to read in whole tiles, only to
     * discard most of the data later. <em>Zooming in</em> should be
     * handled in the image viewer widget at the end of the image
     * processing chain.
     *
     * @param scale the scale factor (a value of less than 1.0 means
     *              the image is zoomed out and is handled specially here)
     *
     * @return true if the new scale value caused a new image to be
     *              generated, requiring an image update in the viewer
     *              widget.
     */
    public boolean setScale( float scale )
        throws IOException
    {
        boolean needsUpdate = false;
        if ( scale > 1 ) {
            scale = 1;
        }
        if ( scale != this.scale ) {
            needsUpdate = true;
            this.scale = scale;
            if ( scale < 1 ) {
                subsample = Math.round( 1.0F / scale );
            }
            else {
                subsample = 1;
            }
            tileCache.flush();
            initImage();
        }
        return needsUpdate;
    }

    /**
     * Return the current scale factor (zooming out is handled here
     * for performance reasons). Zooming in still needs to be done by
     * the image widget.
     */
    public float getScale()
    {
        return scale;
    }

    /**
     * Return the increment to use when accessing the image data if
     * scale is less than 1. A value of 1 means no scaling is done.
     */
    public int getSubsample()
    {
        return subsample;
    }
}
