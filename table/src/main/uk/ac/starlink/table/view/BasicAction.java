package uk.ac.starlink.table.view;

import javax.swing.AbstractAction;

/**
 * Convenience class extending AbstractAction.
 */
public abstract class BasicAction extends AbstractAction {

    public BasicAction( String name, String shortdesc ) {
        this( name, 0, shortdesc );
    }

    public BasicAction( String name, int iconId, String shortdesc ) {
        super( name, null );
        putValue( SHORT_DESCRIPTION, shortdesc );
    }
}
