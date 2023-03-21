package uk.ac.starlink.vo;

import adql.db.DBChecker;
import adql.db.DBColumn;
import adql.db.DBTable;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.db.FunctionDef;
import adql.parser.ADQLParser;
import adql.parser.ADQLQueryFactory;
import adql.parser.ParseException;
import adql.parser.QueryChecker;
import adql.parser.TokenMgrError;
import adql.query.ADQLQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.util.ContentCoding;

/**
 * Handles validation of ADQL queries.
 * In the current implementation the heavy lifting is done by 
 * Gregory Mantelet's ADQL parser.
 *
 * @author   Mark Taylor
 * @since    3 Oct 2011
 */
public class AdqlValidator {

    private final ADQLParser parser_;
    private final QueryChecker checker_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     * Note empty arrays restrict possibilities (to none), but null values
     * allow anything.
     *
     * @param  vtables  table metadata for database to be checked against
     * @param  udfs   array of permitted user-defined-functions,
     *                or null to permit all
     *                (but ignored if vtables is null)
     * @param  geoFuncs  array of permitted ADQL geometry function names,
     *                   or null to permit all those defined by ADQL (sec 2.4)
     *                   (but ignored if vtables is null)
     */
    private AdqlValidator( ValidatorTable[] vtables, FunctionDef[] udfs,
                           String[] geoFuncs ) {
        parser_ = new ADQLParser();
        Collection<DBTable> tableList = vtables == null
                                      ? null
                                      : toDBTables( vtables );
        Collection<FunctionDef> udfList = udfs == null
                                        ? null
                                        : Arrays.asList( udfs );
        Collection<String> geoList = geoFuncs == null
                                   ? null
                                   : Arrays.asList( geoFuncs );
        if ( tableList == null ) {

            /* You can't specify that any table/columns are permitted,
             * so if we have no table metadata, just don't do any checking
             * beyond basic syntax.  In this case the udfList and geoList
             * are ignored. */
            checker_ = null;
        }
        else {
            List<String> csysList = null;
            try {
                checker_ =
                    new DBChecker( tableList, udfList, geoList, csysList );
            }
            catch ( ParseException e ) {
                // At ADQL lib 1.3, this exception is only generated if
                // coordinate system processing goes wrong.  Since we are
                // submitting a null coordinate system list, this exception
                // should be impossible.
                throw new RuntimeException( "Unexpected", e );
            }
        }
    }

    /**
     * Validates an ADQL string.
     * Any throwable returned hopefully includes useful information about
     * the location and nature of the parse error, but that depends on the
     * implementation.
     *
     * @param  query   ADQL query string
     * @throws  Throwable   if the string is not valid ADQL
     */
    public void validate( String query ) throws Throwable {
        ADQLQuery pq = parser_.parseQuery( query );
        if ( checker_ != null ) {
            try {
                checker_.check( pq );
            }
            catch ( ParseException e ) {
                throw e;
            }
            catch ( TokenMgrError e ) {
                throw e;
            }
        }

        /* Another possible check would be to identify unknown
         * User-Defined Functions here by walking the parse tree and
         * noting UserFunction instances.  However, at present, there
         * is no way to identify where in the ADQL text these appear. */
    }

