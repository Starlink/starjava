/*
 * ESO Archive
 *
 * $Id: ImageDisplayPanel.java,v 1.7 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 * Peter W. Draper 2002/10/02  Made return from getPixelValue double precision
 */

package jsky.image.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.text.NumberFormat;
import java.util.Locale;
import javax.media.jai.PlanarImage;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jsky.coords.WorldCoords;
import jsky.image.ImageChangeEvent;
import jsky.image.ImageProcessor;
import jsky.image.fits.codec.FITSImage;
import jsky.util.Resources;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.GridBagUtil;

/**
 * An image display control panel.
 *
 * @version $Revision: 1.7 $
 * @author Allan Brighton
 */
public class ImageDisplayPanel extends JPanel implements MouseMotionListener {

    // hook to the component used to manage image operations
    protected ImageProcessor imageProcessor;

    // hook to the image display component
    protected MainImageDisplay imageDisplay;

    // layout helper
    protected GridBagUtil layout;

    // panel labels and text fields
    protected JLabel objectLabel;
    protected JLabel objectValue;

    protected JLabel xLabel;
    protected JLabel xValue;
    protected JLabel yLabel;
    protected JLabel yValue;
    protected JLabel valueLabel;
    protected JLabel valueValue;

    protected JLabel raLabel;
    protected JLabel raValue;
    protected JLabel decLabel;
    protected JLabel decValue;
    protected JLabel equinoxLabel;
    protected JLabel equinoxValue;

    protected JLabel minLabel;
    protected JLabel minValue;
    protected JLabel maxLabel;
    protected JLabel maxValue;
    protected JLabel bitpixLabel;
    protected JLabel bitpixValue;

    protected JLabel lowLabel;
    protected JTextField lowValue;
    protected JLabel highLabel;
    protected JTextField highValue;
    protected JButton autocutButton;

    protected JLabel scaleLabel;
    protected JMenuBar scaleMenuBar;
    protected JMenu scaleMenu;

    protected JButton zoomInButton;
    protected JButton zoomOutButton;

    // constants (for now)
    protected static final int
            minScale = -10,		// zoom out 10x
            maxScale = 20;		// zoom in 20x

    /** panel orientation: one of SwingConstants.HORIZONTAL, .VERTICAL */
    protected int orient;

    /** number of bits per pixel in source image */
    protected int bitpix;

    /** Used to format pixel coordinates. */
    protected static NumberFormat nf = NumberFormat.getInstance(Locale.US);

    static {
        nf.setMaximumFractionDigits(1);
    }


    /**
     * Create a panel for controlling the given image display
     */
    public ImageDisplayPanel(MainImageDisplay imageDisplay, int orient) {
        this.orient = orient;
        setImageDisplay(imageDisplay);

        // create labels and values
        makePanelItems();

        // organize the labels and fields in a GridBagLayout
        setLayout(new GridBagLayout());
        layout = new GridBagUtil(this, (GridBagLayout) getLayout());

        if (orient == SwingConstants.HORIZONTAL) {
            horizontalPanelLayout();
        }
        else {
            verticalPanelLayout();
        }

        // fill in the values, where known
        updateValues();

        //scaleMenu.setRequestFocusEnabled(true); // XXX ???
    }


    /**
     * Constructor: defaults to vertical layout
     */
    public ImageDisplayPanel(MainImageDisplay imageDisplay) {
        this(imageDisplay, SwingConstants.VERTICAL);
    }

    /**
     * Default constructor: must call setImageDisplay() later
     */
    public ImageDisplayPanel() {
        this(null);
    }


    /**
     * Set the ImageDisplay to work with
     */
    public void setImageDisplay(MainImageDisplay imageDisplay) {
        this.imageDisplay = imageDisplay;
        imageProcessor = imageDisplay.getImageProcessor();

        // Add a mouse motion listener so we can display the current mouse coordinates
        ((Component) imageDisplay).addMouseMotionListener(this);
    }


    public MainImageDisplay getImageDisplay() {
        return imageDisplay;
    }


