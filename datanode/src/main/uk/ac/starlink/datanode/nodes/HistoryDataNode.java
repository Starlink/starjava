package uk.ac.starlink.datanode.nodes;

import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;

/**
 * A {@link uk.ac.starlink.datanode.nodes.DataNode} representing 
 * the HISTORY component of an NDF.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class HistoryDataNode extends DefaultDataNode {

    private HDSObject histobj;
    private HDSObject records;
    private int nrec;
    private String name;
    private String created;

    public HistoryDataNode( HDSObject histobj ) throws NoSuchDataException {
        this.histobj = histobj;

        try {
            name = histobj.datName();
            setLabel( name );

            /* See if it looks like history. */
            if ( ! name.equals( "HISTORY" ) ) {
                throw new NoSuchDataException( "Not called HISTORY" );
            }
            if ( ! histobj.datThere( "RECORDS" ) ) {
                throw new NoSuchDataException( "No RECORDS element" );
            }

            /* Get the history records array. */
            records = histobj.datFind( "RECORDS" );

            /* Get and validate the shape of the records array. */
            long[] shape = records.datShape();
            if ( shape.length != 1 ) {
                throw new NoSuchDataException( "RECORDS object wrong shape" );
            }

            /* Get CREATED object if we can. */
            if ( histobj.datThere( "CREATED" ) ) {
                HDSObject crobj = histobj.datFind( "CREATED" );
                if ( crobj.datShape().length == 0 ) {
                    created = crobj.datGet0c();
                }
            }

            /* Get the current record if we can. */
            nrec = (int) shape[ 0 ];
            if ( histobj.datThere( "CURRENT_RECORD" ) ) {
                HDSObject curobj = histobj.datFind( "CURRENT_RECORD" );
                int current = curobj.datGet0i();
                if ( nrec > current ) {
                    nrec = current;
                }
            }
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( "Error parsing HISTORY object",  e );
        }
        setIconID( IconFactory.HISTORY );
    }

    public HistoryDataNode( String path ) throws NoSuchDataException {
        this( HDSDataNode.getHDSFromPath( path ) );
    }

    public String getDescription() {
        return " (" + nrec + ")";
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
        return false;
    }

    public void configureDetail( DetailViewer dv ) {
        dv.addKeyedItem( "Records", nrec );
        if ( created != null ) {
            dv.addKeyedItem( "Created", created );
        }
        dv.addPane( "History records", new ComponentMaker() {
            public JComponent getComponent() throws HDSException {
                MetamapGroup mmg = new HistoryMetamapGroup( records, nrec );
                return new MetaTable( mmg );
            }
        } );
    }

    private static class HistoryMetamapGroup extends MetamapGroup {

        private static List keyOrder = Arrays.asList( new String[] {
            "ITEM", "DATE", "COMMAND", "USER", "HOST", "DATASET", "TEXT",
        } );

        public HistoryMetamapGroup( HDSObject records, int nrec )
                throws HDSException {
            super( nrec );
            setKeyOrder( keyOrder );
            long[] pos = new long[ 1 ];
            for ( int i = 0; i < nrec; i++ ) {
                addEntry( i, "ITEM", Integer.toString( i + 1 ) );
                pos[ 0 ]++;
                HDSObject rec = records.datCell( pos );
                int ncomp = rec.datNcomp();
                for ( int j = 0; j < ncomp; j++ ) {
                    HDSObject item = rec.datIndex( j + 1 );
                    String name = item.datName();
                    long[] shape = item.datShape();
                    if ( shape.length == 0 ) {
                        addEntry( i, name, item.datGet0c() );
                    }
                    else if ( shape.length == 1 ) {
                        int nline = (int) shape[ 0 ];
                        String[] lines = new String[ nline ];
                        long[] kpos = new long[ 1 ];
                        for ( int k = 0; k < nline; k++ ) {
                            kpos[ 0 ]++;
                            HDSObject line = item.datCell( kpos );
                            lines[ k ] = line.datGet0c();
                        }
                        addEntry( i, name, lines );
                    }
                    else {
                        addEntry( i, name, "??" );
                    }
                }
            }
        }
    }
}
