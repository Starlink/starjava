//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    10/2/98          S.Grosvenor
//      Initial packaging of class
//    12/24/98          S. Grosvenor
//      Much updating functionality previously in subclass promoted
//    01/28/99          S. Grosvenor
//      First release of spectroscopy support
//
//	05/25/00	J. Jones / 588
//		Minor mod to work with JSky 0.10.  BinaryTable.getNRows() method name.
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

import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.IOException;

import nom.tam.fits.*;

/**
 * Implements the Wavelength1DModel as a pair of arrays one containing
 * wavelength points and the other containing data values at each of those
 * wavelengths. This model uses the Wavelength class for management of Wavelength
 * units, but makes no assumptions about the units of the data values.
 * <P>
 * It anticipates (but does not currently require) that the wavelength data
 * is monotonically increasing.  It does NOT assume that the wavelength points
 * are equi-distant.
 * <P>
 * This class also works with the FITS routines to read from a FITS formatted
 * file.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version 07.16.00
 * @author 	Sandy Grosvenor
 *
 **/
public class Wavelength1DArray extends AbstractWavelength1D {

    private double[] fWavelengths;
    private double[] fData;

    private int fNumPoints;
    private static String fWavelengthUnits;

    private static final String STR_WAVELENGTHDOT = "Wavelength1DArray.";
    public static final String DATA_PROPERTY = "Data";

    /**
     * The Stream Unique Identifier for this class.
     **/
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new Wavelength1DArray with no Name, 100 data points, and
     * 100-1100 Nanometers range.
     **/
    public Wavelength1DArray() {
        this(null, new Wavelength(100, Wavelength.NANOMETER),
                new Wavelength(1100, Wavelength.NANOMETER),
                100);
    }

    /**
     * Creates new Wavelenth1DArray of specified number of points and
     * default range (100-1100 Nanometers).
     * @param inVal integer of number of desired points in dataset
     **/
    public Wavelength1DArray(int inVal) {
        this(null, new Wavelength(100, Wavelength.NANOMETER),
                new Wavelength(1100, Wavelength.NANOMETER),
                inVal);
    }

    /**
     * Creates a new Wavelength1DArray with the specified Name, 100 data points, and
     * 100-1100 Nanometers range.
     **/
    public Wavelength1DArray(String inName) {
        this(inName, new Wavelength(100, Wavelength.NANOMETER),
                new Wavelength(1100, Wavelength.NANOMETER),
                100);
    }

    /**
     * Creates a new Wavelength1DArray with no name, specified number of points, and
     * specified minimum and maximum wavelength range.
     * @param inMin Minimum Wavelength for the dataset
     * @param inMax Maximum Wavelength for the dataset
     * @param inPts number of points in the dataset
     **/
    public Wavelength1DArray(Wavelength inMin, Wavelength inMax, int inPts) {
        this(null, inMin, inMax, inPts);
    }

    /**
     * Creates a new Wavelength1DArray from an existing Wavelength1DModel.
     **/
    public Wavelength1DArray(Wavelength1DModel baseModel) {
        super(baseModel.getName());

        fWavelengthUnits = Quantity.getDefaultUnits(Wavelength.class);

        double[] baseData = baseModel.toArrayData(null, null, 0);
        double[] baseWave = baseModel.toArrayWavelengths(null, null, 0);

        fNumPoints = baseWave.length;

        fData = new double[fNumPoints];
        System.arraycopy(baseData, 0, fData, 0, fNumPoints);
        fWavelengths = new double[fNumPoints];
        System.arraycopy(baseWave, 0, fWavelengths, 0, fNumPoints);
    }

    /**
     * Creates a new Wavelength1DArray with no name, specified number of points, and
     * specified minimum and maximum Wavelength range.  The min/max wavelengths
     * are specified as Wavelength objects.
     * @param inName, name for the array
     * @param inMin Minimum Wavelength for the dataset
     * @param inMax Maximum Wavelength for the dataset
     * @param inPts number of points in the dataset
     **/
    public Wavelength1DArray(String inProp, Wavelength inMin, Wavelength inMax, int inPts) {
        this(inProp,
                ((inMin == null) ? Double.NaN : inMin.getValue()),
                ((inMax == null) ? Double.NaN : inMax.getValue()),
                inPts);
    }

