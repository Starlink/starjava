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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

    /**
     * Constructor.
     *
     * @param  vtables  table metadata for database to be checked against
     * @param  allowUdfs  whether unknown functions should cause a parse error
     */
    public AdqlValidator( ValidatorTable[] vtables, boolean allowUdfs ) {
        parser_ = new ADQLParser( new ADQLQueryFactory() );
        Collection<FunctionDef> udfs = allowUdfs
                                     ? null
                                     : new ArrayList<FunctionDef>();
        checker_ = vtables == null
                 ? null
                 : new DBChecker( toDBTables( vtables ), udfs );
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
                     + " [-[no]udfs]"
                     + " <query>"
                     + "\n";
        SchemaMeta[] schMetas = null;
        boolean allowUdfs = false;
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
                          .readTableSet( new java.net.URL( loc ) );
            }
            else if ( arg.equals( "-udfs" ) ) {
                it.remove();
                allowUdfs = true;
            }
            else if ( arg.equals( "-noudfs" ) ) {
                it.remove();
                allowUdfs = false;
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
        new AdqlValidator( vtables, allowUdfs ).validate( query );
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
            String[] names =
                 DefaultDBTable.splitTableName( vtable.getTableName() );
            name_ = names[ 2 ];
            catalogName_ = names[ 0 ];
            schemaName_ = vtable.getSchemaName();
            syntax_ = AdqlSyntax.getInstance();
        }

        public String getADQLName() {
            return name_;
        }

        public String getDBName() {
            return name_;
        }

        public String getADQLSchemaName() {
            return schemaName_;
        }

        public String getDBSchemaName() {
            return schemaName_;
        }

        public String getADQLCatalogName() {
            return catalogName_;
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
            else if ( cnames.contains( colName ) ) {
                return createDBColumn( colName );
            }
            else {
                String rawColName = syntax_.unquote( colName );
                if ( ! colName.equals( rawColName ) &&
                     cnames.contains( rawColName ) ) {
                    return createDBColumn( rawColName );
                }
                else {
                    return null;
                }
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
                        return createDBColumn( syntax_.unquote( it.next() ) );
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
            return new DefaultDBColumn( colName, colName, this );
        }
    }
}
