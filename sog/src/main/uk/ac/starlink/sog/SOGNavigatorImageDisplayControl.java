// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    13-JUN-2002 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.sog;

import jsky.navigator.NavigatorImageDisplayControl;
import jsky.image.gui.ImageDisplayControl;
import jsky.image.gui.DivaMainImageDisplay;

import java.awt.Component;
import java.net.URL;

/**
 * Extents NavigatorImageDisplayControl to override the
 * makeImageDisplay method. This allows us to use a
 * SOGNavigatorImageDisplay
 *
 * @author Peter W. Draper
 * @version $Id$
 */

public class SOGNavigatorImageDisplayControl
    extends NavigatorImageDisplayControl
{
    //  Repeat all NavigatorImageDisplayControl constructors.
    public SOGNavigatorImageDisplayControl( Component parent, int size )
    {
	super( parent, size );
    }
    public SOGNavigatorImageDisplayControl( Component parent )
    {
	super( parent );
    }
    public SOGNavigatorImageDisplayControl( Component parent, URL url )
    {
	super( parent, url );
    }
    public SOGNavigatorImageDisplayControl( Component parent, String filename )
    {
	super( parent, filename );
    }

    /**
     * Make and return the image display window
     */
    protected DivaMainImageDisplay makeImageDisplay()
    {
	return new SOGNavigatorImageDisplay( parent );
    }
}

