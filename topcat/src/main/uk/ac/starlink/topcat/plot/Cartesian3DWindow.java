package uk.ac.starlink.topcat.plot;

import java.awt.Component;

/**
 * Graphics window for viewing 3D scatter plots using Cartesian coordinates.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public class Cartesian3DWindow extends Plot3DWindow {

    /**
     * Constructs a new window.
     *
     * @param   parent  parent component (may be used for postioning)
     */
    public Cartesian3DWindow( Component parent ) {
        super( "3D", new String[] { "X", "Y", "Z" }, parent );

        addHelp( "PlotXyzWindow" );
    }
}
