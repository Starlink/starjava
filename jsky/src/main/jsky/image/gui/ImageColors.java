/*
 * ESO Archive
 *
 * $Id: ImageColors.java,v 1.12 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/12/06  Created
 *
 * Frank Tanner    2001/12/19  Changed constructor to be passed a
 *	 		       BasicImageReadableProcessor interface
 *			       instead of a full blown BasicImageDisplay.
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jsky.image.BasicImageReadableProcessor;
import jsky.image.ImageChangeEvent;
import jsky.image.ImageColorITTs;
import jsky.image.ImageColorLUTs;
import jsky.image.ImageColormap;
import jsky.image.ImageLookup;
import jsky.image.ImageProcessor;
import jsky.util.I18N;


/**
 * Dialog to select image colormaps and color scaling algorithms.
 *
 * @version $Revision: 1.12 $
 * @author Allan Brighton
 */
public class ImageColors extends JPanel {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageColors.class);

    // The top level parent frame (or internal frame) used to close the window
    protected Component parent;

    /** main image display window */
    protected BasicImageReadableProcessor imageDisplay;

    /** Object managing image processing operations (including setting the colormap) */
    protected ImageProcessor imageProcessor;

    /** Array of predefined colormap names */
    String[] colormaps = ImageColorLUTs.getLUTNames();

    /** List displaying colormap names */
    JList colormapList = new JList(colormaps);

    /** Array of predefined intensity transfer table names */
    String[] itts = ImageColorITTs.getITTNames();

    /** List displaying itt names */
    JList intensityList = new JList(itts);

    /** Linear Scale button */
    protected JRadioButton linearScale = new JRadioButton(_I18N.getString("linearScale"));

    /** Logarithmic Scale button */
    JRadioButton logScale = new JRadioButton(_I18N.getString("logarithmic"));

    /** Square Root Scale button */
    JRadioButton sqrtScale = new JRadioButton(_I18N.getString("squareRoot"));

    /** Histogram Equalization button */
    JRadioButton histeqScale = new JRadioButton(_I18N.getString("histogram"));

    /** True if GUI events should be ignored */
    protected boolean ignoreEvents = false;

    /**
     * Constructor
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param imageDisplay The image display window
     */
    public ImageColors(Component parent, BasicImageReadableProcessor imageDisplay) {
        this.parent = parent;
        this.imageDisplay = imageDisplay;
        imageProcessor = imageDisplay.getImageProcessor();

        setLayout(new BorderLayout());
        add(makeMainPanel(), BorderLayout.CENTER);
        add(makeButtonPanel(), BorderLayout.SOUTH);

        imageProcessor.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewColormap())
                    updateDisplay();
            }
        });

        // initialize the display
        updateDisplay();
    }


    /** Make and return the main panel */
    protected JPanel makeMainPanel() {
        JPanel panel = new JPanel();
        panel.add(makeColorScalePanel());
        panel.add(makeColormapListPanel());
        panel.add(makeIntensityListPanel());
        return panel;
    }


    /** Make and return the color scale panel */
    protected JPanel makeColorScalePanel() {
        JPanel panel = new JPanel();
        Border border = BorderFactory.createEtchedBorder();
        panel.setBorder(BorderFactory.createTitledBorder(border,
                _I18N.getString("colorScaleAlgorithm"),
                TitledBorder.TOP,
                TitledBorder.CENTER));

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(linearScale);
        panel.add(logScale);
        panel.add(sqrtScale);
        panel.add(histeqScale);
        //histeqScale.setEnabled(false);

        // Add the radio buttons to the button group
        ButtonGroup grp = new ButtonGroup();
        grp.add(linearScale);
        grp.add(logScale);
        grp.add(sqrtScale);
        grp.add(histeqScale);

        // default selection
        linearScale.setSelected(true);

        ActionListener l = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!ignoreEvents)
                    setScaleAlgorithm(e.getActionCommand());
            }
        };

        linearScale.addActionListener(l);
        logScale.addActionListener(l);
        sqrtScale.addActionListener(l);
        histeqScale.addActionListener(l);

        return panel;
    }


    /**
     * Called when a radio button is selected with the name of the image
     * color scaling algorithm.
     */
    protected void setScaleAlgorithm(String name) {
        if (name.equals(_I18N.getString("linearScale"))) {
            imageProcessor.setScaleAlgorithm(ImageLookup.LINEAR_SCALE);
        }
        else if (name.equals(_I18N.getString("logarithmic"))) {
            imageProcessor.setScaleAlgorithm(ImageLookup.LOG_SCALE);
        }
        else if (name.equals(_I18N.getString("squareRoot"))) {
            imageProcessor.setScaleAlgorithm(ImageLookup.SQRT_SCALE);
        }
        else if (name.equals(_I18N.getString("histogram"))) {
            imageProcessor.setScaleAlgorithm(ImageLookup.HIST_EQ);
        }
        imageProcessor.update();
    }


    /** Make and return the list box with the list of colormaps  */
    protected JPanel makeColormapListPanel() {
        JPanel panel = new JPanel();
        Border border = BorderFactory.createEtchedBorder();
        panel.setBorder(BorderFactory.createTitledBorder(border,
                _I18N.getString("colormap"),
                TitledBorder.TOP,
                TitledBorder.CENTER));

        JScrollPane scrollPane = new JScrollPane(colormapList);
        panel.add(scrollPane);

        // Register to receive selection events
        colormapList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent evt) {
                if (!ignoreEvents) {
                    JList src = (JList) evt.getSource();
                    if (evt.getValueIsAdjusting() == false) {
                        Object[] selectedValues = src.getSelectedValues();
                        if (selectedValues.length >= 1) {
                            setColormap((String) (selectedValues[0]));
                        }
                    }
                }
            }
        });

        return panel;
    }


    /**
     * Called when a colormap is selected from the list
     */
    protected void setColormap(String name) {
        imageProcessor.setColorLookupTable(name);
        imageProcessor.update();
    }


    /** Make and return the list box with the list of intensity tables  */
    protected JPanel makeIntensityListPanel() {
        JPanel panel = new JPanel();
        Border border = BorderFactory.createEtchedBorder();
        panel.setBorder(BorderFactory.createTitledBorder(border,
                _I18N.getString("intensity"),
                TitledBorder.TOP,
                TitledBorder.CENTER));
        JScrollPane scrollPane = new JScrollPane(intensityList);
        panel.add(scrollPane);

        // Register to receive selection events
        intensityList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent evt) {
                if (!ignoreEvents) {
                    JList src = (JList) evt.getSource();
                    if (evt.getValueIsAdjusting() == false) {
                        Object[] selectedValues = src.getSelectedValues();
                        if (selectedValues.length >= 1) {
                            setIntensityLookupTable((String) (selectedValues[0]));
                        }
                    }
                }
            }
        });

        return panel;
    }


    /**
     * Called when an intensity lookup table is selected from the list
     */
    protected void setIntensityLookupTable(String name) {
        imageProcessor.setIntensityLookupTable(name);
        imageProcessor.update();
    }


    /**
     * Make the dialog button panel
     */
    protected JPanel makeButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton resetButton = new JButton(_I18N.getString("reset"));
        resetButton.setToolTipText(_I18N.getString("resetTip"));
        panel.add(resetButton);
        resetButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                reset();
            }
        });

        JButton closeButton = new JButton(_I18N.getString("close"));
        closeButton.setToolTipText(_I18N.getString("closeTip"));
        panel.add(closeButton);
        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                close();
            }
        });

        return panel;
    }


    /**
     * Reset the colormap to the default.
     */
    protected void reset() {
        imageProcessor.setDefaultColormap();
        imageProcessor.update();

        // update the user interface to show the default settings
        linearScale.setSelected(true);

        colormapList.setSelectedValue(ImageColormap.DEFAULT_COLOR_LUT, true);
        intensityList.setSelectedValue("Ramp", true);
    }

    /**
     * Update the display to show the current cut levels and pixel distribution
     */
    void updateDisplay() {
        ignoreEvents = true;
        try {
            int scaleAlg = imageProcessor.getScaleAlgorithm();
            switch (scaleAlg) {
            case ImageLookup.LINEAR_SCALE:
                linearScale.setSelected(true);
                break;
            case ImageLookup.SQRT_SCALE:
                sqrtScale.setSelected(true);
                break;
            case ImageLookup.LOG_SCALE:
                logScale.setSelected(true);
                break;
            case ImageLookup.HIST_EQ:
                histeqScale.setSelected(true);
                break;
            }

            colormapList.setSelectedValue(imageProcessor.getColorLookupTableName(), true);
            intensityList.setSelectedValue(imageProcessor.getIntensityLookupTableName(), true);

        }
        finally {
            ignoreEvents = false;
        }
    }


    /**
     * Close the window
     */
    protected void close() {
        if (parent != null)
            parent.setVisible(false);
    }
}


