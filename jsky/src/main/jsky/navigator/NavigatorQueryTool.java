/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorQueryTool.java,v 1.20 2002/08/04 21:48:51 brighton Exp $
 */

package jsky.navigator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jsky.catalog.Catalog;
import jsky.catalog.QueryArgs;
import jsky.catalog.gui.CatalogQueryPanel;
import jsky.catalog.gui.CatalogQueryTool;
import jsky.catalog.gui.QueryResultDisplay;
import jsky.coords.CoordinateConverter;
import jsky.coords.CoordinateRadius;
import jsky.coords.WorldCoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.graphics.SelectedAreaListener;
import jsky.image.gui.MainImageDisplay;
import jsky.util.gui.DialogUtil;


/**
 * Displays a panel for entering query parameters for a catalog search.
 * This class extends the parent class by adding some
 * buttons to set the search area based on the current image display.
 */
public class NavigatorQueryTool extends CatalogQueryTool implements SelectedAreaListener {

    /** Reference to the main image display window */
    private MainImageDisplay _imageDisplay;

    /** Panel button to select an image area. */
    private JButton _selectAreaButton;

    /** Panel button to set query args from the image being displayed. */
    private JButton _setFromImageButton;

    /** Set to the selected area for a query, or null for the entire image */
    private Rectangle2D _selectedArea;


