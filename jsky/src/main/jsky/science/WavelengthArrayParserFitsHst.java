//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    08/24/01          S.Grosvenor
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

import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.IOException;

import nom.tam.fits.*;

/**

 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version 07.16.00
 * @author 	Sandy Grosvenor
 *
 **/
public class WavelengthArrayParserFitsHst implements Wavelength1DArrayParser {

    String fWlColName;
    String fWlUnits;
    String fFluxColName;
    String fFluxUnits;

    Wavelength1DArray fArray;
    InputStream fStream;

    /**
     */
    public WavelengthArrayParserFitsHst(Wavelength1DArray array, InputStream istream)
            throws WavelengthArrayParseException {
        this(array, istream, null);
    }

    /**
     * optionalArgs, if present should be:
     * [0] wavelength column name
     * [1] flux column name
     * [2] wavelength units
     * [3] flux units
     */
    public WavelengthArrayParserFitsHst(Wavelength1DArray array, InputStream istream, String[] optionalArgs)
            throws WavelengthArrayParseException {
        fArray = array;
        fStream = istream;

        fWlColName = "WAVELENGTH";
        fWlUnits = Wavelength.ANGSTROM;
        fFluxColName = "FLUX";
        fFluxUnits = Flux.FLAM;

        if (optionalArgs != null) {
            if (optionalArgs.length > 0 && optionalArgs[0] != null) fWlColName = optionalArgs[0];
            if (optionalArgs.length > 1 && optionalArgs[1] != null) fFluxColName = optionalArgs[1];
            if (optionalArgs.length > 2 && optionalArgs[2] != null) fWlUnits = optionalArgs[2];
            if (optionalArgs.length > 3 && optionalArgs[3] != null) fFluxUnits = optionalArgs[3];
        }
    }

    private boolean matchesPluralOrNot(String want, String have) {
        if (want.equalsIgnoreCase(have)) return true;
        if (want.endsWith("s")) want = want.substring(0, want.length() - 1);
        if (have.endsWith("s")) have = have.substring(0, have.length() - 1);
        return want.equalsIgnoreCase(have);
    }

    public void parse() throws WavelengthArrayParseException {
        try {
            Fits f = new Fits(fStream);
            BasicHDU hdu = f.readHDU();

            // look for a BinaryTableHDU
            while (hdu != null) {
                if (hdu instanceof BinaryTableHDU) {
                    BinaryTableHDU bdu = (BinaryTableHDU) hdu;
                    BinaryTable data = (BinaryTable) bdu.getData();

                    int wlCol = bdu.findColumn(fWlColName);
                    if (wlCol < 0)
                        for (int i = 0; i < bdu.getNCols(); i++) {
                            if (matchesPluralOrNot(bdu.getColumnName(i), fWlColName)) wlCol = i;
                        }
                    if (wlCol >= 0) {
                        fWlUnits = bdu.getHeader().getStringValue("TUNIT" + (wlCol + 1));
                        fWlUnits = Quantity.getUnitsIgnoreCase(Wavelength.class, fWlUnits);

                        int flCol = bdu.findColumn(fFluxColName);
                        if (flCol < 0)
                            for (int i = 0; i < bdu.getNCols(); i++) {
                                if (matchesPluralOrNot(bdu.getColumnName(i), fFluxColName)) flCol = i;
                            }
                        if (flCol >= 0) {
                            String localFluxUnits = bdu.getHeader().getStringValue("TUNIT" + (flCol + 1));
                            fArray.setNumPoints(data.getNRows());

                            for (int i = 0; i < fArray.getNumPoints(); i++) {
                                Object rawWl = data.getElement(i, wlCol);
                                float foo = ((float[]) rawWl)[0];
                                fArray.setWavelengthAtIndex(i, new Wavelength(((float[]) rawWl)[0], fWlUnits));

                                Object rawData = data.getElement(i, flCol);
                                float foo2 = ((float[]) rawData)[0];
                                fArray.setValueAtIndex(i, ((float[]) rawData)[0]);
                            }
                            return;
                        }
                    }
                }
                hdu = f.readHDU();
            }
            throw new WavelengthArrayParseException("No binary table found with requested columns and units");
        }
        catch (Exception e) {
            if (e instanceof WavelengthArrayParseException) {
                throw (WavelengthArrayParseException) e;
            }
            else {
                throw new WavelengthArrayParseException("Embedded parse exception: " + e.toString());
            }
        }
    }
}

