/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageDisplayToolBar.java,v 1.3 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.gui;

import javax.swing.JButton;

import jsky.util.I18N;
import jsky.util.Resources;
import jsky.util.gui.GenericToolBar;


/**
 * A tool bar for the image display window.
 */
public class ImageDisplayToolBar extends GenericToolBar {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageDisplayToolBar.class);

    /** The target ImageDisplay object */
    protected DivaMainImageDisplay imageDisplay;

    // toolbar buttons
    protected JButton cutLevelsButton;

    /**
     * Create the toolbar for the given window
     */
    public ImageDisplayToolBar(DivaMainImageDisplay imageDisplay) {
        super(imageDisplay, false);
        this.imageDisplay = imageDisplay;
        addToolBarItems();

        openButton.setToolTipText(_I18N.getString("imageOpenTip"));
        backButton.setToolTipText(_I18N.getString("imageBackTip"));
        forwardButton.setToolTipText(_I18N.getString("imageForwardTip"));
    }

    /**
     * Add the items to the tool bar.
     */
    protected void addToolBarItems() {
        super.addToolBarItems();

        addSeparator();

        add(makeCutLevelsButton());
    }

    /**
     * Make the cut levels button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the cut levels button
     */
    protected JButton makeCutLevelsButton() {
        if (cutLevelsButton == null)
            cutLevelsButton = makeButton(_I18N.getString("cutLevelsTip"), imageDisplay.getCutLevelsAction(), false);

        updateButton(cutLevelsButton,
                _I18N.getString("cutLevels"),
                Resources.getIcon("CutLevels24.gif"));
        return cutLevelsButton;
    }


    /**
     * Update the toolbar display using the current text/pictures options.
     * (redefined from the parent class).
     */
    public void update() {
        super.update();

        makeCutLevelsButton();
    }
}

