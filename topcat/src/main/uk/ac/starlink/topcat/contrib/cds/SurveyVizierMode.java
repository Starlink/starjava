package uk.ac.starlink.topcat.contrib.cds;

import cds.vizier.VizieRQueryInterface;
import cds.vizier.VizieRSurvey;
import uk.ac.starlink.util.gui.ArrayTableColumn;

/**
 * VizierMode for queries of Survey catalogues.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public class SurveyVizierMode extends BasicVizierMode {

    private final VizieRQueryInterface vqi_;

    /**
     * Constructor.
     *
     * @param   vqi   vizier query interface
     */
    public SurveyVizierMode( VizieRQueryInterface vqi ) {
        super( "Surveys", createSurveyColumns() );
        vqi_ = vqi;
    }

    protected Queryable[] loadQueryables() {
        VizieRSurvey[] surveys;
        synchronized ( vqi_ ) {
            surveys =
                (VizieRSurvey[])
                vqi_.getSurveys().toArray( new VizieRSurvey[ 0 ] );
        }
        SurveyQueryable[] qs = new SurveyQueryable[ surveys.length ];
        for ( int i = 0; i < surveys.length; i++ ) {
            qs[ i ] = new SurveyQueryable( surveys[ i ] );
        }
        return qs;
    }

    /**
     * Returns the columns for display of SurveyQueryable objects.
     *
     * @return  column list
     */
    private static ArrayTableColumn[] createSurveyColumns() {
        return new ArrayTableColumn[] {
            new ArrayTableColumn( "Name", String.class ) {
                public Object getValue( Object item ) {
                    return getSurvey( item ).getSmallName();
                }
            },
            new ArrayTableColumn( "Description", String.class ) {
                public Object getValue( Object item ) {
                    return getSurvey( item ).getDescription();
                }
            },
            new ArrayTableColumn( "KRows", Integer.class ) {
                public Object getValue( Object item ) {
                    return new Integer( getSurvey( item ).getNbKRow() );
                }
            },
        };
    }

    /**
     * Obtains a VizieRSurvey object from one of the data items in the
     * table used by this object.
     *
     * @param  item   data item in suitable array table
     * @return  VizieRSurvey object
     */
    private static VizieRSurvey getSurvey( Object item ) {
        return ((SurveyQueryable) item).survey_;
    }

    /**
     * Adapter class to present a VizieRSurvey as a Queryable.
     */
    private static class SurveyQueryable implements Queryable {
        private final VizieRSurvey survey_;

        /**
         * Constructor.
         *
         * @param   survey  VizieRSurvey object
         */
        SurveyQueryable( VizieRSurvey survey ) {
            survey_ = survey;
        }

        public String getQuerySource() {
            return survey_.getSmallName();
        }

        public String getQueryId() {
            return survey_.getSmallName();
        }
    }
}
