//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	ReplaceablePropertyVetoException
//
//--- Description -------------------------------------------------------------
//	Extends the PropertyVetoException and adds more detailed fields on why the propery
//  change event is being vetoed.
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	08/19/99	M. Fishman
//		Original implementation.
//
//  09/21/99    S. Grosvenor
//      Moved to sea.event, name changed to ReplaceablePropertyVetoException, now descends
//      from RunTimeException so we don't have to have "throws" clauses everywhere
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

package jsky.util;

import java.beans.PropertyChangeEvent;

/**
 *
 *	Extends the RunTimeException to provide capability similar to PropertyVetoException
 *  capabilities to property change handling.  This implementation works off
 *  of RunTimeException and consequently does not require a user to add
 *  a new change handling method.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		9/21/99
 * @author		M. Fishman
 **/
public class ReplaceablePropertyVetoException extends RuntimeException {

    private PropertyChangeEvent fEvent;
    private Object fVetoSource = null;
    private String fVetoReasonType = null;

    /**
     *
     *  constructor
     *
     *  @param vetoSource the object that is throwing the veto exception
     *  @param vetoReasonType  the reason type for why the property change was vetoed
     *  @param mess Descriptive message
     *  @param evt A PropertyChangeEvent describing the vetoed change.
     *
     *
     **/
    public ReplaceablePropertyVetoException(Object vetoSource,
                                            String vetoReasonType,
                                            String mess,
                                            PropertyChangeEvent evt) {
        super(mess);
        fEvent = evt;
        fVetoSource = vetoSource;
        fVetoReasonType = vetoReasonType;
    }

    /**
     * Gets the vetoed <code>PropertyChangeEvent</code>.
     *
     * @return A PropertyChangeEvent describing the vetoed change.
     */
    public PropertyChangeEvent getPropertyChangeEvent() {
        return fEvent;
    }

    /**
     *
     * get the source of the veto exception
     *
     **/
    public Object getVetoSource() {
        return fVetoSource;
    }

    /**
     *
     * get the reasoning type for this veto exception
     *
     **/
    public String getVetoReasonType() {
        return fVetoReasonType;
    }

    public String toString() {
        return super.toString() + ", event=" + fEvent +
                ", source=" + fVetoSource + ", reason=" +
                fVetoReasonType;
    }
}
