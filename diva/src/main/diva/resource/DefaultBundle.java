/*
 * $Id: DefaultBundle.java,v 1.3 2001/07/22 22:01:36 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.resource;

import java.awt.Toolkit;
import java.awt.Image;

import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

/**
 * A class that bundles the default Diva resources contained
 * in the diva.resource package.
 * 
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 */
public class DefaultBundle extends RelativeBundle {

    /** Create a new Bundle of default resources.
     */
    public DefaultBundle () {
        super("diva.resource.Defaults", null, null);
    }
}


