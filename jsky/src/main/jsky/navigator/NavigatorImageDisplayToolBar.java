/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorImageDisplayToolBar.java,v 1.3 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.navigator;

import javax.swing.JButton;

import jsky.image.gui.ImageDisplayToolBar;
import jsky.util.I18N;
import jsky.util.Resources;


/**
 * A tool bar for the image display window.
 */
public class NavigatorImageDisplayToolBar extends ImageDisplayToolBar {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(NavigatorImageDisplayToolBar.class);

    // toolbar buttons
    protected JButton catalogButton;

    /**
     * Create the toolbar for the given window
     */
    public NavigatorImageDisplayToolBar(NavigatorImageDisplay imageDisplay) {
        super(imageDisplay);
    }

    /**
     * Add the items to the tool bar.
     */
    protected void addToolBarItems() {
        super.addToolBarItems();

        addSeparator();

        add(makeCatalogButton());
    }

    /**
     * Make the catalog button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the catalog button
     */
    protected JButton makeCatalogButton() {
        if (catalogButton == null)
            catalogButton = makeButton(_I18N.getString("showCatalogWindow"),
                    ((NavigatorImageDisplay) imageDisplay).getCatalogBrowseAction(),
                    false);

        updateButton(catalogButton,
                _I18N.getString("catalogs"),
                Resources.getIcon("Catalog24.gif"));
        return catalogButton;
    }


    /**
     * Update the toolbar display using the current text/pictures options.
     * (redefined from the parent class).
     */
    public void update() {
        super.update();
        makeCatalogButton();
    }
}

