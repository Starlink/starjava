//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    06.30.2000       S. Grosvenor
//      initial creation
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
//=== End File Prolog

package jsky.science;

import javax.swing.JOptionPane;
import javax.swing.JDialog;
import java.awt.Component;

import jsky.util.FormatUtilities;

/**
 * The UnitsNotSupportedException is thrown by a Quantity when
 * a request for a value is made in units that are not supported by
 * the Quantity subclass.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version	06.30.2000
 * @author	Sandy Grosvenor
 *
 **/
public class UnitsNotSupportedException extends Exception {

    String fUnits;

    public UnitsNotSupportedException(String units) {
        super("UnitsNotSupportedException, " + units);
        fUnits = units;
    }

    public String getUnits() {
        return fUnits;
    }
}
