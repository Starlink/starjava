package uk.ac.starlink.ttools.plottask;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.MarkStyles;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.StyleSet;

/**
 * StyleFactory for obtaining {@link uk.ac.starlink.tplot.MarkStyle} instances
 * suitable for use with a scatter plot.
 *
 * @author   Mark Taylor
 * @since    8 Aug 2008
 */
public class MarkStyleFactory extends StyleFactory {

    private final int errNdim_;
    private final StyleSet styleSet_ = MarkStyles.spots( "Spots", 2 );

    /** Known shapes. */
    private static final MarkShape[] SHAPES = new MarkShape[] {
        MarkShape.FILLED_CIRCLE,
        MarkShape.OPEN_CIRCLE,
        MarkShape.CROSS,
        MarkShape.CROXX,
        MarkShape.OPEN_SQUARE,
        MarkShape.OPEN_DIAMOND,
        MarkShape.OPEN_TRIANGLE_UP,
        MarkShape.OPEN_TRIANGLE_DOWN,
        MarkShape.FILLED_SQUARE,
        MarkShape.FILLED_DIAMOND,
        MarkShape.FILLED_TRIANGLE_UP,
        MarkShape.FILLED_TRIANGLE_DOWN,
    };

    /**
     * Constructor.
     *
     * @param   prefix   prefix to be prepended to all parameters used by this
     *                   factory
     * @param   errNdim  number of dimensions in which errors may be displayed
     */
    public MarkStyleFactory( String prefix, int errNdim ) {
        super( prefix );
        errNdim_ = errNdim;
    }

