package uk.ac.starlink.treeview;

import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.hds.*;

/**
 * A {@link uk.ac.starlink.treeview.DataNode} the HISTORY component of an NDF.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class HistoryDataNode extends DefaultDataNode {

    private static IconFactory iconMaker = IconFactory.getInstance();
    private HDSObject histobj;
    private HDSObject records;
    private int nrec;
    private Icon icon;
    private JComponent fullView;
    private String name;

    public HistoryDataNode( HDSObject histobj ) throws NoSuchDataException {
        this.histobj = histobj;

        try {
            name = histobj.datName();
            setLabel( name );

            /* See if it looks like history. */
            if ( ! name.equals( "HISTORY" ) ) {
                throw new NoSuchDataException( "Not called HISTORY" );
            }
            records = histobj.datFind( "RECORDS" );
            HDSObject currentobj = histobj.datFind( "CURRENT_RECORD" );
            int current = currentobj.datGet0i();
            long[] shape = records.datShape();
            if ( shape.length != 1 ) {
                throw new NoSuchDataException( "RECORDS object wrong shape" );
            }
            nrec = Math.min( (int) shape[ 0 ], current );
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
    }

    public HistoryDataNode( String path ) throws NoSuchDataException {
        this( HDSDataNode.getHDSFromPath( path ) );
    }

    public String getDescription() {
        return " (" + nrec + ")";
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = iconMaker.getIcon( IconFactory.HISTORY );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "HIS";
    }

    public String getNodeType() {
        return "History component of NDF";
    }

    public String getName() {
        return name;
    }

    public boolean allowsChildren() {
        return true;
    }

    public DataNode[] getChildren() {
        DataNode[] children = new DataNode[ nrec ];
        long[] pos = new long[ 1 ];
        for ( int i = 0; i < nrec; i++ ) {
            pos[ 0 ]++;
            try {
                children[ i ] = 
                    new HistoryRecordDataNode( records.datCell( pos ) );
                children[ i ].setLabel( "RECORD( " + ( i + 1 ) + " )" );
            }
            catch ( HDSException e ) {
                children[ i ] = new ErrorDataNode( e );
            }
            catch ( NoSuchDataException e ) {
                children[ i ] = new ErrorDataNode( e );
            }
        }
        return children;
    }

    public boolean hasFullView() {
        return true;
    }
    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            dv.addKeyedItem( "Records", nrec );
        }
        return fullView;
    }
}
