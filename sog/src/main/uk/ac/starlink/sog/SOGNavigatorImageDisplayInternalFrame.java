// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    13-JUN-2002 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.sog;

import javax.swing.JDesktopPane;

import jsky.navigator.NavigatorImageDisplayInternalFrame;
import jsky.image.gui.ImageDisplayControl;

/**
 * Extends NavigatorImageDisplayInternalFrame so that we can
 * ultimately create instances of SOGNavigatorImageDisplay. We our
 * part here by overriding the makeImageDisplayControl method.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SOGNavigatorImageDisplayInternalFrame
    extends NavigatorImageDisplayInternalFrame
{
    // Repeat all constructors.
    public SOGNavigatorImageDisplayInternalFrame( JDesktopPane desktop,
                                                  int size )
    {
        super( desktop, size );
    }
    public SOGNavigatorImageDisplayInternalFrame( JDesktopPane desktop )
    {
        super( desktop );
    }
    public SOGNavigatorImageDisplayInternalFrame( JDesktopPane desktop,
                                                  int size, String fileOrUrl )
    {
        super( desktop, size, fileOrUrl );
    }
    public SOGNavigatorImageDisplayInternalFrame( JDesktopPane desktop,
                                                  String fileOrUrl )
    {
        super( desktop, fileOrUrl );
    }


    /**
     * Make and return the image display control frame.
     *
     * @param size the size (width, height) to use for the pan and
     *             zoom windows. 
     */
    protected ImageDisplayControl makeImageDisplayControl( int size ) 
    {
        //  Return the SOG version.
        return new SOGNavigatorImageDisplayControl( this, size );
    }
}
