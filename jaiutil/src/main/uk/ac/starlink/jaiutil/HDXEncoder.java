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

import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.ImageEncoderImpl;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An ImageEncoder for the HDX file format. TODO: implement it.
 *
 * @version $Id$
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public class HDXEncoder extends ImageEncoderImpl 
{
    public HDXEncoder( OutputStream output, ImageEncodeParam param ) 
    {
        super( output, param );
        if ( this.param == null ) {
            this.param = new HDXEncodeParam();
        }
    }

    /**
     *  Encodes a RenderedImage and writes the output to the
     *  OutputStream associated with this ImageEncoder.
     */
    public void encode( RenderedImage im ) 
        throws IOException 
    {
	throw new RuntimeException( "HDXEncoder not implemented" );
    }
}
