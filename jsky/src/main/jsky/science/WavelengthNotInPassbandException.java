//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    Jan 27 1999       S. Grosvenor
//      initial creation
//    01/28/99          S. Grosvenor
//      First release of spectroscopy support
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
 * The WavelengthNotInPassbandException may be thrown by an exposure when
 * determines that the target wavelength for spectroscopy is not within
 * the range of the selected grating.
 *<P>
 * This exception also contains a showErrorMessageDialog that will prompt
 * the user for a valid wavelength and return that wavelength to the calling
 * method
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version	Jan 27, 1999
 * @author	Sandy Grosvenor
 *
 **/
public class WavelengthNotInPassbandException extends Exception {

    Passband pBand;
    Wavelength pWL;
    private static String TITLE = "Wavelength Not In Passband Range";

    public WavelengthNotInPassbandException(Passband inBand, Wavelength inWL) {
        super("WavelengthNotInPassbandException");
        pBand = inBand;
        pWL = inWL;
    }

    public String getMessage() {
        return "Target wavelength (" + pWL.toString() +
                ") is not in the\ngrating's band pass (" + pBand.toString() +
                "). \n\nPlease enter a new wavelength";
    }

    private static JOptionPane optionPane = null;

    public void showErrorMessageDialog(Component parent) {
        if (optionPane == null) {
            optionPane = new JOptionPane(
                    this.getMessage(),
                    JOptionPane.ERROR_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);

            optionPane.setWantsInput(true);
        }

        boolean tryAgain = true;
        Object selectedValue = FormatUtilities.formatDouble(pWL.getValue(), 2);
        double bandLow = pBand.getLowWavelength().getValue();
        double bandHigh = pBand.getHighWavelength().getValue();

        while (tryAgain) {

            optionPane.setMessage(this.getMessage());
            optionPane.setInputValue(null);
            optionPane.setInitialSelectionValue(null);

            JDialog dialog = optionPane.createDialog(parent, TITLE);
            dialog.show();

            selectedValue = optionPane.getInputValue();
            if (selectedValue == null) {
                // something other than

                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(parent,
                        "This will leave the wavelength at " +
                        FormatUtilities.formatDouble(pWL.getValue(), 2) + " " +
                        Quantity.getDefaultUnitsAbbrev(Wavelength.class) +
                        "\nIs this what you want?")) {

                    return;  // cancel gets you out without changing pWL
                }
                else {
                    continue;
                }
            }


            try {
                double dWL = Double.NaN;

                if (selectedValue instanceof String) {
                    dWL = new Double((String) selectedValue).doubleValue();
                }
                else if (selectedValue instanceof Number) {
                    dWL = ((Number) selectedValue).doubleValue();
                }

                if ((bandLow <= dWL) && (dWL <= bandHigh)) {
                    pWL = new Wavelength(dWL, pWL.getDefaultUnits());
                    tryAgain = false;
                }

            }
            catch (NumberFormatException e) {
                // ignore this one?
            }
        }

    }

    public Wavelength getWavelength() {
        return pWL;
    }
}
