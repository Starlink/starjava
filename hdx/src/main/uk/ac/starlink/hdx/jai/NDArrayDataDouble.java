// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.hdx.jai;

import java.awt.image.Raster;
import java.io.IOException;

import javax.media.jai.DataBufferDouble;

import uk.ac.starlink.hdx.array.NDArray;
import uk.ac.starlink.hdx.HdxException;

/**
 * Used for double precision NDArray data.
 *
 * @version $Id$
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public class NDArrayDataDouble extends NDArrayData
{
    /** constructor */
    public NDArrayDataDouble( NDArray tiler ) 
    {
	super( tiler );
    }

   /** constructor */
    public NDArrayDataDouble ( NDArray tiler, int[] axes ) 
    {
        super( tiler, axes );
    }

    /** 
     * Fill in the given tile with the appropriate image data.
     */
    public Raster getTile( Raster tile ) 
        throws IOException, HdxException
    {
        //System.out.println( "NDArrayDataDouble: getTile = " + tile );
	DataBufferDouble dataBuffer = (DataBufferDouble) tile.getDataBuffer();
	double[] destArray = dataBuffer.getData();
	fillTile( destArray, tile.getMinX(), tile.getMinY(), 
                  tile.getWidth(), tile.getHeight() );
	return tile;
    }

    /** 
     * Return a prescaled preview image at "1/factor" of the normal
     * size in the given raster tile.
     */
    public Raster getPreviewImage( Raster tile, int factor ) 
        throws IOException
    {
        DataBufferDouble dataBuffer = (DataBufferDouble) tile.getDataBuffer();
        double[] destArray = dataBuffer.getData();
        double[] line = new double[width];

        int tw = tile.getWidth(),
            th = tile.getHeight(),
            w = tw*factor,
            h = th*factor,
            n = 0, 
            m = 0;
        try {
            for ( int j = 0; j < h; j += factor ) {
                n =  m++ * tw;
                fillTile( line, 0, j, width, 1 );
                for ( int i = 0; i < w; i += factor ) {
                    destArray[n++] = line[i];
                }
            }
            return tile;
        }
        catch (Exception e) {
            throw new RuntimeException( e.getMessage() );
        }
    }
}
