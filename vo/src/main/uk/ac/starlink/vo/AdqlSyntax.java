package uk.ac.starlink.vo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Provides utilities related to ADQL syntax.
 * This class is a singleton.
 *
 * @author   Mark Taylor
 * @since    22 Jun 2011
 */
public class AdqlSyntax {

    private static final AdqlSyntax instance_ = new AdqlSyntax();
    private final Set<String> reservedSet_;
    private final Pattern identifierRegex_;

    /**
     * Private constructor controls instantiation.
     */
    private AdqlSyntax() {

        /* Set up identifier syntax. */
        identifierRegex_ = Pattern.compile( "[A-Za-z][A-Za-z0-9_]*" );

        /* Populate the set of reserved words using the lists in the
         * ADQL 2.0 specification. */
        String[] sqlReserved = new String[] {
            "ABSOLUTE", "ACTION", "ADD", "ALL", "ALLOCATE", "ALTER", "AND",
            "ANY", "ARE", "AS", "ASC", "ASSERTION", "AT", "AUTHORIZATION",
            "AVG", "BEGIN", "BETWEEN", "BIT", "BIT_LENGTH", "BOTH", "BY",
            "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG", "CHAR",
            "CHARACTER", "CHARACTER_LENGTH", "CHAR_LENGTH", "CHECK", "CLOSE",
            "COALESCE", "COLLATE", "COLLATION", "COLUMN", "COMMIT", "CONNECT",
            "CONNECTION", "CONSTRAINT", "CONSTRAINTS", "CONTINUE", "CONVERT",
            "CORRESPONDING", "COUNT", "CREATE", "CROSS", "CURRENT",
            "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
            "CURRENT_USER", "CURSOR", "DATE", "DAY", "DEALLOCATE", "DECIMAL",
            "DECLARE", "DEFAULT", "DEFERRABLE", "DEFERRED", "DELETE", "DESC",
            "DESCRIBE", "DESCRIPTOR", "DIAGNOSTICS", "DISCONNECT", "DISTINCT",
            "DOMAIN", "DOUBLE", "DROP", "ELSE", "END", "ESCAPE", "EXCEPT",
            "EXCEPTION", "EXEC", "EXECUTE", "EXISTS", "EXTERNAL", "EXTRACT",
            "FALSE", "FETCH", "FIRST", "FLOAT", "FOR", "FOREIGN", "FOUND",
            "FROM", "FULL", "GET", "GLOBAL", "GO", "GOTO", "GRANT",
            "GROUP", "HAVING", "HOUR", "IDENTITY", "IMMEDIATE", "IN",
            "INDICATOR", "INITIALLY", "INNER", "INPUT", "INSENSITIVE",
            "INSERT", "INT", "INTEGER", "INTERSECT", "INTERVAL", "INTO",
            "IS", "ISOLATION", "JOIN", "KEY", "LANGUAGE", "LAST", "LEADING",
            "LEFT", "LEVEL", "LIKE", "LOCAL", "LOWER", "MATCH", "MAX", "MIN",
            "MINUTE", "MODULE", "MONTH", "NAMES", "NATIONAL", "NATURAL",
            "NCHAR", "NEXT", "NO", "NOT", "NULL", "NULLIF", "NUMERIC",
            "OCTET_LENGTH", "OF", "ON", "ONLY", "OPEN", "OPTION", "OR",
            "ORDER", "OUTER", "OUTPUT", "OVERLAPS", "PAD", "PARTIAL",
            "POSITION", "PRECISION", "PREPARE", "PRESERVE", "PRIMARY",
            "PRIOR", "PRIVILEGES", "PROCEDURE", "PUBLIC", "READ", "REAL",
            "REFERENCES", "RELATIVE", "RESTRICT", "REVOKE", "RIGHT",
            "ROLLBACK", "ROWS", "SCHEMA", "SCROLL", "SECOND", "SECTION",
            "SELECT", "SESSION", "SESSION_USER", "SET", "SIZE", "SMALLINT",
            "SOME", "SPACE", "SQL", "SQLCODE", "SQLERROR", "SQLSTATE",
            "SUBSTRING", "SUM", "SYSTEM_USER", "TABLE", "TEMPORARY", "THEN",
            "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO",
            "TRAILING", "TRANSACTION", "TRANSLATE", "TRANSLATION", "TRIM",
            "TRUE", "UNION", "UNIQUE", "UNKNOWN", "UPDATE", "UPPER", "USAGE",
            "USER", "USING", "VALUE", "VALUES", "VARCHAR", "VARYING", "VIEW",
            "WHEN", "WHENEVER", "WHERE", "WITH", "WORK", "WRITE", "YEAR",
            "ZONE",
        };
        String[] adqlReserved = new String[] {
            "ABS", "ACOS", "ASIN", "ATAN", "ATAN2", "CEILING", "COS",
            "DEGREES", "EXP", "FLOOR", "LOG", "LOG10", "MOD", "PI", "POWER",
            "RADIANS", "RAND", "ROUND", "SIN", "SQRT", "TAN", "TOP",
            "TRUNCATE",
        };
        Set<String> rset = new TreeSet<String>();
        rset.addAll( Arrays.asList( sqlReserved ) );
        rset.addAll( Arrays.asList( adqlReserved ) );
        reservedSet_ = Collections.unmodifiableSet( rset );
        for ( String word : reservedSet_ ) {
            assert isIdentifier( word );
            assert word.equals( normalise( word ) );
        }
    }

    /**
     * Returns an alphabetical list of the reserved words known by this class.
     *
     * @return  unmodifiable set of reserved words
     */
    public Set<String> getReservedWords() {
        return reservedSet_;
    }

    /**
     * Indicates whether a given word is reserved.  The result is not
     * sensitive to the case of the supplied word.
     *
     * @param  word  word to test
     * @return  true iff reserved
     */
    public boolean isReserved( String word ) {
        return reservedSet_.contains( normalise( word ) );
    }

    /**
     * Indicates whether a given word is syntactically permitted to act as
     * an identifier.
     *
     * @param  word  word to test
     * @return  true iff identifier
     */
    public boolean isIdentifier( String word ) {
        return identifierRegex_.matcher( word ).matches();
    }

    /**
     * Returns a string which can be used within an SQL query to refer to
     * an item with the name of a given word.
     * If the given word can be used as it stands, it is returned as given.
     * Otherwise (if it's reserved or syntactically unsuitable) a quoted
     * version of the provided word is returned.
     *
     * @param  word  word to use
     * @return   quoted or unquoted version of <code>word</code>
     */
    public String quoteIfNecessary( String word ) {
        return ( isIdentifier( word ) && ! isReserved( word ) )
             ? word
             : quote( word );
    }

    /**
     * Returns a quoted version of a word.
     *
     * @param  word  word to quote
     * @return  quoted word
     */
    public String quote( String word ) {
        StringBuilder sbuf = new StringBuilder( word.length() + 2 )
            .append( '"' );
        for ( int i = 0; i < word.length(); i++ ) {
            char c = word.charAt( i );
            sbuf.append( c );
            if ( c == '"' ) {
                sbuf.append( c );
            }
        }
        sbuf.append( '"' );
        return sbuf.toString();
    }

    /**
     * Normalises a word into a canonical form suitable for equality matching.
     *
     * @param  word  input word
     * @return  normalised form
     */
    private String normalise( String word ) {
        return word.toUpperCase();
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  AdqlSyntax instance
     */
    public static AdqlSyntax getInstance() {
        return instance_;
    }
}
