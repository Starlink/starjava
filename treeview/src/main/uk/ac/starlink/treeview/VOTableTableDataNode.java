package uk.ac.starlink.treeview;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.votable.Field;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.Table;
import uk.ac.starlink.votable.VOTableFormatException;
import uk.ac.starlink.util.DOMUtils;

public class VOTableTableDataNode extends VOComponentDataNode 
                 implements Draggable, TableNodeChooser.Choosable {

    private Table votable;
    private StarTable startable;
    private String desc;
    private DOMSource domsrc;

    public VOTableTableDataNode( Source xsrc ) throws NoSuchDataException {
        super( xsrc, "TABLE" );
        domsrc = new DOMSource( vocel, systemId );
        try {
            votable = Table.makeTable( domsrc );
        }
        catch ( VOTableFormatException e ) {
            throw new NoSuchDataException( e );
        }
        int nrows = votable.getRowCount();
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
        Element dat = DOMUtils.getChildElementByName( vocel, "DATA" );
        if ( dat != null ) {
            Element imp = 
                    DOMUtils.getFirstElementSibling( dat.getFirstChild() );
            if ( imp != null ) {
                dv.addKeyedItem( "Data implementation", imp.getTagName() );
            }
        }

        /* Generic items. */
        addVOComponentViews( dv, vocel, systemId );
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
            startable = Tables.randomTable( new VOStarTable( domsrc ) );

            /* Remove the "Description" parameter since it is treated 
             * specially by VOTable nodes. */
            for ( Iterator it = startable.getParameters().iterator();
                  it.hasNext(); ) {
                if ( ((DescribedValue) it.next())
                    .getInfo().getName().equals( "Description" ) ) {
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
