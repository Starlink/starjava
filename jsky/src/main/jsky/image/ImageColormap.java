/*
 * ESO Archive
 *
 * $Id: ImageColormap.java,v 1.5 2002/08/04 19:50:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/12/09  Created
 */


package jsky.image;

import javax.media.jai.*;


/**
 * Manages the colormap for an image and provides methods to select a
 * predefined colormap and perform operations on it, such as rotate, shift, and
 * stretch.
 *
 * @version $Revision: 1.5 $
 * @author Allan Brighton
 */
public class ImageColormap implements Cloneable {

    /** Used to make a color image froma grayscale image using a selected colormap */
    protected LookupTableJAI colorLookupTable;

    /** Name of the current color lookup table */
    protected String colorLookupTableName = "Ramp";

    /** Copy of current color lookup table data to use for shift, rotate, scale ops */
    protected byte[][] colorLut;

    /** Used to modify the order of the colors in the color lookup table */
    protected float[] intensityLookupTable;

    /** Name of the current intensity lookup table */
    protected String intensityLookupTableName = "Ramp";

    /** Default color lookup table to use */
    public static final String DEFAULT_COLOR_LUT = "Real";

    /** The number of colors in the display image */
    protected static final int NUM_COLORS = 256;


    /**
     * Default constructor: Initialize with the default colormap.
     */
    public ImageColormap() {
        setColorLookupTable(DEFAULT_COLOR_LUT);
    }


    /**
     * Create a color RGB lookup table that can be added to the image processing chain,
     * so that we can manipulate the image colors.
     *
     * @param name the name of the colormap table to use. This is currently
     * One of: 	"Background", "Blue", "Heat", "Isophot", "Light", "Pastel",
     * "Ramp", "Real", "Smooth", "Staircase", "Standard".
     * User defined maps will be implemented in a later release.
     */
    public void setColorLookupTable(String name) {
        colorLookupTableName = name;
        int maxLut = NUM_COLORS - 1;
        byte[][] blut = new byte[3][NUM_COLORS];
        float[][] flut = ImageColorLUTs.getLUT(name);

        if (intensityLookupTable == null || intensityLookupTableName.equals("Ramp")) {
            // don't use intensity table
            for (int i = 0; i < NUM_COLORS; i++) {
                blut[0][i] = (byte) (flut[i][0] * maxLut);
                blut[1][i] = (byte) (flut[i][1] * maxLut);
                blut[2][i] = (byte) (flut[i][2] * maxLut);
            }
        }
        else {
            // use intensity table
            for (int i = 0; i < NUM_COLORS; i++) {
                int index = (int) ((intensityLookupTable[i] * maxLut) + 0.5);
                blut[0][i] = (byte) (flut[index][0] * maxLut);
                blut[1][i] = (byte) (flut[index][1] * maxLut);
                blut[2][i] = (byte) (flut[index][2] * maxLut);
            }
        }

        // save a copy for manipulation
        colorLut = (byte[][]) blut.clone();
        colorLookupTable = new LookupTableJAI(blut, 0);
    }


    /**
     * Create an intensity lookup table that can be added to the image processing chain
     * to rearrange the order of the colors in the colormap.
     *
     * @param name the name of the intensity lookup table to use. This is currently
     * One of: 	"Equal", "Exponential",	"Gamma", "Jigsaw", "Lasritt", "Logarithmic",
     * "Negative", "Negative Log", "Ramp", "Staircase".
     *
     * User defined intensity lookup tables will be implemented in a later release.
     */
    public void setIntensityLookupTable(String name) {
        intensityLookupTableName = name;
        intensityLookupTable = ImageColorITTs.getITT(name);
        setColorLookupTable(colorLookupTableName);
    }


    /**
     * Save the current colormap state for the next shift, rotate or scale operation.
     */
    public void saveColormap() {
        colorLut = (byte[][]) colorLookupTable.getByteData().clone();
    }


    /**
     * Rotate the colormap by the given amount.
     */
    public void rotateColormap(int amount) {
        byte[][] newLut = new byte[3][NUM_COLORS];
        for (int i = 0; i < NUM_COLORS; i++) {
            int index = (i - amount) % NUM_COLORS;
            if (index < 0)
                index += NUM_COLORS;
            newLut[0][i] = colorLut[0][index];
            newLut[1][i] = colorLut[1][index];
            newLut[2][i] = colorLut[2][index];
        }
        colorLut = (byte[][]) newLut.clone();
        colorLookupTable = new LookupTableJAI(newLut, 0);
    }


    /**
     * Shift the colormap by the given amount.
     */
    public void shiftColormap(int amount) {
        byte[][] newLut = new byte[3][NUM_COLORS];

        for (int i = 0; i < NUM_COLORS; i++) {
            int index = (i - amount);
            if (index < 0)
                index = 0;
            else if (index >= NUM_COLORS)
                index = NUM_COLORS - 1;
            newLut[0][i] = colorLut[0][index];
            newLut[1][i] = colorLut[1][index];
            newLut[2][i] = colorLut[2][index];
        }
        colorLookupTable = new LookupTableJAI(newLut, 0);
    }


    /**
     * Scale the colormap by the given amount.
     */
    public void scaleColormap(int amount) {
        byte[][] newLut = new byte[3][NUM_COLORS];

        int n = NUM_COLORS - 1;
        int index = 0, value = 0;
        int start = Math.min(amount, NUM_COLORS / 2);
        int end = NUM_COLORS - start;
        if (end <= start)
            end = start + 1;
        int dist = end - start + 1;

        for (int i = 0; i < NUM_COLORS; i++) {
            if (i >= start && i <= end) {
                index = ((i - start) * n) / dist;
                if (index < 0)
                    index = 0;
                else if (index > n)
                    index = n;
            }
            else if (i < start) {
                index = 0;
            }
            else {
                index = n;
            }
            if (intensityLookupTable == null)
                index = (byte) ((index / 256.) * n) & 0xff;
            else
                index = (byte) (intensityLookupTable[index] * n) & 0xff;

            newLut[0][i] = colorLut[0][index];
            newLut[1][i] = colorLut[1][index];
            newLut[2][i] = colorLut[2][index];
        }
        colorLookupTable = new LookupTableJAI(newLut, 0);
    }


    /**
     * Reset the colormap to the default.
     */
    public void setDefaultColormap() {
        intensityLookupTableName = "Ramp";
        setColorLookupTable(DEFAULT_COLOR_LUT);
    }


    /**
     * Reset the colormap shift, rotate and scale settings to 0.
     */
    public void resetColormap() {
        setColorLookupTable(colorLookupTableName);
    }


    /** Return the current lookup table used to add color to a grayscale image. */
    public LookupTableJAI getColorLookupTable() {
        return colorLookupTable;
    }


    /** Return the name of the current color lookup table */
    public String getColorLookupTableName() {
        return colorLookupTableName;
    }

    /** Return the name of the current intensity lookup table */
    public String getIntensityLookupTableName() {
        return intensityLookupTableName;
    }

    /**
     * Return true if this object is equivalent to the given one.
     */
    public boolean equals(ImageColormap colormap) {
        return (colorLookupTable == colormap.colorLookupTable
                && colorLookupTableName == colormap.colorLookupTableName
                && colorLut == colormap.colorLut
                && intensityLookupTable == colormap.intensityLookupTable
                && intensityLookupTableName == colormap.intensityLookupTableName);
    }


    /** Return a shallow copy */
    public Object clone() {
	try {
	    return super.clone();
	} 
	catch (CloneNotSupportedException ex) {
	    throw new InternalError(); // won't happen
	}
    }
}
