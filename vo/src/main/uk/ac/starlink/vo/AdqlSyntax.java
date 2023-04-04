package uk.ac.starlink.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.jdbc.SqlSyntax;

/**
 * SqlSyntax instance for the ADQL 2.* grammar.
 *
 * <p>Instances {@link #INSTANCE20} and {@link #INSTANCE21} are currently 
 * provided for ADQL versions 2.0 and 2.1 respectively.
 * However the differences between these are very small
 * (just a few extra reserved words for V2.1) so the method
 * {@link #getInstance} is usually an appropriate way to obtain an
 * instance of this class.
 *
 * @author   Mark Taylor
 * @since    22 Jun 2011
 * @see <a href="http://www.ivoa.net/documents/cover/ADQL-20081030.html"
 *         >ADQL 2.0 Standard</a>
 * @see <a href="http://wiki.ivoa.net/internal/IVOA/IvoaVOQL/adql-bnf-v2.0.html"
 *         >ADQL 2.0 BNF</a>
 */
public class AdqlSyntax extends SqlSyntax {

    /**
     * ADQL reserved words additional to the SQL92 set,
     * taken from the ADQL standard.
     */
    public static final String[] ADQL_RESERVED = new String[] {

        // From ADQL 2.0 section 2.1.2:
        "ABS", "ACOS", "ASIN", "ATAN", "ATAN2", "CEILING", "COS", "DEGREES",
        "EXP", "FLOOR", "LOG", "LOG10", "MOD", "PI", "POWER", "RADIANS",
        "RAND", "ROUND", "SIN", "SQRT", "TAN", "TOP", "TRUNCATE",

        // From ADQL 2.0 section 2.4.1:
        "AREA", "BOX", "CENTROID", "CIRCLE", "CONTAINS", "COORD1", "COORD2",
        "COORDSYS", "DISTANCE", "INTERSECTS", "POINT", "POLYGON", "REGION",
    };

    /**
     * Additional reserved words from ADQL 2.1 section 2.1.4.
     */
    public static final String[] ADQL21_RESERVED = new String[] {
        "BIGINT", "ILIKE", "IN_UNIT",
    };

    private static final Pattern REGULAR_IDENTIFIER_REGEX =
        SQL92_IDENTIFIER_REGEX;
    private static final Pattern DELIMITED_IDENTIFIER_REGEX =
        Pattern.compile( "\"(?:[^\"]|\"\")+\"" );
    private static final Pattern CST_REGEX = createCatalogSchemaTablePattern();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /** Instance for V2.0 of the ADQL standard. */
    public static final AdqlSyntax INSTANCE20 =
        new AdqlSyntax( AdqlVersion.V20 );

    /** Instance for V2.1 of the ADQL standard. */
    public static final AdqlSyntax INSTANCE21 =
        new AdqlSyntax( AdqlVersion.V21 );

    /**
     * Protected constructor to signal that static instances or
     * methods should usually be used to obtain an instance.
     *
     * @param   adqlVersion  version of the ADQL standard
     */
    protected AdqlSyntax( AdqlVersion adqlVersion ) {
        super( getAllReservedWords( adqlVersion ),
               REGULAR_IDENTIFIER_REGEX, '"' );
    }

    /**
     * Indicates whether the given token matches the ADQL
     * <code>&lt;delimited_identifier&gt;</code> production.
     *
     * @param  token  token to test
     * @return  true iff token is a delimited_identifier
     */
    public boolean isAdqlDelimitedIdentifier( String token ) {
        return DELIMITED_IDENTIFIER_REGEX.matcher( token ).matches();
    }

    /**
     * Indicates whether the given token matches the ADQL
     * <code>&lt;column_name&gt;</code> production.
     *
     * @param  token  token to test
     * @return  true iff token is a column_name
     */
    public boolean isAdqlColumnName( String token ) {
        return isIdentifier( token ) || isAdqlDelimitedIdentifier( token );
    }

    /**
     * Indicates whether the given token matches the ADQL
     * <code>&lt;table_name&gt;</code> production.
     *
     * @param  token  token to test
     * @return  true iff token is a table_name
     */
    public boolean isAdqlTableName( String token ) {
        return getCatalogSchemaTable( token ) != null;
    }

