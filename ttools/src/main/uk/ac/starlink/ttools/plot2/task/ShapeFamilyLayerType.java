package uk.ac.starlink.ttools.plot2.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapeModePlotter;
import uk.ac.starlink.ttools.task.ExtraParameter;
import uk.ac.starlink.ttools.task.TableEnvironment;

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

    public String getXmlDescription() {
        return form_.getFormDescription();
    }

    public Parameter<?>[] getAssociatedParameters( String suffix ) {
        return new Parameter<?>[] { createShapeModeParameter( suffix ) };
    }

    public Plotter<?> getPlotter( Environment env, String suffix )
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
             ? plotters_.get( 0 ).getCoordGroup().getBasicPositionCount()
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

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> keyList = new ArrayList<ConfigKey<?>>();
        if ( plotters_.size() > 0 ) {
            ShapeModePlotter plotter = plotters_.get( 0 );
            ShapeMode mode = plotter.getMode();
            keyList.addAll( Arrays.asList( plotter.getStyleKeys() ) );
            keyList.removeAll( Arrays.asList( mode.getConfigKeys() ) );
        }
        return keyList.toArray( new ConfigKey<?>[ 0 ] );
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
        int nmode = plotters_.size();
        ShapeMode[] modes = new ShapeMode[ nmode ];
        for ( int im = 0; im < nmode; im++ ) {
            modes[ im ] = plotters_.get( im ).getMode();
        }
        ShapeModeParameter param =
            new ShapeModeParameter( SHADING_PREFIX, suffix, modes );
        param.setNullPermitted( false );
        param.setDefaultOption( param.getOptions()[ 0 ] );
        return param;
    }

    /**
     * Parameter for selecting a shading option.
     */
    private static class ShapeModeParameter extends ChoiceParameter<ShapeMode>
                                            implements ExtraParameter {

        private final String suffix_;

        /**
         * Constructor.
         *
         * @param  prefix  body of parameter name
         * @param  suffix  layer suffix
         * @param  modes   available value options
         */
        public ShapeModeParameter( String prefix, String suffix,
                                   ShapeMode[] modes ) {
            super( prefix + suffix, ShapeMode.class, modes );
            suffix_ = suffix;
            setPrompt( "Colouring policy" );
            setUsage( super.getUsage() + " <shade-params" + suffix + ">" );
            StringBuffer sbuf = new StringBuffer();
            for ( ShapeMode mode : modes ) {
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
            setDescription( new String[] {
                "<p>Determines how plotted objects in layer " + suffix,
                "are coloured.",
                "This may be influenced by how many objects are plotted",
                "over each other as well as the values of other parameters.",
                "Available options (<ref id='ShapeMode'/>) are:",
                "<ul>",
                items,
                "</ul>",
                "Each of these options comes with its own set of parameters",
                "to specify the details of how colouring is done.",
                "</p>",
            } );
        }

        @Override
        public String stringifyOption( ShapeMode mode ) {
            return mode.getModeName().toLowerCase();
        }

        public String getExtraUsage( TableEnvironment env ) {
            StringBuffer sbuf = new StringBuffer()
                .append( "\n   " )
                .append( "Available shading types, " )
                .append ( "with associated parmeters:\n" );
            for ( ShapeMode mode : getOptions() ) {
                List<String> modeWords = new ArrayList<String>();
                modeWords.add( getName() + "=" + mode.getModeName() );
                Parameter<?>[] coordParams =
                    LayerTypeParameter
                   .getCoordParams( mode.getExtraCoords(), suffix_, false );
                Parameter<?>[] configParams =
                    LayerTypeParameter
                   .getLayerConfigParams( mode.getConfigKeys(),
                                          suffix_, false );
                modeWords.addAll( LayerTypeParameter
                                 .usageWords( coordParams ) );
                modeWords.addAll( LayerTypeParameter
                                 .usageWords( configParams ) );
                sbuf.append( Formatter.formatWords( modeWords, 6 ) );
            }
            return sbuf.toString();
        }
    }
}
