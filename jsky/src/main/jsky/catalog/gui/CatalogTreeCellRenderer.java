/*
 * ESO Archive
 *
 * $Id: CatalogTreeCellRenderer.java,v 1.3 2002/08/04 21:48:50 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/27  Created
 */

package jsky.catalog.gui;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.util.Resources;

/**
 * This local class is used to override the default tree node
 * renderer and provide special catalog dependent icons.
 */
public class CatalogTreeCellRenderer extends DefaultTreeCellRenderer {

    private Icon _imagesvrIcon = Resources.getIcon("imagesvr.gif");
    private Icon _catalogIcon = Resources.getIcon("catalog.gif");
    private Icon _archiveIcon = Resources.getIcon("archive.gif");
    private Icon _namesvrIcon = Resources.getIcon("namesvr.gif");

    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel,
                expanded, leaf, row, hasFocus);

        setBackgroundNonSelectionColor(tree.getBackground());

        if (this instanceof JLabel && value instanceof Catalog) {
	    if (value instanceof CatalogDirectory) {
		setIcon(getOpenIcon());
		setToolTipText(((CatalogDirectory)value).getDescription());
	    }
	    else {
		String servType = ((Catalog) value).getType();
		if (servType.equals("directory")) {
		    setIcon(getOpenIcon());
		}
		if (servType.equals("catalog")) {
		    setIcon(_catalogIcon);
		}
		if (servType.equals("archive")) {
		    setIcon(_archiveIcon);
		}
		if (servType.equals("namesvr")) {
		    setIcon(_namesvrIcon);
		}
		if (servType.equals("imagesvr")) {
		    setIcon(_imagesvrIcon);
		}
		setToolTipText(getText());
	    }
        }

        return this;
    }
}

