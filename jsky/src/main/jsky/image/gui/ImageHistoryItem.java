/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageHistoryItem.java,v 1.6 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jsky.image.ImageChangeEvent;
import jsky.image.ImageProcessor;
import jsky.image.gui.MainImageDisplay;
import jsky.util.gui.DialogUtil;

/**
 * Used to store information about previously viewed images.
 * For downloaded files, the filename is the name of a temp file that can
 * be accessed only in this session. If the application is restarted, the
 * URL will have to be used instead.
 */
public class ImageHistoryItem extends AbstractAction
        implements ChangeListener, Serializable {

    /** The title for this item */
    protected String title;

    /** The RA coordinate of the image center (or Double.NaN if not known) */
    protected double ra;

    /** The Dec coordinate of the image center (or Double.NaN if not known) */
    protected double dec;

    /** The origial image URL */
    protected URL url;

    /** Filename (may be a temp download file) */
    protected String filename;

    /** Colormap used */
    protected String cmap;

    /** Intensity table used */
    protected String itt;

    /** Low cut */
    protected double hcut;

    /** High cut */
    protected double lcut;

    /** True if user set the cut levels */
    protected boolean userSetCutLevels;

    /** Name of the image lookup scale algorithm */
    protected int scaleAlg;

    /** magnification factor */
    protected float scale;

    /**
     * Create an image history item based on the given arguments.
     *
     * @param imageDisplay the image display widget
     * @param ra the image center RA coordinate
     * @param dec the image center Dec coordinate
     * @param title the title for the history menu
     * @param url the URL for the original image
     * @param filename the local filename, if downloaded
     */
    public ImageHistoryItem(MainImageDisplay imageDisplay, double ra, double dec,
                            String title, URL url, String filename) {
        super(title);
        this.ra = ra;
        this.dec = dec;
        this.title = title;
        this.filename = new File(filename).getAbsolutePath();
        this.url = url;
        this.scale = imageDisplay.getScale();

        ImageProcessor imageProcessor = imageDisplay.getImageProcessor();
        this.cmap = imageProcessor.getColorLookupTableName();
        this.itt = imageProcessor.getIntensityLookupTableName();
        this.hcut = imageProcessor.getHighCut();
        this.lcut = imageProcessor.getLowCut();
        this.userSetCutLevels = imageProcessor.isUserSetCutLevels();
        this.scaleAlg = imageProcessor.getScaleAlgorithm();
    }


    /** Return true if this entry matches the given coordinates */
    public boolean match(double ra, double dec) {
        if (Double.isNaN(this.ra) || Double.isNaN(this.dec))
            return false;

        double range = 120.0 / 3600.0;  // valid range in degrees
        double diff = Math.abs(ra - this.ra);
        if (diff > range) {
            // Make sure that if both RAs are around 0, they aren't close enough.
            if (ra > (360.0 - range)) {
                ra = ra - 360;
            }
            else if (ra < range) {
                ra = ra + 360;
            }
            else {
                return false;
            }

            diff = Math.abs(ra - this.ra);
            if (diff > range) {
                return false;
            }
        }

        diff = Math.abs(dec - this.dec);
        if (diff > range) {
            return false;
        }

        return true;
    }

    /**
     * Load the file if it exists, otherwise the URL, and arrange to restore the history
     * settings once the image is loaded.
     */
    public void actionPerformed(ActionEvent evt) {
        MainImageDisplay imageDisplay = ImageDisplayMenuBar.getCurrentImageDisplay();
        if (filename != null && new File(filename).exists()) {
            imageDisplay.addChangeListener(this);
            imageDisplay.setFilename(filename, url);
        }
        else if (url != null) {
            imageDisplay.addChangeListener(this);
            imageDisplay.setURL(url);
        }
        else {
            System.out.println("XXX ImageHistoryItem.actionPerformed: no file and no URL");
        }
    }

    /** Called when the image is actually loaded, so we can restore the settings */
    public void stateChanged(ChangeEvent ce) {
        ImageChangeEvent e = (ImageChangeEvent) ce;
        if (e.isNewImage() && !e.isBefore()) {
            DivaMainImageDisplay imageDisplay = (DivaMainImageDisplay) e.getSource();
            ImageProcessor imageProcessor = imageDisplay.getImageProcessor();
            imageDisplay.removeChangeListener(this);

            // restore image processor settings
            imageProcessor.setColorLookupTable(cmap);
            imageProcessor.setIntensityLookupTable(itt);
            imageProcessor.setScaleAlgorithm(scaleAlg);
            imageProcessor.setCutLevels(lcut, hcut, userSetCutLevels);
            imageDisplay.setScale(scale);

            imageProcessor.update();
        }
    }

    // PWD: added this
    /** Get access to the scale factor */
    public float getScale()
    {
        return scale;
    }
}