    public Parameter[] getParameters( String stSuffix ) {
        List paramList = new ArrayList();
        paramList.add( createColorParameter( stSuffix ) );
        paramList.add( createShapeParameter( stSuffix ) );
        paramList.add( createSizeParameter( stSuffix ) );
        paramList.add( createTransparencyParameter( stSuffix ) );
        if ( errNdim_ > 0 ) {
            paramList.add( createErrorRendererParameter( stSuffix ) );
        }
        return (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
    }

    /**
     * Returns a {@link uk.ac.starlink.tplot.MarkStyle} instance.
     */
    public Style getStyle( Environment env, String stSuffix )
            throws TaskException {

        /* Get the default style for this suffix. */
        MarkStyle style0 =
            (MarkStyle) styleSet_.getStyle( getStyleIndex( stSuffix ) );

        /* Marker shape. */
        ChoiceParameter shapeParam = createShapeParameter( stSuffix );
        shapeParam.setDefaultOption( style0.getShapeId() );
        MarkShape shape = (MarkShape) shapeParam.objectValue( env );

        /* Colour. */
        ColorParameter colorParam = createColorParameter( stSuffix );
        colorParam.setDefaultColor( style0.getColor() );
        Color color = colorParam.colorValue( env );

        /* Marker size. */
        IntegerParameter sizeParam = createSizeParameter( stSuffix );
        int size = sizeParam.intValue( env );

        /* Construct a basic MarkStyle object with the correct 
         * characteristics. */
        MarkStyle style = shape.getStyle( color, size );

        /* Configure transparency (opacity limit). */
        IntegerParameter transparParam =
            createTransparencyParameter( stSuffix );
        transparParam.setDefault( Integer.toString( style0.getOpaqueLimit() ) );
        style.setOpaqueLimit( transparParam.intValue( env ) );

        /* Configure error renderer if appropriate. */
        if ( errNdim_ > 0 ) {
            ChoiceParameter errParam = createErrorRendererParameter( stSuffix );
            errParam.setDefaultOption( style.getErrorRenderer() );
            ErrorRenderer errRend = (ErrorRenderer) errParam.objectValue( env );
            style.setErrorRenderer( errRend == null ? ErrorRenderer.NONE
                                                    : errRend );
        }

        /* Return configured style. */
        return style;
    }

    /**
     * Constructs a parameter for determinining style colour.
     *
     * @param  stSuffix  label identifying dataset
     * @return  new colour parameter
     */
    private ColorParameter createColorParameter( String stSuffix ) {
        ColorParameter param =
            new ColorParameter( paramName( "colour", stSuffix ) );
        param.setPrompt( "Marker colour for data set " + stSuffix );
        param.setDescription( new String[] {
            "<p>Defines the colour of markers plotted.",
            param.getFormatDescription(),
            "</p>",
        } );
        return param;
    }

    /**
     * Constructs a parameter for determining marker size in pixels.
     *
     * @param  stSuffix  label identifying dataset
     * @return  new size parameter
     */
    private IntegerParameter createSizeParameter( String stSuffix ) {
        IntegerParameter param =
            new IntegerParameter( paramName( "size", stSuffix ) );
        param.setPrompt( "Marker size in pixels for data set " + stSuffix );
        param.setDefault( Integer.toString( -1 ) );
        param.setDescription( new String[] {
            "<p>Defines the marker size in pixels for markers plotted in",
            "data set " + stSuffix + ".",
            "If the value is negative, an attempt will be made to use a",
            "suitable size according to how many points there are to be",
            "plotted.",
            "</p>",
        } );
        return param;
    }

    /**
     * Constructs a parameter for determining marker shape.
     *
     * @param   stSuffix  label identifying dataset
     * @return  parameter which returns MarkShapes
     */
    private ChoiceParameter createShapeParameter( String stSuffix ) {
        StyleParameter param =
            new StyleParameter( paramName( "shape", stSuffix ), SHAPES );
        param.setPrompt( "Marker shape for data set " + stSuffix );
        param.setDescription( new String[] {
            "<p>Defines the shapes for the markers that are plotted in",
            "data set " + stSuffix + ".",
            "The following shapes are available:",
            param.getOptionList(),
            "</p>",
        } );
        return param;
    }

    /**
     * Constructs a parameter for determining error rendering style.
     *
     * @param  stSuffix  label identifying dataset
     * @return  parameter giving error renderer
     */
    private ChoiceParameter createErrorRendererParameter( String stSuffix ) {
        final ErrorRenderer[] options;
        if ( errNdim_ == 2 ) {
            options = ErrorRenderer.getOptions2d();
        }
        else if ( errNdim_ == 3 ) {
            options = ErrorRenderer.getOptions3d();
        }
        else {
            options = ErrorRenderer.getOptionsGeneral();
        }
        StyleParameter param =
            new StyleParameter( paramName( "errstyle", stSuffix ), options );
        param.setPrompt( "Error bar style for data set " + stSuffix );
        param.setDescription( new String[] {
            "<p>Defines the way in which error bars (or ellipses, or...)",
            "will be represented for data set " + stSuffix,
            "if errors are being displayed.",
            "The following options are available:",
            param.getOptionList(),
            "</p>",
        } );
        param.setNullPermitted( true );

        /* Prepare custom usage list. */
        if ( Arrays.asList( options ).contains( ErrorRenderer.DEFAULT ) &&
             Arrays.asList( options ).contains( ErrorRenderer.EXAMPLE ) ) {
            param.setUsage( param.getName( ErrorRenderer.DEFAULT ) + "|"
                          + param.getName( ErrorRenderer.EXAMPLE ) + "|"
                          + "..." );
            param.setDefaultOption( ErrorRenderer.DEFAULT );
        }
        else {
            assert false;
            param.setDefaultOption( options[ 1 ] );
        }
        return param;
    }

    /**
     * Constructs a parameter for determining style opacity limit.
     *
     * @param  stSuffix  label identifying dataset
     * @return   parameter returning opacity limit
     */
    private IntegerParameter createTransparencyParameter( String stSuffix ) {
        IntegerParameter param =
            new IntegerParameter( paramName( "transparency", stSuffix ) );
        param.setMinimum( 1 );
        param.setPrompt( "Transparency for markers plotted for data set "
                       + stSuffix );
        param.setDescription( new String[] {
            "<p>Determines the transparency of plotted markers",
            "for data set " + stSuffix + ".",
            "A value of <code>&lt;n&gt;</code> means that opacity is only",
            "achieved (the background is only blotted out)",
            "when <code>&lt;n&gt;</code> pixels of this colour have been",
            "plotted on top of each other.",
            "</p>",
            "<p>The minimum value is 1, which means opaque markers.",
            "</p>",
        } );
        return param;
    }
}
