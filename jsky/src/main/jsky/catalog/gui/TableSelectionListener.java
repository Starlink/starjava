/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TableSelectionListener.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.catalog.gui;

import java.util.EventListener;

/**
 * This defines the interface for listening for selection events on a catalog plot tables.
 *
 * @version $Revision: 1.2 $
 * @author Daniella Malin
 */
public abstract interface TableSelectionListener extends EventListener {

    /**
     * Invoked when the table is selected.
     */
    public void tableSelected(TableSelectionEvent e);

}
