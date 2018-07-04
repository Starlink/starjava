package uk.ac.starlink.topcat.vizier;

import java.util.ArrayList;
import java.util.List;
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
    private static List<MissionColumn<?>> createMissionColumns() {
        List<MissionColumn<?>> list = new ArrayList<MissionColumn<?>>();
        list.add( new MissionColumn<String>( "Name", String.class ) {
            public String getValue( MissionQueryable mq ) {
                return unlog( mq.item_.getName() );
            }
        } );
        list.add( new MissionColumn<String>( "Description", String.class ) {
            public String getValue( MissionQueryable mq ) {
                return mq.item_.getTitle();
            }
        } );
        list.add( new MissionColumn<Integer>( "KRows", Integer.class ) {
            public Integer getValue( MissionQueryable mq ) {
                return mq.item_.getKrows();
            }
        } );
        return list;
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

    /**
     * Utility sub-class of ArrayTableColumn for use with MissionQueryables.
     */
    private static abstract class MissionColumn<C>
            extends ArrayTableColumn<MissionQueryable,C> {

        /**
         * Constructor.
         *
         * @param  name  column name
         * @param  clazz  column content class
         */
        MissionColumn( String name, Class<C> clazz ) {
            super( name, clazz );
        }
    }
}
