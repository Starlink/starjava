//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class ReplacementEvent
//
//--- Description -------------------------------------------------------------
//  An event for notifying when a object's listeners that it is to be replaced
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	08/27/98    S. Grosvenor
//		Original implementation.
//
//  02/27/99    S. Grosvenor Booz-Allen
//      Modified while implementing ApplyResetModules
//
//    05/03/00    S. Grosvenor / 588 Booz-Allen
//      ScienceObject overhaul
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

import java.beans.PropertyChangeEvent;

import jsky.science.ScienceObjectModel;

/**
 * This event to indicate that an object is to be replaced in its entirety by
 * a new object.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		2000.05.03
 * @author		S. Grosvenor / BoozAllen
 **/
public class ReplacementEvent extends java.beans.PropertyChangeEvent {

    public static String REPLACEMENT = "Replacement".intern();

    /**
     * Constructs a new ReplacementEvent.
     *
     * @param oldObject, the "old" object to be replaced
     * @param newObject, the "new" object
     */
    public ReplacementEvent(Object oldObject, Object newObject) {
        super(oldObject, REPLACEMENT, oldObject, newObject);
    }

    /**
     * Returns the new object to be replaced
     *
     * @return the replacing object
     * @deprecated Please use getNewValue() instead
     */
    public Object getNewObject() {
        return getNewValue();
    }

}
