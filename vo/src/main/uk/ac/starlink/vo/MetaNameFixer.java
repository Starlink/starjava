package uk.ac.starlink.vo;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Provides methods for fixing up table and column names reported
 * by TAP services, so that they fit required syntactic constraints.
 * If the services are operating correctly, the behaviour provided
 * by this class should not be required.
 *
 * <p>Concrete instances must implement the abstract
 * {@link #getFixedTableName} and {@link #getFixedColumnName} methods.
 * The <code>fix*</code> methods can then be used to fix up table
 * metadata acquired from some service for use within ADQL queries.
 * The <code>getOriginal*Name</code> methods may be required for
 * subsequent communications with the service (since the original
 * names are the ones that the service knows about).
 *
 * @author   Mark Taylor
 * @since    14 May 2015
 */
public abstract class MetaNameFixer {

    /** Map from original to fixed name for tables with fixed names. */
    private final Map<String,String> origTableNames_;

    /** Map from original to fixed name for columns with fixed names. */
    private final Map<String,String> origColumnNames_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /** Instance which makes no name changes.  May be used multiple times. */
    public static MetaNameFixer NONE = new MetaNameFixer() {
        public String getFixedTableName( String tname, SchemaMeta schema ) {
            return tname;
        }
        public String getFixedColumnName( String cname ) {
            return cname;
        }
        // The fix* methods don't need to be overridden, but replacing them
        // with no-ops may improve performance.
        @Override
        public void fixSchemas( SchemaMeta[] schemas ) {
        }
        @Override
        public void fixTables( TableMeta[] tables, SchemaMeta schema ) {
        }
        @Override
        public void fixColumns( ColumnMeta[] columns ) {
        }
    };

    /**
     * Constructor.
     */
    protected MetaNameFixer() {
        origTableNames_ = new HashMap<String,String>();
        origColumnNames_ = new HashMap<String,String>();
    }

    /**
     * Returns a table name which is syntactically acceptable.
     * If the input name is OK, it should be returned unchanged.
     *
     * @param  tname  original table name
     * @param  schema   schema from which table was acquired
     * @return  original or fixed table name
     */
    public abstract String getFixedTableName( String tname, SchemaMeta schema );

    /**
     * Returns a column name which is syntactically acceptable.
     * If the input name is OK, it should be returned unchanged.
     *
     * @param  cname  original column name
     * @return  original or fixed column name
     */
    public abstract String getFixedColumnName( String cname );

    /**
     * Ensures that the given schemas and their contained tables and columns
     * have syntactically acceptable names.
     *
     * @param  schemas   schemas to fix up
     */
    public void fixSchemas( SchemaMeta[] schemas ) {
        FixCount fc0 = getCurrentFixCount();
        for ( SchemaMeta schema : schemas ) {
            TableMeta[] tables = schema.getTables();
            if ( tables != null ) {
                for ( TableMeta table : tables ) {
                    checkTable( table, schema );
                }
            }
        }
        reportFixes( fc0 );
    }

    /**
     * Ensures that the given tables and their contained columns
     * have syntactically acceptable names.
     *
     * @param  tables  tables to fix up
     * @param  schema   schema containing the given tables
     */
    public void fixTables( TableMeta[] tables, SchemaMeta schema ) {
        if ( tables != null ) {
            FixCount fc0 = getCurrentFixCount();
            for ( TableMeta table : tables ) {
                checkTable( table, schema );
            }
            reportFixes( fc0 );
        }
    }

    /**
     * Ensures that the given columns
     * have syntactically acceptable names.
     *
     * @param  columns  columns to fix up
     */
    public void fixColumns( ColumnMeta[] columns ) {
        if ( columns != null ) {
            FixCount fc0 = getCurrentFixCount();
            for ( ColumnMeta column : columns ) {
                checkColumn( column );
            }
            reportFixes( fc0 );
        }
    }

    /**
     * Returns the unfixed name for a given column.
     *
     * @param  column  column whose name may have been fixed
     * @return  column name prior to fixing
     */
    public String getOriginalColumnName( ColumnMeta column ) {
        String cname = column.getName();
        String origName = origColumnNames_.get( column.getName() );
        return origName == null ? cname : origName;
    }

    /**
     * Returns the unfixed name for a given table.
     *
     * @param   table  table whose name may have been fixed
     * @return   table name prior to fixing
     */
    public String getOriginalTableName( TableMeta table ) {
        String tname = table.getName();
        String origName = origTableNames_.get( table.getName() );
        return origName == null ? tname : origName;
    }

    /**
     * Ensures that the given table and its contents have acceptable names.
     *
     * @param  table  table to fix up
     * @param  schema  schema containing the given table
     */
    private void checkTable( TableMeta table, SchemaMeta schema ) {
        String tname = table.getName();
        if ( tname != null ) {
            String fixedName = getFixedTableName( tname, schema );
            if ( ! tname.equals( fixedName ) ) {
                table.name_ = fixedName;
                if ( origTableNames_.isEmpty() ) {
                    logger_.warning( "Fixed at least one broken TAP table name"
                                   + " (" + tname + " -> " + fixedName + ")" );
                }
                origTableNames_.put( fixedName, tname );
            }
        }
        ColumnMeta[] columns = table.getColumns();
        if ( columns != null ) {
            for ( ColumnMeta column : columns ) {
                checkColumn( column );
            }
        }
    }

    /**
     * Ensures that the given column has an acceptable name.
     *
     * @param  column  column to fix up
     */
    private void checkColumn( ColumnMeta column ) {
        String cname = column.getName();
        if ( cname != null ) {
            String fixedName = getFixedColumnName( cname );
            if ( ! cname.equals( fixedName ) ) {
                column.name_ = fixedName;
                if ( origColumnNames_.isEmpty() ) {
                    logger_.warning( "Fixed at least one broken TAP column name"
                                   + " (" + cname + " -> " + fixedName + ")" );
                }
                origColumnNames_.put( fixedName, cname );
            }
        }
    }

    /**
     * Returns an object that indicates how many tables and columns
     * have been fixed to date by this object.
     *
     * @return  fix count
     */
    private FixCount getCurrentFixCount() {
        return new FixCount( origTableNames_.size(), origColumnNames_.size() );
    }

    /**
     * Performs logging for fixes made since a given point.
     *
     * @param  fc0   fix count since which logging is to be performed
     */
    private void reportFixes( FixCount fc0 ) {
        FixCount fc1 = getCurrentFixCount();
        int ntFix = fc1.nt_ - fc0.nt_;
        int ncFix = fc1.nc_ - fc0.nc_;
        StringBuffer sbuf = new StringBuffer();
        if ( ntFix > 0 ) {
            sbuf.append( ntFix )
                .append( " bad table names" );
        }
        if ( ncFix > 0 ) {
            if ( sbuf.length() > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( ncFix )
                .append( " bad column names" );
        }
        if ( sbuf.length() > 0 ) {
            logger_.config( "Fixed " + sbuf );
        }
    }

    /**
     * Returns a new instance that follows standard ADQL syntax rules.
     * Note this instance should not be shared between multiple metadata sets.
     *
     * @return   new fixer instance
     */
    public static MetaNameFixer createDefaultFixer() {
        return createAdqlFixer( AdqlSyntax.getInstance() );
    }

    /**
     * Returns a new instance that follows rules for a particular ADQL-like
     * syntax.
     * Note this instance should not be shared between multiple metadata sets.
     *
     * @param  syntax   syntax rules
     * @return   new fixer instance
     */
    public static MetaNameFixer createAdqlFixer( final AdqlSyntax syntax ) {
        return new MetaNameFixer() {
            public String getFixedTableName( String tname, SchemaMeta schema ) {
                if ( syntax.isAdqlTableName( tname ) ) {
                    return tname;
                }
                else {
                    String sname = schema.getName();
                    if ( sname != null && sname.length() > 0 ) {
                        String sprefix = sname + ".";
                        if ( tname.startsWith( sprefix ) ) {
                            String tsub = tname.substring( sprefix.length() );
                            return syntax.quoteIfNecessary( sname )
                                 + "."
                                 + syntax.quoteIfNecessary( tsub );
                        }
                    }
                    return syntax.quote( tname );
                }
            }
            public String getFixedColumnName( String cname ) {
                return syntax.isAdqlColumnName( cname )
                     ? cname
                     : syntax.quote( cname );
            }
        };
    }

    /**
     * Aggregates two integers indicating how many fixes a fixer has made.
     */
    private static class FixCount {
        final int nt_;
        final int nc_;

        /**
         * Constructor.
         *
         * @param   nt  number of tables fixed
         * @param   nc  number of columns fixed
         */
        FixCount( int nt, int nc ) {
            nt_ = nt;
            nc_ = nc;
        }
    }
}
