/*
 * ESO Archive
 *
 * $Id: GenericToolBarTarget.java,v 1.3 2002/07/09 13:30:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/31  Created
 */

package jsky.util.gui;

import java.beans.*;
import java.awt.*;
import java.net.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * Application classes that use the GenericToolBar class need to implement
 * this interface. The target class defines an AbstractAction class for each
 * toolbar item, which should also be used in the menubar. The target class
 * can then enable and disable the actions instead of accessing the menubar
 * and toolbar directly to do this.
 */
public abstract interface GenericToolBarTarget {

    /** Return the action for "Open" */
    public AbstractAction getOpenAction();

    /** Return the action for "Back" */
    public AbstractAction getBackAction();

    /** Return the action for "Forward" */
    public AbstractAction getForwAction();
}
