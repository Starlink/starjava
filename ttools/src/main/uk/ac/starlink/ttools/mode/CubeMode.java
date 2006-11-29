package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.task.WordParser;
import uk.ac.starlink.ttools.task.WordsParameter;
import uk.ac.starlink.util.Destination;

/**
 * Output mode for generating an N-dimenaional histogram of data points
 * from a selection of N columns of the input table.
 *
 * @author   Mark Taylor
 * @since    9 May 2006
 */
public class CubeMode implements ProcessingMode {

    private final WordsParameter boundsParam_;
    private final WordsParameter binsizeParam_;
    private final WordsParameter nbinParam_;
    private final OutputStreamParameter outParam_;
    private final ChoiceParameter typeParam_;
    private final Parameter scaleParam_;
    private WordsParameter colsParam_;

    /** Output data types for FITS output. */
    private static final Class[] OUT_TYPES = new Class[] {
        byte.class, short.class, int.class, long.class,
        float.class, double.class,
    };

    /**
     * Constructor.
     */
    public CubeMode() {
        boundsParam_ = new WordsParameter( "bounds" );
        boundsParam_.setNullPermitted( true );
        boundsParam_.setDefault( null );
        boundsParam_.setWordParser( new BoundsParser() );
        boundsParam_.setWordUsage( "[<lo>]:[<hi>]" );
        boundsParam_.setPrompt( "Data bounds for each dimension" );
        boundsParam_.setDescription( new String[] {
           "<p>Gives the bounds for each dimension of the cube in data",
           "coordinates.  The form of the value is a space-separated list",
           "of words, each giving an optional lower bound, then a colon,",
           "then an optional upper bound, for instance",
           "\"1:100 0:20\" to represent a range for two-dimensional output",
           "between 1 and 100 of the first coordinate (table column)",
           "and between 0 and 20 for the second.",
           "Either or both numbers may be omitted to indicate that the",
           "bounds should be determined automatically by assessing the",
           "range of the data in the table.",
           "A null value for the parameter indicates that all bounds should",
           "be determined automatically for all the dimensions.",
           "</p>",
           "<p>If any of the bounds need to be determined automatically",
           "in this way, two passes through the data will be required,",
           "the first to determine bounds and the second",
           "to populate the cube.",
           "</p>",
        } );

        binsizeParam_ = new WordsParameter( "binsizes" );
        binsizeParam_.setWordParser( new DoubleParser() );
        binsizeParam_.setWordUsage( "<size>" );
        binsizeParam_.setPrompt( "Extent of bins in each dimension" );
        binsizeParam_.setDescription( new String[] {
            "<p>Gives the extent of of the data bins (cube pixels) in each",
            "dimension in data coordinates.",
            "The form of the value is a space-separated list of values,",
            "giving a list of extents for the first, second, ... dimension.",
            "Either this parameter or the <code>nbins</code> parameter",
            "must be supplied.",
            "</p>",
        } );
    
        nbinParam_ = new WordsParameter( "nbins" );
        nbinParam_.setWordParser( new IntegerParser() );
        nbinParam_.setWordUsage( "<num>" );
        nbinParam_.setNullPermitted( true );
        nbinParam_.setPrompt( "Number of bins in each dimension" );
        nbinParam_.setDescription( new String[] {
            "<p>Gives the number of bins (cube pixels) in each dimension.",
            "The form of the value is a space-separated list of integers,",
            "giving the number of pixels for the output cube in the",
            "first, second, ... dimension.",
            "Either this parameter or the <code>binsizes</code> parameter",
            "must be supplied.",
            "</p>",
        } );

        outParam_ = new OutputStreamParameter( "out" );
        outParam_.setPreferExplicit( true );
        outParam_.setPrompt( "Location of output FITS file" );
        outParam_.setDescription( new String[] {
            outParam_.getDescription(),
            "<p>The output cube is currently written as",
            "a single-HDU FITS file.",
            "</p>",
        } );

        typeParam_ = new ChoiceParameter( "otype", OUT_TYPES );
        typeParam_.setNullPermitted( true );
        typeParam_.setDefault( null );
        typeParam_.setPrompt( "Type of output array elements" );
        typeParam_.setDescription( new String[] {
            "<p>The type of numeric value which will fill the output array.",
            "If no selection is made, the output type will be",
            "determined automatically as the shortest type required to hold",
            "all the values in the array.",
            "Currently, integers are always signed (no BSCALE/BZERO),",
            "so for instance the largest value that can be recorded",
            "in 8 bits is 127.",
            "</p>",
        } );

        scaleParam_ = new Parameter( "scale" );
        scaleParam_.setUsage( "<col-id>" );
        scaleParam_.setNullPermitted( true );
        scaleParam_.setDefault( null );
        scaleParam_.setPrompt( "Value by which to scale counts" );
        scaleParam_.setDescription( new String[] {
            "<p>Optionally gives a value by which the count in each bin is",
            "scaled.",
            "If this value is <code>null</code> (the default) then for each",
            "row that falls within the bounds of a pixel, the pixel value",
            "will be incremented by 1.",
            "If a column ID is given, then instead of 1 being added,",
            "the value of that column for the row in question is added.",
            "The effect of this is that the output image contains the mean",
            "of the given column for the rows corresponding to each pixel",
            "rather than just a count of them.",
            "</p>",
        } );
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
           "<p>Makes an N-dimensional histogram of the columns in the",
           "input table.",
           "The result is an N-dimensional array which is output as a",
           "FITS file.",
           "</p>",
        } );
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            boundsParam_,
            binsizeParam_,
            nbinParam_,
            outParam_,
            typeParam_,
            scaleParam_,
        };
    }

    public TableConsumer createConsumer( Environment env )
            throws TaskException {

        /* Determine the dimensionality of the output cube (which is the 
         * number of columns in the input table) and configure the 
         * multiplicity of some of the other parameters accordingly. */
        final String[] colIds = colsParam_.wordsValue( env );
        final int ndim = colIds.length;
        boundsParam_.setRequiredWordCount( ndim );
        binsizeParam_.setRequiredWordCount( ndim );
        nbinParam_.setRequiredWordCount( ndim );

        /* Get the scale column ID. */
        final String scaleId = scaleParam_.stringValue( env );

        /* Get the explicitly specified bounds for the output grid. */
        Object[] boundsWords = boundsParam_.parsedWordsValue( env );
        final double[] loBounds = new double[ ndim ];
        final double[] hiBounds = new double[ ndim ];
        if ( boundsWords != null ) {
            for ( int i = 0; i < ndim; i++ ) {
                double[] bounds = (double[]) boundsWords[ i ];
                loBounds[ i ] = bounds[ 0 ];
                hiBounds[ i ] = bounds[ 1 ];
            }
        }
        else {
            Arrays.fill( loBounds, Double.NaN );
            Arrays.fill( hiBounds, Double.NaN );
        }

        /* Get either the number of bins or the extent of each pixel in 
         * each dimension.  If you have one, and the bounds, you can work
         * out the other. */
        Object[] nbinWords = nbinParam_.parsedWordsValue( env );
        final int[] nbins;
        final double[] binsizes;
        if ( nbinWords != null ) {
            binsizeParam_.setNullPermitted( true );
            binsizeParam_.setValueFromString( env, null );
            nbins = new int[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                nbins[ i ] = ((Integer) nbinWords[ i ]).intValue();
                if ( nbins[ i ] <= 0 ) {
                    throw new ParameterValueException( nbinParam_,
                                                       "Non-positive value" );
                }
            }
            binsizes = null;
        }
        else {
            binsizeParam_.setNullPermitted( false );
            Object[] binsizeWords = binsizeParam_.parsedWordsValue( env );
            binsizes = new double[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                binsizes[ i ] = ((Double) binsizeWords[ i ]).doubleValue();
                if ( ! ( binsizes[ i ] > 0 ) ) {
                    throw new ParameterValueException( binsizeParam_,
                                                       "Non-positive value" );
                }
            }
            nbins = null;
        }

        /* Get the destination. */
        Destination dest = outParam_.destinationValue( env );

        /* Get the output datatype size. */
        Class outType = (Class) typeParam_.objectValue( env );

        /* Construct and return the consumer itself. */
        return new CubeWriter( loBounds, hiBounds, nbins, binsizes, colIds,
                               scaleId, dest, outType );
    }

    /**
     * Configures the parameter which acquires the columns used.
     * Since this is used to determine the dimensionality of the cube,
     * it has to be set before the values of this mode's parameters are
     * acquired from the environment (before {@link #createConsumer} is
     * called).
     *
     * @param  colsParam  column enumeration parameter
     */
    public void setColumnsParameter( WordsParameter colsParam ) {
        colsParam_ = colsParam;
    }

    /**
     * WordParser implementation for decoding "lo:hi"-type bounds strings.
     */
    private static class BoundsParser implements WordParser {
        final Pattern boundsRegex_ = Pattern.compile( "(.*):(.*)" );
        public Object parseWord( String word ) throws TaskException {
            Matcher matcher = boundsRegex_.matcher( word );
            if ( matcher.matches() ) {
                double[] bounds = new double[] { Double.NaN, Double.NaN };
                for ( int i = 0; i < 2; i++ ) {
                    String sval = matcher.group( i + 1 ).trim();
                    if ( sval.length() > 0 ) {
                        try {
                            bounds[ i ] = Double.parseDouble( sval );
                        }
                        catch ( NumberFormatException e ) {
                            throw new UsageException( "Bad bound string \""
                                                    + sval + "\"", e );
                        }
                    }
                }
                if ( bounds[ 0 ] >= bounds[ 1 ] ) {
                    throw new UsageException( "Bad bound string \""
                                            + word + "\": lo>=hi" );
                }
                return bounds;
            }
            else {
                throw new UsageException( "Bad <lo>:<hi> bounds string" );
            }
        }
    }

    /**
     * WordParser implementation for decoding floating point numbers.
     */
    private static class DoubleParser implements WordParser {
        public Object parseWord( String word ) throws TaskException {
            try {
                return new Double( Double.parseDouble( word ) );
            }
            catch ( NumberFormatException e ) {
                throw new UsageException( "Bad number format", e );
            }
        }
    }

    /**
     * WordParser implementation for decoding integers.
     */
    private static class IntegerParser implements WordParser {
        public Object parseWord( String word ) throws TaskException {
            try {
                return new Integer( Integer.parseInt( word ) );
            }
            catch ( NumberFormatException e ) {
                throw new UsageException( "Bad integer format", e );
            }
        }
    }
}
