package uk.ac.starlink.treeview;

import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.MouldArrayImpl;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.datanode.nodes.ColumnsMetamapGroup;
import uk.ac.starlink.datanode.nodes.ComponentMaker;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DataObjectException;
import uk.ac.starlink.datanode.nodes.DataType;
import uk.ac.starlink.datanode.nodes.DetailViewer;
import uk.ac.starlink.datanode.nodes.ExceptionComponentMaker;
import uk.ac.starlink.datanode.nodes.IconFactory;
import uk.ac.starlink.datanode.nodes.MetamapGroup;
import uk.ac.starlink.datanode.nodes.MetaTable;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.datanode.nodes.ValueInfoMetamapGroup;
import uk.ac.starlink.datanode.viewers.ArrayBrowser;
import uk.ac.starlink.datanode.viewers.StyledTextArea;
import uk.ac.starlink.datanode.viewers.TextViewer;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.Ndxs;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.util.DataSource;

/**
 * Extends DetailViewer to provide extensive information about 
 * nodes which supply certain generic data objects.
 * This class contains much of the intelligence about how to display
 * specific information about items in DataNodes found in a tree.
 * In particular it contains rules about applications which can be
 * invoked for data objects available from nodes - if a node can 
 * provide a StarTable it can start up TOPCAT for instance.
 * This cannot be done within the DATANODE package itself (though it
 * would be easier to write if it could) since DATANODE is and must
 * be compiled prior to applications like TOPCAT, SPLAT and SoG,
 * since they want to use it.
 *
 * @author   Mark Taylor (Starlink)
 * @since    27 Jan 2005
 */
public class ApplicationDetailViewer implements DetailViewer {

    private final JTabbedPane tabbed_;
    private final StyledTextArea over_;
    private final List actions_ = new ArrayList();
    private DataNode node_;
    private boolean actionsAdded_;

    /* Check that our assumptions about which classes correspond to
     * which DataTypes are correct. */
    static {
        assert DataType.DATA_SOURCE.getDataClass() == DataSource.class;
        assert DataType.TABLE.getDataClass() == StarTable.class;
        assert DataType.NDX.getDataClass() == Ndx.class;
    }

    /**
     * Constructs a DetailViewer which has an overview panel with a given name.
     *
     * @param  overName  overview panel name
     */
    public ApplicationDetailViewer( String overName ) {
        node_ = null;

        /* Construct a tabbed pane.  We have to jump through a few hoops
         * to make sure that scroll bars are always painted in the right
         * place. */
        tabbed_ = new JTabbedPane() {
            private ComponentListener listener;
            public void addNotify() {
                super.addNotify();
                listener = new ComponentAdapter() {
                    public void componentResized( ComponentEvent evt ) {
                        tabbed_.setPreferredSize( getParent().getSize() );
                        tabbed_.revalidate();
                    }
                };
                listener.componentResized( null );
                getParent().addComponentListener( listener );
            }
            public void removeNotify() {
                super.removeNotify();
                getParent().removeComponentListener( listener );
            }
        };

        /* Get the main overview pane and add it as the first item. */
        over_ = new StyledTextArea();
        addPane( overName, over_ );

        /* Ensure that the first time it is revealed, the list of actions
         * will be added as buttons at the bottom. */
        over_.addComponentListener( new ComponentAdapter() {
            public void componentShown( ComponentEvent evt ) {
                if ( ! actionsAdded_ && actions_.size() > 0 ) {
                    over_.addSeparator();
                    over_.addTitle( "External Programs" );
                    for ( Iterator it = actions_.iterator(); it.hasNext(); ) {
                        over_.addAction( (Action) it.next() );
                        if ( it.hasNext() ) {
                            over_.addSpace();
                        }
                    }
                }
                actionsAdded_ = true;
            }
        } );
    }

