package uk.ac.starlink.treeview;

import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import uk.ac.starlink.treeview.votable.Field;
import uk.ac.starlink.treeview.votable.Table;
import uk.ac.starlink.treeview.votable.VOTableFormatException;
import uk.ac.starlink.util.DOMUtils;

public class VOTableTableDataNode extends VOComponentDataNode {

    private Table votable;
    private JComponent fullView;

    public VOTableTableDataNode( Source xsrc ) throws NoSuchDataException {
        super( xsrc, "TABLE" );
        try {
            votable = Table.makeTable( new DOMSource( vocel ) );
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
        catch ( VOTableFormatException e ) {
            throw new NoSuchDataException( e );
        }
    }

    public String getNodeTLA() {
        return "TAB";
    }

    public String getNodeType() {
        return "VOTable table data";
    }

    public short getIconId() {
        return IconFactory.TABLE;
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

            /* DATA element implementation. */
            Element dat = DOMUtils.getChildElementByName( vocel, "DATA" );
            dv.addKeyedItem( "Data implementation", 
                             ( dat != null ) ? dat.getTagName() : "none" );

            /* Fields. */
            dv.addSubHead( "Fields" );
            int ncol = votable.getNumColumns();
            for ( int i = 0; i < ncol; i++ ) {
                Field field = votable.getField( i );
                dv.addText( i + ": " + field );
            }

            /* Table view. */
            if ( dat != null ) {
                dv.addPane( "Table data", new ComponentMaker() {
                    public JComponent getComponent() {
                        return new VOTableViewer( votable );
                    }
                } );
            }

            /* Generic items. */
            addVOComponentViews( dv, vocel );
        }
        return fullView;
    }
}
