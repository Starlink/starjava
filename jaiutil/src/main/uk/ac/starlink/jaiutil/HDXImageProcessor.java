/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     02-OCT-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.jaiutil;

import com.sun.media.jai.codec.ImageCodec;
import java.awt.geom.Rectangle2D;
import java.awt.image.SampleModel;
import javax.media.jai.PlanarImage;
import jsky.image.ImageProcessor;

/**
 * Extends the JSky ImageProcessor class so that HDXImages are
 * processed the same as FITSImages, i.e. are flipped top to bottom by
 * default and have their bad pixel flags set correctly.
 * <p>
 * Use an instance of this class instead of ImageProcessor when you
 * are expecting to deal with HDXImages (see the various constructors
 * for {@link jsky.image.gui.DivaGraphicsImageDisplay} and
 * {@link jsky.image.gui.ImageDisplay} for how to get this used).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class HDXImageProcessor extends ImageProcessor
{
    // static initializer
    static {
        //  Add HDX support.
        ImageCodec.registerCodec(new HDXCodec());
    }

    /**
     * Default constructor.
     *
     * Call setSourceImage(PlanarImage, Rectangle2D.Double) to set the
     * image to process and the region of interest.
     */
    public HDXImageProcessor()
    {
        //  Does nothing.
    }


    /**
     * Constructor.
     *
     * @param sourceImage The source image to process.
     * @param region the region of interest in the image (usually the
     *               visible area) in the coordinates of the source
     *               image (values will be clipped).
     */
    public HDXImageProcessor( PlanarImage sourceImage,
                              Rectangle2D.Double region )
    {
        setSourceImage( sourceImage, region );
    }


    /**
     * Set the source image and the region of interest and perform any
     * requested image processing to make the display image.
     *
     * @param region the region of interest in the image (usually the
     *               visible area) in the coordinates of the source image.
     */
    public void setSourceImage( PlanarImage sourceImage,
                                Rectangle2D.Double region )
    {
        //  Let the super class method do it's usual work.
        super.setSourceImage( sourceImage, region );

        //  Certain sourceImages result in an immediate return from
        //  super class method.
        //  with it.
        if ( sourceImage == null ) {
            return;
        }
        SampleModel sampleModel = sourceImage.getSampleModel();
        if ( sampleModel == null ) {
            return;
        }

        // If this is an HDXImage then we have extra work to do.
        //Object o = sourceImage.getProperty("#hdx_image");
        Object o = sourceImage.getProperty( "#ndx_image" );
        if ( o instanceof HDXImage ) {
            HDXImage hdxImage = (HDXImage) o;

            //  HDX images are always display flipped. Note we access
            //  member directly as setReverseY() has side-effects.
            _reverseY = true;

            // BZERO and BSCALE are applied by NDArray. Should already
            // be 0 and 1, if not FITSImage.
            //_bzero = 0.0;
            //_bscale = 1.0;
            //_rescaledSourceImage = _sourceImage;

            // Check for grayscale images (always)
            if( sampleModel.getNumBands() == 1 ) {

                // Get value of BAD pixels.
                setBlank( hdxImage.getBadValue() );

                //  Set the image cut levels, preserving user's choice
                //  if needed (is repeated from super-class, so not
                //  wildly efficient, but need to repeat for BAD
                //  values to be properly excluded).
                if ( ! isUserSetCutLevels() ) {
                    autoSetCutLevels( region );
                }
                else {
                    setRegionOfInterest(region);
                    setCutLevels( getLowCut(), getHighCut(), true );
                }
            }
        }
    }
}
