package uk.ac.starlink.topcat;

import java.awt.Component;

/**
 * AuxWindow subclass which displays one view of a TopcatModel.
 * This class merely provides common functionality for specific subclasses 
 * (which display table cells, parameters, column metadata etc);
 * it can set up menus, window titles, that sort of thing.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2004
 */
public class TopcatViewWindow extends AuxWindow {

    private final TopcatModel tcModel;
    private final String viewName;

    /**
     * Constructor.
     *
     * @param   tcModel the model which will be displayed in the new window
     * @param   viewName  name of the type of view provided by this window
     * @param   parent   parent component, may be used for window positioning
     */
    protected TopcatViewWindow( TopcatModel tcModel, String viewName,
                                Component parent ) {
        super( null, parent );
        this.tcModel = tcModel;
        this.viewName = viewName;
        configureTitle();
        tcModel.addTopcatListener( new TopcatListener() {
            public void modelChanged( TopcatModel tcModel, int code ) {
                configureTitle();
            }
        } );
    }

    private void configureTitle() {
        setTitle( "TOPCAT(" + tcModel.getID() + "): " + viewName );
        setMainHeading( viewName + " for " + tcModel.toString() );
    }
}