    /**
     * Constructs a DetailViewer which contains basic information
     * (name, node type etc) for a given DataNode.
     *
     * @param  node  the DataNode to which this viewer relates
     */
    public ApplicationDetailViewer( DataNode node ) {
        this( "Overview" );
        node_ = node;

        /* Add the items which apply to all nodes. */
        addIcon( node.getIcon() );
        addSpace();
        addTitle( node.getLabel() );
        String name = node.getName();
        if ( name.trim() != "" ) {
            addKeyedItem( "Name", node.getName() );
        }
        addKeyedItem( "Node type", node.getNodeType() );
        String path = NodeUtil.getNodePath( node );
        if ( path != null ) {
            addKeyedItem( "Path", path );
        }

        /* Add DataSource-specific items. */
        if ( node.hasDataObject( DataType.DATA_SOURCE ) ) {
            try {
                addDataSourceViews( (DataSource) 
                                    node.getDataObject( DataType
                                                       .DATA_SOURCE ) );
            }
            catch ( DataObjectException e ) {
                addPane( "Error opening stream",
                         new ExceptionComponentMaker( e ) );
            }
        }

        /* Add table-specific items. */
        if ( node.hasDataObject( DataType.TABLE ) ) {
            try {
                addTableViews( (StarTable) 
                               node.getDataObject( DataType.TABLE ) );
            }
            catch ( DataObjectException e ) {
                addPane( "Error obtaining table",
                         new ExceptionComponentMaker( e ) );
            }
        }

        /* Add NDX-specific items. */
        if ( node.hasDataObject( DataType.NDX ) ) {
            try {
                addNdxViews( (Ndx) node.getDataObject( DataType.NDX ) );
            }
            catch ( DataObjectException e ) {
                addPane( "Error obtaining NDX",
                         new ExceptionComponentMaker( e ) );
            }
        }
    }

    /**
     * Returns the main component in which this viewer represents its data.
     *
     * @return   tabbed pane
     */
    public JComponent getComponent() {
        return tabbed_;
    }

    /**
     * Selects the numbered pane for display as if the user had selected
     * the tab; number 0 is the initial
     * overview pane, and the others are in the order they were added.
     *
     * @param  index  the index of the pane to select
     */
    public void setSelectedIndex( int index ) {
        tabbed_.setSelectedIndex( index );
    }

    public void addTitle( String title ) {
        over_.addTitle( title );
    }

    public void addSubHead( String text ) {
        over_.addSubHead( text );
    }

    public void addKeyedItem( String name, String value ) {
        over_.addKeyedItem( name, value );
    }

    public void addKeyedItem( String name, Object value ) {
        addKeyedItem( name, value == null ? "null" : value.toString() );
    }

    public void addKeyedItem( String name, double value ) {
        over_.addKeyedItem( name, value );
    }

    public void addKeyedItem( String name, float value ) {
        over_.addKeyedItem( name, value );
    }

    public void addKeyedItem( String name, long value ) {
        over_.addKeyedItem( name, value );
    }

    public void addKeyedItem( String name, int value ) {
        over_.addKeyedItem( name, value );
    }

    public void addKeyedItem( String name, boolean value ) {
        over_.addKeyedItem( name, value );
    }

    public void logError( Throwable th ) {
        over_.logError( th );
    }

    public void addSeparator() {
        over_.addSeparator();
    }

    public void addText( String text ) {
        over_.addText( text );
    }

    public void addSpace() {
        over_.addSpace();
    }

    public void addIcon( Icon icon ) {
        over_.addIcon( icon );
    }

    public void addPane( String title, Component comp ) {
        tabbed_.addTab( title, comp );
    }

    public void addPane( String title, final ComponentMaker maker ) {
        final Container box = new Box( BoxLayout.X_AXIS );
        addPane( title, box );
        tabbed_.addChangeListener( new ChangeListener() {
            private boolean done = false;
            synchronized public void stateChanged( ChangeEvent evt ) {
                if ( tabbed_.getSelectedComponent() == box && ! done ) {
                    done = true;
                    tabbed_.removeChangeListener( this );
                    JComponent comp;
                    try {
                        comp = new JScrollPane( maker.getComponent() );
                    }
                    catch ( Exception e ) {
                        StyledTextArea sta = new StyledTextArea();
                        sta.addTitle( "Error" );
                        sta.setWrap( false );
                        sta.addKeyedItem( "Exception class",
                                          e.getClass().getName() );
                        sta.addKeyedItem( "Message", e.getMessage() );
                        sta.addSubHead( "Stack trace" );
                        PrintWriter pw = new PrintWriter( sta.lineAppender() );
                        e.printStackTrace( pw );
                        comp = sta;
                    }
                    box.add( comp );
                }
            }
        } );
    }

