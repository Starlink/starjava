//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    06/30/2000          S.Grosvenor
//      Initial packaging of class
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//=== End File Prolog=======================================================

package jsky.science;

import jsky.science.Wavelength;
import jsky.science.Quantity;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.Vector;

import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.IOException;

import nom.tam.fits.*;

/**
 * Implements a Wavelength1Dmodel as a formula value=f(wavelength)
 * with a minimum and maximum wavelength, and a number of expected points.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version 06.30.00
 * @author 	Sandy Grosvenor
 *
 **/
public abstract class Wavelength1DFormula extends AbstractWavelength1D {

    protected Wavelength fMinWavelength;
    protected Wavelength fMaxWavelength;
    protected int fNumPoints;

    /**
     * The Stream Unique Identifier for this class.
     **/
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new Wavelength1DFormula, with
     * default number of points (100 points), and
     * default wavelength range (100 to 1100 nanometers).
     **/
    public Wavelength1DFormula() {
        this(null, new Wavelength(100, Wavelength.NANOMETER),
                new Wavelength(1100, Wavelength.NANOMETER),
                100);
    }

    /**
     * Creates a new Wavelength1DFormula of specified number of points,
     * no name, and default wavelength
     * range
     **/
    public Wavelength1DFormula(int inPoints) {
        this(null, new Wavelength(100, Wavelength.NANOMETER),
                new Wavelength(1100, Wavelength.NANOMETER),
                inPoints);
    }

    /**
     * Creates a new Wavelength1DFormula with specified name and default
     * number of points and range.
     **/
    public Wavelength1DFormula(String inName) {
        this(inName, new Wavelength(100, Wavelength.NANOMETER),
                new Wavelength(1100, Wavelength.NANOMETER),
                100);
    }

    /**
     * Creates a new Wavelength1DFormula of with no name and specified
     * number of points and wavelength range.
     *
     * @param inMin Minimum Wavelength for the dataset
     * @param inMax Maximum Wavelength for the dataset
     * @param inPts number of points in the dataset
     **/
    public Wavelength1DFormula(Wavelength inMin, Wavelength inMax, int inPts) {
        this(null, inMin, inMax, inPts);
    }

    /**
     * Creates a new Wavelength1DFormula of with specified name,
     * number of points and wavelength range.
     *
     * @param inName Name of the dataset
     * @param inMin Minimum Wavelength for the dataset
     * @param inMax Maximum Wavelength for the dataset
     * @param inPts number of points in the dataset
     **/
    public Wavelength1DFormula(String inProp, Wavelength inMin, Wavelength inMax, int inPts) {
        super(inProp);
        fMinWavelength = inMin;
        fMaxWavelength = inMax;
        fNumPoints = inPts;
    }

    public Object clone() {
        Wavelength1DFormula newDS = (Wavelength1DFormula) super.clone();
        newDS.setMinWavelength((Wavelength) this.getMinWavelength().clone());
        newDS.setMaxWavelength((Wavelength) this.getMaxWavelength().clone());
        return newDS;
    }

    /**
     * After check with superclass, returns true if objects are same Class and
     * have same min/max and number
     * of points.
     **/
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!super.equals(obj)) return false;
        try {
            Wavelength1DFormula that = (Wavelength1DFormula) obj;
            if (!this.getClass().equals(that.getClass())) return false;

            if (fNumPoints != that.fNumPoints) return false;
            if ((fMinWavelength == null) ? (that.fMinWavelength != null) : !(fMinWavelength.equals(that.fMinWavelength))) return false;
            if ((fMaxWavelength == null) ? (that.fMaxWavelength != null) : !(fMaxWavelength.equals(that.fMaxWavelength))) return false;

            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public Wavelength getMinWavelength() {
        return fMinWavelength;
    }

    public Wavelength getMaxWavelength() {
        return fMaxWavelength;
    }

    public void setMinWavelength(Wavelength newWL) {
        if (fMinWavelength != null) fMinWavelength.removePropertyChangeListener(this);

        Wavelength oldWL = fMinWavelength;
        fMinWavelength = newWL;

        if (fMinWavelength != null) fMinWavelength.addPropertyChangeListener(this);
        firePropertyChange(MINWAVELENGTH_PROPERTY, oldWL, newWL);
    }

    public void setMaxWavelength(Wavelength newWL) {
        if (fMaxWavelength != null) fMaxWavelength.removePropertyChangeListener(this);

        Wavelength oldWL = fMaxWavelength;
        fMaxWavelength = newWL;

        if (fMaxWavelength != null) fMaxWavelength.addPropertyChangeListener(this);
        firePropertyChange(MAXWAVELENGTH_PROPERTY, oldWL, newWL);
    }

    /**
     * overriding to public access
     */
    public void setPending(boolean b) {
        super.setPending(b);
    }

    /**
     * Returns an array of doubles containing containing the formula's value
     * for each wavelength returned by toArrayWavelengths().
     */
    public double[] toArrayData() {
        double[] array = new double[fNumPoints];
        double wl = fMinWavelength.getValue();
        double inc = (fMaxWavelength.getValue() - wl) / fNumPoints;
        for (int i = 0; i < fNumPoints; i++) {
            array[i] = getValue(new Wavelength(wl));
            wl += inc;
        }
        return array;
    }

    /**
     * Creates an array of doubles containing wavelength values at even intervals
     * across the min/max wavelength of the object and of size
     * getNumPoints()
     */
    public double[] toArrayWavelengths() {
        double[] array = new double[fNumPoints];
        double wl = fMinWavelength.getValue();
        double inc = (fMaxWavelength.getValue() - wl) / fNumPoints;
        for (int i = 0; i < fNumPoints; i++) {
            array[i] = wl;
            wl += inc;
        }
        return array;
    }

    /**
     * Returns the wavelength data value for specified index as a Wavelength
     **/
    public Wavelength getWavelengthAtIndex(int index) {
        return new Wavelength(getWavelengthAtIndexAsDouble(index));
    }

    /**
     * Returns the wavelength data value for specified index
     * as a double value in the current default wavelength units
     **/
    public double getWavelengthAtIndexAsDouble(int index) {
        double inc = (fMaxWavelength.getValue() - fMinWavelength.getValue()) / fNumPoints;
        return fMinWavelength.getValue() + index * inc;
    }

    /**
     * This is a trivial implementation that returns the formula value for a
     * specified index. Most subclasses of Wavelength1DFormula will
     * define getValue( Wavelength) and rarely if ever reference this method.
     * <P>
     * However, if
     * you have an array of wavelength values that you are "sharing" across
     * several different Wavelength1DFormula, then overriding this method to
     * define the formula's functionality, and then writing getValue to reference
     * this method may be computationally much faster.
     *
     * See SEA's ExpCalcSpectroscopy for an example.
     */
    public double getValueAtIndex(int i) {
        return getValue(getWavelengthAtIndex(i));
    }


    /**
     * Changes the length of the array that would be returned by toArrayWavelengths()
     * and toArrayData().
     **/
    public void setNumPoints(int newP) {
        int oldP = fNumPoints;
        fNumPoints = newP;
        firePropertyChange(NUMPOINTS_PROPERTY, new Integer(oldP), new Integer(newP));
    }

    /**
     * returns the length of the array that would be returned by toArrayWavelengths()
     * and toArrayData().
     **/
    public int getNumPoints() {
        return fNumPoints;
    }

}
