/*
 * ESO Archive
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 * 
 * $Id$
 * 
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/11/17  Created
 * Peter W. Draper 2002/05/10  Converted for HDX
 */
package uk.ac.starlink.jaiutil;

import com.sun.media.jai.codec.ImageDecodeParam;

public class HDXDecodeParam 
    implements ImageDecodeParam 
{    
    /** 
     * This specifies the size of the preview image. The default is 0,
     * meaning that no preview image will be generated. If a value > 0
     * is specified, it is taken to be the size of the window in which 
     * the preview image should fit.
     */
    private int previewSize = 0;

    public HDXDecodeParam() {}

    /**
     * Set the size of the window used to display the prescaled preview image.
     * This determines the scale factor.
     */
    public void setPreviewSize(int i) {previewSize = i;}

    /** Return the value of the previewSize property. */
    public int getPreviewSize() {return previewSize;}
}

