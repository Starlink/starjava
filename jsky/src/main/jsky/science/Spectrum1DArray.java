//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//      Class Spectrum
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    07/13/00          S.Grosvenor
//      Initial packaging of class
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

import java.beans.PropertyChangeEvent;

/**
 * Implements Spectrum as a Wavelength1DArray.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version 07.13.00
 * @author Sandy Grosvenor
 */
public class Spectrum1DArray extends Wavelength1DArray
        implements Spectrum {

    /**
     * The Stream Unique Identifier for this class.
     **/
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     **/
    public Spectrum1DArray() {
        this("", 0, Flux.COUNTS);
    }

    public Spectrum1DArray(Wavelength1DModel array) {
        this(array, Flux.COUNTS);
    }

    public Spectrum1DArray(String name, int npts) {
        this(name, npts, Flux.COUNTS);
    }

    public Spectrum1DArray(int x) {
        this("", x, Flux.COUNTS);
    }

    public Spectrum1DArray(String name) {
        this(name, 0, Flux.COUNTS);
    }

    public Spectrum1DArray(Wavelength1DModel array, String units) {
        super(array);
        try {
            setFluxUnits(units);
        }
        catch (UnitsNotSupportedException e) {
            // do nothing
        }
    }

    public Spectrum1DArray(String name, int npts, String units) {
        super(npts);
        setName(name);
        try {
            setFluxUnits(units);
        }
        catch (UnitsNotSupportedException e) {
            // do nothing
        }
    }

    /**
     * Always returns false.
     */
    public boolean isNormalizationRequired() {
        return false;
    } // default

    /**
     * Returns the total counts in photons/sec/cm^2.
     * Contrary to STScI's synphot, this is not multiplied by mirror area
     */
    public double getTotalCounts() {
        return getArea(true);
    }

}

