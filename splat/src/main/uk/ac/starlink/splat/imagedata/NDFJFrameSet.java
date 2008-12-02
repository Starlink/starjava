/*
 *  Copyright (C) 2002 Central Laboratory of the Research Councils
 * 
 *  History:
 *    10-JUN-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.imagedata;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Frame;

/**
 * NDFJ specific FrameSet. Only difference is that that NDF FrameSet
 * is passed through the JNI layer as a pointer (other ways, like
 * passing it as a character array are too slow).
 * <p>
 * Note this is package private!
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
class NDFJFrameSet extends FrameSet 
{
    NDFJFrameSet( long pointer )
    {
        this.pointer = pointer;
    }

    NDFJFrameSet()
    {
        super();
    }

    NDFJFrameSet( Frame frame ) {
        super( frame );
    }    

    public long getPointer()
    {
        return pointer;
    }
}
