// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    14-JUN-2002 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.sog;

import javax.swing.JToggleButton;
import javax.swing.ImageIcon;
import javax.swing.Action;

import jsky.navigator.NavigatorImageDisplayToolBar;
import jsky.navigator.NavigatorImageDisplay;

/**
 * Extends NavigatorImageDisplayToolBar to add the demonstration
 * ability to draw AST grid overlays.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SOGNavigatorImageDisplayToolBar
    extends NavigatorImageDisplayToolBar
{
    //  Repeat all constructors.
    public SOGNavigatorImageDisplayToolBar( NavigatorImageDisplay
                                            imageDisplay )
    {
        super( imageDisplay );
    }

    /**
     * ToggleButton to control the drawing of the grid.
     */
    protected JToggleButton gridButton;
    protected JToggleButton photomButton;

    /**
     * Add the items to the tool bar.
     */
    protected void addToolBarItems() {
        super.addToolBarItems();
        addSeparator();
        add( makeGridButton() );
        JToggleButton b = makePhotomButton();
        if ( b != null ) {
            add( b );
        }
    }

    /**
     * Make the grid drawing button, if it does not yet
     * exist. Otherwise update the display using the current options
     * for displaying text or icons.
     *
     * @return the grid toggle button.
     */
    protected JToggleButton makeGridButton()
    {
        if ( gridButton == null ) {
            gridButton =
                makeToggleButton( "display an astrometric grid overlay",
                   ((SOGNavigatorImageDisplay)imageDisplay).getGridAction() );
        }
        updateButton( gridButton, "Grid",
                      new ImageIcon( getClass().
                                     getResource( "images/grid.gif" ) ) );
        return gridButton;
    }

    protected JToggleButton makePhotomButton()
    {
        if ( photomButton == null ) {
            Action photomAction = 
                ((SOGNavigatorImageDisplay)imageDisplay).getPhotomAction();
            if ( photomAction != null ) {
                photomButton = 
                    makeToggleButton( "perform simple aperture photometry",
                                      photomAction );
            }
        }
        if ( photomButton != null ) {
            ImageIcon icon = new ImageIcon
                ( getClass().getResource( "images/aperture_photom.gif" ) );
            updateButton( photomButton, "Photometry", icon );
        }
        return photomButton;
    }

    /**
     * Update the toolbar display using the current text/pictures options.
     * (redefined from the parent class).
     */
    public void update() {
        super.update();
        makeGridButton();
        makePhotomButton();
    }
}
