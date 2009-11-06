package uk.ac.starlink.topcat.vizier;

import uk.ac.starlink.util.gui.ArrayTableColumn;

/**
 * VizierMode for queries of Mission catalogues.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public class MissionVizierMode extends BasicVizierMode {

    /**
     * Constructor.
     */
    public MissionVizierMode() {
        super( "Missions", createMissionColumns() );
    }

    protected Queryable[] loadQueryables() {
        InfoItem[] items = getVizierInfo().getArchives();
        int nm = items.length;
        Queryable[] queryables = new MissionQueryable[ nm ];
        for ( int i = 0; i < nm; i++ ) {
            queryables[ i ] = new MissionQueryable( items[ i ] );
        }
        return queryables;
    }

    /**
     * Returns the columns for display of MissionQueryable objects.
     *
     * @return   column list
     */
    private static ArrayTableColumn[] createMissionColumns() {
        return new ArrayTableColumn[] {
            new ArrayTableColumn( "Name", String.class ) {
                public Object getValue( Object item ) {
                    return unlog( getInfo( item ).getName() );
                }
            },
            new ArrayTableColumn( "Description", String.class ) {
                public Object getValue( Object item ) {
                    return getInfo( item ).getTitle();
                }
            },
            new ArrayTableColumn( "KRows", Integer.class ) {
                public Object getValue( Object item ) {
                    return getInfo( item ).getKrows();
                }
            },
        };
    }

    /**
     * Tidies up the InfoItem name attribute - it seems always to be 
     * prepended with the string "log".
     *
     * @param  logName  name which may have "log" prepended
     * @return   name without "log" prepended
     */
    private static String unlog( String logName ) {
        return logName.startsWith( "log" ) ? logName.substring( 3 )
                                           : logName;
    }

    /**
     * Obtains an InfoItem object from one of the queryables loaded by
     * this object.
     *
     * @param  item   data item in suitable array table
     * @return  InfoItem object
     */
    private static InfoItem getInfo( Object item ) {
        return ((MissionQueryable) item).item_;
    }

    /**
     * Adapter class to present an InfoItem as a Queryable.
     */
    private static class MissionQueryable implements Queryable {
        private final InfoItem item_;

        /**
         * Constructor.
         *
         * @param  item  InfoItem object representing a mission
         */
        MissionQueryable( InfoItem item ) {
            item_ = item;
        }

        public String getQuerySource() {
            return item_.getName();
        }

        public String getQueryId() {
            return unlog( item_.getName() );
        }
    }
}
