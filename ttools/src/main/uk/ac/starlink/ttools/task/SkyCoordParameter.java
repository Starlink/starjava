package uk.ac.starlink.ttools.task;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.StringParameter;

/**
 * Utilities for sky coordinate parameters.
 *
 * @author   Mark Taylor
 * @since    15 May 2014
 */
public class SkyCoordParameter extends StringParameter {

    private static final Pattern RA_REGEX =
        Pattern.compile( "RA_?J?(2000)?", Pattern.CASE_INSENSITIVE );
    private static final Pattern DEC_REGEX =
        Pattern.compile( "DEC?L?_?J?(2000)?", Pattern.CASE_INSENSITIVE );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     *
     * @param  name  parameter name
     * @param  coordName  text name for the coordinate, full word, capitalised,
     *                    for instance "Right ascension"
     * @param  coordSys   name of coordinate system, or null
     * @param  tableDescrip  text denoting table to which the coordinate refers,
     *                       for instance "the input table"
     */
    @SuppressWarnings("this-escape")
    public SkyCoordParameter( String name, String coordName, String coordSys,
                              String tableDescrip ) {
        super( name );
        setUsage( "<expr>" );
        String prompt = coordName + " expression in degrees";
        if ( coordSys != null ) {
            prompt += " (" + coordSys + ")";
        }
        setPrompt( prompt );
        setNullPermitted( true );
        String inSys = coordSys == null
                     ? ""
                     : ( " in the " + coordSys + " coordinate system" );
        setDescription( new String[] {
            "<p>" + coordName + " in degrees" + inSys,
            "for the position of each row of " + tableDescrip + ".",
            "This may simply be a column name, or it may be an",
            "algebraic expression calculated from columns as explained",
            "in <ref id='jel'/>.",
            "If left blank, an attempt is made to guess from UCDs,",
            "column names and unit annotations what expression to use.",
            "</p>",
        } );
    }

    /**
     * Utility method to create a parameter representing Right Ascension.
     *
     * @param   paramName  parameter name
     * @param  coordSys   name of coordinate system, or null
     * @param  tableDescrip  text denoting table to which the coordinate refers,
     *                       for instance "the input table"
     * @return  new RA parameter
     */
    public static StringParameter createRaParameter( String paramName,
                                                     String coordSys,
                                                     String tableDescrip ) {
        return new SkyCoordParameter( paramName, "Right ascension", coordSys,
                                      tableDescrip );
    }

    /**
     * Utility method to create a parameter representing Declination.
     *
     * @param   paramName  parameter name
     * @param  coordSys   name of coordinate system, or null
     * @param  tableDescrip  text denoting table to which the coordinate refers,
     *                       for instance "the input table"
     * @return  new declination parameter
     */
    public static StringParameter createDecParameter( String paramName,
                                                      String coordSys,
                                                      String tableDescrip ) {
        return new SkyCoordParameter( paramName, "Declination", coordSys,
                                      tableDescrip );
    }

    /**
     * Looks at table columns and makes a guess at a JEL expression which
     * will give Right Ascension in degrees.
     *
     * @param  inTable  table 
     * @return   expression for RA in degrees, or null if none can be found
     */
    public static String guessRaDegreesExpression( StarTable inTable ) {
        return guessDegreesExpression( inTable, "ra", RA_REGEX );
    }

    /**
     * Looks at table columns and makes a guess at a JEL expression which
     * will give Declination in degrees.
     *
     * @param  inTable  table
     * @return   expression for Dec in degrees, or null if none can be found
     */
    public static String guessDecDegreesExpression( StarTable inTable ) {
        return guessDegreesExpression( inTable, "dec", DEC_REGEX );
    }

    /**
     * Looks at table columns and makes a guess at a JEL expression which
     * will give RA/Dec values in degrees.
     *
     * @param  inTable  input table
     * @param  ucdAtom  UCD atom describing value "ra" or "dec"
     * @param  nameRegex  regular expression for matching a column name
     *                    giving the quantity (not necessarily in degrees)
     * @return  JEL expression giving angle in degrees, or null if none
     *          can be found
     */
    private static String guessDegreesExpression( StarTable inTable,
                                                  String ucdAtom,
                                                  Pattern nameRegex ) {

        /* Prepare possible matching UCD1 and UCD1+ strings. */
        String atom = ucdAtom.toLowerCase();
        final String ucd1Part = "pos_eq_" + atom;
        final String ucd1Full = ucd1Part + "_main";
        final String ucd1pPart = "pos.eq." + atom;
        final String ucd1pFull = ucd1pPart + ";meta.main";

        /* Examine each column, assigning a score to columns that look like
         * they might be what we're after.  The best score is retained. */
        int bestIndex = -1;
        int score = 0;
        int ncol = inTable.getColumnCount();
        for ( int i = 0; i < ncol; i++ ) {
            ColumnInfo info = inTable.getColumnInfo( i );
            if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                String ucd = info.getUCD();
                String name = info.getName();
                if ( ucd != null && ucd.length() > 0 ) {
                    ucd = ucd.trim().toLowerCase();

                    /* Full UCD match. */
                    if ( score < 20 &&
                         ( ucd.equals( ucd1Full ) ||
                           ucd.equals( ucd1pFull ) ) ) {
                        bestIndex = i;
                        score = 20;
                    }

                    /* Partial UCD match. */
                    if ( score < 10 &&
                         ( ucd.equals( ucd1Part ) ||
                           ucd.equals( ucd1pPart ) ) ) {
                        bestIndex = i;
                        score = 10;
                    }
                }
                if ( name != null && name.length() > 0 ) {

                    /* Name match. */
                    if ( score < 5 &&
                         nameRegex.matcher( name.trim() ).matches() ) {
                        bestIndex = i;
                        score = 5;
                    }
                }
            }
        }

        /* No leads?  Bail out. */
        if ( bestIndex < 0 ) {
            assert score == 0;
            return null;
        }

        ColumnInfo info = inTable.getColumnInfo( bestIndex );
        logger_.info( "Identified column " + info + " as "
                    + ucdAtom.toUpperCase() );

        /* Try to work out the units of the best guess column we have. */
        String units = info.getUnitString();
        double factor;
        if ( units == null || units.trim().length() == 0 ) {
            logger_.info( "No units listed for column " + info.getName()
                        + " - assuming degrees" );
            factor = 1.0;
        }
        else if ( units.toLowerCase().startsWith( "deg" ) ) {
            factor = 1.0;
        }
        else if ( units.toLowerCase().startsWith( "rad" ) ) {
            factor = 180 / Math.PI;
        }
        else {
            logger_.info( "Units for column " + info.getName() + " listed as "
                        + units + " - assuming degrees" );
            factor = 1.0;
        }

        /* Construct and return the final expression.  It's a column reference
         * possibly multiplied by a unit conversion factor.  Use the $n form
         * of column reference since it won't be scuppered by spaces etc
         * in the column name. */
        String expr = "$" + ( bestIndex + 1 );
        if ( factor != 1.0 ) {
            expr = factor + "*" + expr;
        }
        logger_.config( ucdAtom.toUpperCase() + ": " + expr );
        return expr;
    }
}
