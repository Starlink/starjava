/*
 * $Id: AndFilter.java,v 1.1 2002/05/19 22:03:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.util;

import diva.util.Filter;

/**
 * A composite  filter which ANDs two filters together.
 *
 * @see Filter
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 */
public class AndFilter implements Filter {
    private Filter _f1, _f2;

    public AndFilter(Filter f1, Filter f2) {
        _f1 = f1;
        _f2 = f2;
    }

    public boolean accept (Object o) {
        return (_f1.accept(o) && _f2.accept(o));
    }
}


