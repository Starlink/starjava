package uk.ac.starlink.topcat.contrib.cds;

import cds.vizier.VizieRQueryInterface;
import cds.vizier.VizieRMission;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.util.gui.ArrayTableColumn;

/**
 * VizierMode for queries of Mission catalogues.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public class MissionVizierMode extends BasicVizierMode {

    private final VizieRQueryInterface vqi_;
    private static final Pattern MISSION_SOURCE_REGEX =
        Pattern.compile( ".*&-source=([^&]*)&.*" );

    /**
     * Constructor.
     *
     * @param  vqi  vizier query interface
     */
    public MissionVizierMode( VizieRQueryInterface vqi ) {
        super( "Missions", createMissionColumns() );
        vqi_ = vqi;
    }

    protected Queryable[] loadQueryables() {
        VizieRMission[] missions;
        synchronized ( vqi_ ) {
            missions =
                (VizieRMission[])
                vqi_.getMissions().toArray( new VizieRMission[ 0 ] );
        }
        MissionQueryable[] qs = new MissionQueryable[ missions.length ];
        for ( int i = 0; i < missions.length; i++ ) {
            qs[ i ] = new MissionQueryable( missions[ i ] );
        }
        return qs;
    }

    /**
     * Returns the columns for display of MissionQueryable objects.
     *
     * @return  column list
     */
    private static ArrayTableColumn[] createMissionColumns() {
        return new ArrayTableColumn[] {
            new ArrayTableColumn( "Name", String.class ) {
                public Object getValue( Object item ) {
                    return getMission( item ).getSmallName();
                }
            },
            new ArrayTableColumn( "Description", String.class ) {
                public Object getValue( Object item ) {
                    return getMission( item ).getDescription();
                }
            },
            new ArrayTableColumn( "KRows", Integer.class ) {
                public Object getValue( Object item ) {
                    return new Integer( getMission( item ).getNbKRow() );
                }
            },
        };
    }

    /**
     * Obtains a VizieRMission object from one of the data items in the
     * table used by this object.
     *
     * @param  item  data item in suitable array table
     * @return   VizieRMission object
     */
    private static VizieRMission getMission( Object item ) {
        return ((MissionQueryable) item).mission_;
    }

    /**
     * Adapter class to present a VizieRMission as a Queryable.
     */
    private static class MissionQueryable implements Queryable {
        private final VizieRMission mission_;

        /**
         * Constructor.
         *
         * @param   mission  VizieRMission object
         */
        MissionQueryable( VizieRMission mission ) {
            mission_ = mission;
        }

        public String getQuerySource() {

            /* Complications.  There is a potential difference between the
             * result of getSmallName() and the source itself (something to
             * do with a "log" prefix.  Carve it out of a dummy search URL. */
            String uargs = mission_.getQueryUrl( "x", 0 ).getQuery();
            Matcher matcher = MISSION_SOURCE_REGEX.matcher( uargs );
            if ( matcher.matches() ) {
                String src =
                    VizierTableLoadDialog.urlDecode( matcher.group( 1 ) );
                assert src.endsWith( mission_.getSmallName() );
                return src;
            }
            else {
                assert false;
                return mission_.getSmallName();
            }
        }

        public String getQueryId() {
            return mission_.getSmallName();
        }
    }
}
