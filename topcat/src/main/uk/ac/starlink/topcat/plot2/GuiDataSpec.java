package uk.ac.starlink.topcat.plot2;

import java.io.IOException;
import java.util.Arrays;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowData;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.data.AbstractDataSpec;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.UserDataReader;

/**
 * DataSpec implementation used by TOPCAT classes.
 * All DataSpecs in use in the TOPCAT application are instances of this class.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class GuiDataSpec extends AbstractDataSpec {

    private final TopcatModel tcModel_;
    private final RowSubset subset_;
    private final GuiCoordContent[] contents_;
    private final String maskId_;

    /**
     * Constructor.
     *
     * @param  tcModel  topcat model supplying data
     * @param  subset  row inclusion mask
     * @param  contents   coordinate value definitions
     */
    public GuiDataSpec( TopcatModel tcModel, RowSubset subset,
                        GuiCoordContent[] contents ) {
        tcModel_ = tcModel;
        subset_ = subset;
        contents_ = contents;
        maskId_ = subset.getMaskId();
    }

    public StarTable getSourceTable() {
        return tcModel_.getDataModel();
    }

    public int getCoordCount() {
        return contents_.length;
    }

    public Coord getCoord( int ic ) {
        return contents_[ ic ].getCoord();
    }

    public String getCoordId( int ic ) {
        String[] dataLabels = contents_[ ic ].getDataLabels();
        DomainMapper[] dms = contents_[ ic ].getDomainMappers();
        StringBuffer sbuf = new StringBuffer();
        int nu = dataLabels.length;
        for ( int iu = 0; iu < nu; iu++ ) {
            sbuf.append( dataLabels[ iu ] );
            DomainMapper dm = dms[ iu ];
            if ( dm != null ) {
                sbuf.append( "|" )
                    .append( dm.getSourceName() );
            }
            sbuf.append( ";" );
        }
        return sbuf.toString();
    }

    public String getMaskId() {
        return maskId_;
    }

    public ValueInfo[] getUserCoordInfos( int ic ) {
        ColumnData[] colDatas = contents_[ ic ].getColDatas();
        int nu = colDatas.length;
        ValueInfo[] infos = new ValueInfo[ nu ];
        for ( int iu = 0; iu < nu; iu++ ) {
            infos[ iu ] = colDatas[ iu ].getColumnInfo();
        }
        return infos;
    }

    public DomainMapper[] getUserCoordMappers( int ic ) {
        return contents_[ ic ].getDomainMappers();
    }

    public UserDataReader createUserDataReader() {
        int ncoord = contents_.length;
        final Object[][] userRows = new Object[ ncoord ][];
        for ( int ic = 0; ic < ncoord; ic++ ) {
            GuiCoordContent content = contents_[ ic ];
            int nu = content.getDataLabels().length;
            assert content.getColDatas().length == nu;
            userRows[ ic ] = new Object[ nu ];
        }

        /* Different instances of this class need to be usable concurrently,
         * according to the DataSpec contract.  I *think* these are. */
        return new UserDataReader() {
            public boolean getMaskFlag( RowData rdata, long irow ) {
                return subset_.isIncluded( irow );
            }
            public Object[] getUserCoordValues( RowData rdata, long irow,
                                                int icoord )
                    throws IOException {
                ColumnData[] cdatas = contents_[ icoord ].getColDatas();
                int nu = cdatas.length;
                Object[] userRow = userRows[ icoord ];
                for ( int iu = 0; iu < nu; iu++ ) {
                    userRow[ iu ] = cdatas[ iu ].readValue( irow );
                }
                return userRow;
            }
        };
    }

    public boolean isMaskTrue() {
        return RowSubset.ALL.equals( subset_ );
    }

    public boolean isCoordBlank( int icoord ) {
        for ( String expr : contents_[ icoord ].getDataLabels() ) {
            if ( expr != null && expr.trim().length() > 0 ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the topcat model supplying the data for this data spec.
     *
     * @return  topcat model
     */
    public TopcatModel getTopcatModel() {
        return tcModel_;
    }

    /**
     * Returns the row subset forming the row mask for this dataspec.
     *
     * @return  row subset
     */
    public RowSubset getRowSubset() {
        return subset_;
    }

    /**
     * Returns the GuiCoordContent corresponding to a particular coordinate.
     *
     * @param   ic   coord index
     * @return   user-supplied coordinate information
     */
    public GuiCoordContent getGuiCoordContent( int ic ) {
        return contents_[ ic ];
    }

    /**
     * Returns the strings supplied by the user to identify the user values
     * corresponding to a particular coordinate.
     *
     * @param   ic   coord index
     * @return   array of user input strings
     */
    public String[] getCoordDataLabels( int ic ) {
        return contents_[ ic ].getDataLabels();
    }

    /**
     * Returns the number of rows associated with this dataspec if it
     * can be determined quickly.  If it would require a count, return -1.
     *
     * @return   row count or -1
     */
    public long getKnownRowCount() {
        Long count = tcModel_.getSubsetCounts().get( subset_ );
        return count == null ? -1L : count.longValue();
    }

    /**
     * Returns the number of rows associated with this data spec.
     * In most cases this will execute quickly, but if necessary a count
     * will be carried out by scanning the associated RowSubset.
     * The result may not be 100% reliable.  If the result is not known,
     * -1 may be returned, though this shouldn't happen.
     *
     * @return   number of tuples in this object's tuple sequence,
     *           or -1 if not known (shouldn't happen)
     */
    @Slow
    public long getRowCount() {

        /* If the row count for the relevant subset is already known,
         * use that. */
        long knownCount = getKnownRowCount();
        if ( knownCount >= 0 ) {
            return knownCount;
        }

        /* If not, count it now. */
        else {
            long nrow = tcModel_.getDataModel().getRowCount();
            long count = 0;
            for ( long ir = 0; ir < nrow; ir++ ) {
                if ( subset_.isIncluded( ir ) ) {
                    count++;
                }
            }

            /* Having got the result, save it for later. */
            tcModel_.getSubsetCounts().put( subset_, Long.valueOf( count ) );
            return count;
        }
    }

    /**
     * Retrieves a TopcatModel from a data spec used within topcat.
     * It does this by casting the supplied dataSpec to a GuiDataSpec.
     * All DataSpecs within topcat are an instance of GuiDataSpec,
     * though that is not enforced at compile-time.
     *
     * @param  dataSpec  data spec
     * @return  topcat model
     */
    public static TopcatModel getTopcatModel( DataSpec dataSpec ) {
        return dataSpec == null ? null
                                : ((GuiDataSpec) dataSpec).getTopcatModel();
    }
}
