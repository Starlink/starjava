/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorToolBar.java,v 1.4 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.navigator;

import javax.swing.JButton;

import jsky.util.I18N;
import jsky.util.Resources;
import jsky.util.gui.GenericToolBar;


/**
 * A tool bar for the Navigator window.
 */
public class NavigatorToolBar extends GenericToolBar {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(NavigatorToolBar.class);

    /** The target Navigator object */
    protected Navigator navigator;

    // toolbar buttons
    protected JButton imageDisplayButton;

    /**
     * Create the toolbar for the given window
     */
    public NavigatorToolBar(Navigator navigator) {
        super(navigator, false);
        this.navigator = navigator;
        addToolBarItems();

        openButton.setToolTipText(_I18N.getString("catalogOpen"));
        backButton.setToolTipText(_I18N.getString("catalogBack"));
        forwardButton.setToolTipText(_I18N.getString("catalogForward"));
    }

    /**
     * Add the items to the tool bar.
     */
    protected void addToolBarItems() {
        super.addToolBarItems();

        addSeparator();

        add(makeImageWinButton());
    }

    /**
     * Make the image window button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the image window button
     */
    protected JButton makeImageWinButton() {
        if (imageDisplayButton == null)
            imageDisplayButton = makeButton(_I18N.getString("showImageWin"), navigator.getImageDisplayAction(), false);

        updateButton(imageDisplayButton,
                _I18N.getString("image"),
                Resources.getIcon("ImageDisplay24.gif"));
        return imageDisplayButton;
    }


    /**
     * Update the toolbar display using the current text/pictures options.
     * (redefined from the parent class).
     */
    public void update() {
        super.update();
        makeImageWinButton();
    }
}

