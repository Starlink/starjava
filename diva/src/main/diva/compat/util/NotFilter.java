/*
 * $Id: NotFilter.java,v 1.1 2002/05/19 22:03:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.util;

import diva.util.Filter;

/**
 * A filter which returns the complement of a given
 * filter.
 *
 * @see Filter
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 */
public class NotFilter implements Filter {
    private Filter _filter;

    public NotFilter(Filter f) {
        _filter = f;
    }

    public boolean accept (Object o) {
        return !_filter.accept(o);
    }
}


