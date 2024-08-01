package uk.ac.starlink.table.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Summarises some aspects of the syntax of an SQL variant,
 * and provides some utilities based on that.
 *
 * @author   Mark Taylor
 * @since    7 Jun 2013
 */
public class SqlSyntax {

    private final SortedSet<String> reservedWords_;
    private final Pattern identifierRegex_;
    private final char quoteChar_;

    /** Standard regular expression for identifiers, as per SQL92. */
    public static final Pattern SQL92_IDENTIFIER_REGEX =
        Pattern.compile( "[A-Za-z][A-Za-z0-9_]*" );

    /**
     * SQL92 list of reserved words.
     * This list was actually taken from ADQL 2.0 standard.
     */
    public static final String[] SQL92_RESERVED = new String[] {
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

    /** Reserved words taken from the MySQL 5.0.3 manual.  */
    private static final String[] MYSQL_RESERVED = new String[] {
        "ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ASENSITIVE",
        "BEFORE", "BETWEEN", "BIGINT", "BINARY", "BLOB", "BOTH", "BY",
        "CALL", "CASCADE", "CASE", "CHANGE", "CHAR", "CHARACTER", "CHECK",
        "COLLATE", "COLUMN", "CONDITION", "CONNECTION", "CONSTRAINT",
        "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT_DATE",
        "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR",
        "DATABASE", "DATABASES", "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE",
        "DAY_SECOND", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELAYED",
        "DELETE", "DESC", "DESCRIBE", "DETERMINISTIC", "DISTINCT",
        "DISTINCTROW", "DIV", "DOUBLE", "DROP", "DUAL", "EACH", "ELSE",
        "ELSEIF", "ENCLOSED", "ESCAPED", "EXISTS", "EXIT", "EXPLAIN",
        "FALSE", "FETCH", "FLOAT", "FOR", "FORCE", "FOREIGN", "FROM",
        "FULLTEXT", "GOTO", "GRANT", "GROUP", "HAVING", "HIGH_PRIORITY",
        "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND", "IF", "IGNORE",
        "IN", "INDEX", "INFILE", "INNER", "INOUT", "INSENSITIVE", "INSERT",
        "INT", "INTEGER", "INTERVAL", "INTO", "IS", "ITERATE", "JOIN",
        "KEY", "KEYS", "KILL", "LEADING", "LEAVE", "LEFT", "LIKE",
        "LIMIT", "LINES", "LOAD", "LOCALTIME", "LOCALTIMESTAMP", "LOCK",
        "LONG", "LONGBLOB", "LONGTEXT", "LOOP", "LOW_PRIORITY", "MATCH",
        "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", "MIDDLEINT",
        "MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD", "MODIFIES", "NATURAL",
        "NOT", "NO_WRITE_TO_BINLOG", "NULL", "NUMERIC", "ON", "OPTIMIZE",
        "OPTION", "OPTIONALLY", "OR", "ORDER", "OUT", "OUTER", "OUTFILE",
        "PRECISION", "PRIMARY", "PROCEDURE", "PURGE", "READ", "READS",
        "REAL", "REFERENCES", "REGEXP", "RENAME", "REPEAT", "REPLACE",
        "REQUIRE", "RESTRICT", "RETURN", "REVOKE", "RIGHT", "RLIKE",
        "SCHEMA", "SCHEMAS", "SECOND_MICROSECOND", "SELECT", "SENSITIVE",
        "SEPARATOR", "SET", "SHOW", "SMALLINT", "SONAME", "SPATIAL",
        "SPECIFIC", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING",
        "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT",
        "SSL", "STARTING", "STRAIGHT_JOIN", "TABLE", "TERMINATED",
        "THEN", "TINYBLOB", "TINYINT", "TINYTEXT", "TO", "TRAILING",
        "TRIGGER", "TRUE", "UNDO", "UNION", "UNIQUE", "UNLOCK", "UNSIGNED",
        "UPDATE", "USAGE", "USE", "USING", "UTC_DATE", "UTC_TIME",
        "UTC_TIMESTAMP", "VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER",
        "VARYING", "WHEN", "WHERE", "WHILE", "WITH", "WRITE", "XOR",
        "YEAR_MONTH", "ZEROFILL",
    };

    /**
     * Reserved words for Oracle,
     * taken from O'Reilly SQL in a Nutshell, 3rd edition
     * (via http://oreilly.com/sql/excerpts/sql-in-nutshell/shared-platform-specific-keywords.htm)
     */
    private static final String[] ORACLE_RESERVED = new String[] {
        "ACCESS", "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUDIT", 
        "BETWEEN", "BY", "CHAR", "CHECK", "CLUSTER", "COLUMN", "COMMENT", 
        "COMPRESS", "CONNECT", "CREATE", "CURRENT", "DATE", "DECIMAL", 
        "DEFAULT", "DELETE", "DESC", "DISTINCT", "DROP", "ELSE", "EXCLUSIVE", 
        "EXISTS", "FILE", "FLOAT", "FOR", "FROM", "GRANT", "GROUP", "HAVING", 
        "IDENTIFIED", "IMMEDIATE", "IN", "INCREMENT", "INDEX", "INITIAL", 
        "INSERT", "INTEGER", "INTERSECT", "INTO", "IS", "LEVEL", "LIKE", 
        "LOCK", "LONG", "MAXEXTENTS", "MINUS", "MLSLABEL", "MODE", "MODIFY", 
        "NOAUDIT", "NOCOMPRESS", "NOT", "NOWAIT", "NULL", "NUMBER", "OF", 
        "OFFLINE", "ON", "ONLINE", "OPTION", "OR", "ORDER", "PCTFREE", 
        "PRIOR", "PRIVILEGES", "PUBLIC", "RAW", "RENAME", "RESOURCE", 
        "REVOKE", "ROW", "ROWID", "ROWNUM", "ROWS", "SELECT", "SESSION", 
        "SET", "SHARE", "SIZE", "SMALLINT", "START", "SUCCESSFUL", "SYNONYM", 
        "SYSDATE", "TABLE", "THEN", "TO", "TRIGGER", "UID", "UNION", 
        "UNIQUE", "UPDATE", "USER", "VALIDATE", "VALUES", "VARCHAR", 
    };

    /**
     * Reserved words for PostgreSQL,
     * taken from O'Reilly SQL in a Nutshell, 3rd edition
     * (via http://oreilly.com/sql/excerpts/sql-in-nutshell/shared-platform-specific-keywords.htm)
     */
    private static final String[] POSTGRES_RESERVED = new String[] {
        "ABORT", "ADD", "ALL", "ALLOCATE", "ALTER", "ANALYZE", "AND", "ANY",
        "ARE", "AS", "ASC", "ASSERTION", "AT", "AUTHORIZATION", "AVG", 
        "BEGIN", "BETWEEN", "BINARY", "BIT", "BIT_LENGTH", "BOTH", "BY",
        "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG", "CHAR", 
        "CHAR_LENGTH", "CHARACTER", "CHARACTER_LENGTH", "CHECK", "CLOSE",
        "CLUSTER", "COALESCE", "COLLATE", "COLLATION", "COLUMN", "COMMIT",
        "CONNECT", "CONNECTION", "CONSTRAINT", "CONTINUE", "CONVERT", "COPY",
        "CORRESPONDING", "COUNT", "CREATE", "CROSS", "CURRENT", 
        "CURRENT_DATE", "CURRENT_SESSION", "CURRENT_TIME", 
        "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATE", "DEALLOCATE",
        "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELETE", "DESC", "DESCRIBE",
        "DESCRIPTOR", "DIAGNOSTICS", "DISCONNECT", "DISTINCT", "DO",
        "DOMAIN", "DROP", "ELSE", "END", "ESCAPE", "EXCEPT", "EXCEPTION",
        "EXEC", "EXECUTE", "EXISTS", "EXPLAIN", "EXTEND", "EXTERNAL",
        "EXTRACT", "FALSE", "FETCH", "FIRST", "FLOAT", "FOR", "FOREIGN",
        "FOUND", "FROM", "FULL", "GET", "GLOBAL", "GO", "GOTO", "GRANT",
        "GROUP", "HAVING", "IDENTITY", "IN", "INDICATOR", "INNER", "INPUT",
        "INSERT", "INTERSECT", "INTERVAL", "INTO", "IS", "JOIN", "LAST",
        "LEADING", "LEFT", "LIKE", "LISTEN", "LOAD", "LOCAL", "LOCK",
        "LOWER", "MAX", "MIN", "MODULE", "MOVE", "NAMES", "NATIONAL", 
        "NATURAL", "NCHAR", "NEW", "NO", "NONE", "NOT", "NOTIFY", "NULL",
        "NULLIF", "NUMERIC", "OCTET_LENGTH", "OFFSET", "ON", "OPEN",
        "OR", "ORDER", "OUTER", "OUTPUT", "OVERLAPS", "PARTIAL", "POSITION",
        "PRECISION", "PREPARE", "PRESERVE", "PRIMARY", "PRIVILEGES",
        "PROCEDURE", "PUBLIC", "REFERENCES", "RESET", "REVOKE", "RIGHT",
        "ROLLBACK", "ROWS", "SCHEMA", "SECTION", "SELECT", "SESSION",
        "SESSION_USER", "SET", "SETOF", "SHOW", "SIZE", "SOME", "SQL",
        "SQLCODE", "SQLERROR", "SQLSTATE", "SUBSTRING", "SUM", "SYSTEM_USER",
        "TABLE", "TEMPORARY", "THEN", "TO", "TRAILING", "TRANSACTION",
        "TRANSLATE", "TRANSLATION", "TRIM", "TRUE", "UNION", "UNIQUE",
        "UNKNOWN", "UNLISTEN", "UNTIL", "UPDATE", "UPPER", "USAGE",
        "USER", "USING", "VACUUM", "VALUE", "VALUES", "VARCHAR", "VARYING",
        "VERBOSE", "VIEW", "WHEN", "WHENEVER", "WHERE", "WITH", "WORK",
        "WRITE",
    };

    /**
     * Reserved words for SQL Server,
     * taken from O'Reilly SQL in a Nutshell, 3rd edition
     * (via http://oreilly.com/sql/excerpts/sql-in-nutshell/shared-platform-specific-keywords.htm)
     */
    private static final String[] SQLSERVER_RESERVED = new String[] {
        "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUTHORIZATION", 
        "BACKUP", "BEGIN", "BETWEEN", "BREAK", "BROWSE", "BULK", "BY", 
        "CASCADE", "CASE", "CHECK", "CHECKPOINT", "CLOSE", "CLUSTERED", 
        "COALESCE", "COLLATE", "COLUMN", "COMMIT", "COMPUTE", "CONSTRAINT", 
        "CONTAINS", "CONTAINSTABLE", "CONTINUE", "CONVERT", "CREATE", 
        "CROSS", "CURRENT", "CURRENT_DATE", "CURRENT_TIME", 
        "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATABASE", 
        "DBCC", "DEALLOCATE", "DECLARE", "DEFAULT", "DELETE", "DENY", 
        "DESC", "DISK", "DISTINCT", "DISTRIBUTED", "DOUBLE", "DROP", 
        "DUMP", "ELSE", "END", "ERRLVL", "ESCAPE", "EXCEPT", "EXEC", 
        "EXECUTE", "EXISTS", "EXIT", "EXTERNAL", "FETCH", "FILE", 
        "FILLFACTOR", "FOR", "FOREIGN", "FREETEXT", "FREETEXTTABLE", "FROM", 
        "FULL", "FUNCTION", "GOTO", "GRANT", "GROUP", "HAVING", "HOLDLOCK", 
        "IDENTITY", "IDENTITY_INSERT", "IDENTITYCOL", "IF", "IN", "INDEX", 
        "INNER", "INSERT", "INTERSECT", "INTO", "IS", "JOIN", "KEY", "KILL", 
        "LEFT", "LIKE", "LINENO", "LOAD", "NATIONAL", "NOCHECK", 
        "NONCLUSTERED", "NOT", "NULL", "NULLIF", "OF", "OFF", "OFFSETS", 
        "ON", "OPEN", "OPENDATASOURCE", "OPENQUERY", "OPENROWSET", "OPENXML", 
        "OPTION", "OR", "ORDER", "OUTER", "OVER", "PERCENT", "PIVOT", "PLAN", 
        "PRECISION", "PRIMARY", "PRINT", "PROC", "PROCEDURE", "PUBLIC", 
        "RAISERROR", "READ", "READTEXT", "RECONFIGURE", "REFERENCES", 
        "REPLICATION", "RESTORE", "RESTRICT", "RETURN", "REVERT", "REVOKE", 
        "RIGHT", "ROLLBACK", "ROWCOUNT", "ROWGUIDCOL", "RULE", "SAVE", 
        "SCHEMA", "SECURITYAUDIT", "SELECT", "SESSION_USER", "SET", 
        "SETUSER", "SHUTDOWN", "SOME", "STATISTICS", "SYSTEM_USER", "TABLE", 
        "TABLESAMPLE", "TEXTSIZE", "THEN", "TO", "TOP", "TRAN", "TRANSACTION", 
        "TRIGGER", "TRUNCATE", "TSEQUAL", "UNION", "UNIQUE", "UNPIVOT", 
        "UPDATE", "UPDATETEXT", "USE", "USER", "VALUES", "VARYING", "VIEW", 
        "WAITFOR", "WHEN", "WHERE", "WHILE", "WITH", "WRITETEXT", 
    };

    /**
     * Constructor.
     *
     * @param  reservedWords  list of words considered reserved for this dialect
     * @param  identifierRegex  regular expression for an identifier token
     *                          in this dialect
     * @param  quoteChar  character which may be used to quote words in
     *                    this dialect (thus avoiding their usual parsing);
     *                    words are quoted with a copy of this character
     *                    at start and end, doubled if this character is
     *                    embedded
     */
    @SuppressWarnings("this-escape")
    public SqlSyntax( String[] reservedWords, Pattern identifierRegex,
                      char quoteChar ) {
        TreeSet<String> words = new TreeSet<String>();
        for ( int i = 0; i < reservedWords.length; i++ ) {
            words.add( normalise( reservedWords[ i ] ) );
        }
        reservedWords_ = Collections.unmodifiableSortedSet( words );
        identifierRegex_ = identifierRegex;
        quoteChar_ = quoteChar;
        for ( String word : reservedWords_ ) {
            assert isIdentifier( word );
            assert word.equals( normalise( word ) );
        }
    }

    /**
     * Returns an alphabetical list of the reserved words known by this class,
     * in normalised (upper case) form.
     *
     * @return  unmodifiable set of reserved words
     */
    public SortedSet<String> getReservedWords() {
        return reservedWords_;
    }

    /**
     * Indicates whether a given word is reserved.  The result is not
     * sensitive to the case of the supplied word.
     *
     * @param  word  word to test
     * @return  true iff reserved
     */
    public boolean isReserved( String word ) {
        return reservedWords_.contains( normalise( word ) );
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
     * <p>Note that quoting words is <b>not</b> harmless - unlike
     * for instance shell syntax, quotes are not just stripped off
     * where present before processing, but instead in SQL92 and hence
     * ADQL they modify the interpretation of what's quoted.
     * This is something to do with the way case folding is handled,
     * and I (mbt) didn't know about it until Markus Demleitner
     * persuaded me it's actually true.  As it happens MySQL behaves
     * contrary to the standard in this respect (quoting of column names -
     * but not table names?? - is harmless) but (for instance) PostgreSQL
     * does not.  Therefore do not use this method indiscriminately,
     * use <code>quoteIfNecessary</code> instead.
     *
     * @param  word  word to quote
     * @return  quoted word
     */
    public String quote( String word ) {
        StringBuilder sbuf = new StringBuilder( word.length() + 2 )
            .append( quoteChar_ );
        for ( int i = 0; i < word.length(); i++ ) {
            char c = word.charAt( i );
            sbuf.append( c );

            /* Double up quote characters if they need quoting.
             * This is how it works in SQL92 anyway. */
            if ( c == quoteChar_ ) {
                sbuf.append( c );
            }
        }
        sbuf.append( quoteChar_ );
        return sbuf.toString();
    }

    /**
     * Normalises a word into a canonical form suitable for equality matching.
     *
     * <p>Note this method is called from the constructor, which is why it's
     * static and not overridable.
     *
     * @param  word  input word
     * @return  normalised form
     */
    private static String normalise( String word ) {
        return word.toUpperCase();
    }

    /**
     * Returns a list of words that are known to be reserved in a mixed
     * bag of popular RDBMSs.
     *
     * @return  reserved word list
     */
    public static String[] getParanoidReservedWords() {
        Collection<String> words = new HashSet<String>();
        words.addAll( Arrays.asList( SQL92_RESERVED ) );
        words.addAll( Arrays.asList( MYSQL_RESERVED ) );
        words.addAll( Arrays.asList( ORACLE_RESERVED ) );
        words.addAll( Arrays.asList( POSTGRES_RESERVED ) );
        words.addAll( Arrays.asList( SQLSERVER_RESERVED ) );
        List<String> wordList = new ArrayList<String>( words );
        Collections.sort( wordList );
        return wordList.toArray( new String[ 0 ] );
    }
}
