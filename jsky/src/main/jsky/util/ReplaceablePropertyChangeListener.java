//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class ReplaceablePropertyChangeListener
//
//--- Development History -----------------------------------------------------
//
//	08/27/98    S. Grosvenor
//
//		Original implementation.
//
//  03/17/99    S. Grosvenor
//      Renamed/merge of PropertyChange and Replacement handling
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

package jsky.util;

/**
 * The listener interface for receiving notification of when an object is to
 * be replaced by a different instance.  Used, for example, by Modules in SEA when
 * changes are held until the Apply button is pressed.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		03/17/99
 * @author		S. Grosvenor
 **/
public interface ReplaceablePropertyChangeListener extends java.beans.PropertyChangeListener {

    /**
     * This method is called for each object that is to be replaced
     *
     * @param event	the event that contains details about replacement
     **/
    public void replaceObject(ReplacementEvent event)
            throws ReplacementVetoException;

}
