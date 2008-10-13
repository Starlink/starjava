package uk.ac.starlink.ttools.plottask;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.BarStyles;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.StyleSet;

/**
 * StyleFactory for BarStyle objects suitable for use with a histogram.
 *
 * @author   Mark Taylor
 * @since    14 Aug 2008
 */
public class BarStyleFactory extends StyleFactory {

    private final StyleSet styleSet_ = BarStyles.sideFilled( "Filled" );

    /**
     * Constructor.
     *
     * @param  prefix  prefix to be prepended to all parameters used by this
     *                 factory
     */
    public BarStyleFactory( String prefix ) {
        super( prefix );
    }

    public Parameter[] getParameters( String stSuffix ) {
        return new Parameter[] {
            createColorParameter( stSuffix ),
            createShapeParameter( stSuffix ),
            createLineWidthParameter( stSuffix ),
            createDashParameter( stSuffix ),
        };
    }

    public Style getStyle( Environment env, String stSuffix )
            throws TaskException {

        /* Get the default style for this suffix. */
        BarStyle style0 =
            (BarStyle) styleSet_.getStyle( getStyleIndex( stSuffix ) );

        /* Bar shape. */
        ChoiceParameter shapeParam = createShapeParameter( stSuffix );
        shapeParam.setDefaultOption( new BarShape( style0.getForm(),
                                                   style0.getPlacement() ) );
        BarShape shape = (BarShape) shapeParam.objectValue( env );

        /* Colour. */
        ColorParameter colorParam = createColorParameter( stSuffix );
        colorParam.setDefaultColor( style0.getColor() );
        Color color = colorParam.colorValue( env );

        /* Construct a basic BarStyle object with the correct 
         * characteristics. */
        BarStyle style = new BarStyle( color, shape.form_, shape.placement_ );

        /* Configure line width. */
        IntegerParameter lineParam = createLineWidthParameter( stSuffix );
        lineParam.setDefault( Integer.toString( style0.getLineWidth() ) );
        style.setLineWidth( lineParam.intValue( env ) );

        /* Configure dash. */
        DashParameter dashParam = createDashParameter( stSuffix );
        dashParam.setDefaultOption( style0.getDash() );
        style.setDash( dashParam.dashValue( env ) );

        /* Return configured style. */
        return style;
    }

    /**
     * Constructs a parameter for determining style colour.
     *
     * @param  stSuffix  label identifying dataset
     * @return  new colour parameter
     */
    private ColorParameter createColorParameter( String stSuffix ) {
        ColorParameter param =
            new ColorParameter( paramName( "colour", stSuffix ) );
        param.setPrompt( "Bar colour for data set " + stSuffix );
        param.setDescription( new String[] {
            "<p>Defines the colour of bars plotted for data set",
            stSuffix + ".",
            param.getFormatDescription(),
            "</p>",
            "<p>For most purposes, either the American or the British spelling",
            "is accepted for this parameter name.",
            "</p>",
        } );
        return param;
    }

    /**
     * Constructs a parameter for determining bar form (shape).
     * This combines both Form and Placement, since some combinations
     * of the two don't work very well.
     *
     * @param  stSuffix  label identifying dataset
     * @return  new bar form parameter
     */
    private ChoiceParameter createShapeParameter( String stSuffix ) {
        StyleParameter param =
            new StyleParameter( paramName( "barstyle", stSuffix ) );
        param.addOption( new BarShape( BarStyle.FORM_FILLED,
                                       BarStyle.PLACE_ADJACENT ), "fill" );
        param.addOption( new BarShape( BarStyle.FORM_OPEN,
                                       BarStyle.PLACE_ADJACENT ), "open" );
        param.addOption( new BarShape( BarStyle.FORM_TOP,
                                       BarStyle.PLACE_OVER ), "tops" );
        param.addOption( new BarShape( BarStyle.FORM_SPIKE,
                                       BarStyle.PLACE_ADJACENT ), "spikes" );
        param.addOption( new BarShape( BarStyle.FORM_FILLED,
                                       BarStyle.PLACE_OVER ), "fillover" );
        param.addOption( new BarShape( BarStyle.FORM_OPEN,
                                       BarStyle.PLACE_OVER ), "openover" );
        param.setDefault( "fill" );
        param.setPrompt( "Histogram bar style for dataset " + stSuffix );
        param.setDescription( new String[] {
            "<p>Defines how histogram bars will be drawn for dataset",
            stSuffix + ".",
            "The options are:",
            param.getOptionList(),
            "</p>",
        } );
        return param;
    }

    /**
     * Constructs a parameter for determining the line width which may have
     * an effect on bar drawing.
     *
     * @param  stSuffix  label identifying dataset
     * @return   new line width parameter
     */
    private IntegerParameter createLineWidthParameter( String stSuffix ) {
        IntegerParameter param =
            new IntegerParameter( paramName( "linewidth", stSuffix ) );
        param.setPrompt( "Line width for lines in dataset " + stSuffix );
        param.setDescription( new String[] {
            "<p>Defines the line width for lines drawn as part of the bars",
            "for dataset " + stSuffix + ".",
            "Only certain bar styles are affected by the line width.",
            "</p>",
        } );
        param.setDefault( Integer.toString( 2 ) );
        param.setMinimum( 1 );
        return param;
    }

    /**
     * Constructs a parameter for selecting line dash pattern which may have
     * an effect on bar drawing.
     *
     * @param  stSuffix  label identifying dataset
     * @return  new dash parameter
     */
    private DashParameter createDashParameter( String stSuffix ) {
        DashParameter param =
            new DashParameter( paramName( "dash", stSuffix ) );
        param.setPrompt( "Dash pattern for lines in dataset " + stSuffix );
        param.setDescription( new String[] {
            "<p>Defines the dashing pattern for lines drawn for dataset",
            stSuffix + ".",
            param.getFormatDescription(),
            "Only certain bar styles are affected by the dash pattern.",
            "</p>",
        } );
        return param;
    }

    /**
     * Aggregates bar style Form and Placement.
     * The <code>equals</code> method is implemented intelligently.
     */
    private static class BarShape {
        final BarStyle.Form form_;
        final BarStyle.Placement placement_;

        /**
         * Constructor.
         *
         * @param  form  bar form
         * @param  placement  bar placement
         */
        BarShape( BarStyle.Form form, BarStyle.Placement placement ) {
            form_ = form;
            placement_ = placement;
        }

        public int hashCode() {
            return form_.hashCode() + 100 * placement_.hashCode();
        }

        public boolean equals( Object o ) {
            if ( o instanceof BarShape ) {
                BarShape other = (BarShape) o;
                return this.form_.equals( other.form_ )
                    && this.placement_.equals( other.placement_ );
            }
            else {
                return false;
            }
        }
    }
}
