package uk.ac.starlink.treeview;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.util.DOMUtils;

public class VOTableTableDataNode extends VOComponentDataNode 
                 implements Draggable, TableNodeChooser.Choosable {

    private TableElement votable;
    private StarTable startable;
    private String desc;

    public VOTableTableDataNode( Source xsrc ) throws NoSuchDataException {
        super( xsrc, "TABLE" );
        if ( ! ( getElement() instanceof TableElement ) ) {
            throw new NoSuchDataException( "Not a TABLE element" );
        }
        votable = (TableElement) getElement();
        long nrows = votable.getRowCount();
        desc = "(" 
             + votable.getColumnCount() 
             + "x" 
             + ( ( nrows >= 0 ) ? "" + nrows : "?" )
             + ")";
        setIconID( IconFactory.TABLE );
    }

    public String getNodeTLA() {
        return "TAB";
    }

    public String getNodeType() {
        return "VOTable table data";
    }

    public String getDescription() {
        return desc;
    }

    public boolean allowsChildren() {
        return false;
    }

    public void configureDetail( DetailViewer dv ) {

        /* DATA element implementation. */
        VOElement dat = vocel.getChildByName( "DATA" );
        if ( dat != null ) {
            VOElement[] dataChildren = dat.getChildren();
            if ( dataChildren.length > 0 ) {
                String imp = dataChildren[ 0 ].getTagName();
                dv.addKeyedItem( "Data implementation", imp );
            }
        }

        /* Generic items. */
        addVOComponentViews( dv, vocel );
        try {
            StarTableDataNode.addDataViews( dv, getStarTable() );
        }
        catch ( final IOException e ) {
            dv.addPane( "Error reading table", new ComponentMaker() {
                public JComponent getComponent() {
                    return new TextViewer( e );
                }
            } );
        }
    }

    public void customiseTransferable( DataNodeTransferable trans )
            throws IOException {
        StarTableDataNode.customiseTransferable( trans, getStarTable() );
    }

    public StarTable getStarTable() throws IOException {
        if ( startable == null ) {
            startable = Tables.randomTable( new VOStarTable( votable ) );

            /* Remove the "Description" parameter since it is treated 
             * specially by VOTable nodes. */
            for ( Iterator it = startable.getParameters().iterator();
                  it.hasNext(); ) {
                if ( ((DescribedValue) it.next())
                    .getInfo().getName().equalsIgnoreCase( "Description" ) ) {
                    it.remove();
                }
            }
        }
        return startable;
    }

    public boolean isStarTable() {
        return true;
    }
}
