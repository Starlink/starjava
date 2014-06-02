package uk.ac.starlink.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.jdbc.SqlSyntax;

/**
 * SqlSyntax instance for the ADQL 2.0 grammar.
 * This class is a singleton.
 *
 * @author   Mark Taylor
 * @since    22 Jun 2011
 * @see    <a href="http://www.ivoa.net/documents/cover/ADQL-20081030.html"
 *            >ADQL 2.0 Standard</a>
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
    private static final AdqlSyntax instance_ = new AdqlSyntax();

    /**
     * Private constructor controls instantiation.
     */
    private AdqlSyntax() {
        super( getAllReservedWords(), SqlSyntax.SQL92_IDENTIFIER_REGEX, '"' );
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  AdqlSyntax instance
     */
    public static AdqlSyntax getInstance() {
        return instance_;
    }

    /**
     * Returns the full list of reserved words including SQL92 and ADQL.
     *
     * @return   reserved words array
     */
    private static String[] getAllReservedWords() {
        List<String> reserved = new ArrayList<String>();
        reserved.addAll( Arrays.asList( SQL92_RESERVED ) );
        reserved.addAll( Arrays.asList( ADQL_RESERVED ) );
        return reserved.toArray( new String[ 0 ] );
    }
}
