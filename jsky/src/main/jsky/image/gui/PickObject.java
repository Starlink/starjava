/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: PickObject.java,v 1.8 2002/08/16 22:21:13 brighton Exp $
 */

package jsky.image.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.text.NumberFormat;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jsky.coords.CoordinateConverter;
import jsky.image.ImageChangeEvent;
import jsky.util.I18N;
import jsky.util.Resources;
import jsky.util.gui.DialogUtil;


/**
 * A User interface for selecting objects (stars/galaxies) in an image.
 * <p>
 * Note that GUI here has some catalog related features ("Add" button, "Table Name" field),
 * but the code that implements these features is defined in the jsky.navigator package.
 *
 * @see jsky.navigator.Navigator
 * @see jsky.navigator.NavigatorImageDisplay
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 */
public class PickObject extends PickObjectGUI
        implements ImageGraphicsHandler, ChangeListener {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(PickObject.class);

    /** Default size (width, height) of the window displaying the image */
    public static final int IMAGE_SIZE = 200;

    /** Default zoom factor for the image */
    public static final int IMAGE_ZOOM = 10;

    // The top level parent frame (or internal frame) used to close the window
    private Component _parent;

    /** The target image display */
    private MainImageDisplay _mainImageDisplay;

    /** Displays the magnified image at the mouse pointer */
    private ImageZoom _imageZoom;

    /** The zoom image display */
    private BasicImageDisplay _imageDisplay;

    /** Listens for mouse clicks to select objects (stars/galaxies) in the image, when enabled */
    private MouseListener _pickObjectListener;

    /** Width (and height) of the area of the image being examined, in source image pixels */
    private int _zoomWidth;

    /** Contains information about the selected position. */
    private PickObjectStatistics _stats;

    /** Used to mark positions (background, black mark) */
    private Stroke _bgStroke = new BasicStroke(5.0F);

    /** Used to mark positions (foreground, white mark) */
    private Stroke _fgStroke = new BasicStroke(3.0F);

    /** Set to true when picking an object */
    private boolean _pickMode = false;

    /** Set to true when zooming in or out after selecting an object. */
    private boolean _updating = false;


    /**
     * Initialize a PickObject panel.
     *
     * @param parent the parent frame or internal frame
     * @param mainImageDisplay the target image display
     */
    public PickObject(Component parent, MainImageDisplay mainImageDisplay) {
        super();

        _parent = parent;
        _mainImageDisplay = mainImageDisplay;
        _imageZoom = new ImageZoom(_mainImageDisplay, IMAGE_SIZE, IMAGE_SIZE, IMAGE_ZOOM);
        _imageZoom.setActive(false);
        _imageZoom.setPropagateScale(false);
        imagePanel.add(_imageZoom);

        // used to mark the currently selected position
        _imageDisplay = _imageZoom.getImageDisplay();
        _imageDisplay.addImageGraphicsHandler(this);

        _mainImageDisplay.addImageGraphicsHandler(this);
        _mainImageDisplay.addChangeListener(this);

        zoomInButton.setIcon(Resources.getIcon("ZoomIn24.gif"));
        zoomInButton.setText("");
        zoomOutButton.setIcon(Resources.getIcon("ZoomOut24.gif"));
        zoomOutButton.setText("");
    }


    /**
     * Start tracking and displaying changes in the mouse position over the main image and
     * and wait for a mouse click to select a position.
     */
    public void pickObject() {
        _imageZoom.setActive(true);
        _imageZoom.updateRect();

        if (!_mainImageDisplay.isWCS()) {
            DialogUtil.error(this, _I18N.getString("thisImageNoWCSError"));
            return;
        }
        if (_mainImageDisplay.getFitsImage() == null) {
            DialogUtil.error(this, _I18N.getString("thisFeatureOnlyForFITSError"));
            return;
        }
        if (_pickObjectListener == null) {
            _pickObjectListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (!isAutoAddMode())
                        _cancel();
                    _pickedObject();
                }
            };
        }
        Component c = (Component) _mainImageDisplay;
        c.removeMouseListener(_pickObjectListener);
        c.addMouseListener(_pickObjectListener);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        pickButton.setText(_I18N.getString("cancel"));
        _pickMode = true;
        _stats = null;
    }


    /** Return an object with information about the selected position, or null if no position was selected. */
    public PickObjectStatistics getStatistics() {
        return _stats;
    }


    /** Add an action listener for the "Add" button. */
    public void addActionListener(ActionListener l) {
        addButton.removeActionListener(l);
        addButton.addActionListener(l);
    }

    /** Remove an action listener for the "Add" button. */
    public void removeActionListener(ActionListener l) {
        addButton.removeActionListener(l);
    }


    /**
     * Callers can check this value to know if objects should be
     * automatically added to a table when picked.
     */
    public boolean isAutoAddMode() {
        return autoAddCheckBox.isSelected();
    }

    /** Returns true while zooming in or out after selecting an object. */
    public boolean isUpdate() {
        return _updating;
    }


    /**
     * Update the data display based on the current position.
     */
    private void _pickedObject() {
        Point2D.Double p = _imageDisplay.getOrigin();
        _imageDisplay.getCoordinateConverter().canvasToUserCoords(p, false);

        int zoomScale = (int) _imageDisplay.getScale();
        _zoomWidth = ((Component) _imageDisplay).getWidth() / zoomScale;

        _stats = new PickObjectStatistics(_mainImageDisplay, (int) p.x, (int) p.y, _zoomWidth, _zoomWidth);
        _updateDisplay();

        if (isAutoAddMode()) {
            addButton.setEnabled(true);
            addButton.doClick();
            addButton.setEnabled(false);
        }
        else {
            // don't allow adding invalid statistics
            addButton.setEnabled(true);
        }
    }


    /**
     * Update the data display using the given statistics.
     */
    private void _updateDisplay() {
        // use this static object to format the numbers
        NumberFormat nf = ImageDisplayStatusPanel.nf;

        imageXField.setText(nf.format(_stats.getImageX()));
        imageYField.setText(nf.format(_stats.getImageY()));
        raField.setText(_stats.getCenterPos().getRA().toString());
        decField.setText(_stats.getCenterPos().getDec().toString());
        equinoxField.setText("2000");

        if (_stats.getStatus()) {
            fwhmField.setForeground(Color.black);
            fwhmField.setText(nf.format(_stats.getFwhmX()) + " : " + nf.format(_stats.getFwhmY()));
        }
        else {
            fwhmField.setForeground(Color.red);
            fwhmField.setText(_I18N.getString("cantDo"));
        }

        angleField.setText(nf.format(_stats.getAngle()));
        peakField.setText(nf.format(_stats.getObjectPeak()));
        backgroundField.setText(nf.format(_stats.getMeanBackground()));
        pixelsField.setText(nf.format(_zoomWidth));

        ((Component) _imageDisplay).repaint();
        ((Component) _mainImageDisplay).repaint();
    }


    /** Called when the Pick/Cancel button is pressed */
    void pickObject(ActionEvent e) {
        if (_pickMode) {
            _cancel();
        }
        else {
            pickObject();
        }
        addButton.setEnabled(false);
    }

    /** Cancel the current pick operation */
    private void _cancel() {
        Component c = (Component) _mainImageDisplay;
        c.removeMouseListener(_pickObjectListener);
        _imageZoom.setActive(false);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        pickButton.setText(_I18N.getString("pick"));
        _pickMode = false;
    }

    /** Called when the Close button is pressed */
    void close(ActionEvent e) {
        _cancel();
        if (_parent != null)
            _parent.setVisible(false);
    }

    /** Called when the Add button is pressed */
    void add(ActionEvent e) {
        // This only disables the button to avoid adding the same item twice.
        // The rest of the work is done in class NavgatorImageDisplay
        addButton.setEnabled(false);
    }

    /** Called when the Zoom In button is pressed */
    void zoomIn(ActionEvent e) {
        _zoom(true);
    }

    /** Called when the Zoom Out button is pressed */
    void zoomOut(ActionEvent e) {
        _zoom(false);
    }

    /**
     * Zoom in (if in is true), otherwise out by one step.
     *
     * @param in true if zooming in, otherwise zoom out
     */
    private void _zoom(boolean in) {
        float scale = _imageDisplay.getScale();

        if (in) {
            if (scale >= 50)
                scale = 50;
            else
                scale += 1;
        }
        else {
            if (scale <= 2)
                scale = 2;
            else
                scale -= 1;
        }

        _imageDisplay.setScale(scale);
        _imageDisplay.updateImage();
        _magChanged();
    }


    /**
     * Called to update the display after the magnification has changed
     */
    private void _magChanged() {
        float zoomScale = _imageDisplay.getScale();
        magLabel.setText("" + ((int) zoomScale) + "x");

        Component c = (Component) _imageDisplay;
        int w = c.getWidth(), h = c.getHeight();
        _zoomWidth = (int) (w / zoomScale);


        if (_stats != null) {
            Point2D.Double p = new Point2D.Double(_stats.getImageX(), _stats.getImageY());
            CoordinateConverter cc = _mainImageDisplay.getCoordinateConverter();
            cc.imageToScreenCoords(p, false);
            _imageZoom.zoom((int) p.x, (int) p.y, true);
        }

        _updating = true;
        try {
            _pickedObject();
        }
        finally {
            _updating = false;
        }
    }


    /**
     * Called when the main image changes in some way. The change event
     * (ImageChangeEvent) describes what changed.
     */
    public void stateChanged(ChangeEvent ce) {
        ImageChangeEvent e = (ImageChangeEvent) ce;
        if (e.isNewImage()) {
            _stats = null;  // new image, reset statistics
        }
    }


    /**
     * Implements the ImageGraphicsHandler interface.
     * Called each time the image is repainted.
     *
     * @see ImageGraphicsHandler
     */
    public void drawImageGraphics(BasicImageDisplay imgDisp, Graphics2D g) {
        if (_stats == null)
            return;

        // convert to radian
        double rad = _stats.getAngle() / 57.2958;

        // deltas for X and Y axis
        double x = _stats.getFwhmX(), y = _stats.getFwhmY();
        double dxX = Math.cos(rad) * x / 2.0;
        double dyX = Math.sin(rad) * x / 2.0;
        double dxY = Math.cos(rad) * y / 2.0;
        double dyY = Math.sin(rad) * y / 2.0;

        // compute end points for X-axis and convert them to canvas coordinates
        double xc = _stats.getImageX(), yc = _stats.getImageY();
        CoordinateConverter cc = imgDisp.getCoordinateConverter();
        Point2D.Double px1 = new Point2D.Double(xc + dxX, yc + dyX);
        Point2D.Double px2 = new Point2D.Double(xc - dxX, yc - dyX);
        cc.imageToScreenCoords(px1, false);
        cc.imageToScreenCoords(px2, false);

        // the Y-axis is rotated "by hand" so that it appears perpendicular to the X-axis
        Point2D.Double py1 = new Point2D.Double(xc + dyY, yc - dxY);
        Point2D.Double py2 = new Point2D.Double(xc - dyY, yc + dxY);
        cc.imageToScreenCoords(py1, false);
        cc.imageToScreenCoords(py2, false);

        // draw X and Y axis lines with an outer thick black line
        // and inner thin white line
        Line2D.Double lx = new Line2D.Double(px1, px2);
        Line2D.Double ly = new Line2D.Double(py1, py2);

        // save values
        Stroke stroke = g.getStroke();
        Paint paint = g.getPaint();

        g.setStroke(_bgStroke);
        g.setPaint(Color.black);
        g.draw(lx);
        g.draw(ly);

        g.setStroke(_fgStroke);
        g.setPaint(Color.white);
        g.draw(lx);
        g.draw(ly);

        // restore values
        g.setStroke(stroke);
        g.setPaint(paint);
    }
}

