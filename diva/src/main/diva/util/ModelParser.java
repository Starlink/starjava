/*
 * $Id: ModelParser.java,v 1.1 2000/05/10 22:02:15 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

import java.io.IOException;
import java.io.Reader;

/**
 * ModelParser is an interface that should be extended by application
 * specified model parsers.  It's job is to parse data into an
 * application specific data structure.
 *
 * @author Heloise Hse  (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 */
public interface ModelParser {
    /**
     * Parse the data in the given charater stream into a data
     * structure and return the data structure.
     */
    public Object parse(Reader reader) throws java.lang.Exception;    
}

