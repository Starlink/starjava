/*
 * Copyright (C) 2002-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-MAY-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.jaiutil;

import java.awt.image.Raster;
import java.io.IOException;

import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Requirements;

/**
 * An abstract base class for performing data type specific operations
 * when accessing NDArray data.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public abstract class NDArrayData
{
    /**
     * Object used to access the image tiles
     */
    protected ArrayAccess tiler;

    /**
     * Width in pixels of the image data
     */
    protected int width;

    /**
     * Height in pixels of the image data
     */
    protected int height;

    /**
     * Number of axes (Currently only the width and height are considered)
     */
    protected int naxis;

    /**
     * Constructor.
     *
     * @param tiler the NDArray
     */
    public NDArrayData( NDArray nda )
        throws IOException
    {
        //  Make sure we have random access. Does nothing if already
        //  have this, but is needed for remote data (this causes a
        //  local copy to be created).
        Requirements req = 
            new Requirements( AccessMode.READ ).setRandom( true );
        nda = NDArrays.toRequiredArray( nda, req );

        this.tiler = nda.getAccess();
        long[] axes = null;
        try {
            axes = nda.getShape().getDims();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
	naxis = axes.length;
	this.width = (int) axes[naxis-2];
	this.height = (int) axes[naxis-1];
    }

    /** 
     * Constructor.
     *
     * @param tiler the NDArray
     * @param axes an array containing the dimensions of the image
     */
    public NDArrayData( NDArray nda, int[] axes ) 
        throws IOException
    {
        Requirements req = 
            new Requirements( AccessMode.READ ).setRandom( true );
        nda = NDArrays.toRequiredArray( nda, req );

	this.tiler = nda.getAccess();
	naxis = axes.length;
	this.width = axes[naxis-2];
	this.height = axes[naxis-1];
    }

    /**
     * Fill the given array with image data starting at the given offsets
     * and with the given width and height in image pixels.
     *
     * @param destArray the image data array
     * @param x the x offset in the image data
     * @param y the y offset in the image data
     * @param w the width of the data to get
     * @param h the height of the data to get
     */
    protected void fillTile( Object destArray, int x, int y, int w, int h )
        throws IOException
    {
	long[] corners;
        int[] lengths;
        
        // Why do I do this?
        //if ( x == 0 ) x = 1;
        //if ( y == 0 ) y = 1;
        x++; y++;

        //  TODO: test 3 and 4D cases.
	switch(naxis) {
           case 2:
               corners = new long[]{x, y};
               lengths = new int[]{w, h};
               break;
           case 3:
               corners = new long[]{0, y, x};
               lengths = new int[]{0, h, w};
               break;
           case 4:
               corners = new long[]{0, 0, y, x};
               lengths = new int[]{0, 0, h, w};
               break;
           default:
               throw new RuntimeException( "Unsupported number of axes" );
	}

        //  Add in any origin information.
        long[] origins = tiler.getShape().getOrigin();
        for ( int i = 0; i < origins.length; i++ ) {
            corners[i] += origins[i] - 1;
        }

	//  The dimensions of the arguments to tiler.getTile() are
        //  dependent on NAXIS.
        try {
            tiler.readTile( destArray, new NDShape( corners, lengths ) );
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 
     * Fill the given tile with the appropriate image data 
     */
    public abstract Raster getTile( Raster tile, int subsample, 
                                    int width, int height ) 
        throws IOException;

    /** 
     * Return a prescaled preview image at "1/factor" of the normal
     * size in the given raster tile.
     */
    public abstract Raster getPreviewImage( Raster tile, int factor )
        throws IOException;
}