    public void addScalingPane( String title, final ComponentMaker maker ) {
        final Container box = new Box( BoxLayout.X_AXIS );
        addPane( title, box );
        tabbed_.addChangeListener( new ChangeListener() {
            private boolean done = false;
            synchronized public void stateChanged( ChangeEvent evt ) {
                if ( tabbed_.getSelectedComponent() == box && ! done ) {
                    done = true;
                    tabbed_.removeChangeListener( this );
                    JComponent comp;
                    try {
                        comp = maker.getComponent();
                    }
                    catch ( Exception e ) {
                        StyledTextArea sta = new StyledTextArea();
                        sta.addTitle( "Error" );
                        sta.setWrap( false );
                        sta.addKeyedItem( "Exception class",
                                          e.getClass().getName() );
                        sta.addKeyedItem( "Message", e.getMessage() );
                        sta.addSubHead( "Stack trace" );
                        PrintWriter pw = new PrintWriter( sta.lineAppender() );
                        e.printStackTrace( pw );
                        comp = new JScrollPane( sta );
                    }
                    box.add( comp );
                }
            }
        } );
    }

    /**
     * Adds an action which can be invoked from this viewer.
     *
     * @param  act  action
     */
    public void addAction( Action act ) {
        actions_.add( act );
    }

    /**
     * Configure this viewer for datasource-specific properties of a node.
     *
     * @param  datsrc  data source
     */
    private void addDataSourceViews( final DataSource datsrc ) {
        try {
            long size = datsrc.getLength();
            if ( size >= 0 ) {
                addKeyedItem( "Bytes", size );
            }
            byte[] intro = datsrc.getIntro();
            if ( intro.length > 0 ) {
                addPane( "Hex dump", new ComponentMaker() {
                    public JComponent getComponent() throws IOException {
                        return new HexDumper( datsrc.getInputStream(),
                                              datsrc.getLength() );
                    }
                } );
            }
        }
        catch ( final IOException e ) {
            addPane( "Error reading data", new ExceptionComponentMaker( e ) );
        }
    }

