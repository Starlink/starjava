//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	QuantityRange
//
//--- Description -------------------------------------------------------------
// A range object which contain two quantities to make up the range
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	12/15/99	M. Fishman
//
//		Original implementation.
//
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
//
//=== End File Prolog =========================================================

package jsky.science;

/**
 *
 * A range object which contain two Quantities to make up the range.
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		12/15/99
 * @author		M. Fishman
 **/
public class QuantityRange {

    /**
     * Minimum possible Quantity (largest negative value).
     */
    public static Quantity MIN_QUANTITY = new Time(-1.0 * Double.MAX_VALUE);

    /**
     * Maximum possible Quantity/
     */
    public static Quantity MAX_QUANTITY = new Time(Double.MAX_VALUE);

    /**
     * A QuantityRange with null values for both max and min
     */
    public static final QuantityRange INVALID_RANGE = new QuantityRange(null, null);

    /**
     * An all-encompassing range, from MIN_QUANTITY to MAX_QUANTITY
     */
    public static final QuantityRange ALL_RANGE = new QuantityRange(MIN_QUANTITY, MAX_QUANTITY);

    /**
     * Lower bound of the range
     */
    private Quantity fMinRange;

    /**
     * upper bound of the range
     */
    private Quantity fMaxRange;

    /**
     *
     * Creates a QuantityRange from specified min and max
     *
     * @param min the minimum range value
     * @param max the maximum range value
     *
     **/
    public QuantityRange(Quantity min, Quantity max) {
        fMinRange = min;
        fMaxRange = max;
    }

    /**
     *
     * Get the minimum range value.
     *
     **/
    public Quantity getMinimumRange() {
        return fMinRange;
    }

    /**
     *
     * Get the maximum range value.
     *
     **/
    public Quantity getMaximumRange() {
        return fMaxRange;
    }

    /**
     *
     * Returns whether or not this range intersects the parameter range
     *
     * @param range the range to compare against
     *
     * @return true if the ranges intersect and false otherwise
     *
     */
    public boolean intersects(QuantityRange range) {
        boolean intersects = true;
        if (((range.getMinimumRange().getValue() < fMinRange.getValue()) &&
                (range.getMaximumRange().getValue() < fMinRange.getValue())) ||
                ((range.getMinimumRange().getValue() > fMaxRange.getValue()) &&
                (range.getMaximumRange().getValue() > fMaxRange.getValue()))) {
            intersects = false;
        }
        return intersects;
    }

    /**
     *
     * Represent the range as a String, in the Quantity's default units
     *
     **/
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(fMinRange.toString());
        sb.append("...");
        sb.append(fMaxRange.toString());
        return sb.toString();
    }

}
