//=== File Prolog =============================================================
//	This code was adapted by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Development History -----------------------------------------------------
//
//	06/15/00	S. Grosvenor / 588 Booz-Allen
//		Original implementation
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
//=== End File Prolog====================================================================

package jsky.science;

/**
 * Abstract class that provides initial functionality for Wavelength1DModel
 * without committing to the underlying storage structure of the dataset.
 *
 * <P>This code was originally developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 */
public abstract class AbstractWavelength1D extends AbstractScienceObjectNode
        implements Wavelength1DModel {

    public static final String NUMPOINTS_PROPERTY = "NumPoints";
    public static final String MINWAVELENGTH_PROPERTY = "MinWavelength";
    public static final String MAXWAVELENGTH_PROPERTY = "MaxWavelength";

    public AbstractWavelength1D() {
        super();
    }

    public AbstractWavelength1D(String name) {
        super(name);
    }

    /**
     * Calls getArea(true);
     */
    public double getArea() {
        return getArea(true);
    }

    /**
     * use getArea( minwavelength, maxwavelength, interpolate) instead
     */
    public double getArea(Wavelength minWl, Wavelength maxWl) {
        return getArea(minWl, maxWl, true);
    }

    /**
     * Returns the area "under the curve" of the model from the specified
     * minimum to maximum wavelengths.
     * @param interpolate, when true, the data points are treated as points
     * on a curve and the area calculation uses linear interpolation between
     * points.  When false, the data points are treated as "bins" with no
     * interpolation between points.
     */
    public double getArea(boolean interpolate) {
        return getArea(Wavelength.MIN_VALUE, Wavelength.MAX_VALUE, interpolate);
    }

    /**
     * Returns the area "under the curve" of the model from the specified
     * minimum to maximum wavelengths.
     * If min or max is outside a defined wl area, then 0 values are assumed
     * @param minWl the starting wavelength for area
     * @param maxWl the maximum wavelength for calculations.
     * @param interpolate When true, the data points are treated as points
     * on a curve and the area calculation uses linear interpolation between
     * points.  When false, the data points are treated as "bins" with no
     * interpolation between points.
     * <p>
     * This method may be overridden by subclasses (such as Wavelength1DArray) to
     * create a more efficient array of wavelengths and data values to pass
     * to the main calculateArea() method
     */
    public double getArea(Wavelength minWl, Wavelength maxWl, boolean interpolate) {
        double[] wlArray = toArrayWavelengths(minWl, maxWl, 0);
        double[] dataArray = toArrayData(minWl, maxWl, 0);
        return calculateArea(wlArray, dataArray, minWl, maxWl, interpolate);
    }

    /**
     * @param wlArray array of doubles representing the wavelength values
     * @param wl
     * @param minWl the starting wavelength for area
     * @param maxWl the maximum wavelength for calculations.
     * @param interpolate When true, the data points are treated as points
     * on a curve and the area calculation uses linear interpolation between
     * points.  When false, the data points are treated as "bins" with no
     * interpolation between points.
     */
    protected double calculateArea(double[] wlArray, double[] dataArray,
                                   Wavelength minWl, Wavelength maxWl, boolean interpolate) {
        double area = 0;
        double minL = minWl.getValue();
        double maxL = maxWl.getValue();

        // iLeft is first array point to right of minWl
        int iLeft = getIndexOf(minWl, wlArray);

        // if ileft off end of array, assume 0 values to left of array
        if (iLeft >= 0) {
            // otherwise calc area from min wl to 1eft most "real" index (ileft)
            if (interpolate) {
                area += calcArea(minL, wlArray[iLeft], getValue(minWl), dataArray[iLeft], interpolate);
            }
        }
        else {
            iLeft = 0;
        }

        // iRight is last array point to left of maxWL
        int iRight = getIndexOf(maxWl, wlArray) - 1;
        if (iRight < 0) {
            iRight = wlArray.length - 1;
        }
        else {
            // otherwise calc area from right most "real" index (iright) to maxwl
            area += calcArea(wlArray[iRight], maxL, dataArray[iRight], getValue(maxWl), interpolate);
        }

        // add in area for each segment of contained throughput
        for (int i = iLeft; i < iRight; i++) {
            area += calcArea(wlArray[i], wlArray[i + 1], dataArray[i], dataArray[i + 1], interpolate);
        }
        return area;
    }

    /**
     * Calculates the area under a curve of rectangle define by lower left and upper right
     * coordinates.  xl and xr define left/right edges
     * and yl and yr define the height of left/right edges respectivelly
     **/
    protected double calcArea(double xl, double xr, double yl, double yr, boolean interpolate) {
        if (interpolate)
            return (xr - xl) * (yl + (yr - yl) / 2);
        else
            return (xr - xl) * yl;
    }

    /**
     * Looks up the index of a specified wavelength in the dataset to have a wavelength greater/equal to
     * specified wavelength.  Returns the index of first element >= requested wavelength
     * <P>
     * Note: this implementation assumes that the wavelength points are monotonically
     * increasing.
     *
     * @param targetWL is a target Wavelength in the default units of the dataset
     * @param wlArray is the array of wavelength in double units in which to lock
     * @param exactOnly when true only exact wavelength matches will be returned, when
     * false the index of first wavelength >= target will be returned.
     * search is done on a binary basis for efficiency
     **/
    protected int getIndexOf(Wavelength wl, double[] wlArray) {
        return getIndexOf(wl.getValue(), wlArray, false);
    }

    /**
     * Looks up the index of a specified wavelength in the dataset to have a
     * wavelength greater/equal to specified wavelength.  Returns the
     * index of first element >= requested wavelength
     * @param wl is a target Wavelength as a double value in the default units of the dataset
     * @param wlArray is the array of wavelength in double units in which to lock
     **/
    protected int getIndexOf(double wl, double[] wlArray) {
        return getIndexOf(wl, wlArray, false);
    }

    /**
     * Looks up the index of a specified wavelength in the dataset to have a wavelength greater/equal to
     * specified wavelength.  Can restrict implementation to an exact match, or return the
     * index of first element >= requested wavelength
     * @param targetWL is a target Wavelength in the default units of the dataset
     * @param wlArray is the array of wavelength in double units in which to lock
     * @param exactOnly when true only exact wavelength matches will be returned, when
     * false the index of first wavelength >= target will be returned.
     * search is done on a binary basis for efficiency
     **/
    protected int getIndexOf(Wavelength wl, double[] wlArray, boolean exactOnly) {
        return getIndexOf(wl.getValue(), wlArray, exactOnly);
    }

    /**
     * Looks up the index of a specified wavelength in the dataset to have a wavelength greater/equal to
     * specified wavelength (as a double).  Can restrict implementation to an exact match, or return the
     * index of first element >= requested wavelength
     * @param targetWL is a double of the wavelength in the default units of the dataset
     * @param wlArray is the array of wavelength in which to lock
     * @param exactOnly when true only exact wavelength matches will be returned, when
     * false the index of first wavelength >= target will be returned.
     * search is done on a binary basis for efficiency
     **/
    protected int getIndexOf(double targetWl, double[] wlArray, boolean exactOnly) {
        if (wlArray.length == 0) {
            return -1;
        }
        if (targetWl < wlArray[0] || wlArray[wlArray.length - 1] < targetWl) return -1;

        double w1 = wlArray[0];
        double w2 = wlArray[wlArray.length - 1];

        if (wlArray.length == 0 || Double.isNaN(targetWl)) {
            return -1;
        }
        else if ((targetWl < w1) || (targetWl > w2)) {
            return -1;
        }
        else {
            int leftI = 0;
            double leftWl = wlArray[leftI];
            if (targetWl == leftWl) return leftI;

            int rightI = wlArray.length - 1;
            double rightWl = wlArray[rightI];
            if (targetWl == rightWl) return rightI;

            while (rightI - leftI > 1) {
                if (targetWl == leftWl)
                    return leftI;
                else if (targetWl == rightWl)
                    return rightI;
                else {
                    // in between
                    int midI = leftI + (rightI - leftI) / 2;
                    double midWl = wlArray[midI];
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
            if (targetWl == leftWl)
                return leftI;
            else
                return (exactOnly ? -1 : rightI);
        }
    }

    /**
     * Default implementation: returns false
     */
    public boolean isEditable() {
        return false;
    }

    /**
     * default implementation, does nothing
     */
    public void setValue(Wavelength inWl, double newVal) {
    } // does nothing

    /**
     * passes current default wavelength units down into the real workhorse
     */
    public double[] toArrayWavelengths(Wavelength minWL, Wavelength maxWL, int nPts) {
        return toArrayWavelengths(minWL, maxWL, nPts, Wavelength.getDefaultUnits(Wavelength.class));
    }

    /**
     * Default implementation for returning an array of data values for
     * a specified set of wavelength values.
     * <P>
     * Note: subclasses may be able to override this "brute force"
     * implementation with less resource-intensive implementation
     */
    public double[] toArrayData(double[] wllist) {
        double[] ret = new double[wllist.length];
        for (int i = 0; i < wllist.length; i++) {
            ret[i] = getValue(new Wavelength(wllist[i]));
        }
        return ret;
    }

    protected String fFluxUnits = null;

    /**
     * a DUMMY implementation: Saves the string but does NOTHING with it.
     */
    public void setFluxUnits(String units) throws UnitsNotSupportedException {
        fFluxUnits = units;
    }

    public String getFluxUnits() {
        return fFluxUnits;
    }

}