    /**
     * Create a NavigatorQueryTool for searching the given catalog.
     *
     * @param catalog The catalog to use.
     * @param queryResultDisplay used to display query results other than tables
     * @param imageDisplay the image display to use to plot catalog symbols
     */
    public NavigatorQueryTool(final Catalog catalog, QueryResultDisplay queryResultDisplay,
                              MainImageDisplay imageDisplay) {
        super(catalog, queryResultDisplay);
	setImageDisplay(imageDisplay);

        // update the RA,Dec display if a different equinox is chosen
        getCatalogQueryPanel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String s = getCatalogQueryPanel().getLabelForComponent(cb);
                if (s == null)
                    return;
                if (s.equalsIgnoreCase("equinox")) {
                    if (_selectedArea != null)
                        setSelectedArea(_selectedArea);
                    else
                        setFromImage(false);
                }
            }
        });
    }


    /**
     * Create a NavigatorQueryTool for searching the given catalog.
     *
     * @param catalog The catalog to use.
     * @param queryResultDisplay use to display query results
     */
    public NavigatorQueryTool(Catalog catalog, QueryResultDisplay queryResultDisplay) {
	this(catalog, queryResultDisplay, null);

	if (queryResultDisplay instanceof Navigator) {
	    Navigator navigator = (Navigator)queryResultDisplay;
	    setImageDisplay(navigator.getImageDisplay());
	}
    }


    /** Return a reference to the main image display window, or null if there isn't one. */
    public MainImageDisplay getImageDisplay() {
        return _imageDisplay;
    }

    /** Set the window used to display images (from image servers, etc...). */
    public void setImageDisplay(MainImageDisplay im) {
        _imageDisplay = im;
        updateImageButtonStates();
	Catalog catalog = getCatalog();
        setFromImage(catalog != null && catalog.isImageServer());
    }

    /** Set the enabled state of the image related buttons. */
    protected void updateImageButtonStates() {
        boolean b = (_imageDisplay != null);
        _selectAreaButton.setEnabled(b);
        _setFromImageButton.setEnabled(b);
    }

    /**
     * Make and return the button panel
     * (Redefined from the parent class to add 2 buttons).
     */
    public JPanel makeButtonPanel() {
        JPanel buttonPanel = super.makeButtonPanel();

        _selectAreaButton = new JButton("Select Area...");
        _selectAreaButton.setToolTipText("Drag out an area of the image to use for the query");
        _selectAreaButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectArea();
            }
        });
        buttonPanel.add(_selectAreaButton);

        _setFromImageButton = new JButton("Set From Image");
        _setFromImageButton.setToolTipText("Set the query parameters from the currently displayed image");
        _setFromImageButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setFromImage(false);
            }
        });
        buttonPanel.add(_setFromImageButton);

        return buttonPanel;
    }


    /** Pop up a dialog asking the user to select an area of the image */
    protected void selectArea() {
        if (_imageDisplay == null) {
            DialogUtil.error("There is no image display associated with this window.");
            return;
        }
        DialogUtil.message("Please drag out an area of the image with the left mouse button.");
        _imageDisplay.getCanvasGraphics().selectArea(this);
    }

    /**
     * Invoked when an area of the image canvas has been dragged out to
     * select the region of the query.
     *
     * @param r the selected area of the image in screen coordinates.
     */
    public void setSelectedArea(Rectangle2D r) {
        _selectedArea = r;
        if (_imageDisplay == null || r == null)
            return;
        CoordinateConverter wcs = _imageDisplay.getCoordinateConverter();
        if (!wcs.isWCS()) {
            //DialogUtil.error("The image does not support world coordinates.");
            return;
        }

        // get the center position
        Point2D.Double center = new Point2D.Double(r.getX() + r.getWidth() / 2.,
                r.getY() + r.getHeight() / 2.);
        wcs.screenToWorldCoords(center, false);
        double equinox = wcs.getEquinox();
        WorldCoords centerPos = new WorldCoords(center, equinox);

        // get the radius in arcmin
        Point2D.Double p = new Point2D.Double(r.getX(), r.getY());
        wcs.screenToWorldCoords(p, false);
        WorldCoords origin = new WorldCoords(p, equinox);
        double radius = centerPos.dist(origin);

        // get the image width and height in arcmin
        Point2D.Double dims = new Point2D.Double(r.getWidth(), r.getHeight());
        wcs.screenToWorldCoords(dims, true);
        double width = dims.x * 60; // convert deg to arcmin
        double height = dims.y * 60;

        // set the values in the query panel
        CatalogQueryPanel catalogQueryPanel = getCatalogQueryPanel();
        QueryArgs queryArgs = catalogQueryPanel.getQueryArgs();
        getCatalog().setRegionArgs(queryArgs, new CoordinateRadius(centerPos, radius, width, height));
        catalogQueryPanel.setQueryArgs(queryArgs);
    }


    /**
     * Set the search coordinates and radius to match the image being displayed.
     *
     * @param useDefaultSize if true, use the default width and height of 15 arcmin with the
     *  center coordinates of the image, instead of using the image size.
     */
    protected void setFromImage(boolean useDefaultSize) {
        _selectedArea = null;
        if (_imageDisplay == null)
            return;
        WorldCoordinateConverter wcs = _imageDisplay.getWCS();
        if (wcs == null) {
            //DialogUtil.error("The image does not support world coordinates.");
            return;
        }

        // get the center position
        WorldCoords centerPos = _imageDisplay.getBasePos();

        // get the radius in arcmin
        Point2D.Double p1 = new Point2D.Double(1., 1.);
        wcs.imageToWorldCoords(p1, false);
        double equinox = wcs.getEquinox();
        WorldCoords origin = new WorldCoords(p1, equinox);

        double radius, width, height;
        if (useDefaultSize) {
            width = 15.;
            height = 15.;
            radius = Math.sqrt(2. * 15. * 15.) / 2.;
        }
        else {
            radius = centerPos.dist(origin);
            width = wcs.getWidthInDeg() * 60; // convert deg to arcmin
            height = wcs.getHeightInDeg() * 60;
        }

        // set the values in the query panel
        CatalogQueryPanel catalogQueryPanel = getCatalogQueryPanel();
        QueryArgs queryArgs = catalogQueryPanel.getQueryArgs();
        try {
            getCatalog().setRegionArgs(queryArgs, new CoordinateRadius(centerPos, radius, width, height));
            catalogQueryPanel.setQueryArgs(queryArgs);
        }
        catch (Exception e) {
        }
    }
}


