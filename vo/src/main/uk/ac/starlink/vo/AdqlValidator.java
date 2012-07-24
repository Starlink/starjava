package uk.ac.starlink.vo;

import adql.db.DBChecker;
import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.parser.ADQLParser;
import adql.parser.ParseException;
import adql.parser.QueryChecker;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Handles validation of ADQL queries.
 * The hard work is done by Gregory Mantelet's ADQL parser.
 *
 * @author   Mark Taylor
 * @since    3 Oct 2011
 */
public class AdqlValidator {

    private final ADQLParser parser_;
    private final QueryChecker checker_;

    /**
     * Constructor.
     *
     * @param  tmetas  table metadata for tables to be accessed
     */
    public AdqlValidator( TableMeta[] tmetas ) {
        parser_ = new ADQLParser();
        parser_.setDebug( false );
        checker_ = tmetas == null ? null
                                  : new DBChecker( toDBTables( tmetas ) );
    }

    /**
     * Validates an ADQL string.
     *
     * @param  query   ADQL query string
     * @throws  ParseException   if the string is not valid ADQL
     */
    public void validate( String query ) throws ParseException {
        ADQLQuery pq = parser_.parseQuery( query );
        if ( checker_ != null ) {
            checker_.check( pq );
        }

        /* Another possible check would be to identify unknown
         * User-Defined Functions here by walking the parse tree and
         * noting UserFunction instances.  However, at present, there
         * is no way to identify where in the ADQL text these appear. */
    }

    /**
     * Adapts an array of TableMeta objects to a DBTable collection.
     *
     * @param  tmetas  table metadata in VO package form
     * @return  table metadata in ADQLParser-friendly form
     */
    private static Collection<DBTable> toDBTables( TableMeta[] tmetas ) {
        Collection<DBTable> tList = new ArrayList<DBTable>();
        for ( int i = 0; i < tmetas.length; i++ ) {
            tList.add( toDBTable( tmetas[ i ] ) );
        }
        return tList;
    }

    /**
     * Adapts a TableMeta object to a DBTable.
     *
     * @param   tmeta  table metadata in VO package form
     * @return  table metadata in ADQLParser-friendly form
     */
    private static DBTable toDBTable( TableMeta tmeta ) {
        DefaultDBTable dbTable = new DefaultDBTable( tmeta.getName() );
        ColumnMeta[] colMetas = tmeta.getColumns();
        for ( int ic = 0; ic < colMetas.length; ic++ ) {
            dbTable.addColumn( new DefaultDBColumn( colMetas[ ic ].getName(),
                                                    dbTable ) );
        }
        return dbTable;
    }

    /**
     * Tests parser.  Use <code>-h</code> for usage.
     */
    public static void main( String[] args )
            throws ParseException,
            java.io.IOException, org.xml.sax.SAXException {
        String usage = "\n   Usage: " + AdqlValidator.class.getName()
                     + " [-meta <tmeta-url>] <query>\n";
        TableMeta[] tmetas = null;
        String query;
        ArrayList<String> argList =
            new ArrayList<String>( java.util.Arrays.asList( args ) );
        try {
            if ( argList.get( 0 ).equals( "-meta" ) ) {
                argList.remove( 0 );
                String loc = argList.remove( 0 );
                tmetas = TableSetSaxHandler
                        .readTableSet( new java.net.URL( loc ) );
            }
            query = argList.remove( 0 );
        }
        catch ( RuntimeException e ) {
            query = null;
        }
        if ( query == null || ! argList.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
            return;
        }
        new AdqlValidator( tmetas ).validate( query );
    }
}
