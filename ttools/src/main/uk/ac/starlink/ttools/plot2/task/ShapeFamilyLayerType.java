package uk.ac.starlink.ttools.plot2.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter.ShapeModePlotter;

/**
 * LayerType that represents a family of ShapeModePlotters.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2014
 */
public class ShapeFamilyLayerType implements LayerType {

    private final ShapeForm form_;
    private final List<ShapeModePlotter> plotters_;

    /** Base name of associated Shading parameter. */
    public static final String SHADING_PREFIX = "shading";

    /**
     * Constructor.
     *
     * @param  form  shape form
     * @param  plotters   list of plotters with the given Form;
     *                    this list may be adjusted during the life of the
     *                    object (with care)
     */
    public ShapeFamilyLayerType( ShapeForm form,
                                 List<ShapeModePlotter> plotters ) {
        form_ = form;
        plotters_ = plotters;
    }

    /**
     * Returns the fixed form associated with this layer type.
     *
     * @return  form
     */
    public ShapeForm getShapeForm() {
        return form_;
    }

    /**
     * Returns the family of plotters associated with this layer type.
     *
     * @return  plotters
     */
    public ShapeModePlotter[] getShapeModePlotters() {
        return plotters_.toArray( new ShapeModePlotter[ 0 ] );
    }

    public String getName() {
        return form_.getFormName();
    }

    public Parameter[] getAssociatedParameters( String suffix ) {
        return new Parameter[] { createShapeModeParameter( suffix ) };
    }

    public Plotter getPlotter( Environment env, String suffix )
            throws TaskException {
        ShapeMode mode = new ParameterFinder<Parameter<ShapeMode>>() {
            public Parameter<ShapeMode> createParameter( String sfix ) {
                return createShapeModeParameter( sfix );
            }
        }.getParameter( env, suffix )
         .objectValue( env );
        for ( ShapeModePlotter plotter : plotters_ ) {
            if ( plotter.getMode().equals( mode ) ) {
                return plotter;
            }
        }
        throw new TaskException( "Unknown mode " + mode );
    }

    public int getPositionCount() {
        return plotters_.size() > 0
             ? plotters_.get( 0 ).getCoordGroup().getPositionCount()
             : 0;
    }

    public Coord[] getExtraCoords() {
        List<Coord> coordList = new ArrayList<Coord>();
        if ( plotters_.size() > 0 ) {
            ShapeModePlotter plotter = plotters_.get( 0 );
            ShapeMode mode = plotter.getMode();
            coordList.addAll( Arrays.asList( plotter.getCoordGroup()
                                                    .getExtraCoords() ) );
            coordList.removeAll( Arrays.asList( mode.getExtraCoords() ) );
        }
        return coordList.toArray( new Coord[ 0 ] );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> keyList = new ArrayList<ConfigKey>();
        if ( plotters_.size() > 0 ) {
            ShapeModePlotter plotter = plotters_.get( 0 );
            ShapeMode mode = plotter.getMode();
            keyList.addAll( Arrays.asList( plotter.getStyleKeys() ) );
            keyList.removeAll( Arrays.asList( mode.getConfigKeys() ) );
        }
        return keyList.toArray( new ConfigKey[ 0 ] );
    }

    /**
     * Returns the ShapeMode parameter that is required alongside this
     * LayerType in order to determine the Plotter to use.
     *
     * @param   suffix   layer suffix
     * @return   shape mode choice parameter
     */
    public ChoiceParameter<ShapeMode>
            createShapeModeParameter( String suffix ) {
        ChoiceParameter<ShapeMode> param =
                new ChoiceParameter<ShapeMode>( SHADING_PREFIX + suffix,
                                                ShapeMode.class ) {
            @Override
            public String stringifyOption( ShapeMode mode ) {
                return mode.getModeName().toLowerCase();
            }
        };
        for ( ShapeModePlotter plotter : plotters_ ) {
            param.addOption( plotter.getMode() );
        }
        param.setNullPermitted( false );
        param.setDefaultOption( param.getOptions()[ 0 ] );
        param.setPrompt( "Colouring policy" );
        StringBuffer sbuf = new StringBuffer();
        for ( ShapeMode mode : param.getOptions() ) {
            String mname = mode.getModeName();
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( "<ref id='shading-" )
                .append( mname )
                .append( "' plaintextref='yes'>" )
                .append( mname )
                .append( "</ref>" )
                .append( "</code>" )
                .append( "</li>\n" );
        }
        String items = sbuf.toString();
        param.setDescription( new String[] {
            "<p>Determines how plotted objects are coloured.",
            "Available options are:",
            "<ul>",
            items,
            "</ul>",
            "</p>",
        } );
        return param;
    }
}
