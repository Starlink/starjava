//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	DetailedPropertyVetoException
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
//
//		Original implementation.
//
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
//package gov.nasa.gsfc.util.gui;

package jsky.timeline;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;


/**
 *
 * Extends the PropertyVetoException and adds more detailed fields on why the propery
 * change event is being vetoed.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		08/19/99
 * @author		M. Fishman
 **/
public class DetailedPropertyVetoException extends PropertyVetoException {

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
    public DetailedPropertyVetoException(Object vetoSource,
                                         String vetoReasonType,
                                         String mess,
                                         PropertyChangeEvent evt) {
        super(mess, evt);
        fVetoSource = vetoSource;
        fVetoReasonType = vetoReasonType;
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
}
