package uk.ac.starlink.datanode.nodes;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.SwingAuthenticator;
import uk.ac.starlink.table.jdbc.TerminalAuthenticator;
import uk.ac.starlink.util.DataSource;

/**
 * DataNode representing a StarTable.
 */
public class StarTableDataNode extends DefaultDataNode {

    private StarTable startable;
    private String name;
    private String description;

    private static StarTableFactory tabfact;

    public StarTableDataNode( StarTable startable ) {
        this.startable = startable;
        name = startable.getName();
        if ( name == null ) {
            name = "Table";
        }
        setLabel( name );
        long nrow = startable.getRowCount();
        description = new StringBuffer()
           .append( startable.getColumnCount() )
           .append( 'x' )
           .append( nrow > 0 ? Long.toString( nrow ) : "*" )
           .toString();
        setIconID( IconFactory.TABLE );
        registerDataObject( DataType.TABLE, startable );
    }

    public StarTableDataNode( String loc ) throws NoSuchDataException {
        this( makeStarTable( loc ) );
    }

    public StarTableDataNode( DataSource datsrc ) throws NoSuchDataException {
        this( makeStarTable( datsrc ) );
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getNodeTLA() {
        return "TAB";
    }

    public String getNodeType() {
        return "StarTable";
    }

    public boolean hasDataObject( DataType dtype ) {
        if ( DataType.TABLE.equals( dtype ) ) {
            return true;
        }
        else {
            return super.hasDataObject( dtype );
        }
    }

    public Object getDataObject( DataType dtype ) throws DataObjectException {
        if ( DataType.TABLE.equals( dtype ) ) {
            assert StarTable.class.equals( dtype.getDataClass() );
            return startable;
        }
        else {
            return super.getDataObject( dtype );
        }
    }
  
    public static StarTable makeStarTable( String loc )
            throws NoSuchDataException {
        try {
            return getTableFactory().makeStarTable( loc );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
    }

    public static StarTable makeStarTable( DataSource datsrc )
            throws NoSuchDataException {
        try {
            return getTableFactory().makeStarTable( datsrc );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
    }

    public static StarTableFactory getTableFactory() {
        if ( tabfact == null ) {
            tabfact = new StarTableFactory( false ) {
                public JDBCHandler getJDBCHandler() {
                    JDBCHandler handler = super.getJDBCHandler();

                    /* If we're operating a GUI by now, ensure that the
                     * authentication is done in a GUI fashion. */
                    if ( handler.getAuthenticator() 
                         instanceof TerminalAuthenticator &&
                         NodeUtil.hasGUI() ) {
                        SwingAuthenticator auth = new SwingAuthenticator();
                        handler.setAuthenticator( auth );
                    }
                    return handler;
                }
            };
        }
        return tabfact;
    }
    
}
