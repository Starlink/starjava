// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    13-JUN-2002 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.sog;

import jsky.navigator.NavigatorImageDisplayFrame;
import jsky.navigator.NavigatorImageDisplay;
import jsky.image.gui.ImageDisplayToolBar;
import jsky.image.gui.ImageDisplayControl;
import jsky.image.gui.DivaMainImageDisplay;

/**
 * Extends NavigatorImageDisplayFrame so that we can ultimately create
 * instances of SOGNavigatorImageDisplay and SOGImageDisplayToolBar.
 * We our part here by overriding the makeImageDisplayControl
 * and makeToolBar methods
 *
 *
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SOGNavigatorImageDisplayFrame
    extends NavigatorImageDisplayFrame
{
    // Repeat all constructors.
    public SOGNavigatorImageDisplayFrame( int size )
    {
        super( size );
    }
    public SOGNavigatorImageDisplayFrame()
    {
        super();
    }
    public SOGNavigatorImageDisplayFrame( int size, String fileOrUrl )
    {
        super( size, fileOrUrl );
    }
    public SOGNavigatorImageDisplayFrame( String fileOrUrl )
    {
        super( fileOrUrl );
    }

    /**
     * Make and return the image display control frame.
     *
     * @param size the size (width, height) to use for the pan and
     *             zoom windows.
     */
    protected ImageDisplayControl makeImageDisplayControl( int size )
    {
        return new SOGNavigatorImageDisplayControl( this, size );
    }

    /**
     * Make and return the toolbar
     */
    protected ImageDisplayToolBar
        makeToolBar( DivaMainImageDisplay mainImageDisplay )
    {
        return new SOGNavigatorImageDisplayToolBar
            ( (NavigatorImageDisplay) mainImageDisplay );
    }
}
