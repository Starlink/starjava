/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-JAN-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.util.gui;

import org.w3c.dom.Element;

/**
 * An interface for component configurations that can be serialised to
 * XML and subsequently stored and restored to a backing store using
 * a {@link StoreControlFrame}.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface StoreSource
{
    /**
     * Save the configuration with the given Element as the root of
     * the document.
     */
    public void saveState( Element rootElement );

    /**
     * Restore a previously saved configuration created by the 
     * {@link #saveState} method.
     */
    public void restoreState( Element rootElement );

    /**
     * Return a name for this application. This is used to create a
     * directory for the configuration store.
     */
    public String getApplicationName();

    /**
     * Return a name for the configuration store (without any
     * directory information).
     */
    public String getStoreName();

    /**
     * Get a name for the top-level element associated with this
     * configuration.
     */
    public String getTagName();
}
