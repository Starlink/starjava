/*
 * ESO Archive
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 * 
 * $Id$
 * 
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 * Peter W. Draper 2002/05/10  Converted for HDX.
 */
package uk.ac.starlink.jaiutil;

import com.sun.media.jai.codec.ForwardSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecodeParam;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.SeekableStream;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A subclass of ImageCodec that handles HDX encapsulated data.
 *
 * @version $Id$
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public final class HDXCodec extends ImageCodec 
{

    /** 
     * Constructs an instance of HDXCodec. 
     */
    public HDXCodec() 
    {
        //  Null constructor.
    }

    /** 
     * Returns the name of the format handled by this codec. 
     */
    public String getFormatName() 
    {
        return "hdx";
    }

    /**
     * Returns <code>Object.class</code> since no DecodeParam
     * object is required for decoding.
     */
    public Class getDecodeParamClass() 
    {
        return Object.class;
    }

    /** 
     * Returns null since no encoder exists. TODO: HDX should be able
     * to save itself.
     */
    public Class getEncodeParamClass() 
    {
        return null;
    }

    /** 
     * Returns true if the image is encodable by this codec. 
     */
    public boolean canEncodeImage( RenderedImage im,
                                   ImageEncodeParam param ) 
    {
        SampleModel sampleModel = im.getSampleModel();
        int dataType = sampleModel.getTransferType();
        int numBands = sampleModel.getNumBands();
        if ( numBands != 1 /* && numBands != 3 XXX not impl */ ) {
            return false;
        }
        return true;
    }

    /**
     * Instantiates a HDXEncoder to write to the given OutputStream.
     *
     * @param dst the OutputStream to write to.
     * @param param an instance of HDXEncodeParam used to control the 
     *              encoding process, or null.  A ClassCastException 
     *              will be thrown if param is non-null but not an 
     *              instance of HDXEncodeParam.
     */
    protected ImageEncoder createImageEncoder( OutputStream dst, 
                                               ImageEncodeParam param ) 
    {
        HDXEncodeParam p = null;
        if ( param != null ) {
            p = (HDXEncodeParam) param; // May throw a ClassCast exception
        }
        return new HDXEncoder( dst, p );
    }

    /**
     * Instantiates a HDXDecoder to read from the given InputStream.
     *
     * <p> 
     * By overriding this method, HDXCodec is able to ensure that a 
     * ForwardSeekableStream is used to wrap the source InputStream 
     * instead of the a general (and more expensive) subclass of
     * SeekableStream.  Since the HDX decoder does not require the 
     * ability to seek backwards in its input, this allows for greater
     * efficiency (TODO: not very problematic for HDX cf. FITS).
     *
     * @param src the InputStream to read from.
     * @param param an instance of ImageDecodeParam used to control
     *              the decoding process, or null.
     */
    protected ImageDecoder createImageDecoder( InputStream src,
                                               ImageDecodeParam param ) 
    {
	try {
	    return new HDXDecoder( new ForwardSeekableStream( src ),
                                   param );
	}
	catch ( Exception e ) {
            e.printStackTrace();
	    throw new RuntimeException( e.getMessage() );
	}
    }

    /**
     * Instantiates a HDXDecoder to read from the given SeekableStream.
     *
     * @param src the SeekableStream to read from.
     * @param param an instance of ImageDecodeParam used to control
     *              the decoding process, or null. 
     */
    protected ImageDecoder createImageDecoder( SeekableStream src, 
                                               ImageDecodeParam param ) 
    {
	try {
	    return new HDXDecoder( src, param );
	}
	catch ( Exception e ) {
	    e.printStackTrace();
	    throw new RuntimeException( e.getMessage() );
	}
    }

    /**
     * Returns the number of bytes from the beginning of the data required
     * to recognize it as being in FITS format.
     */
    public int getNumHeaderBytes() 
    {
         return 5;
    }

    /**
     * Returns true if the header bytes indicate XML format. All XML
     * files are assumed to have some HDX content.
     *
     * @param header an array of bytes containing the initial bytes of the
     *        input data.     
     */
    public boolean isFormatRecognized( byte[] header ) 
    {
       return ( ( header[0] == '<' ) &&
                 ( header[1] == '?' ) &&
                 ( header[2] == 'x' || header[2] == 'X' ) &&
                 ( header[3] == 'm' || header[3] == 'M' ) &&
                 ( header[4] == 'l' || header[4] == 'L' ) );
    }
}
