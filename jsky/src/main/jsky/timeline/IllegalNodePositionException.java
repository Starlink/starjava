//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	IllegalNodePositionException
//
//--- Description -------------------------------------------------------------
//	An exception that gets thrown when a time line node is set to an illegal position
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	07/12/99	M. Fishman
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


/**
 *
 * An exception that gets thrown when a time line node is set to an illegal position.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		06/23/99
 * @author		M. Fishman / 588
 **/
public class IllegalNodePositionException extends Exception {

    /**
     *
     * Constructs an Exception with no specified detail message
     *
     **/
    public IllegalNodePositionException() {
        super();
    }

    /**
     *
     * Constructs an Exception with the specified detail message
     *
     * @param message the detail message
     *
     **/
    public IllegalNodePositionException(String message) {
        super(message);
    }
}