    /**
     * Make the display panel items
     */
    protected void makePanelItems() {
        final int
                right = JLabel.RIGHT,
                left = JLabel.LEFT;

        objectLabel = new JLabel("Object:", right);
        objectValue = new JLabel("", left);

        xLabel = new JLabel("X:", right);
        xValue = new JLabel("", left);
        yLabel = new JLabel("Y:", right);
        yValue = new JLabel("", left);
        valueLabel = new JLabel("Value:", right);
        valueValue = new JLabel("", left);

        raLabel = new JLabel("RA:", right);
        raValue = new JLabel("", left);
        decLabel = new JLabel("DEC:", right);
        decValue = new JLabel("", left);
        equinoxLabel = new JLabel("Equinox:", right);
        equinoxValue = new JLabel("2000", left);

        minLabel = new JLabel("Min:", right);
        minValue = new JLabel("", left);
        maxLabel = new JLabel("Max:", right);
        maxValue = new JLabel("", left);
        bitpixLabel = new JLabel("Bitpix:", right);
        bitpixValue = new JLabel("", left);

        lowLabel = new JLabel("Low:", right);
        lowValue = new JTextField(6);
        lowValue.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                setCutLevels();
            }
        });
        highLabel = new JLabel("High:", right);
        highValue = new JTextField(6);
        highValue.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                setCutLevels();
            }
        });
        autocutButton = new JButton("Auto Set Cutlevels");
        autocutButton.setToolTipText("Set the image cut levels using median filtering");
        autocutButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                autoSetCutLevels();
            }
        });

        // add a menu with zoom settings
        scaleLabel = new JLabel("Scale:", right);
        makeScaleMenu();

        // add buttons "Z" and "z" to zoom in and out by 1 factor
        makeZoomButtons();

        // keep the panel display up to date with the image
        imageProcessor.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                updateValues();
            }
        });
    }

    /**
     * Add a menu with zoom settings
     */
    protected void makeScaleMenu() {
        scaleMenuBar = new JMenuBar();
        scaleMenu = new JMenu("1x");
        // scaleMenu.setBorder(BorderFactory.createEtchedBorder());
        scaleMenuBar.add(scaleMenu);

        ButtonGroup group = new ButtonGroup();
        for (int i = minScale; i <= -2; i++) {
            String s = "1/" + -i + "x";
            addScaleMenuItem(group, s, -1.0f / i);
        }

        for (int i = 1; i <= maxScale; i++) {
            String s = Integer.toString(i) + "x";
            addScaleMenuItem(group, s, (float) i);
        }

        imageDisplay.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewScale()) {
                    String s = getScaleLabel(imageDisplay.getScale());
                    scaleMenu.setText(s);
                    int n = scaleMenu.getItemCount();
                    for (int i = 0; i < n; i++) {
                        JRadioButtonMenuItem b = (JRadioButtonMenuItem) scaleMenu.getItem(i);
                        if (b.getText().equals(s))
                            b.setSelected(true);
                    }
                }
            }
        });
    }


    /**
     * Add a radio button menu item to the scale menu and given group
     * with the given label and scale value.
     */
    protected void addScaleMenuItem(ButtonGroup group, String label, float value) {
        JRadioButtonMenuItem b = new JRadioButtonMenuItem(label);
        b.setActionCommand(Float.toString(value));
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setScale(Float.parseFloat(e.getActionCommand()));
            }
        });
        group.add(b);
        scaleMenu.add(b);
    }


    /**
     * Set the scale for the image to the given value and update the menu
     * label.
     */
    public void setScale(float value) {
        imageDisplay.setScale(value);
        imageDisplay.updateImage();
        scaleMenu.setText(getScaleLabel(value));
    }

    /**
     * Get the scale menu label for the given float scale factor.
     */
    public String getScaleLabel(float value) {
        int factor;
        if (value < 1.0)
            factor = Math.round(-1.0f / value);
        else
            factor = Math.round(value);

        if (factor < 0)
            return "1/" + -factor + "x";
        else
            return Integer.toString(factor) + "x";
    }


    /**
     * Zoom the image in or out, depending on the given argument.
     */
    public void incScale(boolean zoomIn) {
        float scale = imageDisplay.getScale();
        int factor;
        if (scale < 1.0)
            factor = Math.round(-1.0f / scale);
        else
            factor = Math.round(scale);

        // System.out.println("incScale: scale = " + scale + ", factor = " + factor);

        if (zoomIn) {
            factor++;
            if (factor == -1 || factor == 0)
                factor = 1;
        }
        else {
            factor--;
            if (factor == -1 || factor == 0)
                factor = -2;
        }
        if (factor < minScale)
            factor = minScale;
        if (factor > maxScale)
            factor = maxScale;

        if (factor < 0)
            scale = -1.0f / factor;
        else {
            scale = (float) factor;
        }
        // System.out.println("incScale II: scale = " + scale + ", factor = " + factor);
        setScale(scale);
    }


    /**
     * Add buttons "Z" and "z" to zoom in and out by 1 factor.
     */
    protected void makeZoomButtons() {
        zoomInButton = new JButton(Resources.getIcon("magnify.xbm"));
        zoomInButton.setToolTipText("Zoom in");
        zoomInButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                incScale(true);
            }
        });

        zoomOutButton = new JButton(Resources.getIcon("shrink.xbm"));
        zoomInButton.setToolTipText("Zoom out");
        zoomOutButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                incScale(false);
            }
        });
    }


    /**
     * Combine the panel items in a horizontal layout
     */
    protected void horizontalPanelLayout() {
        int east = GridBagConstraints.EAST,
                west = GridBagConstraints.WEST,
                none = GridBagConstraints.NONE,
                horizontal = GridBagConstraints.HORIZONTAL;

        int r = 0;		// row in layout

        //        component      x  y  w  h  weightx weighty  fill        anchor
        //        ---------      -  -  -  -  ------- -------  ----        ------
        layout.add(objectLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(objectValue, 1, r, 5, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(xLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(xValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        layout.add(yLabel, 2, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(yValue, 3, r, 1, 1, 1.0, 0.0, horizontal, west);
        layout.add(valueLabel, 4, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(valueValue, 5, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;

        layout.add(raLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(raValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        layout.add(decLabel, 2, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(decValue, 3, r, 1, 1, 1.0, 0.0, horizontal, west);
        layout.add(equinoxLabel, 4, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(equinoxValue, 5, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(minLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(minValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        layout.add(maxLabel, 2, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(maxValue, 3, r, 1, 1, 1.0, 0.0, horizontal, west);
        layout.add(bitpixLabel, 4, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(bitpixValue, 5, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(lowLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(lowValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        layout.add(highLabel, 2, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(highValue, 3, r, 1, 1, 1.0, 0.0, horizontal, west);
        layout.add(autocutButton, 4, r, 2, 1, 0.0, 0.0, none, west);
        r++;
        layout.add(scaleLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(scaleMenuBar, 1, r, 1, 1, 0.0, 0.0, none, west);

        // put the buttons together in a panel
        JPanel panel = new JPanel();
        panel.add(zoomInButton);
        panel.add(zoomOutButton);
        layout.add(panel, 2, r, 4, 1, 1.0, 0.0, none, west);
    }


    /**
     * Combine the panel items in a vertical layout
     */
    protected void verticalPanelLayout() {
        int east = GridBagConstraints.EAST,
                west = GridBagConstraints.WEST,
                none = GridBagConstraints.NONE,
                horizontal = GridBagConstraints.HORIZONTAL;

        int r = 0;		// row in layout

        //        component      x  y  w  h  weightx weighty  fill        anchor
        //        ---------      -  -  -  -  ------- -------  ----        ------

        layout.add(objectLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(objectValue, 1, r, 1, 1, 0.0, 0.0, horizontal, west);
        r++;
        layout.add(xLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(xValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(yLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(yValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(valueLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(valueValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;

        layout.add(raLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(raValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(decLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(decValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(equinoxLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(equinoxValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(minLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(minValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(maxLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(maxValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(bitpixLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(bitpixValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(lowLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(lowValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(highLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(highValue, 1, r, 1, 1, 1.0, 0.0, horizontal, west);
        r++;
        layout.add(autocutButton, 0, r, 2, 1, 0.0, 0.0, horizontal, west);
        r++;
        layout.add(scaleLabel, 0, r, 1, 1, 0.0, 0.0, horizontal, east);
        layout.add(scaleMenuBar, 1, r, 1, 1, 0.0, 0.0, none, west);
        r++;
        layout.add(zoomInButton, 0, r, 1, 1, 0.0, 0.0, none, west);
        layout.add(zoomOutButton, 1, r, 1, 1, 0.0, 0.0, none, west);
    }


    /**
     * fill in the label and text field values, where known
     */
    protected void updateValues() {
        PlanarImage im = imageProcessor.getRescaledSourceImage();
        if (im != null) {
            SampleModel sampleModel = im.getSampleModel();
            if (sampleModel != null) {
                int dataType = sampleModel.getDataType();
                if (dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE) {
                    // double or float image type, show full preceission
                    lowValue.setText(String.valueOf((float) imageProcessor.getLowCut()));
                    highValue.setText(String.valueOf((float) imageProcessor.getHighCut()));
                    minValue.setText(String.valueOf((float) imageProcessor.getMinValue()));
                    maxValue.setText(String.valueOf((float) imageProcessor.getMaxValue()));
                }
                else {
                    // integral image type
                    lowValue.setText(String.valueOf((int) imageProcessor.getLowCut()));
                    highValue.setText(String.valueOf((int) imageProcessor.getHighCut()));
                    minValue.setText(String.valueOf((int) imageProcessor.getMinValue()));
                    maxValue.setText(String.valueOf((int) imageProcessor.getMaxValue()));
                }

                objectValue.setText(getObjectName());
                bitpix = getBitsPerPixel();
                bitpixValue.setText(bitpix != 0 ? Integer.toString(bitpix) : "");
            }
        }
    }


    /**
     * Return the name of the source image, if known, otherwise an empty string.
     * (XXX or the file name, or URL, ... ?)
     */
    public String getObjectName() {
        FITSImage fitsImage = imageDisplay.getFitsImage();
        if (fitsImage != null) {
            Object prop = fitsImage.getProperty("OBJECT");
            if (prop instanceof String)
                return (String) prop;
        }
        return "";
    }

    /**
     * Return the number of bits per pixel in the source image.
     * Floating point images are denoted by -32 for float, or -64 for double.
     */
    public int getBitsPerPixel() {
        PlanarImage im = imageProcessor.getSourceImage();
        if (im != null) {
            SampleModel sm = im.getSampleModel();
            int dataType = sm.getDataType();
            int bitpix = sm.getSampleSize(0);
            if (dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE) {
                return -bitpix;
            }
            return bitpix;
        }
        return 0;
    }

    /**
     * Set the image cut levels according to the values in the low and high cut level
     * fields.
     */
    protected void setCutLevels() {
        double low, high;
        try {
            low = Double.parseDouble(lowValue.getText());
            high = Double.parseDouble(highValue.getText());
        }
        catch (NumberFormatException e) {
            DialogUtil.error("Please type in a number for the low or high pixel value");
            return;
        }
        imageProcessor.setCutLevels(low, high);
        imageProcessor.update();
    }

    /**
     * Automatically set the image cut levels based on the image data
     */
    protected void autoSetCutLevels() {
        imageProcessor.autoSetCutLevels(imageDisplay.getVisibleArea());
        imageProcessor.update();
    }


    /**
     * Invoked when a mouse button is pressed on the image and then
     * dragged.
     */
    public void mouseDragged(MouseEvent e) {
    }

    /*
     * Invoked when the mouse button has been moved over the image
     * (with no buttons down).
     */
    public void mouseMoved(MouseEvent e) {
        if (imageDisplay == null)
            return;

        // fill in the panel labels with the current values
        Point2D.Double p;
        try {
            // image coords
            p = new Point2D.Double(e.getX(), e.getY());
            imageDisplay.getCoordinateConverter().screenToImageCoords(p, false);
            xValue.setText(nf.format(p.getX()));
            yValue.setText(nf.format(p.getY()));

            // world coords
            if (imageDisplay.isWCS()) {
                imageDisplay.getCoordinateConverter().imageToWorldCoords(p, false);
                double equinox = imageDisplay.getWCS().getEquinox();
                WorldCoords wcs = new WorldCoords(p.getX(), p.getY(), equinox);
                String[] ar = wcs.format(equinox);
                raValue.setText(ar[0]);
                decValue.setText(ar[1]);
            }
            else {
                raValue.setText("");
                decValue.setText("");
                equinoxValue.setText("");
            }

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // try to extract the pixel value under the mouse and display it
            p = new Point2D.Double(e.getX(), e.getY());
            imageDisplay.getCoordinateConverter().screenToUserCoords(p, false);
            double pixel = imageDisplay.getPixelValue(p, 0);//PWD: made double

            PlanarImage im = imageProcessor.getRescaledSourceImage();
            if (im != null) {
                SampleModel sampleModel = im.getSampleModel();
                if (sampleModel != null) {
                    int dataType = sampleModel.getDataType();
                    if (pixel == imageProcessor.getBlank()) {
                        valueValue.setText("blank");
                    }
                    else {
                        valueValue.setText(String.valueOf(pixel));
                    }
                }
            }
        }
        catch (Exception ex) {
            //ex.printStackTrace();
        }
    }
}
