/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *    06-FEB-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.util.EventListener;

/**
 * Defines an interface to be used when listening for new data columns
 * created by a ColumnGenerator.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface ColumnGeneratorListener 
    extends EventListener {

    /**
     * Accept a column that has just been generated. The source is
     * the instance of ColumnGenerator that is related to the data.
     */
    public void acceptGeneratedColumn( Object source, double[] column );
}
