package uk.ac.starlink.treeview;

import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 * Convenience class extending AbstractAction.
 */
public abstract class BasicAction extends AbstractAction {

    public BasicAction( String name, String shortdesc ) {
        this( name, null, shortdesc );
    }

    public BasicAction( String name, Icon icon, String shortdesc ) {
        super( name, icon );
        putValue( SHORT_DESCRIPTION, shortdesc );
    }
}
