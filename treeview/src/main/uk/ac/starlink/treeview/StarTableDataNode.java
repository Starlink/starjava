package uk.ac.starlink.treeview;

import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
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
import uk.ac.starlink.table.formats.TextTableBuilder;
import uk.ac.starlink.table.formats.WDCTableBuilder;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.SwingAuthenticator;
import uk.ac.starlink.table.jdbc.TerminalAuthenticator;
import uk.ac.starlink.topcat.TableViewer;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * DataNode representing a StarTable.
 */
public class StarTableDataNode extends DefaultDataNode
                 implements Draggable, TableNodeChooser.Choosable {

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

    public void configureDetail( DetailViewer dv ) {
        addDataViews( dv, startable );
    }

    public static void addDataViews( DetailViewer dv, 
                                     final StarTable startable ) {

        /* Get and display info about the table. */
        int ncols = startable.getColumnCount();
        long nrows = startable.getRowCount();
        dv.addKeyedItem( "Columns", ncols );
        if ( nrows >= 0 ) {
            dv.addKeyedItem( "Rows", nrows );
        }
        URL url = startable.getURL();
        if ( url != null ) {
            dv.addKeyedItem( "URL", url );
        }

        class RandomTableGetter {
            StarTable startab;
            StarTable randomTable;
            public RandomTableGetter( StarTable startab ) {
                this.startab = startab;
            }
            public StarTable getRandomTable() throws IOException {
                if ( randomTable == null ) {
                    randomTable = Tables.randomTable( startab );
                }
                return randomTable;
            }
        }
        final RandomTableGetter tgetter = new RandomTableGetter( startable );

        int npar = 0;
        final List params = startable.getParameters();
        if ( startable.getParameters().size() > 0 ) {
            dv.addSubHead( "Parameters" );
            for ( Iterator it = startable.getParameters().iterator();
                  it.hasNext(); ) {
                Object item = it.next();
                if ( item instanceof DescribedValue ) {
                    DescribedValue dval = (DescribedValue) item;
                    dv.addKeyedItem( dval.getInfo().getName(), 
                                     dval.getValueAsString( 250 ) );
                    npar++;
                }
            }
        }

        dv.addSubHead( "Columns" );
        for ( int i = 0; i < ncols; i++ ) {
            ColumnInfo info = startable.getColumnInfo( i );
            dv.addKeyedItem( "Column " + ( i + 1 ), info.getName() );
            if ( i > 4 ) {
                dv.addText( "    ..." );
                break;
            }
        }
        if ( ncols > 0 ) {
            dv.addPane( "Columns", new ComponentMaker() {
                public JComponent getComponent() {
                    MetamapGroup metagroup = 
                        new ColumnsMetamapGroup( startable );
                    return new MetaTable( metagroup );
                }
            } );
        }
        if ( npar > 0 ) {
            dv.addPane( "Parameters", new ComponentMaker() {
                public JComponent getComponent() {
                    MetamapGroup metagroup = 
                        new ValueInfoMetamapGroup( params );
                    return new MetaTable( metagroup );
                }
            } );
        }
        if ( ncols > 0 && nrows != 0 ) {
            dv.addPane( "Table data", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    StarTable rtab = tgetter.getRandomTable();
                    StarJTable sjt = new StarJTable( rtab, true );
                    sjt.configureColumnWidths( 800, 100 );
                    return sjt;
                }
            } );
        }

        List actions = new ArrayList();
        if ( ncols > 0 ) {
            Icon tcic = IconFactory.getIcon( IconFactory.TOPCAT );
            Action topcatAct = new AbstractAction( "TOPCAT", tcic ) {
                public void actionPerformed( ActionEvent evt ) {
                    try {
                        new TableViewer( tgetter.getRandomTable(), null );
                    }
                    catch ( IOException e ) {
                        beep();
                        e.printStackTrace();
                    }
                }
            };
            actions.add( topcatAct );
        }
        dv.addActions( (Action[]) actions.toArray( new Action[ 0 ] ) );
    }

    public static void customiseTransferable( DataNodeTransferable trans,
                                              final StarTable startable ) {
        DataSource datsrc = new DataSource() {
            public URL getURL() {
                return startable.getURL();
            }
            public String getName() {
                String name = startable.getName();
                return name == null ? "Table" : name;
            }
            public InputStream getRawInputStream() throws IOException {
                PipedOutputStream ostrm = new PipedOutputStream();
                PipedInputStream istrm = new PipedInputStream( ostrm );
                final PrintStream pstrm = new PrintStream( ostrm );
                new Thread() {
                    public void run() {
                        try {
                            new VOTableWriter().writeStarTable( startable, 
                                                                pstrm );
                        }
                        catch ( IOException e ) {
                            // May well catch an IOException if the reader
                            // stops reading
                        }
                        finally {
                            pstrm.close();
                        }
                    }
                }.start();
                return istrm;
            }
        };
        trans.addDataSource( datsrc, "application/xml" );
    }

    public void customiseTransferable( DataNodeTransferable trans ) {
        customiseTransferable( trans, startable );
    }

    public StarTable getStarTable() {
        return startable;
    }

    public boolean isStarTable() {
        return true;
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
            tabfact = new StarTableFactory() {
                public JDBCHandler getJDBCHandler() {
                    JDBCHandler handler = super.getJDBCHandler();

                    /* If we're operating a GUI by now, ensure that the
                     * authentication is done in a GUI fashion. */
                    if ( handler.getAuthenticator() 
                         instanceof TerminalAuthenticator &&
                         TreeviewUtil.hasGUI() ) {
                        SwingAuthenticator auth = new SwingAuthenticator();
                        handler.setAuthenticator( auth );
                    }
                    return handler;
                }
            };

            /* Remove some of the more generic tablel builders from this
             * factory.  TextTableBuilder, for instance, tries to build
             * a table from pretty much any stream, and eventually fails
             * only after it's read enough to make sure that it's 
             * definitely not a text-format table.  This would end up doing
             * much more work than we want to.  We will forego the 
             * possibility of identifying tables in these free-text formats. */
            for ( Iterator it = tabfact.getBuilders().iterator();
                  it.hasNext(); ) {
                TableBuilder builder = (TableBuilder) it.next();
                if ( builder instanceof TextTableBuilder ||
                     builder instanceof WDCTableBuilder ) {
                    it.remove();
                }
            }
        }
        return tabfact;
    }
    
}
