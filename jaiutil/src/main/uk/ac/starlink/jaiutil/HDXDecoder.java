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
 */
package uk.ac.starlink.jaiutil;

import com.sun.media.jai.codec.ImageDecodeParam;
import com.sun.media.jai.codec.ImageDecoderImpl;
import com.sun.media.jai.codec.SeekableStream;

import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * An ImageDecoder for the HDX file format.
 *
 * @version $Id$
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public class HDXDecoder extends ImageDecoderImpl
{
    /**
     * Saved reference to the HDXImage object. This provides real
     * access to HDX image data.
     */
    private HDXImage hdxImage = null;

    /**
     * Constructor
     */
    public HDXDecoder( SeekableStream input, ImageDecodeParam param )
        throws IOException
    {
        super( input, param );
        hdxImage = new HDXImage( input, (HDXDecodeParam) param, 0 );
    }

    /**
     * Return the current RenderedImage that has been extracted from
     * the SeekableStream,
     */
    public RenderedImage decodeAsRenderedImage()
        throws IOException
    {
        return hdxImage;
    }

    /**
     * Returns a RenderedImage that contains the decoded contents of
     * the SeekableStream associated with this ImageDecoder. The given
     * page of a multi-page image is decoded. If the page does not
     * exist, an IOException will be thrown. Page numbering begins at
     * zero. For HDX "pages" are just the NDXs.
     */
    public RenderedImage decodeAsRenderedImage( int page ) 
        throws IOException 
    {
        hdxImage.setNDX( page );
        return hdxImage;
    }


    /**
     * Returns the number of pages (NDXs) present in the current stream.
     * <p>
     * Note: The HDX codec defines properties, such as "#num_pages",
     * "#hdx_image", and "#preview_image" that give direct access to
     * the NDXs, so that you can tell ahead of time if an HDX
     * structure contains an image.
     */
    public int getNumPages() 
        throws java.io.IOException 
    {
        return hdxImage.getNumNDXs();
    }
}