    /**
     * Attempts to fix common errors in a submitted query.
     * If some changes can be made that would make the query more correct,
     * the fixed query is returned.  If no such changes can be made for
     * whatever reason, null is returned.
     *
     * @param   query   input ADQL
     * @return   EITHER ADQL which resembles, but is not identical to,
     *           the input but which has a better chance of being correct;
     *           OR null
     */
    public String fixup( String query ) {
        if ( query != null && query.trim().length() > 0 ) {
            try {
                String fixQuery = parser_.tryQuickFix( query );
                return query.equals( fixQuery ) ? null : fixQuery;
            }
            catch ( ParseException e ) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns a basic validator instance.
     *
     * @return  new vanilla validator
     */
    public static AdqlValidator createValidator() {
        return new AdqlValidator( (ValidatorTable[]) null,
                                  (FunctionDef[]) null, (String[]) null );
    }

    /**
     * Returns a validator instance that knows about available tables.
     *
     * @param  vtables  table metadata for database to be checked against
     * @return  vanilla validator
     */
    public static AdqlValidator createValidator( ValidatorTable[] vtables ) {
        return new AdqlValidator( vtables,
                                  (FunctionDef[]) null, (String[]) null );
    }

    /**
     * Creates an instance given a set of table metadata and a TapLanguage
     * description object.
     * The language object's TapLanguageFeature map is examined to determine
     * what UDFs and ADQL geometry functions are supported.
     * In the case of no description of UDFs and geom functions,
     * no restrictions are imposed.
     *
     * @param  vtables  table metadata
     * @param  lang   language specifics
     */
    public static AdqlValidator createValidator( ValidatorTable[] vtables,
                                                 TapLanguage lang ) {
        Map<String,TapLanguageFeature[]> featMap =
            lang == null ? null : lang.getFeaturesMap();
        if ( featMap == null ) {
            featMap = new HashMap<String,TapLanguageFeature[]>();
        }

        /* Work out supported ADQL geometry functions. */
        TapLanguageFeature[] geoFeats =
            featMap.get( TapCapability.ADQLGEO_FEATURE_TYPE );
        List<String> geoList = new ArrayList<String>();
        if ( geoFeats != null ) {
            for ( TapLanguageFeature geoFeat : geoFeats ) {
                String fname = geoFeat.getForm();
                if ( fname != null && fname.trim().length() > 0 ) {
                    geoList.add( fname.trim() );
                }
            }
        }
        String[] geoFuncs = geoList.size() > 0
                          ? geoList.toArray( new String[ 0 ] )
                          : null;

        /* Work out supported user-defined functions. */
        TapLanguageFeature[] udfFeats =
            featMap.get( TapCapability.UDF_FEATURE_TYPE );
        List<FunctionDef> udfList = new ArrayList<FunctionDef>();
        if ( udfFeats != null ) {
            for ( TapLanguageFeature udfFeat : udfFeats ) {
                String form = udfFeat.getForm();
                FunctionDef udf;
                try {
                    udf = FunctionDef.parse( form );
                }
                catch ( ParseException e ) {
                    udf = null;

                    /* Arguably this should be a WARNING, but at time of
                     * writing the ADQL library fails to parse many
                     * reasonable UDFs, since it doesn't know about
                     * ADQL 2.1 types.  So demote it to an INFO for now
                     * to reduce logging noise. */
                    logger_.log( Level.INFO,
                                 "Failed to parse UDF def \"" + form + "\"",
                                 e );
                }
                if ( udf != null ) {
                    udfList.add( udf );
                }
            }
        }
        FunctionDef[] udfs = udfList.size() > 0
                           ? udfList.toArray( new FunctionDef[ 0 ] )
                           : null;

        /* Construct a suitable validator object. */
        return new AdqlValidator( vtables, udfs, geoFuncs );
    }

    /**
     * Adapts an array of ValidatorTable objects to a DBTable collection.
     *
     * @param  vtables  table metadata in VO package form
     * @return  table metadata in ADQLParser-friendly form
     */
    private static Collection<DBTable> toDBTables( ValidatorTable[] vtables ) {
        Collection<DBTable> tList = new ArrayList<DBTable>();
        for ( int i = 0; i < vtables.length; i++ ) {
            tList.add( new ValidatorDBTable( vtables[ i ] ) );
        }
        return tList;
    }

    /**
     * Tests parser.  Use <code>-h</code> for usage.
     */
    public static void main( String[] args )
            throws Throwable,
            java.io.IOException, org.xml.sax.SAXException {
        String usage = "\n   Usage: " + AdqlValidator.class.getName()
                     + " [-meta <tmeta-url>]"
                     + " <query>"
                     + "\n";
        SchemaMeta[] schMetas = null;
        ArrayList<String> argList =
        new ArrayList<String>( java.util.Arrays.asList( args ) );
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.startsWith( "-h" ) ) {
                System.out.println( usage );
                return;
            }
            else if ( arg.equals( "-meta" ) && it.hasNext() ) {
                it.remove();
                String loc = it.next();
                it.remove();
                schMetas = TableSetSaxHandler
                          .readTableSet( new java.net.URL( loc ),
                                         ContentCoding.GZIP );
            }
        }
        if ( argList.size() != 1 ) {
            System.err.println( usage );
            System.exit( 1 );
            return;
        }
        String query = argList.remove( 0 );
        final ValidatorTable[] vtables;
        if ( schMetas != null ) {
            List<ValidatorTable> vtList = new ArrayList<ValidatorTable>();
            for ( SchemaMeta schMeta : schMetas ) {
                final String sname = schMeta.getName();
                for ( TableMeta tmeta : schMeta.getTables() ) {
                    final String tname = tmeta.getName();
                    final Collection<String> colNames;
                    ColumnMeta[] cmetas = tmeta.getColumns();
                    if ( cmetas == null ) {
                        colNames = null;
                    }
                    else {
                        colNames = new ArrayList<String>();
                        for ( ColumnMeta cmeta : cmetas ) {
                            colNames.add( cmeta.getName() );
                        }
                    }
                    vtList.add( new ValidatorTable() {
                        public String getSchemaName() {
                            return sname;
                        }
                        public String getTableName() {
                            return tname;
                        }
                        public Collection<String> getColumnNames() {
                            return colNames;
                        }
                    } );
                }
            }
            vtables = vtList.toArray( new ValidatorTable[ 0 ] );
        }
        else {
            vtables = null;
        }
        FunctionDef[] udfs = null;
        String[] geoFuncs = null;
        new AdqlValidator( vtables, udfs, geoFuncs ).validate( query );
    }

