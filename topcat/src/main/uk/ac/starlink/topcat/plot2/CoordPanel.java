package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ConstantColumn;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.StorageType;
import uk.ac.starlink.util.gui.ComboBoxBumper;

/**
 * GUI component for entry of Coord values as table column expressions.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class CoordPanel extends JPanel {

    private final Coord[] coords_;
    private final ActionForwarder forwarder_;
    private final JComboBox[][] colSelectors_;
    private final boolean autoPopulate_;

    /**
     * Constructor.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  autoPopulate  if true, some attempt will be made to
     *                       fill in the fields with non-blank values
     *                       when a table is selected
     */
    public CoordPanel( Coord[] coords, boolean autoPopulate ) {
        super( new BorderLayout() );
        coords_ = coords;
        forwarder_ = new ActionForwarder();
        autoPopulate_ = autoPopulate;

        /* Place entry components for each required coordinate. */
        int nc = coords.length;
        colSelectors_ = new JComboBox[ nc ][];
        LabelledComponentStack stack = new LabelledComponentStack();
        for ( int ic = 0; ic < nc; ic++ ) {
            ValueInfo[] userInfos = coords[ ic ].getUserInfos();
            int nu = userInfos.length;
            colSelectors_[ ic ] = new JComboBox[ nu ];
            for ( int iu = 0; iu < nu; iu++ ) {
                JComboBox cs = ColumnDataComboBoxModel.createComboBox();
                colSelectors_[ ic ][ iu ] = cs;
                cs.addActionListener( forwarder_ );
                JComponent line = Box.createHorizontalBox();
                line.add( cs );
                line.add( Box.createHorizontalStrut( 5 ) );
                line.add( new ComboBoxBumper( cs ) );

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
                stack.addLine( userInfos[ iu ].getName(), null, line, true );
            }
        }

        /* Place the lot at the top of the component so it doesn't fill
         * vertical space. */
        add( stack, BorderLayout.NORTH );
    }

    /**
     * Constructs a coord panel for entry of multiple copies of a single group
     * of coordinates.
     *
     * @param  coords  template coordinates
     * @param  ncopy   number of copies of group
     * @param  whether to attempt to fill in values when table changes
     */
    public CoordPanel( Coord[] coords, int ncopy, boolean autoPopulate ) {
        this( multiplyCoords( coords, ncopy ), autoPopulate );
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
    public ActionListener getActionForwarder() {
        return forwarder_;
    }

    /**
     * Sets the table with reference to which this panel will resolve
     * coordinate descriptions.
     *
     * @param  tcModel   table from which coordinate values will be drawn
     */
    public void setTable( TopcatModel tcModel ) {
        int is = 1;
        for ( int ic = 0; ic < coords_.length; ic++ ) {
            JComboBox[] colsels = colSelectors_[ ic ];
            ValueInfo[] userInfos = coords_[ ic ].getUserInfos();
            int nu = colsels.length;
            for ( int iu = 0; iu < nu; iu++ ) {
                JComboBox cs = colsels[ iu ];
                if ( tcModel == null ) {
                    cs.setSelectedItem( null );
                    cs.setEnabled( false );
                }
                else {
                    ComboBoxModel model =
                        new ColumnDataComboBoxModel( tcModel,
                                                     userInfos[ iu ]
                                                    .getContentClass(), true );
                    cs.setModel( model );

                    /* In case of autopopulate, just pick the first few
                     * columns in the table and fill them in for the
                     * required coordinates. */
                    if ( autoPopulate_ && is < model.getSize() ) {
                        cs.setSelectedIndex( is++ );
                    }
                    cs.setEnabled( true );
                }
            }
        }
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
            JComboBox[] colsels = colSelectors_[ ic ];
            int nu = colsels.length;
            ColumnData[] coldats = new ColumnData[ nu ];
            String[] datlabs = new String[ nu ];
            for ( int iu = 0; iu < nu; iu++ ) {
                Object colitem = colsels[ iu ].getSelectedItem();
                if ( colitem instanceof ColumnData ) {
                    coldats[ iu ] = (ColumnData) colitem;
                    datlabs[ iu ] = colitem.toString();
                }
                else if ( ! coord.isRequired() ) {
                    ValueInfo info = coord.getUserInfos()[ iu ];
                    coldats[ iu ] =
                        new ConstantColumn( new ColumnInfo( info ), null );
                    datlabs[ iu ] = null;
                }
                else {
                    return null;
                }
            }
            contents[ ic ] = new GuiCoordContent( coord, datlabs, coldats );
        }
        return contents;
    }

    /**
     * Returns the selector component model for a given user coordinate.
     *
     * @param  ic   coord index
     * @param  iu   user info index for the given coord
     * @return   selector model
     */
    public ColumnDataComboBoxModel getColumnSelector( int ic, int iu ) {
        return (ColumnDataComboBoxModel) colSelectors_[ ic ][ iu ].getModel();
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
        colSelectors_[ ic ][ iu ].setModel( model );
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
    private static Coord[] multiplyCoords( Coord[] coords, int ncopy ) {
        List<Coord> coordList = new ArrayList<Coord>();
        for ( int ig = 0; ig < ncopy; ig++ ) {
            for ( int ic = 0; ic < coords.length; ic++ ) {
                Coord coord = coords[ ic ];
                coordList.add( ncopy == 1 ? coord : relabel( coord, ig ) );
            }
        }
        return coordList.toArray( new Coord[ 0 ] );
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
     * @param  point index, used to label the coordinate
     * @return   new coord like the input one
     */
    private static Coord relabel( final Coord baseCoord, int iPoint ) {
        String iptxt = Integer.toString( iPoint + 1 );
        final ValueInfo[] infos = baseCoord.getUserInfos().clone();
        int nuc = infos.length;
        for ( int iuc = 0; iuc < nuc; iuc++ ) {
            DefaultValueInfo info = new DefaultValueInfo( infos[ iuc ] );
            info.setName( info.getName() + iptxt );
            info.setDescription( info.getDescription()
                               + " for point " + iptxt );
            infos[ iuc ] = info;
        }
        return new Coord() {
            public ValueInfo[] getUserInfos() {
                return infos;
            }
            public boolean isRequired() {
                return true;
            }
            public StorageType getStorageType() {
                return baseCoord.getStorageType();
            }
            public List<Class<? extends DomainMapper>> getUserDomains() {
                return baseCoord.getUserDomains();
            }
            public Object userToStorage( Object[] userCoords,
                                         DomainMapper[] userMappers ) {
                return baseCoord.userToStorage( userCoords, userMappers );
            }
        };
    }
}
