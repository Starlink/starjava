package uk.ac.starlink.topcat.plot2;

import java.io.IOException;
import java.util.Arrays;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.ttools.plot2.data.AbstractDataSpec;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.UserDataReader;

/**
 * DataSpec implementation used by TOPCAT classes.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class GuiDataSpec extends AbstractDataSpec {

    private final StarTable table_;
    private final RowSubset subset_;
    private final GuiCoordContent[] contents_;

    /**
     * Constructor.
     *
     * @param  table  table supplying data
     * @param  subset  row inclusion mask
     * @param  contents   coordinate value definitions
     */
    public GuiDataSpec( StarTable table, RowSubset subset,
                        GuiCoordContent[] contents ) {
        table_ = table;
        subset_ = subset;
        contents_ = contents;
    }

    public StarTable getSourceTable() {
        return table_;
    }

    public int getCoordCount() {
        return contents_.length;
    }

    public Coord getCoord( int ic ) {
        return contents_[ ic ].getCoord();
    }

    public Object getCoordId( int ic ) {
        return Arrays.asList( contents_[ ic ].getDataLabels() );
    }

    public Object getMaskId() {
        return subset_;
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
            public boolean getMaskFlag( RowSequence rseq, long irow ) {
                return subset_.isIncluded( irow );
            }
            public Object[] getUserCoordValues( RowSequence rseq, long irow,
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
}