    /**
     * Defines table metadata for tables known to the validator.
     */
    public interface ValidatorTable {

        /**
         * Returns the fully-qualified name of this table,
         * which may include a schema part.
         *
         * @return  table name
         */
        String getTableName();

        /**
         * Returns the name of the schema to which this table belongs,
         * if known.  In practice, it only seems to be necessary if
         * the table name does not include a schema part.
         *
         * @return  schema name
         */
        String getSchemaName();

        /**
         * Returns a collection of column names associated with this table.
         * A null return value means that the list of column names is
         * not known.
         *
         * <p>The return value of this call may change over the lifetime of
         * this object.
         *
         * @return  column array, or null
         */
        Collection<String> getColumnNames();
    }

    /**
     * DBTable implementation that adapts a ValidatorTable instance.
     * Some of the implementation was done with reference to the
     * source code of the adql.db.DefaultDBTable class.
     */
    private static class ValidatorDBTable implements DBTable {
        private final ValidatorTable vtable_;
        private String name_;
        private String schemaName_;
        private String catalogName_;
        private AdqlSyntax syntax_;
  
        /**
         * Constructor.
         *
         * @param    vtable  validator table supplying behaviour
         */
        ValidatorDBTable( ValidatorTable vtable ) {
            vtable_ = vtable;
            syntax_ = AdqlSyntax.getInstance();
            String[] names =
                syntax_.getCatalogSchemaTable( vtable.getTableName() );

            /* I'm still not certain I'm assigning the schema name here
             * correctly, or exactly what it's used for. */
            if ( names != null ) {
                name_ = names[ 2 ];
                schemaName_ = vtable.getSchemaName();
                catalogName_ = names[ 0 ];
            }
            else {
                name_ = vtable.getTableName();
                schemaName_ = vtable.getSchemaName();
                catalogName_ = null;
            }
        }

        public String getADQLName() {
            return syntax_.unquote( name_ );
        }

        public String getDBName() {
            return name_;
        }

        public String getADQLSchemaName() {
            return syntax_.unquote( schemaName_ );
        }

        public String getDBSchemaName() {
            return schemaName_;
        }

        public String getADQLCatalogName() {
            return syntax_.unquote( catalogName_ );
        }

        public String getDBCatalogName() {
            return catalogName_;
        }

        public DBColumn getColumn( String colName, boolean isAdqlName ) {

            /* Note the value of vtable.getColumnNames may change over the
             * lifetime of the vtable object, so don't cache the result. */
            Collection<String> cnames = vtable_.getColumnNames();
            if ( cnames == null ) {
                return createDBColumn( colName );
            }
            else {
                for ( String cn : cnames ) {
                    if ( colName.equals( isAdqlName ? syntax_.unquote( cn )
                                                    : cn ) ) {
                        return createDBColumn( cn );
                    }
                }
                return null;
            }
        }

        public Iterator<DBColumn> iterator() {

            /* Note the value of vtable.getColumnNames may change over the
             * lifetime of the vtable object, so don't cache the result. */
            Collection<String> cnames = vtable_.getColumnNames();
            if ( cnames == null ) {
                return new ArrayList<DBColumn>().iterator();
            }
            else {
                final Iterator<String> it = cnames.iterator();
                return new Iterator<DBColumn>() {
                    public boolean hasNext() {
                        return it.hasNext();
                    }
                    public DBColumn next() {
                        return createDBColumn( it.next() );
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }

        public DBTable copy( String dbName, String adqlName ) {
            String dfltName = DefaultDBTable.joinTableName( new String[] {
                                  catalogName_, schemaName_, name_,
                              } );
            DefaultDBTable copy =
                new DefaultDBTable( dbName == null ? dfltName : dbName,
                                    adqlName == null ? dfltName : adqlName );
            for ( DBColumn col : this ) {
                copy.addColumn( col.copy( col.getDBName(), col.getADQLName(),
                                          copy ) );
            }
            return copy;
        }

        private DBColumn createDBColumn( String colName ) {
            String rawColName = syntax_.unquote( colName );
            return new DefaultDBColumn( colName, rawColName, this );
        }
    }
}
