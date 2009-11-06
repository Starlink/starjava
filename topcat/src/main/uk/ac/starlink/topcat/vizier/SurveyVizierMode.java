package uk.ac.starlink.topcat.vizier;

import uk.ac.starlink.util.gui.ArrayTableColumn;

/**
 * VizierMode for queries of Survey catalogues.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public class SurveyVizierMode extends BasicVizierMode {

    /**
     * Constructor.
     */
    public SurveyVizierMode() {
        super( "Surveys", createSurveyColumns() );
    }

    protected Queryable[] loadQueryables() {
        InfoItem[] items = getVizierInfo().getSurveys();
        int ns = items.length;
        Queryable[] queryables = new SurveyQueryable[ ns ];
        for ( int i = 0; i < ns; i++ ) {
            queryables[ i ] = new SurveyQueryable( items[ i ] );
        }
        return queryables;
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
                    return getInfo( item ).getName();
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
     * Obtains an InfoItem object from one of the queryables loaded by
     * this object.
     *
     * @param  item   data item in suitable array table
     * @return  InfoItem object
     */
    private static InfoItem getInfo( Object item ) {
        return ((SurveyQueryable) item).item_;
    }

    /**
     * Adapter class to present an InfoItem as a Queryable.
     */
    private static class SurveyQueryable implements Queryable {
        private final InfoItem item_;

        /**
         * Constructor.
         *
         * @param   item  InfoItem object representing a survey
         */
        SurveyQueryable( InfoItem item ) {
            item_ = item;
        }

        public String getQuerySource() {
            return item_.getName();
        }

        public String getQueryId() {
            return item_.getName();
        }
    }
}
