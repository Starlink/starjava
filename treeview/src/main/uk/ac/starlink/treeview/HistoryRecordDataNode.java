package uk.ac.starlink.treeview;

import java.util.Arrays;
import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSException;

/**
 * A {@link uk.ac.starlink.treeview.DataNode} representing a record in the
 * HISTORY component of an NDF.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class HistoryRecordDataNode extends DefaultDataNode {

    private Icon icon;
    private JComponent fullView;
    private String name;
    private String date;
    private String command;
    private String user;
    private String host;
    private String dataset;
    private String[] text;
    private static IconFactory iconMaker = IconFactory.getInstance();

    public HistoryRecordDataNode( HDSObject hobj ) 
           throws NoSuchDataException {
        try {
            name = hobj.datName();
            setLabel( name );
            for ( int i = 0; i < hobj.datNcomp(); i++ ) {
                HDSObject ch = hobj.datIndex( i + 1 );
                String cname = ch.datName();
                if ( cname.equals( "DATE" ) ) {
                    date = ch.datGet0c();
                }
                else if ( cname.equals( "COMMAND" ) ) {
                    command = ch.datGet0c();
                }
                else if ( cname.equals( "USER" ) ) {
                    user = ch.datGet0c();
                }
                else if ( cname.equals( "HOST" ) ) {
                    host = ch.datGet0c();
                }
                else if ( cname.equals( "DATASET" ) ) {
                    dataset = ch.datGet0c();
                }
                else if ( cname.equals( "TEXT" ) ) {

                    /* Iterate over elements getting text strings. 
                     * This will work regardless of text array shape 
                     * (which is possibly overkill). */
                    long[] dims = ch.datShape();
                    long[] origin = new long[ dims.length ];
                    Arrays.fill( origin, 1L );
                    OrderedNDShape oshape = 
                        new OrderedNDShape( origin, dims, Order.COLUMN_MAJOR );
                    text = new String[ (int) oshape.getNumPixels() ];
                    int j = 0;
                    for ( Iterator it = oshape.pixelIterator(); 
                          it.hasNext(); ) {
                        text[ j++ ] = ch.datCell( (long[]) it.next() )
                                        .datGet0c();
                    }
                }
            }
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
    }

    public String getDescription() {
        return ( ( date == null ) ? ""
                                  : ( " <" + date + ">" ) );
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = iconMaker.getIcon( IconFactory.HISTORY_RECORD );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "HRE";
    }

    public String getNodeType() {
        return "History record";
    }

    public String getName() {
        return name;
    }

    public boolean allowsChildren() {
        return false;
    }

    public boolean hasFullView() {
        return true;
    }
    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            if ( date != null ) {
                dv.addKeyedItem( "Date", date.toString() );
            }
            if ( command != null ) {
                dv.addKeyedItem( "Command", command );
            }
            if ( user != null ) {
                dv.addKeyedItem( "User", user );
            }
            if ( host != null ) {
                dv.addKeyedItem( "Host", host );
            }
            if ( dataset != null ) {
                dv.addKeyedItem( "Dataset", dataset );
            }
            if ( text != null ) {
                dv.addSubHead( "Text" );
                for ( int i = 0; i < text.length; i++ ) {
                    dv.addText( text[ i ] );
                }
            }
        }
        return fullView;
    }

    public String getDate() {
        return date;
    }
    public String getCommand() {
        return command;
    }
    public String getUser() {
        return user;
    }
    public String getHost() {
        return host;
    }
    public String getDataset() {
        return dataset;
    }
    public String[] getText() {
        return text;
    }
}