    /**
     * Takes an ADQL <code>&lt;table_name&gt;</code> token
     * and returns a 3-element array giving the catalog, schema and table
     * (delimited or regular) identifiers.
     *
     * <p>For a non-null result, the input token must match the ADQL
     * <code>&lt;table_name&gt;</code> production, which is the same
     * rule that TAP_SCHEMA table_name columns must follow, which
     * roughly means it's of the form [catalog.[schema.[table]]].
     * Either catalog alone, or both catalog and schema, may be null.
     * The return value is either a three-element array (with 1, 2 or 3
     * non-null elements) in case of a legal table_name input,
     * or null in case of an illegal table_name input.
     *
     * @param    tableName   table_name string
     * @return   3-element array giving (catalog, schema, table) identifiers,
     *           or null for parse failure
     */
    public String[] getCatalogSchemaTable( String tableName ) {
        if ( tableName != null ) {
            Matcher matcher = CST_REGEX.matcher( tableName );
            if ( matcher.matches() ) {
                return new String[] {
                    matcher.group( 1 ),
                    matcher.group( 2 ),
                    matcher.group( 3 ),
                };
            }
        }
        return null;
    }

    /**
     * Takes a regular-or-delimited-identifier and returns its raw form.
     * For a regular identifier, the output is the same as the input.
     *
     * @param  identifier  regular or delimited identifier, or null
     * @return  identifier with no surrounding quotes or other escaping,
     *          or null for null input
     */
    public String unquote( String identifier ) {
        if ( identifier == null ) {
            return null;
        }
        int leng = identifier.length();
        if ( leng > 0 &&
             identifier.charAt( 0 ) == '"' &&
             identifier.charAt( leng - 1 ) == '"' ) {
            return identifier.substring( 1, leng - 1 )
                             .replaceAll( "\"\"", "\"" );
        }
        else {
            return identifier;
        }
    }

    /**
     * Returns ADQL text representing a character literal.
     * This quotes the supplied string by surrounding it with single quotes,
     * escaping any internal single quote characters appropriately.
     *
     * @param  txt  raw text
     * @return   character literal suitable for insertion into ADQL text
     */
    public String characterLiteral( String txt ) {
        return "'" + txt.replaceAll( "'", "''" ) + "'";
    }

    /**
     * Returns an instance of this class.
     * This currently returns the instance representing ADQL 2.1.
     * There is very little difference between this and the ADQL 2.0 instance,
     * just a few extra reserved words, so it's suitable for general use
     * in most cases.
     *
     * @return  AdqlSyntax instance
     */
    public static AdqlSyntax getInstance() {
        return INSTANCE21;
    }

    /**
     * Returns the full list of reserved words including SQL92 and ADQL.
     *
     * @param    adqlVersion  version of the ADQL language
     * @return   reserved words array
     */
    private static String[] getAllReservedWords( AdqlVersion adqlVersion ) {
        List<String> reserved = new ArrayList<String>();
        reserved.addAll( Arrays.asList( SQL92_RESERVED ) );
        reserved.addAll( Arrays.asList( ADQL_RESERVED ) );
        switch ( adqlVersion ) {
            case V21:
                reserved.addAll( Arrays.asList( ADQL21_RESERVED ) );
                break;
            case V20:
                break;
            default:
                logger_.warning( "Unknown ADQL version: " + adqlVersion );
        }
        return reserved.toArray( new String[ 0 ] );
    }

    /**
     * Returns a regex pattern with three capturing groups, for Catalog,
     * Schema and Table regular-or-delimited-identifiers.
     *
     * @return  compiled regex
     */
    private static Pattern createCatalogSchemaTablePattern() {
        String regularIdentifier = REGULAR_IDENTIFIER_REGEX.pattern();
        String delimitedIdentifier = DELIMITED_IDENTIFIER_REGEX.pattern();
        String identifierCapture =
            "(" + regularIdentifier + "|" + delimitedIdentifier + ")";
        String dot = "[.]";
        String catSchTab =
             "(?:" + identifierCapture + dot + ")??"
           + "(?:" + identifierCapture + dot + ")??"
           + identifierCapture;
        return Pattern.compile( catSchTab );
    }
}
