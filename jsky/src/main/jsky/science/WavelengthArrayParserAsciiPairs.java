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
public class WavelengthArrayParserAsciiPairs implements Wavelength1DArrayParser {

    String fWlColName;
    String fWlUnits;
    String fFluxColName;
    String fFluxUnits;

    Wavelength1DArray fArray;
    Reader fReader;

    /**
     */
    public WavelengthArrayParserAsciiPairs(Wavelength1DArray array, InputStream istream)
            throws WavelengthArrayParseException {
        this(array, istream, null);
    }

    /**
     * optionalArgs, if present should be:
     * [0] wavelength units
     * [1] flux units
     */
    public WavelengthArrayParserAsciiPairs(Wavelength1DArray array, InputStream istream, String[] optionalArgs)
            throws WavelengthArrayParseException {
        this(array, new java.io.InputStreamReader(istream), optionalArgs);
    }

    /**
     * optionalArgs, if present should be:
     * [0] wavelength units
     * [1] flux units
     */
    public WavelengthArrayParserAsciiPairs(Wavelength1DArray array, Reader rdr, String[] optionalArgs)
            throws WavelengthArrayParseException {
        fArray = array;
        fReader = rdr;

        fWlColName = "WAVELENGTH";
        fWlUnits = Wavelength.ANGSTROM;
        fFluxColName = "FLUX";
        fFluxUnits = Flux.FLAM;

        if (optionalArgs != null) {
            if (optionalArgs.length > 0 && optionalArgs[0] != null) fWlUnits = optionalArgs[0];
            if (optionalArgs.length > 1 && optionalArgs[1] != null) fFluxUnits = optionalArgs[1];
        }
    }

    /**
     * Parser of return stream from a StringReader, with data expected
     * to be a pair of columns.  The left column is wavelength values, the right
     * column should be data values.
     * <P>
     * This is designed to be compatible with the HST STSDAS ttools tdump and tprint formats
     * All comment lines beginning with '#' are ignored.  The wavelength units
     * must be in Angstroms, no units are assumed for the data values they will be
     * stored as doubles.
     *
     * @param reader  the Reader who's contents are to be parsed
     * @param wlUnits a string containing the wavelength units contained in the string.
     *    If this is set to null, Wavelength.ANGSTROM will be assumed
     *
     * @throws WavelengthArrayParseException if there is any error in the parsing.  No cleanup
     *    of the the dataset up to the point of the parse error is performed
     **/
    public void parse() throws WavelengthArrayParseException {
        if (fWlUnits == null) fWlUnits = Wavelength.ANGSTROM;
        int addStep = 500;

        StreamTokenizer streamer = new StreamTokenizer(fReader);
        streamer.commentChar('#');

        int i = 0;
        int nToks = 0;
        boolean expectingData = false;
        try {
            int nextT = streamer.nextToken();
            nToks++;
            while (nextT != -1) {
                if (streamer.ttype == -3) {
                    if (!streamer.sval.startsWith("E") && !streamer.sval.startsWith("e")) {
                        throw new WavelengthArrayParseException(
                                streamer.sval + ", encountered after " + i + " valid pairs.");
                    }
                    try {
                        int ex = new Integer(streamer.sval.substring(1)).intValue();
                        if (expectingData)
                            fArray.setWavelengthAtIndex(i, fArray.getWavelengthAtIndexAsDouble(i) * Math.pow(10.0, (double) ex));
                        else
                            fArray.setValueAtIndex(i - 1, fArray.getValueAtIndex(i - 1) * Math.pow(10.0, (double) ex));
                    }
                    catch (Exception e) {
                        throw new WavelengthArrayParseException(
                                streamer.sval + ", encountered after " + i + " valid pairs.");
                    }
                }
                else {
                    if (streamer.ttype != -2) {
                        throw new WavelengthArrayParseException(
                                streamer.ttype + ", encountered after " + i + " valid pairs.");
                    }
                    if (i >= fArray.getNumPoints()) {
                        fArray.setNumPoints(fArray.getNumPoints() + addStep);
                    }
                    if (expectingData) {
                        fArray.setValueAtIndex(i++, streamer.nval);
                    }
                    else {
                        fArray.setWavelengthAtIndex(i, new Wavelength(streamer.nval, fWlUnits));
                    }
                    expectingData = !expectingData;
                }
                nextT = streamer.nextToken();
                nToks++;
            }
        }
        catch (IOException e) {
            throw new WavelengthArrayParseException(e.toString());
        }

        if (i < fArray.getNumPoints()) {
            fArray.setNumPoints(i);
        }
    }

}

