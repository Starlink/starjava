package uk.ac.starlink.table;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapper table which makes deep copies of the table metadata,
 * including column metadata and table parameters.
 * Changes can safely be made to the metadata of this table without
 * affecting the base table.
 * Note that the metadata will not track changes in the column count
 * of the base table.
 * 
 * @author   Mark Taylor
 * @since    23 Jul 2010
 */
public class MetaCopyStarTable extends WrapperStarTable {

    private String name_;
    private URL url_;
    private List paramList_;
    private ColumnInfo[] colInfos_;

    /**
     * Constructor.
     *
     * @param  base   base table
     */
    public MetaCopyStarTable( StarTable base ) {
        super( base );
        name_ = base.getName();
        url_ = base.getURL();
        paramList_ = new ArrayList();
        for ( Iterator it = base.getParameters().iterator(); it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof DescribedValue ) {
                DescribedValue dval = (DescribedValue) item;
                paramList_.add( new DescribedValue( dval.getInfo(),
                                                    dval.getValue() ) );
            }
        }
        int ncol = base.getColumnCount();
        colInfos_ = new ColumnInfo[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            colInfos_[ icol ] = new ColumnInfo( base.getColumnInfo( icol ) );
        }
    }

    public String getName() {
        return name_;
    }

    public void setName( String name ) {
        name_ = name;
    }

    public URL getURL() {
        return url_;
    }

    public void setURL( URL url ) {
        url_ = url;
    }

    public List getParameters() {
        return paramList_;
    }

    public void setParameters( List paramList ) {
        paramList_ = paramList;
    }

    public DescribedValue getParameterByName( String name ) {
        for ( Iterator it = getParameters().iterator(); it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof DescribedValue &&
                 name.equals( ((DescribedValue) item).getInfo().getName() ) ) {
                return (DescribedValue) item;
            }
        }
        return null;
    }

    public void setParameter( DescribedValue dval ) {
        String name = dval.getInfo().getName();
        List paramList = getParameters();
        for ( Iterator it = paramList.iterator(); it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof DescribedValue &&
                 name.equals( ((DescribedValue) item).getInfo().getName() ) ) {
                it.remove();
            }
        }
        paramList.add( dval );
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public void setColumnInfo( int icol, ColumnInfo colInfo ) {
        colInfos_[ icol ] = colInfo;
    }
}
