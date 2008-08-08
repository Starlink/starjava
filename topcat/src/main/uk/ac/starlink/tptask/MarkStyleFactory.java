package uk.ac.starlink.tptask;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.tplot.MarkShape;
import uk.ac.starlink.tplot.MarkStyle;
import uk.ac.starlink.tplot.MarkStyles;
import uk.ac.starlink.tplot.Style;
import uk.ac.starlink.tplot.StyleSet;

/**
 * StyleFactory implementation for obtaining 
 * {@link uk.ac.starlink.tplot.MarkStyle} instances.
 *
 * @author   Mark Taylor
 * @since    8 Aug 2008
 */
public class MarkStyleFactory implements StyleFactory {

    private final String prefix_;
    private final StyleSet styleSet_ = MarkStyles.spots( "Spots", 2 );
    private final List suffixList_;

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
     */
    public MarkStyleFactory( String prefix ) {
        prefix_ = prefix;
        suffixList_ = new ArrayList();
    }

    public Parameter[] getParameters( String stSuffix ) {
        return new Parameter[] {
            createColorParameter( stSuffix ),
            createShapeParameter( stSuffix ),
            createSizeParameter( stSuffix ),
            createTransparencyParameter( stSuffix ),
        };
    }

    /**
     * Returns a {@link uk.ac.starlink.tplot.MarkStyle} instance.
     */
    public Style getStyle( Environment env, String stSuffix )
            throws TaskException {

        /* Get the default style for this suffix. */
        if ( ! suffixList_.contains( stSuffix ) ) {
            suffixList_.add( stSuffix );
        }
        int iSet = suffixList_.indexOf( stSuffix );
        MarkStyle style0 = (MarkStyle) styleSet_.getStyle( iSet );

        /* Marker shape. */
        ChoiceParameter shapeParam = createShapeParameter( stSuffix );
        shapeParam.setDefaultOption( style0.getShapeId() );
        MarkShape shape = (MarkShape) shapeParam.objectValue( env );

        /* Colour. */
        ColorParameter colorParam = createColorParameter( stSuffix );
        colorParam.setDefault( ColorParameter
                              .getStringValue( style0.getColor() ) );
        Color color = colorParam.colorValue( env );

        /* Marker size. */
        IntegerParameter sizeParam = createSizeParameter( stSuffix );
        int size = sizeParam.intValue( env );

        /* Transparency (opacity limit). */
        IntegerParameter transparParam =
            createTransparencyParameter( stSuffix );
        transparParam.setDefault( Integer.toString( style0.getOpaqueLimit() ) );
        int opaqueLimit = transparParam.intValue( env );

        /* Construct, configure and return MarkStyle object. */
        MarkStyle style = shape.getStyle( color, size );
        style.setOpaqueLimit( opaqueLimit );
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

    /**
     * Assembles a parameter name from a base name and a dataset suffix.
     *
     * @param  baseName  parameter base name
     * @param  stSuffix  label identifying dataset
     * @return  parameter name
     */
    private String paramName( String baseName, String stSuffix ) {
        return prefix_ + baseName + stSuffix;
    }
}
