package uk.ac.starlink.topcat.plot2;

import gnu.jel.CompilationException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ConstantColumn;
import uk.ac.starlink.table.Domain;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ColumnDataComboBox;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.DomainMapperComboBox;
import uk.ac.starlink.topcat.IdentifiedColumnData;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.StorageType;
import uk.ac.starlink.util.gui.ComboBoxBumper;

/**
 * Basic implementation of CoordPanel.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class BasicCoordPanel implements CoordPanel {

    private final Coord[] coords_;
    private final ConfigSpecifier cspec_;
    private final ActionForwarder forwarder_;
    private final List<List<ColumnDataComboBox>> colSelectors_;
    private final CoordStack stack_;
    private final JComponent panel_;   
    private final JComponent controlBox_;
    private TopcatModel tcModel_;

    /**
     * Constructs a CoordPanel for selecting just Coords.
     *
     * @param  coords  coordinate definitions for which values are required
     */
    public BasicCoordPanel( Coord[] coords ) {
        this( coords, new ConfigKey<?>[ 0 ] );
    }

    /**
     * Constructs a CoordPanel for selecting Coords and Config values.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  configKeys   config value keys
     */
    public BasicCoordPanel( Coord[] coords, ConfigKey<?>[] configKeys ) {
        this( coords, configKeys, createDefaultStack() );
    }

    /**
     * Constructs a CoordPanel for selecting Coords and Config values
     * with a custom stack.
     * For the stack parameter {@link #createDefaultStack} may be used
     * if there are no special requirements.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  configKeys   config value keys
     * @param  stack   coord stack implementation
     */
    public BasicCoordPanel( Coord[] coords, ConfigKey<?>[] configKeys,
                            CoordStack stack ) {
        panel_ = new JPanel( new BorderLayout() );
        coords_ = coords;
        stack_ = stack;
        forwarder_ = new ActionForwarder();
        controlBox_ = Box.createVerticalBox();

        /* Set up config specifiers for the given config keys. */
        cspec_ = new ConfigSpecifier( configKeys );
        cspec_.addActionListener( forwarder_ );
        if ( configKeys.length > 0 ) {
            controlBox_.add( new LineBox( null, cspec_.getComponent(), true ) );
        }

        /* Place entry components for each required coordinate. */
        int nc = coords.length;
        colSelectors_ = new ArrayList<List<ColumnDataComboBox>>();
        for ( int ic = 0; ic < nc; ic++ ) {
            Input[] inputs = coords[ ic ].getInputs();
            int ni = inputs.length;
            colSelectors_.add( new ArrayList<ColumnDataComboBox>() );
            for ( int ii = 0; ii < ni; ii++ ) {
                InputMeta meta = inputs[ ii ].getMeta();
                Domain<?> domain = inputs[ ii ].getDomain();
                final ColumnDataComboBox cs =
                    new ColumnDataComboBox( domain,
                                            ColumnDataComboBox
                                           .DFLT_AUTOCOMPLETE );
                colSelectors_.get( ic ).add( cs );
                cs.addActionListener( forwarder_ );
                JComponent line = Box.createHorizontalBox();
                line.add( cs );
                line.add( Box.createHorizontalStrut( 5 ) );
                line.add( new ComboBoxBumper( cs ) );
                DomainMapperComboBox dmSelector = cs.getDomainMapperSelector();
                if ( dmSelector != null ) {
                    dmSelector.addActionListener( forwarder_ );
                    line.add( Box.createHorizontalStrut( 10 ) );
                    line.add( dmSelector.getComponent() );
                }

                /* Set the width to a small value, but add it to the stack
                 * with xfill true.  This has the effect of making it 
                 * fill the width of the panel, and not force horizontal
                 * scrolling until it's really small.  Since the panel is
                 * not basing its size on the preferred size of these
                 * components, that works out OK. */
                Dimension size = new Dimension( cs.getMinimumSize() );
                size.width = 80;
                cs.setMinimumSize( size );
                cs.setPreferredSize( cs.getMinimumSize() );
                JLabel label = stack_.addCoordLine( meta.getLongName(), line );

                /* Arrange for the coordinate entry labels to display tooltips
                 * giving the current value in a stilts-friendly format. */
                label.addMouseListener( InstantTipper.getInstance() );
                final String shortName = meta.getShortName();
                ActionListener tipListener = new ActionListener() {
                    public void actionPerformed( ActionEvent evt ) {
                        StringBuffer tbuf = new StringBuffer()
                            .append( shortName )
                            .append( '=' );
                        Object colItem = cs.getSelectedItem();
                        if ( colItem instanceof ColumnData ) {
                            tbuf.append( colItem.toString() );
                        }
                        label.setToolTipText( tbuf.toString() );
                    }
                };
                cs.addActionListener( tipListener );
                tipListener.actionPerformed( null );
            }
        }
        if ( nc > 0 ) {
            controlBox_.add( new LineBox( null, stack_.getPanel(), true ) );
        }

        /* Place the lot at the top of the component so it doesn't fill
         * vertical space. */
        panel_.add( controlBox_, BorderLayout.NORTH );
    }

    /**
     * Adds a row of buttons to the control panel for a given array of actions.
     *
     * @param  acts  custom  actions
     */
    public void addButtons( Action[] acts ) {
        Box line = Box.createHorizontalBox();
        for ( Action act : acts ) {
            line.add( new JButton( act ) );
            line.add( Box.createHorizontalStrut( 10 ) );
        }
        line.add( Box.createHorizontalGlue() );
        controlBox_.add( new LineBox( null, line, true ) );
    }

    /**
     * Returns the coordinates which this panel is getting values for.
     *
     * @return  coords
     */
    public Coord[] getCoords() {
        return coords_;
    }

    /**
     * Returns this panel's config specifier.
     *
     * @return  specifier for config values, if there are any
     */
    public ConfigSpecifier getConfigSpecifier() {
        return cspec_;
    }

    /**
     * Returns the config map associated with this panel.
     *
     * @return   result of <code>getConfigSpecifier().getSpecifiedValue()</code>
     */
    public ConfigMap getConfig() {
        return cspec_.getSpecifiedValue();
    }

    /**
     * Returns the graphical component for this object.
     *
     * @return  component
     */
    public JComponent getComponent() {
        return panel_;
    }

    /**
     * Returns the component listing all the coordinates.
     *
     * @return  coordinate stack
     */
    public CoordStack getStack() {
        return stack_;
    }

    /**
     * Adds a listener which will be notified when the coordinate selection
     * changes.
     *
     * @param  listener  listener
     */
    public void addActionListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    /**
     * Removes a listener which was added previously.
     *
     * @param  listener  listener
     */
    public void removeActionListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    /**
     * Returns an object which will forward actions to listeners registered
     * with this panel.
     *
     * @return  action forwarder
     */
    public ActionForwarder getActionForwarder() {
        return forwarder_;
    }

    /**
     * Sets the table with reference to which this panel will resolve
     * coordinate descriptions.
     *
     * <p>If the existing selected coordinate values still make sense
     * (if the new table has sufficiently compatible column names),
     * they are retained.  If the columns cannot be retained they are
     * cleared, and in that case if the <code>autopopulate</code> parameter
     * is set, some default columns will be used.
     *
     * @param  tcModel   table from which coordinate values will be drawn
     * @param  autoPopulate   whether to autopopulate columns when old ones
     *                        can't be used or are absent
     */
    public void setTable( TopcatModel tcModel, boolean autoPopulate ) {
        tcModel_ = tcModel;
        int is = 1;
        int ninPreferred = 0;
        int ninPopulated = 0;
        for ( int ic = 0; ic < coords_.length; ic++ ) {
            List<ColumnDataComboBox> colsels = colSelectors_.get( ic );
            Coord coord = coords_[ ic ];
            boolean isPref = isPreferredCoord( coord );
            Input[] inputs = coord.getInputs();
            int ni = colsels.size();
            if ( isPref ) {
                ninPreferred += ni;
            }
            for ( int ii = 0; ii < ni; ii++ ) {
                InputMeta meta = inputs[ ii ].getMeta();
                JComboBox<ColumnData> cs = colsels.get( ii );
                Object sel0 = cs.getSelectedItem();
                String str0 = sel0 instanceof ColumnData
                            ? sel0.toString()
                            : null;
                cs.setSelectedItem( null );
                if ( tcModel == null ) {
                    cs.setEnabled( false );
                }
                else {
                    final Domain<?> domain = inputs[ ii ].getDomain();
                    ColumnDataComboBoxModel.Filter filter =
                        info -> domain.getPossibleMapper( info ) != null;
                    ColumnDataComboBoxModel model =
                        new ColumnDataComboBoxModel( tcModel, filter, true,
                                                     false );

                    /* If there was a previous value for the column,
                     * and if it can be used with the new table, re-use it. */
                    if ( str0 != null ) {
                        ColumnData cdata;
                        try {
                            cdata = model.stringToColumnData( str0 );
                        }
                        catch ( CompilationException e ) {
                            cdata = null;
                        }
                        if ( cdata != null ) {
                            model.setSelectedItem( cdata );
                            if ( isPref ) {
                                ninPopulated++;
                            }
                        }
                    }

                    cs.setModel( model );
                    cs.setEnabled( true );
                }
            }
        }

        /* Autopopulate only if none of the existing columns can be used.  
         * There are other possibilities, such as autopopulating those
         * columns which can't be re-used, but for now keep it simple. */
        if ( autoPopulate && ninPopulated == 0 && ninPreferred > 0 ) {
            autoPopulate();
        }
    }

    /**
     * Makes some attempt to fill in the fields with non-blank values.
     * The default implementation fills in the first few suitable columns,
     * but subclasses are encouraged to override this behaviour if something
     * smarter is possible.
     */
    public void autoPopulate() {
        int is = -1;
        for ( int ic = 0; ic < coords_.length; ic++ ) {
            if ( coords_[ ic ].isRequired() ) {
                for ( JComboBox<ColumnData> cs : colSelectors_.get( ic ) ) {
                    is = getNextSuitableSelectorIndex( cs, is + 1 );
                    if ( is < cs.getItemCount() ) {
                        cs.setSelectedIndex( is );
                    }
                }
            }
        }
    }

    /**
     * Returns the currently configured topcat model.
     *
     * @return  table from most recent call to setTable
     */
    public TopcatModel getTable() {
        return tcModel_;
    }

    /**
     * Returns the coordinate values currently selected in this panel.
     * If there is insufficient information to contribute to a plot
     * (not all of the
     * {@link uk.ac.starlink.ttools.plot2.data.Coord#isRequired required}
     * coord values are filled in)
     * then null will be returned.
     *
     * @return   nCoord-element array of coord contents, or null
     */
    public GuiCoordContent[] getContents() {
        int npc = coords_.length;
        GuiCoordContent[] contents = new GuiCoordContent[ npc ];
        for ( int ic = 0; ic < npc; ic++ ) {
            Coord coord = coords_[ ic ];
            List<ColumnDataComboBox> colsels = colSelectors_.get( ic );
            int nu = colsels.size();
            ColumnData[] coldats = new ColumnData[ nu ];
            String[] datlabs = new String[ nu ];
            DomainMapper[] dms = new DomainMapper[ nu ];
            String[] valueIds = new String[ nu ];
            for ( int iu = 0; iu < nu; iu++ ) {
                ColumnDataComboBox colsel = colsels.get( iu );
                IdentifiedColumnData icdata = colsel.getIdentifiedColumnData();
                if ( icdata != null ) {
                    ColumnData cdata = icdata.getColumnData();
                    coldats[ iu ] = cdata;
                    datlabs[ iu ] = cdata.toString();
                    dms[ iu ] = colsel.getDomainMapper();
                    valueIds[ iu ] = icdata.getId();
                }
                else if ( ! coord.isRequired() ) {
                    Input input = coord.getInputs()[ iu ];
                    DomainMapper dm = input.getDomain().getMappers()[ 0 ];
                    ColumnInfo info =
                        new ColumnInfo( input.getMeta().getLongName(),
                                        dm.getSourceClass(),
                                        input.getMeta().getShortDescription() );
                    coldats[ iu ] = new ConstantColumn( info, null );
                    datlabs[ iu ] = null;
                    dms[ iu ] = dm;
                    valueIds[ iu ] = null;
                }
                else {
                    return null;
                }
            }
            contents[ ic ] =
                new GuiCoordContent( coord, datlabs, coldats, dms, valueIds );
        }
        return contents;
    }

    /**
     * Returns the selector component model for a given user coordinate.
     * If no columndata-specific model has been set, null may be returned.
     *
     * @param  ic   coord index
     * @param  iu   user info index for the given coord
     * @return   selector model, or null
     */
    public ColumnDataComboBoxModel getColumnSelector( int ic, int iu ) {
        ComboBoxModel<ColumnData> model =
            colSelectors_.get( ic ).get( iu ).getModel();
        return model instanceof ColumnDataComboBoxModel
             ? (ColumnDataComboBoxModel) model
             : null;
    }

    /**
     * Resets the selector component model for a given user coordinate.
     *
     * @param  ic   coord index
     * @param  iu   user info index for the given coord
     * @param  model  new selector model
     */
    public void setColumnSelector( int ic, int iu,
                                   ColumnDataComboBoxModel model ) {
        colSelectors_.get( ic ).get( iu ).setModel( model );
    }

    /**
     * Indicates whether a coordinate is one that ought to get filled in
     * if possible.
     * The CoordPanel implementation simply returns
     * coord.{@link uk.ac.starlink.ttools.plot2.data.Coord#isRequired},
     * but subclasses can override this if more nuanced behaviour is necessary.
     *
     * <p>In particular in order for autopopulation to work correctly,
     * it may be necessary to return true for all members of a group of
     * coordinates for which at least one has to be filled in
     * for a viable plot.
     *
     * @param  coord   candidate coordinate
     * @return  true if we should try hard to get a value
     */
    public boolean isPreferredCoord( Coord coord ) {
        return coord.isRequired();
    }

    /**
     * Returns a list of coordinates which do not correspond to the
     * selectors displayed here, but which should not be acquired by
     * other means.
     *
     * <p>This is a hack to work round situations when coordinates are
     * added into results by non-obvious means.  In most cases
     * the output result will be an empty array, which is what the
     * implementation in this class does.  But subclasses can override
     * it for special behaviour.
     *
     * @return   list of coords which this panel will arrange to provide
     *           values for in some non-standard way
     */
    public Coord[] getAdditionalManagedCoords() {
        return new Coord[ 0 ];
    }

    /**
     * Returns a list of column metadata items for the items in a
     * list model of columns.
     *
     * @param  model  column list model
     * @return  list of valueinfos
     */
    public static ValueInfo[] getInfos( ColumnDataComboBoxModel model ) {
        List<ValueInfo> list = new ArrayList<>();
        for ( int i = 0; i < model.getSize(); i++ ) {
            ColumnData cdata = model.getColumnDataAt( i );
            if ( cdata != null ) {
                ValueInfo info = cdata.getColumnInfo();
                if ( info != null ) {
                    list.add( info );
                }
            }
        }
        return list.toArray( new ValueInfo[ 0 ] );
    }

    /**
     * Tries to find an item of a given combo box model matching a given
     * metadata item.  If it finds it, it will set the selection and
     * return true.
     *
     * @param   model   list model
     * @param  info   template for selection value
     * @return  true if selection was successfully performed
     */
    public static boolean populate( ColumnDataComboBoxModel model,
                                    ValueInfo info ) {
        for ( int i = 0; i < model.getSize(); i++ ) {
            ColumnData cdata = model.getColumnDataAt( i );
            if ( cdata != null &&
                 infoMatches( cdata.getColumnInfo(), info ) ) {
                model.setSelectedItem( cdata );
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a CoordStack implementation suitable for general-purpose use.
     *
     * @return  new coord stack 
     */
    public static CoordStack createDefaultStack() {
        final LabelledComponentStack lstack = new LabelledComponentStack();
        return new CoordStack() {
            public JComponent getPanel() {
                return lstack;
            }
            public JLabel addCoordLine( String labelTxt, JComponent line ) {
                lstack.addLine( labelTxt, null, line, true );
                JLabel[] labels = lstack.getLabels();
                return labels[ labels.length - 1 ];
            }
        };
    }

    /**
     * Returns a list of coordinates which is like multiple copies of a
     * supplied group.  The returned coords have metadata which
     * distinguish them from each other, currently an integer appended
     * to their name.  The returned coords are not totally respectable,
     * but their metadata is OK.
     *
     * @param   coords  basic coordinates
     * @param  ncopy   number of copies to make
     * @return   array of ncopy copies of coords
     */
    public static Coord[] multiplyCoords( Coord[] coords, int ncopy ) {
        List<Coord> coordList = new ArrayList<>();
        for ( int ig = 0; ig < ncopy; ig++ ) {
            for ( int ic = 0; ic < coords.length; ic++ ) {
                Coord coord = coords[ ic ];
                coordList.add( ncopy == 1 ? coord : relabel( coord, ig ) );
            }
        }
        return coordList.toArray( new Coord[ 0 ] );
    }

    /**
     * Indicates whether two infos match.
     * The criterion is that both name and UCD are the same.
     *
     * @param  info1  first item
     * @param  info2  second item
     * @return  true iff match
     */
    private static boolean infoMatches( ValueInfo info1, ValueInfo info2 ) {
        return Objects.equals( info1.getName(), info2.getName() )
            && Objects.equals( info1.getUCD(), info2.getUCD() );
    }

    /**
     * Returns the index of an entry in a column selector suitable for
     * use as an example plotting coordinate.
     *
     * @param  selector   selector containing options
     * @param  is   first possible value for output
     * @return   index &gt;=is of a suitable entry in selector,
     *           or a value &gt;=the number of entries in selector
     */
    private static int
            getNextSuitableSelectorIndex( JComboBox<ColumnData> selector,
                                          int is ) {

        /* This is hacky.  Basically it picks the next non-null entry
         * (if the entry is in the selector it must be at least of the right
         * type) but with the adjustment that entries with a UCD like "meta.id"
         * are avoided.  The point is that in practice a source_id or
         * similar is often present in the first column, but it's rarely
         * a sensible quantity to plot.  This fiddling therefore often
         * results in giving you a useful rather than silly default plot
         * when a plot window is opened. */
        while ( is < selector.getItemCount() ) {
            ColumnData cdata = selector.getItemAt( is );
            ColumnInfo info = cdata == null ? null : cdata.getColumnInfo();
            String ucd = info == null ? null : info.getUCD();
            boolean avoid =
                   info == null
                || ( ucd != null && ucd.toLowerCase().startsWith( "meta.id" ) );
            if ( ! avoid ) {
                return is;
            }
            is++;
        }
        return is;
    }

    /**
     * Returns a Coord like a given one but with modified metadata.
     *
     * <p>The returned Coord is not of the right subclass, hence does not
     * have the appropriate type-specific read*Coord method.
     * However that doesn't matter, because we're just using the results
     * from this call to represent coordinate metadata, not for reading data.
     *
     * @param  baseCoord  coord on which to base the copy
     * @param  iPoint  point index, used to label the coordinate
     * @return   new coord like the input one
     */
    private static Coord relabel( final Coord baseCoord, int iPoint ) {
        String iptxt = PlotUtil.getIndexSuffix( iPoint );
        final Input[] inputs = baseCoord.getInputs().clone();
        int ni = inputs.length;
        for ( int ii = 0; ii < ni; ii++ ) {
            Input input0 = inputs[ ii ];
            InputMeta meta0 = input0.getMeta();
            InputMeta meta =
                new InputMeta( meta0.getShortName() + iptxt,
                               meta0.getLongName() + " (" + iptxt + ")" );
            meta.setShortDescription( meta0.getShortDescription()
                                    + " for point " + iptxt );
            meta.setXmlDescription( meta0.getXmlDescription()
                                  + ( "<p>Applies to point "
                                    + iptxt + ".</p>" ) );
            inputs[ ii ] = input0.withMeta( meta );
        }
        return new Coord() {
            public Input[] getInputs() {
                return inputs;
            }
            public boolean isRequired() {
                return true;
            }
            public StorageType getStorageType() {
                return baseCoord.getStorageType();
            }
            public Function<Object[],?> inputStorage( ValueInfo[] infos,
                                                      DomainMapper[] dms ) {
                return baseCoord.inputStorage( infos, dms );
            }
        };
    }

    /**
     * Interface for presenting a list of coordinate selection components.
     * The term "stack" here is used in a simply visual sense.
     * @see  #createDefaultStack
     */
    public static interface CoordStack {

        /**
         * Adds a labelled line to the stack of components.
         *
         * @param  labelTxt  text of displayed label to associate with the
         *                   component
         * @param  line   component to add
         * @return   label component displaying supplied <code>labelTxt</code>
         */
        JLabel addCoordLine( String labelTxt, JComponent line );

        /**
         * Returns the visual component displaying this stack.
         *
         * @return  panel for user interaction
         */
        JComponent getPanel();
    }
}
