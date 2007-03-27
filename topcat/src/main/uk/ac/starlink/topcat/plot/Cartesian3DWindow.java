package uk.ac.starlink.topcat.plot;

import java.awt.Component;

/**
 * Graphics window for viewing 3D scatter plots using Cartesian coordinates.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public class Cartesian3DWindow extends Plot3DWindow {

    private static final ErrorRenderer[] ERROR_RENDERERS =
        ErrorRenderer.getOptions3d();

    /**
     * Constructs a new window.
     *
     * @param   parent  parent component (may be used for postioning)
     */
    public Cartesian3DWindow( Component parent ) {
        super( "3D", new String[] { "X", "Y", "Z" }, parent, 3,
               new CartesianPlot3D() );

        for ( int ierr = 0; ierr < 3; ierr++ ) {
            getToolBar().add( getErrorModeModels()[ ierr ]
                                                 .createOnOffToolbarButton() );
        }
        getJMenuBar().add( createErrorMenu( ERROR_RENDERERS ) );
        getToolBar().addSeparator();

        addHelp( "Cartesian3DWindow" );
    }

    protected StyleEditor createStyleEditor() {
        return new MarkStyleEditor( false, true, ERROR_RENDERERS,
                                    getErrorModeModels() );
    }
}
