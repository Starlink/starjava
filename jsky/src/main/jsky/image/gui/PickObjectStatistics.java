/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: PickObjectStatistics.java,v 1.6 2002/08/16 22:21:13 brighton Exp $
 */

package jsky.image.gui;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.FileOutputStream;
import java.util.Vector;

import javax.media.jai.PlanarImage;

import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.image.fits.codec.FITSImage;
import jsky.image.gui.ImageZoom;
import jsky.science.IQE;

import java.io.DataOutputStream;


/**
 * Gathers statistics about a given area of the image using a centroid algorithm.
 *
 * @version $Revision: 1.6 $
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public class PickObjectStatistics {

    public static final int ID = 0;
    public static final int RA = 1;
    public static final int DEC = 2;
    public static final int IMAGE_X = 3;
    public static final int IMAGE_Y = 4;
    public static final int FWHM_X = 5;
    public static final int FWHM_Y = 6;
    public static final int ANGLE = 7;
    public static final int PEAK = 8;
    public static final int BACKGROUND = 9;

    /** The names of the fields returned by the getRow() or getFields() methods. */
    public static final String[] FIELD_NAMES = {
        "Id", "RA", "Dec", "Image_X", "Image_Y", "FWHM_X", "FWHM_Y", "Angle", "Peak", "Background"
    };

    /** The number of columns or fields returned by the getRow() or getFields() methods. */
    public static final int NUM_FIELDS = FIELD_NAMES.length;


    /** The target image display */
    private MainImageDisplay _imageDisplay;

    /** The calculated centroid position */
    private WorldCoords _centerPos;

    /** The calculated centroid position in image coordinates */
    private double _imageX;

    /** The calculated centroid position in image coordinates */
    private double _imageY;

    /** Object holding the image statistics */
    private IQE _iqe;


    /**
     * Constructor
     *
     * @param imageDisplay used to access the image data and world coordinates
     */
    PickObjectStatistics(MainImageDisplay imageDisplay) {
        _imageDisplay = imageDisplay;
    }

    /**
     * Constructor: Find the center of the object at the given x,y user coordinates
     * using a centroid algorithm and gather statistics on the image object.
     *
     * @param imageDisplay used to access the image data and world coordinates
     * @param x the X user coordinate origin of the region of the image to examine
     * @param y the Y user coordinate origin of the region of the image to examine
     * @param w the width in user coordinates of the area of the image to examine
     * @param h the height in user coordinates of the area of the image to examine
     */
    PickObjectStatistics(MainImageDisplay imageDisplay, int x, int y, int w, int h) {
        this(imageDisplay);
        calculateStatistics(x, y, w, h);
    }

    /** The calculated centroid position */
    public WorldCoords getCenterPos() {
        return _centerPos;
    }

    /** The center X position in image coordinates */
    public double getImageX() {
        return _imageX;
    }

    /** The center Y position in image coordinates */
    public double getImageY() {
        return _imageY;
    }

    /** FWHM in X */
    public double getFwhmX() {
        return _iqe.getFwhmX();
    }

    /** FWHM in Y */
    public double getFwhmY() {
        return _iqe.getFwhmY();
    }

    /** angle of major axis, degrees, along X */
    public double getAngle() {
        return _iqe.getAngle();
    }

    /** peak value of object above background */
    public double getObjectPeak() {
        return _iqe.getObjectPeak();
    }

    /** mean background level */
    public double getMeanBackground() {
        return _iqe.getMeanBackground();
    }

    /** Return the status of the last pick operation */
    public boolean getStatus() {
        return _iqe != null && _iqe.getStatus();
    }


    /**
     * Find the center of the object at the given x,y user coordinates
     * using a centroid algorithm and gather statistics on the image object.
     *
     * @param x the X user coordinate origin of the region of the image to examine
     * @param y the Y user coordinate origin of the region of the image to examine
     * @param w the width in user coordinates of the area of the image to examine
     * @param h the height in user coordinates of the area of the image to examine
     */
    public void calculateStatistics(int x, int y, int w, int h) {
        // get the image data as an array of floats
        Rectangle r = new Rectangle(x, y, w, h);
        double[] temp = _imageDisplay.getPixelValues(r, 0);

        // PWD: Need float values for IQE function, so fudge
        // this. Cannot use System.arraycopy as types differ?
        float[] ar = new float[temp.length];
        try {
            for ( int i = 0; i < temp.length; i++ ) {
                ar[i] = (float) temp[i];
            }
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }
        if (ar == null) {
            //System.out.println("XXX _imageDisplay.getPixelValues() returned null for " + r);
            return;
        }
        else {
            /*XXX
	    // --------- XXX start test ----------------
	    // display the image section being used
	    float[][] imData = new float[h][w];
	    int n = 0;
	    for(int j = h-1; j >= 0; j--) {  // FITS order
		for(int i = 0; i < w; i++) {
		    imData[j][i] = ar[n++];
		}
	    }
	    try {
		new FITSImage(imData).getFits().write(new DataOutputStream(new FileOutputStream("x.fits")));
		ImageDisplayControlFrame f = new ImageDisplayControlFrame("x.fits");
	    }
	    catch(Exception e) {
		e.printStackTrace();
	    }
	    // --------- XXX end test ----------------
	    XXX*/
        }

        // examine the image data
        _iqe = new IQE(ar, w, h);

        Point2D.Double center;
        if (_iqe.getStatus()) {
            // get the user coordinates from the IQE offsets
            // Note that we have to do some conversions to get back to user coordinates
            center = new Point2D.Double(x + _iqe.getMeanX(), y + h - 1 - _iqe.getMeanY()); // FITS order
            if (_imageDisplay.getScale() > 1.0f) {
                // FITS coords start at (1, 1) at mag 1, otherwise (.5, .5)
                center.x += 0.5;
                center.y += 0.5;
            }
        }
        else {
            // if we could not get the FWHM (because the user clicked on the background
            // of the image), we still want to return the X,Y values of the area clicked.
            center = new Point2D.Double(x + w / 2.0, y + h / 2.0);
        }

        // get the center position in image coordinates
        CoordinateConverter cc = _imageDisplay.getCoordinateConverter();
        cc.userToImageCoords(center, false);
        _imageX = center.x;
        _imageY = center.y;

        // get the center position in world coordinates
        cc.imageToWorldCoords(center, false);
        _centerPos = new WorldCoords(center, cc.getEquinox());
    }


    /**
     * Return an array of objects describing the columns returned by getRow().
     * This can be used to create a catalog table to add the rows to.
     */
    public static FieldDesc[] getFields() {
        FieldDescAdapter[] fields = new FieldDescAdapter[NUM_FIELDS];

        for (int i = 0; i < NUM_FIELDS; i++)
            fields[i] = new FieldDescAdapter(FIELD_NAMES[i]);

        fields[ID].setIsId(true);
        fields[RA].setIsRA(true);
        fields[DEC].setIsDec(true);

        return fields;
    }


    /**
     * Return a vector containing the information in this object that
     * can be used to add a row to a catalog table.
     * The contents of the vector (column headings, etc.) are described
     * by the result of the getFields() method.
     */
    public Vector getRow() {
        if (_centerPos == null || _iqe == null)
            return null;

        Vector row = new Vector(NUM_FIELDS);
        for (int i = 0; i < NUM_FIELDS; i++) {
            switch (i) {
            case ID:
                row.add("P" + System.currentTimeMillis());
                break;
            case RA:
                row.add(_centerPos.getRA().toString());
                break;
            case DEC:
                row.add(_centerPos.getDec().toString());
                break;
            case IMAGE_X:
                row.add(new Double(_imageX));
                break;
            case IMAGE_Y:
                row.add(new Double(_imageY));
                break;
            case FWHM_X:
                double d1 = _iqe.getFwhmX();
                if (d1 != 0)
                    row.add(new Double(d1));
                else
                    row.add(null);
                break;
            case FWHM_Y:
                double d2 = _iqe.getFwhmY();
                if (d2 != 0)
                    row.add(new Double(d2));
                else
                    row.add(null);
                break;
            case ANGLE:
                row.add(new Double(_iqe.getAngle()));
                break;
            case PEAK:
                row.add(new Double(_iqe.getObjectPeak()));
                break;
            case BACKGROUND:
                row.add(new Double(_iqe.getMeanBackground()));
                break;
            }
        }
        return row;
    }
}

