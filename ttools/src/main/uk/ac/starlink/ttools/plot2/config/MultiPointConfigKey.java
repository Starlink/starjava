package uk.ac.starlink.ttools.plot2.config;

import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import uk.ac.starlink.ttools.gui.MarkStyleSelectors;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorModeSelection;
import uk.ac.starlink.ttools.plot.ErrorRenderer;

/**
 * Config key that specifies a multi-point renderer.
 * This goes by the name of an ErrorRenderer, but is just an object that
 * draws some shape based on multiple data positions.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2013
 */
public class MultiPointConfigKey extends OptionConfigKey<ErrorRenderer> {

    private final ErrorRenderer[] renderers_;
    private final ErrorMode[] modes_;

    /**
     * Constructor.
     *
     * @param   meta  metadata
     * @param   renderers   renderer options
     * @param   modes   error mode objects, used with renderers to draw icon
     */
    public MultiPointConfigKey( ConfigMeta meta, ErrorRenderer[] renderers,
                                ErrorMode[] modes ) {
        super( meta, ErrorRenderer.class, renderers,
               chooseDefault( renderers )  );
        renderers_ = renderers;
        modes_ = modes;
    }

    public String getXmlDescription( ErrorRenderer renderer ) {
        return null;
    }

    /**
     * Returns the error mode array used which combines with a renderer to
     * work out how to paint an icon.
     *
     * @return  error mode array
     */
    public ErrorMode[] getErrorModes() {
        return modes_;
    }

    @Override
    public Specifier<ErrorRenderer> createSpecifier() {
        int naxis = modes_.length;
        ErrorModeSelection[] modeSelections = new ErrorModeSelection[ naxis ];
        for ( int ia = 0; ia < naxis; ia++ ) {
            final ErrorMode mode = modes_[ ia ];
            modeSelections[ ia ] = new ErrorModeSelection() {
                public ErrorMode getErrorMode() {
                    return mode;
                }
                public void addActionListener( ActionListener listener ) {
                }
                public void removeActionListener( ActionListener listener ) {
                }
            };
        }
        JComboBox rendererComboBox =
            MarkStyleSelectors.createErrorSelector( renderers_,
                                                    getDefaultValue(),
                                                    modeSelections );
        return new ComboBoxSpecifier<ErrorRenderer>( rendererComboBox ) {
            public String stringify( ErrorRenderer value ) {
                return valueToString( value );
            }
        };
    }

    /**
     * Selects a sensible default from a list of renderers.
     * It just picks the first non-blank one.
     *
     * @param  renderers  list of all options
     * @return  sensible default
     */
    private static ErrorRenderer chooseDefault( ErrorRenderer[] renderers ) {
        for ( int ir = 0; ir < renderers.length; ir++ ) {
            ErrorRenderer rend = renderers[ ir ];
            if ( rend != null && rend != ErrorRenderer.NONE ) {
                return rend;
            }
        }
        assert false;
        return ErrorRenderer.DEFAULT;
    }
}
