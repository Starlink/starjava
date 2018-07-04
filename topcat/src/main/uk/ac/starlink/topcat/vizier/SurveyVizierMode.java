package uk.ac.starlink.topcat.vizier;

import java.util.ArrayList;
import java.util.List;
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
    private static List<SurveyColumn<?>> createSurveyColumns() {
        List<SurveyColumn<?>> list = new ArrayList<SurveyColumn<?>>();
        list.add( new SurveyColumn<String>( "Name", String.class ) {
            public String getValue( SurveyQueryable survey ) {
                return survey.item_.getName();
            }
        } );
        list.add( new SurveyColumn<String>( "Description", String.class ) {
            public String getValue( SurveyQueryable survey ) {
                return survey.item_.getTitle();
            }
        } );
        list.add( new SurveyColumn<Integer>( "KRows", Integer.class ) {
            public Integer getValue( SurveyQueryable survey ) {
                return survey.item_.getKrows();
            }
        } );
        return list;
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

    /**
     * Utility sub-class of ArrayTableColumn for use with SurveyQueryables.
     */
    private static abstract class SurveyColumn<C>
            extends ArrayTableColumn<SurveyQueryable,C> {

        /**
         * Constructor.
         *
         * @param  name  column name
         * @param  clazz  column content class
         */
        SurveyColumn( String name, Class<C> clazz ) {
            super( name, clazz );
        }
    }
}