    /**
     * Creates a new Wavelength1DArray with no name, specified number of points, and
     * specified minimum and maximum wavelength values in the curent
     * default Wavelength units.
     * @param inName, name for the array
     * @param inMin Minimum wavelength value in default units for the dataset
     * @param inMax Maximum wavelength value in default units for the dataset
     * @param inPts number of points in the dataset
     **/
    protected Wavelength1DArray(String inProp, double wLo, double wHi, int inPts) {
        super(inProp);

        fWavelengthUnits = Quantity.getDefaultUnits(Wavelength.class);
        fNumPoints = Math.max(1, inPts);

        fWavelengths = new double[fNumPoints];
        if (!Double.isNaN(wLo) && !Double.isNaN(wHi)) {
            fWavelengths[0] = wLo;
            if (fNumPoints > 1) {
                fWavelengths[fNumPoints - 1] = wHi;
                double increment = (wHi - wLo) / (fNumPoints - 1);
                for (int i = 1; i < fNumPoints - 1; i++) {
                    fWavelengths[i] = fWavelengths[i - 1] + increment;
                }
            }
        }
        fData = new double[fNumPoints];
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!super.equals(obj))
            return false;
        Wavelength1DArray that = (Wavelength1DArray) obj;
        if (fNumPoints != that.fNumPoints)
            return false;

        if (fData == null && that.fData == null &&
                fWavelengths == null && that.fWavelengths == null)
            return true;

        if (fWavelengths == null && that.fWavelengths != null) return false;
        if (fWavelengths != null && that.fWavelengths == null) return false;
        if (fData == null && that.fData != null) return false;
        if (fData != null && that.fData == null) return false;

