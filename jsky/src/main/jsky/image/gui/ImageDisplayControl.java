/*
 * ESO Archive
 *
 * $Id: ImageDisplayControl.java,v 1.11 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;

import jsky.util.I18N;


/**
 * Combines an ImageDisplay with a control panel, zoom, and pan windows.
 *
 * @version $Revision: 1.11 $
 * @author Allan Brighton
 */
public class ImageDisplayControl extends JPanel {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageDisplayControl.class);

    /** The top level parent frame (or internal frame) used to close the window */
    protected Component parent;

    /** Pan window */
    protected ImagePanner imagePanner;

    /** Panel containing the pan and zoom windows */
    protected JPanel panZoomPanel;

    //protected ImageDisplayPanel imageDisplayPanel;

    /** Zoom window */
    protected ImageZoom imageZoom;

    /** Main image display */
    protected DivaMainImageDisplay imageDisplay;

    /** Color bar */
    protected ImageColorbar colorbar;

    /** Panel displaying information about the current mouse position */
    protected ImageDisplayStatusPanel imageDisplayStatusPanel;

    /** Optional filename to load image from. */
    protected String filename = "";	// name of image file, if known

    /** Used to toggle the visibility of the pan/zoom windows */
    protected BasicArrowButton panZoomToggleButton;


    /**
     * Construct an ImageDisplayControl widget.
     *
     * @param parent the top level parent frame (or internal frame) used to close the window
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public ImageDisplayControl(Component parent, int size) {
        super();
        this.parent = parent;

        imageDisplay = makeImageDisplay();
        imagePanner = makePanWindow(size);
        //imageDisplayPanel = makeImagePanel();
        imageZoom = makeZoomWindow(size);
        panZoomToggleButton = makePanZoomToggleButton();

        // This is just the panel to hold the pan and zoom windows and
        // the toggle button. We need to redefine paintComponent to avoid
        // having the toggle button disappear when the pan image is painted.
        panZoomPanel = new JPanel() {

            public synchronized void paintComponent(Graphics g) {
                super.paintComponent(g);
                panZoomToggleButton.repaint();
            }
        };

        colorbar = makeColorbar();
        imageDisplayStatusPanel = makeStatusPanel();
        imageDisplayStatusPanel.setImageDisplay(imageDisplay);

        makeLayout(size);
    }

    /**
     * Make an ImageDisplayControl widget with the default settings
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     *
     */
    public ImageDisplayControl(Component parent) {
        this(parent, ImagePanner.DEFAULT_SIZE);
    }


    /**
     * Make an ImageDisplayControl widget with the default settings and display the contents
     * of the image file pointed to by the URL.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param url The URL for the image to load
     */
    public ImageDisplayControl(Component parent, URL url) {
        this(parent);
        imageDisplay.setURL(url);
    }


    /**
     * Make an ImageDisplayControl widget with the default settings and display the contents
     * of the image file.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param filename The image file to load
     */
    public ImageDisplayControl(Component parent, String filename) {
        this(parent);
        imageDisplay.setFilename(filename);
    }

    /** Make and return the image display window */
    protected DivaMainImageDisplay makeImageDisplay() {
        return new DivaMainImageDisplay(parent);
    }

    /**
     * Make and return the pan window.
     *
     * @param size the size (width, height) to use for the pan window.
     */
    protected ImagePanner makePanWindow(int size) {
        return new ImagePanner(imageDisplay, size, size);
    }

    /**
     * Make and return the image display panel
     */
    //protected ImageDisplayPanel makeImagePanel() {
    //return new ImageDisplayPanel(imageDisplay, SwingConstants.VERTICAL);
    //}

    /**
     * Make and return the zoom window.
     *
     * @param size the size (width, height) to use for the zoom window.
     */
    protected ImageZoom makeZoomWindow(int size) {
        return new ImageZoom(imageDisplay, size, size, 4.0F);
    }


    /**
     * Make and return a button for showing and hiding the pan/zoom panel
     */
    protected BasicArrowButton makePanZoomToggleButton() {
        final BasicArrowButton b = new BasicArrowButton(SwingConstants.NORTH);
        b.setToolTipText(_I18N.getString("panZoomToggleTip"));
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (panZoomPanel.isVisible()) {
                    panZoomPanel.setVisible(false);
                    imageZoom.setActive(false);
                    b.setDirection(SwingConstants.SOUTH);
                }
                else {
                    panZoomPanel.setVisible(true);
                    imageZoom.setActive(true);
                    b.setDirection(SwingConstants.NORTH);
                }
            }
        });

        return b;
    }

    /**
     * Make and return the colorbar window.
     */
    protected ImageColorbar makeColorbar() {
        return new ImageColorbar(imageDisplay);
    }

    /** Make and return the status panel */
    protected ImageDisplayStatusPanel makeStatusPanel() {
        return new ImageDisplayStatusPanel();
    }

    /**
     * This method is resposible for the window layout for this widget.
     *
     * @param size the initial size (width, height) to use for the pan and zoom windows.
     */
    protected void makeLayout(int size) {
        setLayout(new BorderLayout());
        colorbar.setBorder(BorderFactory.createEtchedBorder());
        colorbar.setPreferredSize(new Dimension(0, 20));
        JPanel imagePanel = new JPanel();

        // The layout is a bit tricky here, since we want to have the pan and zoom windows
        // appear inside the image frame, overlapping the image, to save space.
        GridBagLayout layout = new GridBagLayout();
        imagePanel.setLayout(layout);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = gbc.gridheight = 1;
        gbc.weightx = gbc.weighty = 1;
        layout.setConstraints(imageDisplay, gbc);

        // Put the pan and zoom windows in a separate panel
        panZoomPanel.setLayout(new BoxLayout(panZoomPanel, BoxLayout.Y_AXIS));
        imagePanner.setBorder(new LineBorder(getBackground(), 1));
        panZoomPanel.add(imagePanner);
        imageZoom.setBorder(new LineBorder(getBackground(), 1));
        panZoomPanel.add(imageZoom);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.gridwidth = gbc.gridheight = 1;
        gbc.weightx = gbc.weighty = 0;
        layout.setConstraints(panZoomPanel, gbc);
        layout.setConstraints(panZoomToggleButton, gbc);

        // Note that the order is important below, so that the panZoomPanel is on top
        imagePanel.add(panZoomToggleButton);
        imagePanel.add(panZoomPanel);
        imagePanel.add(imageDisplay);

        add(imagePanel, BorderLayout.CENTER);

        JPanel bot = new JPanel();
        bot.setLayout(new BorderLayout());
        bot.add(colorbar, BorderLayout.NORTH);
        bot.add(imageDisplayStatusPanel, BorderLayout.SOUTH);
        add(bot, BorderLayout.SOUTH);
    }


    /** Return the main image display widget */
    public DivaMainImageDisplay getImageDisplay() {
        return imageDisplay;
    }

    /** Return the pan window */
    public ImagePanner getImagePanner() {
        return imagePanner;
    }

    /** Return the zoom window */
    public ImageZoom getImageZoom() {
        return imageZoom;
    }

    /** Return the status panel window */
    public ImageDisplayStatusPanel getImageDisplayStatusPanel() {
        return imageDisplayStatusPanel;
    }
}