    /**
     * Configure this viewer for table-specific properties of a node.
     *
     * @param  table  table
     */
    private void addTableViews( final StarTable startable ) {

        /* Write basic information about the table. */
        int ncol = startable.getColumnCount();
        long nrow = startable.getRowCount();
        addKeyedItem( "Columns", ncol );
        if ( nrow >= 0 ) {
            addKeyedItem( "Rows", nrow );
        }
        URL url = startable.getURL();
        if ( url != null ) {
            addKeyedItem( "URL", url );
        }

        /* Construct an object which can acquire table data in a 
         * deferred way. */
        final RandomTableGetter tgetter = new RandomTableGetter( startable );

        /* Add a columns panel. */
        if ( ncol > 0 ) {
            addPane( "Columns", new ComponentMaker() {
                public JComponent getComponent() {
                    MetamapGroup metagroup =
                        new ColumnsMetamapGroup( startable );
                    return new MetaTable( metagroup );
                }
            } );
        }

        /* Add a paramaters panel. */
        final List params = startable.getParameters();
        if ( params.size() > 0 ) {
            addPane( "Parameters", new ComponentMaker() {
                public JComponent getComponent() {
                    MetamapGroup metagroup =
                        new ValueInfoMetamapGroup( params );
                    return new MetaTable( metagroup );
                }
            } );
        }

        /* Add a table data panel. */
        if ( ncol > 0 && nrow != 0 ) {
            addPane( "Table data", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    StarTable rtab = tgetter.getRandomTable();
                    StarJTable sjt = new StarJTable( rtab, true );
                    sjt.configureColumnWidths( 800, 100 );
                    return sjt;
                }
            } );
        }

        /* Add a button to launch TOPCAT. */
        if ( ncol > 0 ) {
            addAction( new TopcatDisplayAction( tgetter ) );
        }
    }

    /**
     * Configure this viewer for NDX-specific properties of a node.
     *
     * @param   ndx  NDX
     */
    public void addNdxViews( final Ndx ndx ) {
        final RandomNdxGetter ngetter = new RandomNdxGetter( ndx );

        final FrameSet ast = ( NodeUtil.hasAST() && ndx.hasWCS() ) 
                           ? ndx.getAst() 
                           : null;
        final NDShape shape = ngetter.getShape();
        final int ndim = shape.getNumDims();
        final int endim = ngetter.getEffectiveNumDims();

        /* Add view panels as appropriate. */
        if ( NodeUtil.hasAST() && ndx.hasWCS() &&
             ndim == 2 && endim == 2 ) {
            addScalingPane( "WCS grids", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    return new GridPlotter( shape, ast );
                }
            } );
        }

        addScalingPane( "Pixel values", new ComponentMaker() {
            public JComponent getComponent() throws IOException {
                if ( endim == 2 && ndim != 2 ) {
                    return new ArrayBrowser( ngetter.getEffectiveImage() );
                }
                else {
                    return new ArrayBrowser( ngetter.getRandomImage() );
                }
            }
        } );

        addPane( "Statistics", new ComponentMaker() {
            public JComponent getComponent() {
                try {
                    return new StatsViewer( ngetter.getRandomImage() );
                }
                catch ( IOException e ) {
                    return new TextViewer( e );
                }
            }
        } );

        if ( endim == 1 && NodeUtil.hasAST() ) {
            addScalingPane( "Graph view", new ComponentMaker() {
                public JComponent getComponent()
                        throws IOException, SplatException {
                    return new GraphViewer( ndx );
                }
            } );
        }

        if ( ( ndim == 2 || endim == 2 ) && NodeUtil.hasJAI() ) {
            addPane( "Image display", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    if ( endim == 2 && ndim != 2 ) {
                        return new ImageViewer( ngetter.getEffectiveImage(),
                                                null );
                    }
                    else {
                        return new ImageViewer( ngetter.getRandomImage(),
                                                ast );
                    }
                }
            } );
        }

        if ( endim > 2 && NodeUtil.hasJAI() ) {
            addPane( "Slices", new ComponentMaker() {
                public JComponent getComponent() {
                    try {
                        if ( endim != ndim ) {
                            return new SliceViewer( ngetter.getRandomImage(),
                                                    null );
                        }
                        else {
                            return new SliceViewer( ngetter.getRandomImage(),
                                                    ast );
                        }
                    }
                    catch ( IOException e ) {
                        return new TextViewer( e );
                    }
                }
            } );
        }

        if ( endim == 3 && NodeUtil.hasJAI() ) {
            addPane( "Collapsed", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    if ( endim != ndim ) {
                        return new CollapseViewer( ngetter.getEffectiveImage(),
                                                   null );
                    }
                    else {
                        return new CollapseViewer( ngetter.getRandomImage(),
                                                   ast );
                    }
                }
            } );
        }

        /* Add actions as appropriate. */
        if ( ndim == 1 && endim == 1 ) {
            final SplatNdxDisplayer displayer = SplatNdxDisplayer.getInstance();
            if ( displayer.canDisplay( ndx ) ) {
                addAction( new NdxDisplayAction( displayer, ndx, "SPLAT",
                                                 IconFactory.SPLAT ) );
            }
        }

        if ( ndim == 2 && endim == 2 ) {
            final SogNdxDisplayer displayer = SogNdxDisplayer.getInstance();
            if ( displayer.canDisplay( ndx ) ) {
                addAction( new NdxDisplayAction( displayer, ndx, "SoG",
                                                 IconFactory.SOG ) );
            }
        }
    }

    /**
     * Returns an NDArray the same as the one input, but with any
     * degenerate dimensions (ones with an extent of unity) removed.
     * This leaves the pixel sequence unchanged.  If nothing needs to
     * be done (no degenerate dimensions) the original array will be
     * returned.
     * 
     * @param   nda1   the array to be reshaped (maybe)
     * @return  an array with the same data, but no degenerate dimensions.
     *          May be the same as <tt>nda1</tt>
     */
    private static NDArray effectiveArray( NDArray nda1 ) {
        int isig = 0;
        NDShape shape1 = nda1.getShape();
        long[] origin1 = shape1.getOrigin();
        long[] dims1 = shape1.getDims();
        int ndim1 = shape1.getNumDims();
        int ndim2 = 0;
        for ( int i1 = 0; i1 < ndim1; i1++ ) {
            if ( dims1[ i1 ] > 1 ) { 
                ndim2++;
            }
        }
        long[] origin2 = new long[ ndim2 ];
        long[] dims2 = new long[ ndim2 ];
        int i2 = 0;
        for ( int i1 = 0; i1 < ndim1; i1++ ) {
            if ( dims1[ i1 ] > 1 ) {
                origin2[ i2 ] = origin1[ i1 ];
                dims2[ i2 ] = dims1[ i1 ];
                i2++;
            }
        }
        assert i2 == ndim2;
        NDShape shape2 = new NDShape( origin2, dims2 );
        assert shape2.getNumPixels() == shape1.getNumPixels();
        NDArray nda2 = new BridgeNDArray( new MouldArrayImpl( nda1, shape2 ) );
        return nda2;
    }

    /**
     * Helper class to defer creation of a random table until it's required.
     */
    private static class RandomTableGetter {
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

        public StarTable getTable() {
            return randomTable == null ? startab
                                       : randomTable;
        }

        public URL getURL() {
            return startab.getURL();
        }
    }

    /**
     * Helper class to defer creation of a random-access version of an
     * array until such time as it is required.
     */
    private static class RandomNdxGetter {
        final Ndx ndx;
        NDArray image;
        NDArray effectiveImage;
        NDShape shape;

        public RandomNdxGetter( Ndx ndx ) {
            this.ndx = ndx;
        }

        public NDShape getShape() {
            if ( shape == null ) {
                shape = ndx.getImage().getShape();
            }
            return shape;
        }

        public NDArray getRandomImage() throws IOException {
            if ( image == null ) {
                Requirements req = new Requirements( AccessMode.READ )
                                  .setRandom( true );
                image = NDArrays.toRequiredArray( Ndxs.getMaskedImage( ndx ),
                                                  req );
                shape = image.getShape();
            }
            return image;
        }

        /**
         * Returns the dimensionality of the effective array
         * (that is omitting any degenerate dimensions which only have
         * an extent of 1 pixel).
         *
         * @return effective dimensionality
         */
        public int getEffectiveNumDims() {
            long[] dims = getShape().getDims();
            int endim = 0;
            for ( int i = 0; i < dims.length; i++ ) {
                if ( dims[ i ] > 1 ) {
                    endim++;
                }
            }
            return endim;
        }

        /* Get a version with degenerate dimensions collapsed.  Should really
         * also get a corresponding WCS and use that, but I haven't worked
         * out how to do that properly yet, so the views below either use
         * the original array with its WCS or the effective array with
         * a blank WCS. */
        public NDArray getEffectiveImage() throws IOException {
            if ( effectiveImage == null ) {
                if ( shape.getNumPixels() > 1 ) {
                    effectiveImage = effectiveArray( getRandomImage() );
                }
                else {
                    effectiveImage = getRandomImage();
                }
            }
            return effectiveImage;
        }
    }

    /**
     * Helper class providing an action which displays a table.
     * It tries to do it using SOAP to an existing TOPCAT instance first,
     * but if that doesn't exist it starts up TOPCAT in the current JVM.
     */
    private class TopcatDisplayAction extends AbstractAction {

        final RandomTableGetter tgetter_;
        final URL url_;
        final String location_;
        ControlWindow controlWindow_;

        TopcatDisplayAction( RandomTableGetter tgetter ) {
            super( "TOPCAT", IconFactory.getIcon( IconFactory.TOPCAT ) );
            tgetter_ = tgetter;
            url_ = tgetter.getURL();
            location_ = url_ == null ? NodeUtil.getNodePath( node_ )
                                     : url_.toString();
        }

        public void actionPerformed( ActionEvent evt ) {
            localDisplay();
        }

        private void localDisplay() {
            if ( controlWindow_ == null ) {
                controlWindow_ = ControlWindow.getInstance();
            }
            try {
                final StarTable table = tgetter_.getRandomTable();
                controlWindow_.addTable( table, location_, true );
            }
            catch ( IOException e ) {
                Toolkit.getDefaultToolkit().beep();
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper class providing an action which invokes an NdxDisplayer.
     * It tries the RPC method first, and if that fails it does it in
     * the local JVM.  It just beeps if that fails too.
     * It tries not to do anything time-consuming in the event-dispatch
     * thread.
     */
    private static class NdxDisplayAction extends AbstractAction {
        NdxDisplayer displayer;
        Ndx ndx;
        NdxDisplayAction( NdxDisplayer displayer, Ndx ndx, String name,
                          short iconId ) {
            super( name, IconFactory.getIcon( iconId ) );
            this.displayer = displayer;
            this.ndx = ndx;
        }
        public void actionPerformed( ActionEvent evt ) {
            new Thread() {
                public void run() {
                    if ( ! displayer.soapDisplay( ndx ) ) {
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                if ( ! displayer.localDisplay( ndx, true ) ) {
                                    Toolkit.getDefaultToolkit().beep();
                                }
                            }
                        } );
                    }
                }
            }.start();
        }
    }

}
