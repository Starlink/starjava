/*
 * ESO Archive
 *
 * $Id: FITSCodec.java,v 1.4 2002/08/04 19:50:39 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.fits.codec;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.media.jai.codec.ForwardSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageDecodeParam;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.SeekableStream;

import java.awt.image.*;

/**
 * A subclass of <code>ImageCodec</code> that handles
 * the FITS image format.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public final class FITSCodec extends ImageCodec {

    /** Constructs an instance of <code>FITSCodec</code>. */
    public FITSCodec() {
    }

    /** Returns the name of the format handled by this codec. */
    public String getFormatName() {
        return "fits";
    }

    /**
     * Returns <code>Object.class</code> since no DecodeParam
     * object is required for decoding.
     */
    public Class getDecodeParamClass() {
        return Object.class;
    }

    /** Returns <code>null</code> since no encoder exists. */
    public Class getEncodeParamClass() {
        return null;
    }

    /** Returns true if the image is encodable by this codec. */
    public boolean canEncodeImage(RenderedImage im,
                                  ImageEncodeParam param) {
        SampleModel sampleModel = im.getSampleModel();

        int dataType = sampleModel.getTransferType();

        int numBands = sampleModel.getNumBands();
        if (numBands != 1 /* && numBands != 3 XXX not impl */) {
            return false;
        }

        return true;
    }

    /**
     * Instantiates a <code>FITSEncoder</code> to write to the
     * given <code>OutputStream</code>.
     *
     * @param dst the <code>OutputStream</code> to write to.
     * @param param an instance of <code>FITSEncodeParam</code> used to
     *        control the encoding process, or <code>null</code>.  A
     *        <code>ClassCastException</code> will be thrown if
     *        <code>param</code> is non-null but not an instance of
     *        <code>FITSEncodeParam</code>.
     */
    protected ImageEncoder createImageEncoder(OutputStream dst, ImageEncodeParam param) {
        FITSEncodeParam p = null;
        if (param != null) {
            p = (FITSEncodeParam) param; // May throw a ClassCast exception
        }

        return new FITSEncoder(dst, p);
    }

    /**
     * Instantiates a <code>FITSDecoder</code> to read from the
     * given <code>InputStream</code>.
     *
     * <p> By overriding this method, <code>FITSCodec</code> is able to
     * ensure that a <code>ForwardSeekableStream</code> is used to
     * wrap the source <code>InputStream</code> instead of the a
     * general (and more expensive) subclass of
     * <code>SeekableStream</code>.  Since the FITS decoder does not
     * require the ability to seek backwards in its input, this allows
     * for greater efficiency.
     *
     * @param src the <code>InputStream</code> to read from.
     * @param param an instance of <code>ImageDecodeParam</code> used to
     *        control the decoding process, or <code>null</code>.
     */
    protected ImageDecoder createImageDecoder(InputStream src, ImageDecodeParam param) {
        // Add buffering for efficiency (XXX no: done in FITS I/O classes)
        //if (!(src instanceof BufferedInputStream)) {
        //    src = new BufferedInputStream(src);
        //}
        try {
            return new FITSDecoder(new ForwardSeekableStream(src), param);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instantiates a <code>FITSDecoder</code> to read from the
     * given <code>SeekableStream</code>.
     *
     * @param src the <code>SeekableStream</code> to read from.
     * @param param an instance of <code>ImageDecodeParam</code> used to
     *        control the decoding process, or <code>null</code>.
     */
    protected ImageDecoder createImageDecoder(SeekableStream src, ImageDecodeParam param) {
        try {
            return new FITSDecoder(src, param);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the number of bytes from the beginning of the data required
     * to recognize it as being in FITS format.
     */
    public int getNumHeaderBytes() {
        return 6;
    }

    /**
     * Returns <code>true</code> if the header bytes indicate FITS format.
     *
     * @param header an array of bytes containing the initial bytes of the
     *        input data.     */
    public boolean isFormatRecognized(byte[] header) {
        return ((header[0] == 'S') &&
                (header[1] == 'I') &&
                (header[2] == 'M') &&
                (header[3] == 'P') &&
                (header[4] == 'L') &&
                (header[5] == 'E'));
    }
}
