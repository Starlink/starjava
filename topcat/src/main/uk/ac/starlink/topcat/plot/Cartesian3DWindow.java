package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.plot.CartesianPlot3D;
import uk.ac.starlink.ttools.plot.ErrorRenderer;

/**
 * Graphics window for viewing 3D scatter plots using Cartesian coordinates.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public class Cartesian3DWindow extends Plot3DWindow {

    private static final ErrorRenderer[] ERROR_RENDERERS =
        ErrorRenderer.getOptions3d();
    private static final String[] AXIS_NAMES = new String[] { "X", "Y", "Z" };

    /**
     * Constructs a new window.
     *
     * @param   parent  parent component (may be used for postioning)
     */
    @SuppressWarnings("this-escape")
    public Cartesian3DWindow( Component parent ) {
        super( "3D Plot (old)", AXIS_NAMES, 3, parent,
               createErrorModeModels( AXIS_NAMES ), createPlot() );

        getPointSelectorToolBar().addSeparator();
        for ( int ierr = 0; ierr < 3; ierr++ ) {
            getPointSelectorToolBar().add( getErrorModeModels()[ ierr ]
                                          .createOnOffToolbarButton() );
        }
        getJMenuBar().add( createErrorModeMenu() );
        getJMenuBar()
            .add( createMarkerStyleMenu( getStandardMarkStyleSets() ) );
        getJMenuBar().add( createErrorRendererMenu( ERROR_RENDERERS ) );
        getToolBar().addSeparator();

        addHelp( "Cartesian3DWindow" );
    }

    protected StyleEditor createStyleEditor() {
        return new MarkStyleEditor( false, true, ERROR_RENDERERS,
                                    ErrorRenderer.DEFAULT,
                                    getErrorModeModels() );
    }

    /**
     * Generates a plot object to be used with this window.
     *
     * @return  3D plot
     */
    private static CartesianPlot3D createPlot() {
        return new CartesianPlot3D() {
            protected boolean paintMemoryError( OutOfMemoryError e ) {
                TopcatUtils.memoryErrorLater( e );
                return true;
            }
        };
    }
}