        // to get here, both fwavelengths and fdata must not be null
        for (int i = 0; i < fNumPoints; i++) {
            if (fWavelengths[i] != that.fWavelengths[i])
                return false;
            if (fData[i] != that.fData[i])
                return false;
        }
        return true;
    }

    /**
     * Overriding to public access
     */
    public void setPending(boolean b) {
        super.setPending(b);
    }


    /**
     * Sets the Wavelength value for specified index.
     * @param index index in the array for the new wavelength point
     * @param value for the wavelength as a Wavelength
     **/
    public void setWavelengthAtIndex(int index, Wavelength inWL) {
        // uses current wavelength default units
        setWavelengthAtIndex(index, inWL.getValue());
    }

    /**
     * Sets the wavelength value for specified index as a double in the current
     * default Wavelength units.
     * @param index index in the array for the new wavelength point
     * @param value for the wavelength as a double in the current
     * default Wavelength units
     **/
    public void setWavelengthAtIndex(int index, double inWL) {
        fWavelengths[index] = inWL;
        firePropertyChange(DATA_PROPERTY, null, null);
    }

    /**
     * Returns the area "under the curve" of the model from the specified
     * minimum to maximum wavelengths.
     * If min or max is outside a defined wl area, then 0 values are assumed
     */
    public double getArea(Wavelength minWl, Wavelength maxWl, boolean interpolate) {
        return calculateArea(fWavelengths, fData, minWl, maxWl, interpolate);
    }

    /**
     * Returns the entire array of wavelength data points
     **/
    public double[] toArrayWavelengths() {
        return fWavelengths;
    }

    /**
     * Returns a subset of the array wavelengths with parameters specified as
     * Wavelengths
     */
    public double[] toArrayWavelengths(Wavelength minW, Wavelength maxW, int nPts, String units) {
        return toArrayWavelengths(
                ((minW == null) ? Double.NaN : minW.getValue(units)),
                ((maxW == null) ? Double.NaN : maxW.getValue(units)),
                nPts);
    }

    /**
     * Returns an array of wavelength datapoints.  Overridden here to return
     * a subset of the existing wavelength points if possible rather than new
     * set of points.
     * <P>To return a subset, the min and max wavelength must match existing data
     * points and the number of requested points must either be 0 or match
     * the number of points in the existing wavelength subset.
     * <P>If the min and max wavelengths do not match existing points
     * or the number of points does not match, then a new array of wavelengths
     * is return from min to max specified wavelengths and equal increments
     * to provide for the specified number of points
     */
    protected double[] toArrayWavelengths(double minwl, double maxwl, int nPts) {
        if ((minwl == fWavelengths[0]) && (maxwl == fWavelengths[fNumPoints - 1]) &&
                (nPts == fNumPoints)) {
            // matches whole array
            return fWavelengths;
        }
        else {
            int iLeft = (Double.isNaN(minwl) ? 0 : getIndexOf(minwl));
            int iRight = (Double.isNaN(maxwl) ? fNumPoints - 1 : getIndexOf(maxwl));
            double[] ret;

            if (((nPts == (iRight - iLeft + 1)) || (nPts == 0)) &&
                    (iRight >= 0) && (iLeft >= 0) &&
                    ((fWavelengths[iRight] == maxwl) || Double.isNaN(maxwl)) &&
                    ((fWavelengths[iLeft] == minwl) || Double.isNaN(minwl))) {
                // have matching left and right indices and right number
                // of points to be just a subset of the existing wavelength array
                nPts = iRight - iLeft + 1;
                ret = new double[nPts];
                System.arraycopy(fWavelengths, iLeft, ret, 0, nPts);
            }
            else {
                // go to parent for new even increments array
                if (nPts == 0) nPts = fNumPoints;
                ret = new double[nPts];

                double delta = (maxwl - minwl) / (nPts - 1);
                double wl = minwl;
                for (int i = 0; i < nPts; i++) {
                    ret[i] = minwl;
                    minwl += delta;
                }
            }
            return ret;
        }
    }

    /**
     * Returns an array of data points for the specifed set of wavelengths
     */
    public double[] toArrayData(double[] wllist) {
        double[] ret = new double[wllist.length];
        for (int i = 0; i < wllist.length; i++) {
            ret[i] = getValue(wllist[i]);
        }
        return ret;
    }

    /**
     * Returns the current data points array
     **/
    public double[] toArrayData() {
        return fData;
    }

    /**
     * Returns a set of data values to match the requested range of wavelengths
     * in Wavelengths
     */
    public double[] toArrayData(Wavelength minW, Wavelength maxW, int nPts) {
        return toArrayData(
                ((minW == null) ? Double.NaN: minW.getValue()),
                ((maxW == null) ? Double.NaN: maxW.getValue()),
                nPts);
    }

    /**
     * Returns a set of data values to match the requested range of wavelengths
     * in doubles.  The precise sub wavelengths used is obtained from the
     * toArrayWavelengths( double minwl, double maxwl, int npts) results
     */
    protected double[] toArrayData(double minwl, double maxwl, int nPts) {
        if ((minwl == fWavelengths[0]) && (maxwl == fWavelengths[fNumPoints - 1]) &&
                (nPts == fNumPoints)) {
            return fData;
        }
        else {
            return toArrayData(toArrayWavelengths(minwl, maxwl, nPts));
        }
    }

    /**
     * Replaces the data arrays with the inbound dataset
     * protected so that only descendents can do this, no checking for units
     * or other protections exist
     **/
    public void replaceDataSet(Wavelength1DArray newData) {
        double[] oldData = fData;
        if (newData != null) {
            fNumPoints = newData.getNumPoints();
            fWavelengths = newData.fWavelengths;
            fData = newData.fData;
        }
        else {
            fNumPoints = 0;
            fWavelengths = null;
            fData = null;
        }
        setException(null);
        firePropertyChange(DATA_PROPERTY, oldData, fData);
    }

    /**
     * Returns the wavelength data value for specified index as a Wavelength
     **/
    public Wavelength getWavelengthAtIndex(int index) {
        return ((fWavelengths == null || index >= fNumPoints || index < 0) ?
                null :
                new Wavelength(fWavelengths[index], fWavelengthUnits));
    }

    /**
     * Returns the wavelength data value for specified index
     * as a double value in the current default wavelength units
     **/
    public double getWavelengthAtIndexAsDouble(int index) {
        return ((fWavelengths == null || index >= fNumPoints || index < 0) ? Double.NaN : fWavelengths[index]);
    }

    /**
     * Returns the units at which the wavelength double values are currently stored
     */
    public String getWavelengthUnits() {
        return fWavelengthUnits;
    }

    /**
     * Returns the 1st index in the dataset to have a wavelength greater/equal to
     * specified wavelength.
     * @param targetWL is a double of the wavelength in the default units of the dataset
     * search is done on a binary basis for efficiency
     **/
    public int getIndexOf(double targetWL) {
        return getIndexOf(targetWL, fWavelengths);
    }

    /**
     * Returns the 1st index in the dataset to have a wavelength greater/equal to
     * specified wavelength.
     * @param targetWL is a double of the wavelength in the default units of the dataset
     * search is done on a binary basis for efficiency
     **/
    public int getIndexOf(Wavelength targetWL) {
        return getIndexOf(targetWL, fWavelengths);
    }

    /**
     * Sets the data value at the specified Wavelength in the array.  If the exact
     * wavelength does not exist, and new value is inserted into the list.
     */
    public void setValue(Wavelength wavelength, double value) {
        int index = getIndexOf(wavelength, fWavelengths, true);
        if (index < 0) {
            // do not have this wavelength, insert a slot
            insert(getIndexOf(wavelength, fWavelengths, false), wavelength, value);
        }
        else {
            setValueAtIndex(index, value);
        }

    }

    /**
     * Inserts a new wavelength/data pair at index, growing the array size by 1
     */
    private void insert(int index, Wavelength wl, double value) {
        double[] newData = new double[fData.length + 1];
        double[] newWave = new double[fWavelengths.length + 1];

        synchronized (this) {
            System.arraycopy(this.fData, 0, newData, 0, index);
            System.arraycopy(this.fData, index, newData, index + 1, fData.length - index);

            System.arraycopy(this.fWavelengths, 0, newWave, 0, index);
            System.arraycopy(this.fWavelengths, index, newWave, index + 1, fData.length - index);

            fData = newData;
            fWavelengths = newWave;
            fNumPoints = fData.length;

            fData[index] = value;
            fWavelengths[index] = wl.getValue();
        }
        firePropertyChange(DATA_PROPERTY, null, null);
    }


    /**
     * Changes the stored data value at the specified index
     */
    public void setValueAtIndex(int index, double inVal) {
        fData[index] = inVal;
        firePropertyChange(DATA_PROPERTY, null, null);
    }

    /**
     * Returns the data value at the specified index
     */
    public double getValueAtIndex(int index) {
        return ((fData == null || index >= fNumPoints || index < 0) ? 0 : fData[index]);
    }

    public Object clone() {
        Wavelength1DArray newDS = (Wavelength1DArray) super.clone();

        if (newDS.fWavelengths != null) {
            newDS.fWavelengths = new double[fNumPoints];
            System.arraycopy(this.fWavelengths, 0, newDS.fWavelengths, 0, fNumPoints);
        }
        if (newDS.fData != null) {
            newDS.fData = new double[fNumPoints];
            System.arraycopy(this.fData, 0, newDS.fData, 0, fNumPoints);
        }

        return newDS;
    }

    /**
     * Returns the data value at the specified Wavelength.  If the wavelength
     * is not a specific point in the array, then the value is interpolated
     * based on the previous and subsequent wavelengths in the array
     */
    public double getValue(Wavelength inWl) {
        if (inWl == null) return Double.NaN;
        return getValue(inWl.getValue(fWavelengthUnits));
    }

    /**
     * Returns the data value at a wavelength specified as a double.
     * If the wavelength
     * is not a specific point in the array, then the value is interpolated
     * based on the previous and subsequent wavelengths in the array
     */
    protected double getValue(double targetWl) {
        if (fWavelengths.length == 0 || Double.isNaN(targetWl)) {
            return Double.NaN;
        }
        else if (targetWl < fWavelengths[0] ||
                fWavelengths[fWavelengths.length - 1] < targetWl) {
            return 0.;
        }

        int leftI = 0;
        int rightI = fWavelengths.length - 1;

        double leftWl = fWavelengths[leftI];
        double rightWl = fWavelengths[rightI];

        while (rightI - leftI >= 1) {
            if (targetWl == leftWl) {
                return fData[leftI];
            }
            else if (targetWl == rightWl) {
                return fData[rightI];
            }
            else if ((rightI - leftI) == 1) {
                // in between these two indices, interpolate
                return fData[leftI] + (fData[rightI] - fData[leftI]) *
                        (targetWl - leftWl) / (rightWl - leftWl);
            }
            else {
                // in between, move the indices together
                int midI = leftI + (rightI - leftI) / 2;
                double midWl = fWavelengths[midI];
                if (targetWl >= midWl) {
                    leftI = midI;
                    leftWl = midWl;
                }
                else {
                    rightI = midI;
                    rightWl = midWl;
                }
            }
        }
        // if we fall out, if right==left and no catch has happend
        return fData[leftI];
    }

    private static double[] NaN = null;
    private static int NaNSize = 1000;

    /**
     * Sets all datapoints to <code>Double.NaN</code>.
     * @param dataOnly, boolean, if true the wavelengths will be left untouched
     *  only the data will be made Double.NaN
     **/
    public void setAllNaN(boolean dataOnly) {
        if (NaN == null) {
            NaN = new double[NaNSize];
            for (int i = 0; i < NaNSize; i++) NaN[i] = Double.NaN;
        }

        for (int i = 0; i < fNumPoints; i += NaNSize) {
            int nCopy = Math.min(NaNSize, fNumPoints - i);

            if (!dataOnly && fWavelengths != null) {
                System.arraycopy(NaN, 0, fWavelengths, i, nCopy);
            }
            if (fData != null) {
                System.arraycopy(NaN, 0, fData, i, nCopy);
            }
        }
        firePropertyChange(DATA_PROPERTY, null, null);
    }

    /**
     * Sets all datapoints data and wavelength to <code>Double.NaN</code>.
     **/
    public void setAllNaN() {
        setAllNaN(false);
    }

    /**
     * Adds the specified dataset to this dataset, converting the specified
     * dataset to use existing wavelength points, then adds them to current data.
     *
     * <P>also ASSUMES that wavelength and data units are the same, no checking is done
     *
     * Note: Use the mergeData() method instead if the specified dataset has different
     * wavelengths and you want use the specified dataset's wavelength to determine
     * where to add the corresponding data value.
     */
    public void add(Wavelength1DModel that) {
        double[] thatData = that.toArrayData(fWavelengths);
        for (int i = 0; i < fNumPoints; i++) {
            setValueAtIndex(i, fData[i] + thatData[i]);
        }
    }

    /**
     * Folds all inDS point values into this dataset, checking each wavelength
     * in inDS and add the data value to the appropriate location in this dataset.
     *
     *  <P> ASSUMES that the data units are the same -- no checking is done.
     *
     * Note: Use the add() method if you're adding a dataset that has same wavelengths as
     * this dataset or if you just want to add each data value directly to this dataset's
     * data value without checking for the wavelengths.
     */
    public void mergeData(Wavelength1DModel inDS) {
        double[] datasetList = inDS.toArrayWavelengths(null, null, 0);
        double[] datasetValue = inDS.toArrayData(null, null, 0);
        int datasetPoints = inDS.getNumPoints();

        // now loop through the dataset points and determine location to add
        for (int i = 0; i < datasetPoints; i++) {
            double wavelength = datasetList[i];
            double dataValue = datasetValue[i];

            if (fNumPoints == 1) {
                fData[0] = fData[0] + dataValue;
            }
            else if (fNumPoints > 1) {
                int location = 0;
                int index = 0;
                boolean done = false;
                while (!done && index < fNumPoints) {
                    if (index == 0) {
                        if (wavelength >= 0.0 && wavelength < fWavelengths[index + 1]) {
                            location = index;
                            done = true;
                        }
                    }
                    else if (index == fNumPoints - 1) {
                        if (wavelength >= fWavelengths[index]) {
                            location = index;
                            done = true;
                        }
                    }
                    else {
                        if (wavelength >= fWavelengths[index] && wavelength < fWavelengths[index + 1]) {
                            location = index;
                            done = true;
                        }
                    }
                    index++;
                }
                fData[location] = fData[location] + dataValue;
            }
        }
    }

    /**
     * Used to reduce the number of data points.  Every nPts will be aggregated to a single
     * point in the new dataset. Wavelength for each new point is the "left most" wavelength
     * of points aggregated together
     **/
    public void combineData(Wavelength1DModel baseDs, int nPts) {
        double baseWl[] = baseDs.toArrayWavelengths(null, null, 0);
        double baseData[] = baseDs.toArrayData(null, null, 0);
        int basePts = baseWl.length;

        if (nPts <= 1) {

            fData = new double[basePts];
            fWavelengths = new double[basePts];
            setNumPoints(baseWl.length, 0, false);
            System.arraycopy(baseData, 0, fData, 0, basePts);
            System.arraycopy(baseWl, 0, fWavelengths, 0, basePts);
        }
        else {
            int newP = basePts / nPts;
            fWavelengths = new double[newP];
            fData = new double[newP];
            Wavelength tmpWl = null;

            for (int base = 0; base < newP; base++) {
                fWavelengths[base] = baseWl[base * nPts];
                double newData = 0.0;
                for (int n = 0; n < nPts; n++)
                    if (base * nPts + n < basePts)
                        newData += baseData[base * nPts + n];
                fData[base] = newData;
            }
            fNumPoints = newP;
        }
    }

    /**
     * Resets the length and wavelength elements to match the input Wavelength1DArray
     * uses the getData() method to interpolate between old wavelength values and
     * new wavelength values
     **/
    public void alignData(Wavelength1DModel baseDs) {
        double baseWL[] = baseDs.toArrayWavelengths(null, null, 0);
        int nPoints = baseWL.length;

        double tmp[] = new double[nPoints];
        for (int i = 0; i < nPoints; i++)
            tmp[i] = getValue(new Wavelength(baseWL[i]));

        fData = tmp;
        fWavelengths = baseWL;
        fNumPoints = nPoints;
    }

    /**
     * Trims off zero value datapoints and the beginning or end of the wavelength
     **/
    public void trim() {
        int firstNonZero = 0;
        int lastNonZero = fNumPoints - 1;

        if (fData[firstNonZero] == 0) {
            while (firstNonZero < fNumPoints && fData[firstNonZero] == 0) firstNonZero++;
            firstNonZero--; // retreat one element to get 1 zero in list
        }
        if (fData[lastNonZero] == 0) {
            while (lastNonZero > 0 && fData[lastNonZero] == 0) lastNonZero--;
            lastNonZero++;
        }

        if (firstNonZero == 0 && lastNonZero == fNumPoints) return; // no trimming needed
        int start = lastNonZero - firstNonZero + 1;
        if (start < 0) {
            // if start < 0, then whole dataset is 0s, trim to 2 points, the edges
            if (fNumPoints >= 2) {
                fWavelengths[1] = fWavelengths[fNumPoints - 1];
                setNumPoints(2, 0, true);
            }
        }
        else {
            setNumPoints(start, firstNonZero, true);
        }
    }

    /**
     * Changes the length of the dataset, with a new total length and preserving
     * the old dataset copying from a specified starting index
     * @param newP, the new number of points
     * @param start, the starting index in the old dataset to save (points below the start index
     *   will be dropped
     * @param preserve, when false the old data values will not be saved, and the new array
     *   will be all zero
     **/
    private void setNumPoints(int newP, int start, boolean preserve) {
        if (newP != fNumPoints) {
            int oldP = fNumPoints;
            double oldW[] = fWavelengths;
            double oldD[] = fData;
            fNumPoints = newP;
            fWavelengths = new double[newP];
            fData = new double[newP];
            if (preserve) {
                int copyP = Math.min(oldP - start, newP);
                System.arraycopy(oldW, start, fWavelengths, 0, copyP);
                System.arraycopy(oldD, start, fData, 0, copyP);
            }
        }
    }

    /**
     * Changes the length of the dataset, with a new total length, preserves the
     * old data truncate the end if new number of points is less than current
     * or filling with zeros if the new number is more than the current.
     * @param newP, the new number of points
     **/
    public void setNumPoints(int newP) {
        int oldP = fNumPoints;
        setNumPoints(newP, 0, true);
        firePropertyChange(NUMPOINTS_PROPERTY, new Integer(oldP), new Integer(newP));
    }

    /**
     * Returns the number of points in the array
     */
    public int getNumPoints() {
        return fNumPoints;
    }

    /**
     * Multiplies every element in the array by the parameter
     */
    public void multiply(double m) {
        for (int i = 0; i < fNumPoints; i++) {
            fData[i] *= m;
        }
    }

    /**
     * Passes a null String as the units to parse( StringReader, String), causing
     * Angstroms to be the assumed wavelength units
     *
     * @param istr  an InputStream to the data to be parsed, this may includes binary FITS files
     * or text-based data
     * @throws WavelengthArrayParseException
     * @deprecated use Wavelength1DArrayParser.parse instead
     **/
    public void parse(InputStream istr)
            throws WavelengthArrayParseException {
        parse(istr, null);
    }

    /**
     * Passes a null String as the units to parse( StringReader, String), causing
     * Angstroms to be the assumed wavelength units
     *
     * @param reader  a Reader to the data to be parsed, ASSUME only text-based inputs
     * @throws WavelengthArrayParseException
     * @deprecated use Wavelength1DArrayParser.parse instead
     **/
    public void parse(Reader rdr)
            throws WavelengthArrayParseException {
        parseAscii(rdr, null);
    }

    /**
     * Looks to parse data from a reader and fill the array, looking first to see if
     * the data is a binary FITS file.  Otherwise it assumes a _very_ basic variant of
     * and ASCII fits file where it expects non-comment characters to alternate
     * between a wavelength in Angstroms, and a data value.  The parse makes some
     * bold assumptions: all white space ignore, wavelength are monotonically ascending.
     * <P>
     * No conversion of the data value is performed
     * For FITS binary data the wavelength units are converted from their source
     * units.  And ASCII wavelengths are assumed to be in wlUnits
     *
     * @param reader  a Reader to the data to be parsed
     * @param wlUnits the input units for an ascii data string, ignore if reader
     * contains binary FITS data
     * @throws WavelengthArrayParseException
     * @deprecated use Wavelength1DArrayParser.parse instead
     **/
    public void parse(InputStream istr, String wlUnits)
            throws WavelengthArrayParseException {
        parseFits(istr, "WAVELENGTH", wlUnits, "FLUX", Flux.FLAM);
    }

    /**
     * looks to parse a FITS formated input stream.  If FITS format not
     * found, tries a basic space/tab/comma seperate ascii format
     *
     * @throws WavelengthArrayParseException
     * @deprecated use Wavelength1DArrayParser.parse instead
     **/
    public void parseFits(InputStream istr, String wlColName, String wlUnits, String dataColName, String dataUnits)
            throws WavelengthArrayParseException {
        try {
            new WavelengthArrayParserFitsHst(this, istr,
                    new String[]{"WAVELENGTH", "FLUX", wlUnits, Flux.FLAM}).parse();
        }
        catch (WavelengthArrayParseException wape) {
            throw wape;
        }
        catch (Exception fe) {
            // boldly assume we have ascii input since the Fits format kicked out
            new WavelengthArrayParserAsciiPairs(this, istr, new String[]{wlUnits, Flux.FLAM}).parse();
        }
    }

    /**
     * looks to parse a FITS formated input stream.  If FITS format not
     * found, tries a basic space/tab/comma seperate ascii format
     *
     * @throws WavelengthArrayParseException
     * @deprecated use Wavelength1DArrayParser.parse instead
     **/
    public void parseAscii(Reader rdr, String wlUnits)
            throws WavelengthArrayParseException {
        new WavelengthArrayParserAsciiPairs(this, rdr, new String[]{wlUnits, Flux.FLAM}).parse();
    }


    public void setFluxUnits(String units) throws UnitsNotSupportedException {
        if (fFluxUnits != null && fFluxUnits.equals(units)) return;

        if (fFluxUnits != null && units != null) {
            Wavelength1DArray array = Flux.convertWavelength1DModel(this, fFluxUnits, units);
            replaceDataSet(array);
        }
        fFluxUnits = units;
    }

}

