package uk.ac.starlink.vo;

import java.io.IOException;

/**
 * Defines an object that can make global queries (from the registry
 * or some registry-like entity) about all TAP services and tables
 * satisfying certain constraints.
 *
 * <p>This interface defines a couple of methods that implementations
 * must implement, and a number of interfaces defining parameter and
 * return types used by those methods.
 *
 * @author   Mark Taylor
 * @since    30 Jun 2015
 */
public interface TapServiceFinder {

    /**
     * Reads basic metadata about all known TAP services.
     * May be slow.
     *
     * @return  list of TAP services
     */
    public Service[] readAllServices() throws IOException;

    /**
     * Locates a list of tables from a global list of all known tables
     * served by all known TAP services that satisfy a given constraint.
     *
     * @param   constraint  object indicating the tables of interest
     * @return   list of tables matching constraint
     */
    public Table[] readSelectedTables( Constraint constraint )
            throws IOException;

    /**
     * Basic metadata describing a TAP service.
     */
    public interface Service {

        /**
         * Returns the IVO Identifier for this service.
         *
         * @return  service ivoid
         */
        String getId();

        /**
         * Returns the short name of this service.
         *
         * @return  service name
         */
        String getName();

        /**
         * Returns the title of this service.
         *
         * @return  service title
         */
        String getTitle();

        /**
         * Returns a textual description of this service.
         *
         * @return   service description
         */
        String getDescription();

        /**
         * Returns the TAP base URL for access to this service.
         *
         * @return  TAP base URL
         */
        String getServiceUrl();

        /**
         * Returns the number of tables provided by this service.
         * If not known, -1 may be returned.
         *
         * @return  table count, or -1
         */
        int getTableCount();
    }

    /**
     * Basic metadata describing a table provided by a TAP service.
     */
    public interface Table {

        /**
         * Returns the IVO Identifier of the service containing this table.
         *
         * @return  service ivoid
         */
        String getServiceId();

        /**
         * Returns the name of this table.
         *
         * @return  table name
         */
        String getName();

        /**
         * Returns a textual description of this table.
         *
         * @return  table description
         */
        String getDescription();
    }

    /**
     * Describes constraints on tables to be found by a certain query.
     */
    public interface Constraint {

        /**
         * Returns a list of search terms to be matched against target items.
         *
         * @return   search keywords
         */
        String[] getKeywords();

        /**
         * Returns a list of the metadata items against which the supplied
         * keywords are to be matched.
         *
         * @return   search targets
         */
        Target[] getTargets();

        /**
         * Indicates how the search terms will be combined when matching
         * against the search targets.
         *
         * @return   if true, all keywords must be matched,
         *           if false, any keyword must be matched
         */
        boolean isAndKeywords();
    }

    /**
     * Enumerates those metadata items against which search terms
     * can be matched.
     */
    public enum Target {

        /** Table name. */
        TABLE_NAME( "Table Name", false,
                    "table_name", "table_name", "table_name" ),

        /** Table description. */
        TABLE_DESCRIP( "Table Description", true,
                       "table_desc", "table_description", "table_description" ),

        /** Service name. */
        SERVICE_META( "Service", false, null, null, null );

        private final String displayName_;
        private final boolean isWords_;
        private final String glotsTablesCol_;
        private final String rrTablesCol_;
        private final String rrTapTablesCol_;

        /**
         * Constructor.
         *
         * @param  displayName   label for display in user interface
         * @param  isWords    true if the content of this item is to be
         *                    interpreted as a bag of words,
         *                    false if it's more like a single string
         * @param  glotsTablesCol  column name in GAVO glots.tables table
         *                         corresponding to this value;
         *                         null if not applicable
         * @param  rrTablesCol   column name in RegTAP rr.tables table
         *                       corresponding to this value;
         *                       null if not applicable
         * @param  rrTapTablesCol  column name in RegTAP 1.2 rr.tap_tables table
         *                         corresponding to this value;
                                   null if not applicable
         */
        Target( String displayName, boolean isWords, String glotsTablesCol,
                String rrTablesCol, String rrTapTablesCol ) {
            displayName_ = displayName;
            isWords_ = isWords;
            glotsTablesCol_ = glotsTablesCol;
            rrTablesCol_ = rrTablesCol;
            rrTapTablesCol_ = rrTapTablesCol;
        }

        /**
         * Returns the name to be used for identifying this target in the
         * user interface.
         *
         * @return   display name
         */
        public String getDisplayName() {
            return displayName_;
        }

        /**
         * Indicates whether this target is more like a bag of words or a
         * single string.
         *
         * @return true for bag of words, false for single string
         */
        boolean isWords() {
            return isWords_;
        }

        /**
         * Indicates whether this target is for matching against service
         * metadata.  Currently, all targets are either SERVICE_META
         * or one of the ones with a GloTS column.  If that changes,
         * the definition or implementation of this method will change.
         *
         * @return  true iff this instance is SERVICE_META
         */
        boolean isServiceMeta() {
            return glotsTablesCol_ == null;
        }

        /**
         * Returns name of the column in the glots.tables table to which 
         * this target corresponds.
         *
         * @return  glots.tables column name; null iff isServiceMeta()==true
         */
        String getGlotsTablesCol() {
            return glotsTablesCol_;
        }

        /**
         * Returns the name of the column in the RegTAP rr.tables table
         * to which this target corresponds.
         *
         * @return  rr.tables column name; null iff isServiceMeta()==true
         */
        String getRrTablesCol() {
            return rrTablesCol_;
        }

        /**
         * Returns the name of the column in the RegTAP 1.2 rr.tap_tables table
         * to which this target corresponds.
         *
         * @return  rr.tap_tables column name; null iff isServiceMeta()==true
         */
        String getRrTapTablesCol() {
            return rrTapTablesCol_;
        }

        /**
         * Tests whether a given service matches given search terms
         * under the rules of this constraint.  May only be executed
         * if {@link #isServiceMeta} returns true.
         * 
         * @param   service   service to test
         * @param   keywords  search terms
         * @param   isAnd  true to combine search terms with AND, false for OR
         * @return  true iff service matches search terms
         * @throws   UnsupportedOperationException if isServiceMeta()==false
         */
        boolean matchesService( Service service, String[] keywords,
                                boolean isAnd ) {
            if ( isServiceMeta() ) {
                String[] kws = keywords.clone();
                for ( int i = 0; i < kws.length; i++ ) {
                    kws[ i ] = kws[ i ].toLowerCase();
                }
                return matchText( service.getId(), kws, isAnd )
                    || matchText( service.getName(), kws, isAnd )
                    || matchText( service.getTitle(), kws, isAnd );
            }
            else {
                throw new UnsupportedOperationException( "Wrong target type" );
            }
        }

        /**
         * Determines whether a test string matches a list of keywords.
         *
         * @param   txt   string to test
         * @param   lcKeywords  search terms folded to lower case
         * @param   isAnd  true to combine search terms with AND, false for OR
         * @return   true iff txt matches search terms
         */
        private static boolean matchText( String txt, String[] lcKeywords,
                                          boolean isAnd ) {
            txt = txt.toLowerCase();
            if ( isAnd ) {
                for ( String kw : lcKeywords ) {
                    if ( txt.indexOf( kw ) < 0 ) {
                        return false;
                    }
                }
                return true;
            }
            else {
                for ( String kw : lcKeywords ) {
                    if ( txt.indexOf( kw ) >= 0 ) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
