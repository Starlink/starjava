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
//    97/11/17          S.Grosvenor
//      Initial packaging of class
//    97/11/25
//      Cleaned up comments, ordered methods, redid event handling
//      to use standard java.beans.PropertyChangeSupportx
//    8/9/99            S.Grosvenor
//          static methods moved to new class SpectrumManager
//    8/27/99           S. Grosvenor
//          added SpectrumSubModule support
//    6/25/00           S. Grosvenor
//          converted to an interface and new wavelength model
//    11/27/2000        S. Grosvenor
//          cleaned up javadoc for Jsky submission
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

/**
 * Extends the Wavelength1DModel to provide additional functionality for needed to
 * act as a Spectrum.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version 11.27.2000
 * @author 	Sandy Grosvenor
 *
 **/
public interface Spectrum extends Wavelength1DModel {

    /**
     * Return true if the spectrum "must" be used with a Normalizer to set the
     * overall brightness.  Return false, if a normalizer is optional.
     **/
    boolean isNormalizationRequired();

    /**
     * Returns total counts in the model in units of photons / second / cm^2
     * (cm^2 is per centimeter squared of mirror area).
     */
    public double getTotalCounts();

}

