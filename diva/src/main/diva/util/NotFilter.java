/*
 * $Id: NotFilter.java,v 1.2 2000/05/02 00:45:25 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

/**
 * A filter which returns the complement of a given
 * filter.
 *
 * @see Filter
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
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

