//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class ReplacementVetoException
//
//--- Description -------------------------------------------------------------
//	may be thrown by a replaceObject method if the replacement has failed or
//  otherwise been rejected.
//
//--- Development History -----------------------------------------------------
//
//	02/25/99	S. Grosvenor / 588 Booz-Allen
//		Original implementation.
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

package jsky.util;

/**
 * A ReplacementVetoException is thrown when a requested replaceObject event
 * is unacceptable
 **/
public class ReplacementVetoException extends Exception {

    private ReplacementEvent evt;

    /**
     * @param mess Descriptive message
     * @param evt A ReplacementEvent describing the vetoed change.
     */
    public ReplacementVetoException(String mess, ReplacementEvent evt) {
        super(mess);
        this.evt = evt;
    }

    public ReplacementEvent getReplacementEvent() {
        return evt;
    }

}

