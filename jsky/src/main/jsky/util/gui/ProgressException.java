/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ProgressException.java,v 1.2 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.io.IOException;

/**
 * An exception that is thrown when (or at some point after) the user
 * presses the Stop button in a ProgressPanel.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class ProgressException extends IOException {

    public ProgressException(String msg) {
        super(msg);
    }
}

