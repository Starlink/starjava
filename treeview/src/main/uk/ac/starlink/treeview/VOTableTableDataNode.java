package uk.ac.starlink.treeview;

import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.treeview.votable.Field;
import uk.ac.starlink.treeview.votable.RandomVOStarTable;
import uk.ac.starlink.treeview.votable.Table;
import uk.ac.starlink.treeview.votable.VOTableFormatException;
import uk.ac.starlink.util.DOMUtils;

public class VOTableTableDataNode extends VOComponentDataNode {

    private Table votable;
    private StarTable startable;
    private JComponent fullView;
    private String desc;

    public VOTableTableDataNode( Source xsrc ) throws NoSuchDataException {
        super( xsrc, "TABLE" );
        try {
            votable = Table.makeTable( new DOMSource( vocel, systemId ) );
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
        catch ( VOTableFormatException e ) {
            throw new NoSuchDataException( e );
        }
        int nrows = votable.getNumRows();
        desc = "(" 
             + votable.getNumColumns() 
             + "x" 
             + ( ( nrows > 0 ) ? "" + nrows : "?" )
             + ")";
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
            if ( dat != null ) {
                Element imp = 
                    DOMUtils.getFirstElementSibling( dat.getFirstChild() );
                if ( imp != null ) {
                    dv.addKeyedItem( "Data implementation", imp.getTagName() );
                }
            }

            /* Generic items. */
            addVOComponentViews( dv, vocel );

            /* Fields. */
            dv.addSubHead( "Columns" );
            int ncol = votable.getNumColumns();
            for ( int i = 0; i < ncol; i++ ) {
                Field field = votable.getField( i );
                dv.addText( ( i + 1 ) + ": " + field.getHandle() );
            }

            /* Column view. */
            dv.addPane( "Column details", new ComponentMaker() {
                public JComponent getComponent() {
                    MetamapGroup metagroup =
                        new StarTableMetamapGroup( getStarTable() );
                    return new MetaTable( metagroup );
                }
            } );

            /* Table view. */
            if ( dat != null ) {
                dv.addPane( "Table contents", new ComponentMaker() {
                    public JComponent getComponent() {
                        return new TreeviewJTable( getStarTable() );
                    }
                } );
            }
        }
        return fullView;
    }

    private StarTable getStarTable() {
        if ( startable == null ) {
            startable = new RandomVOStarTable( votable );
        }
        return startable;
    }
}
