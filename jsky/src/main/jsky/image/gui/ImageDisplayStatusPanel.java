/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageDisplayStatusPanel.java,v 1.16 2002/08/16 22:21:13 brighton Exp $
 */


package jsky.image.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jsky.coords.WorldCoords;
import jsky.image.ImageChangeEvent;
import jsky.image.ImageProcessor;
import jsky.util.I18N;
import jsky.util.Resources;


/**
 * Displays the image coordinates, pixel value, and world coordinates at the mouse pointer
 * position.
 *
 * @version $Revision: 1.16 $
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public class ImageDisplayStatusPanel extends JPanel implements MouseMotionListener {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageDisplayStatusPanel.class);

    /** Text field used to display the current zoom magnification. */
    protected JTextField zoomTextField;

    /** Text field used to display the image coordinates at the mouse position. */
    protected JTextField imageCoordsTextField;

    /** Text field used to display the image pixel value at the mouse position. */
    protected JTextField pixelValueTextField;

    /** Text field used to display the world coordinates at the mouse position. */
    protected JTextField worldCoordsTextField;

    /** The image window being monitored (for mouse motion) */
    protected MainImageDisplay imageDisplay;

    /** Used to format pixel coordinates. */
    protected static NumberFormat nf = NumberFormat.getInstance(Locale.US);

    static {
        nf.setMaximumFractionDigits(1);
        nf.setGroupingUsed(false);
    }

    /**
     * Default constructor
     */
    public ImageDisplayStatusPanel() {

        final Color bgColor = getBackground();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        JButton zoomIn = new JButton(Resources.getIcon("ZoomIn24.gif"));
        zoomIn.setToolTipText(_I18N.getString("zoomIn"));
        zoomIn.setFocusPainted(false);
        zoomIn.setPreferredSize(new Dimension(26, 24));
        zoomIn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zoom(true);
            }
        });
        add(zoomIn);

        JButton zoomOut = new JButton(Resources.getIcon("ZoomOut24.gif"));
        zoomOut.setToolTipText(_I18N.getString("zoomOut"));
        zoomOut.setFocusPainted(false);
        zoomOut.setPreferredSize(new Dimension(26, 24));
        zoomOut.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zoom(false);
            }
        });
        add(zoomOut);

        JButton zoomFit = new JButton(Resources.getIcon("AlignCenter24.gif"));
        zoomFit.setToolTipText(_I18N.getString("zoomFit"));
        zoomFit.setFocusPainted(false);
        zoomFit.setPreferredSize(new Dimension(26, 24));
        zoomFit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zoomFit();
            }
        });
        add(zoomFit);

        JButton zoomNormal = new JButton(Resources.getIcon("Zoom24.gif"));
        zoomNormal.setToolTipText(_I18N.getString("zoomNormal"));
        zoomNormal.setFocusPainted(false);
        zoomNormal.setPreferredSize(new Dimension(26, 24));
        zoomNormal.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zoomNormal();
            }
        });
        add(zoomNormal);

        zoomTextField = new JTextField(0);
        zoomTextField.setEditable(false);
        zoomTextField.setToolTipText(_I18N.getString("currentMagnification"));
        zoomTextField.setHorizontalAlignment(JTextField.CENTER);
        zoomTextField.setBackground(bgColor);
        zoomTextField.setBorder(BorderFactory.createLoweredBevelBorder());
        zoomTextField.setText("1x");
        add(zoomTextField);

        imageCoordsTextField = new JTextField(10);
        imageCoordsTextField.setEditable(false);
        imageCoordsTextField.setToolTipText(_I18N.getString("imagePixelCoordsAtMousePos"));
        imageCoordsTextField.setHorizontalAlignment(JTextField.CENTER);
        imageCoordsTextField.setBackground(bgColor);
        imageCoordsTextField.setBorder(BorderFactory.createLoweredBevelBorder());
        add(imageCoordsTextField);

        pixelValueTextField = new JTextField(8);
        pixelValueTextField.setEditable(false);
        pixelValueTextField.setToolTipText(_I18N.getString("imagePixelValueAtMousePos"));
        pixelValueTextField.setBackground(bgColor);
        pixelValueTextField.setHorizontalAlignment(JTextField.CENTER);
        pixelValueTextField.setBorder(BorderFactory.createLoweredBevelBorder());
        add(pixelValueTextField);

        worldCoordsTextField = new JTextField(17);
        worldCoordsTextField.setEditable(false);
        worldCoordsTextField.setToolTipText(_I18N.getString("worldCoordsAtMousePos"));
        worldCoordsTextField.setHorizontalAlignment(JTextField.CENTER);
        worldCoordsTextField.setBackground(bgColor);
        worldCoordsTextField.setBorder(BorderFactory.createLoweredBevelBorder());
        add(worldCoordsTextField);
    }

    /**
     * Initialize with the given image display.
     *
     * @param imageDisplay the image display to monitor
     */
    public ImageDisplayStatusPanel(MainImageDisplay imageDisplay) {
        this();
        setImageDisplay(imageDisplay);
    }


    /**
     * Set the image window to be monitored (for mouse motion).
     *
     * @param target the image display to monitor
     */
    public void setImageDisplay(MainImageDisplay target) {
        this.imageDisplay = target;
        // Add a mouse motion listener so we can display the current mouse coordinates
        ((Component) imageDisplay).addMouseMotionListener(this);

        // update the magnification display
        imageDisplay.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewScale() || e.isNewImage()) {
                    zoomTextField.setText(ImageDisplayMenuBar.getScaleLabel(imageDisplay.getScale()));
                }
            }
        });
    }


    /**
     * Return the image window being monitored (for mouse motion).
     */
    public MainImageDisplay getImageDisplay() {
        return imageDisplay;
    }


    /**
     * Zoom in (if in is true), otherwise out by one step.
     *
     * @param in true if zooming in, otherwise zoom out
     */
    protected void zoom(boolean in) {
        float zoomFactor = imageDisplay.getScale();

        if (in) {
            if (zoomFactor < 1.0) {
		int i = Math.round(1.0F/zoomFactor) - 1;
                zoomFactor = 1.0F/(float)i;
	    }
            else {
                zoomFactor += 1; 
	    }
        }
        else {
            if (zoomFactor <= 1.0) {
		int i = Math.round(1.0F/zoomFactor) + 1;
                zoomFactor = 1.0F/(float)i;
	    }
            else {
                zoomFactor -= 1;
	    }
        }

        if (zoomFactor < ImageDisplayMenuBar.MIN_SCALE || zoomFactor > ImageDisplayMenuBar.MAX_SCALE) {
            return;
	}

        imageDisplay.setScale(zoomFactor);
        imageDisplay.updateImage();
    }

    /**
     * Zoom the image in or out by an integer factor to fit in the window.
     */
    protected void zoomFit() {
        imageDisplay.scaleToFit();
        imageDisplay.updateImage();
    }

    /**
     * Return the image to the default scale (no zoom).
     */
    protected void zoomNormal() {
        imageDisplay.setScale(1.0F);
        imageDisplay.updateImage();
    }


    /**
     * Invoked when a mouse button is pressed on the image and then
     * dragged.
     */
    public void mouseDragged(MouseEvent e) {
        update(e);
    }


    /*
     * Invoked when the mouse button has been moved over the image
     * (with no buttons down).
     */
    public void mouseMoved(MouseEvent e) {
        update(e);
    }

    /**
     * Update the status panel with the current position information
     */
    protected void update(MouseEvent e) {
        if (imageDisplay == null)
            return;

        // fill in the text items with the current values
        Point2D.Double p;
        try {
            // image coords
            p = new Point2D.Double(e.getX(), e.getY());
            imageDisplay.getCoordinateConverter().screenToImageCoords(p, false);
            imageCoordsTextField.setText(nf.format(p.getX()) + ", " + nf.format(p.getY()));

            // world coords
            worldCoordsTextField.setText("");
            if (imageDisplay.isWCS()) {
                imageDisplay.getCoordinateConverter().imageToWorldCoords(p, false);
                double equinox = imageDisplay.getWCS().getEquinox();
                WorldCoords wcs = new WorldCoords(p.getX(), p.getY(), equinox);

                // XXX should we display the coordinates in the equinox of the image? Or user defined?
                /*
		String[] ar = wcs.format(equinox);

		String equinoxStr;
		if (equinox == 2000.)
		    equinoxStr = "J2000";
		else if (equinox == 1950.)
		    equinoxStr = "B1950";
		else
		    equinoxStr = Double.toString(equinox);

		worldCoordsTextField.setText(ar[0] + ", " + ar[1] + " (" + equinoxStr + ")");
		*/
                worldCoordsTextField.setText(wcs.toString());
            }

        }
        catch (Exception ex) {
            //ex.printStackTrace();
        }

        try {
            pixelValueTextField.setText("");
            // try to extract the pixel value under the mouse and display it
            p = new Point2D.Double(e.getX(), e.getY());
            imageDisplay.getCoordinateConverter().screenToUserCoords(p, false);
            double pixel = imageDisplay.getPixelValue(p, 0); //PWD:made double

            ImageProcessor imageProcessor = imageDisplay.getImageProcessor();
            if (pixel == imageProcessor.getBlank()) {
                pixelValueTextField.setText(_I18N.getString("blank"));
            }
            else {
                pixelValueTextField.setText(String.valueOf(pixel));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}


