package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.UIManager;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.util.IconUtils;

/**
 * GUI component that allows the user to choose a HiPS survey
 * from an available list.
 * 
 * @author   Mark Taylor
 * @since    22 Oct 2019
 */
public class HipsSelector extends JPanel {

    private final JTextField field_;
    private final JButton button_;
    private final ActionForwarder forwarder_;
    private final JTextField idField_;
    private final JLabel iconLabel_;
    private final JComponent[] lines_;
    private HipsSurvey[] surveys_;
    private JPopupMenu popupMenu_;

    /**
     * Constructor.
     */
    public HipsSelector() {
        super( new BorderLayout() );
        field_ = new JTextField();
        button_ = new JButton( new AbstractAction( "Select" ) {
            public void actionPerformed( ActionEvent evt ) {
                popupMenu_.show( button_, 0, button_.getHeight() );
            }
        } );
        forwarder_ = new ActionForwarder();
        field_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                setSelectedSurvey( null );
                // also invokes forwarder
            }
        } );
        JComponent box = Box.createVerticalBox();
        JComponent inputLine = Box.createHorizontalBox();
        inputLine.add( field_ );
        inputLine.add( Box.createHorizontalStrut( 5 ) );
        inputLine.add( button_ );
        inputLine.add( Box.createHorizontalStrut( 5 ) );
        JComponent displayLine = Box.createHorizontalBox();
        idField_ = new JTextField();
        idField_.setFont( UIManager.getFont( "TextField.font" ) );
        idField_.setEditable( false );
        idField_.setBorder( BorderFactory.createEmptyBorder() );
        iconLabel_ = new JLabel();
        displayLine.add( iconLabel_ );
        displayLine.add( Box.createHorizontalStrut( 5 ) );
        displayLine.add( idField_ );
        box.add( inputLine );
        box.add( displayLine );
        add( box, BorderLayout.NORTH );
        lines_ = new JComponent[] { inputLine, displayLine };
    }

    /**
     * Sets the list of available surveys.
     *
     * @param  surveys  available HiPS surveys
     */
    public void setSurveys( HipsSurvey[] surveys ) {
        surveys_ = surveys;

        /* Arrange the supplied objects into a tree structure. */
        Node root = new Node();
        for ( HipsSurvey survey : surveys ) {
            String[] path = survey.getPath();
            if ( path != null && path.length > 0 ) {
                Node node = root;
                for ( String el : path ) {
                    node = node.getChild( el );
                }
                node.survey_ = survey;
            }
        }

        /* Compact the tree so that branches with only one sub-branch
         * hide that level of structure (it just makes the UI more fiddly). */
        amalgamateSingles( root );

        /* Turn the tree data structure into a hierarchy of JMenuItems. */
        popupMenu_ = new JPopupMenu( "Select HiPS" );
        for ( Map.Entry<String,Node> entry : root.children_.entrySet() ) {
            popupMenu_.add( createMenuItem( entry.getKey(),
                                            entry.getValue() ) );
        }

        /* Populate the text field with a likely initial value if it's
         * not already filled in. */
        String txt = field_.getText();
        if ( txt == null || txt.trim().length() == 0 ) {
            HipsSurvey example = getExampleSurvey( surveys );
            if ( example != null ) {
                setSelectedSurvey( example );
            }
        }
    }

    /**
     * Adds a listener for changes to this selector.
     *
     * @param  listener  listener
     */
    public void addActionListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    /**
     * Removes a previously added listener.
     *
     * @param  listener  listener
     */
    public void removeActionListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    /**
     * Returns the field that contains the current selection.
     * This may have been populated by selecting from the survey list,
     * but it is also permissible to type into it, since the hips2fits
     * service just tries string matching where required.
     * 
     * @return   field containing HiPS name/ID
     */
    public JTextField getTextField() {
        return field_;
    }

    /**
     * Returns the sub-components of this GUI element as an array
     * of vertically stacked lines.
     *
     * @return  sub-component array
     */
    public JComponent[] getLines() {
        return lines_;
    }

    /**
     * Sets the currently selected survey.
     * This method is used internally.
     *
     * @param  survey  new selection
     */
    public void setSelectedSurvey( HipsSurvey survey ) {
        if ( survey != null ) {
            field_.setText( survey.getShortName() );
            field_.setCaretPosition( 0 );
        }
        idField_.setText( survey == null ? null : survey.getCreatorDid() );
        idField_.setCaretPosition( 0 );
        idField_.setToolTipText( survey == null ? null : survey.getObsTitle() );
        double frac = survey == null ? Double.NaN : survey.getMocSkyFraction();
        iconLabel_.setIcon( createCoverageIcon( frac ) );
        iconLabel_.setToolTipText( frac >= 0 && frac <= 1
                                 ? "Sky Coverage: " + ((int) (100 * frac)) + "%"
                                 : null );
                             
        forwarder_.actionPerformed( new ActionEvent( this, 0, "select" ) );
    }

    /**
     * Recursively constructs a menu item for use in the Hips selection menu.
     *
     * @param  name   name associated with the node
     * @param  node   tree node
     * @return   menu item corresponding to the node
     */
    private JMenuItem createMenuItem( String name, Node node ) {
        int nchild = node.children_.size();
        final HipsSurvey survey = node.survey_;

        /* Leaf. */
        if ( survey != null ) {
            assert nchild == 0;
            String displayName = survey.getShortName();
            String descrip = survey.getObsTitle();
            double frac = survey == null ? Double.NaN
                                         : survey.getMocSkyFraction();
            Icon icon = createCoverageIcon( frac );
            return new JMenuItem( new BasicAction( displayName,
                                                   icon, descrip ) {
                public void actionPerformed( ActionEvent evt ) {
                    setSelectedSurvey( survey );
                }
            } );
        }

        /* Branch. */
        else {
            assert nchild > 1;
            JMenu menu = new JMenu( name );
            for ( Map.Entry<String,Node> entry :
                  sortEntries( node.children_.entrySet() ) ) {
                menu.add( createMenuItem( entry.getKey(), entry.getValue() ) );
            }
            return menu;
        }
    }

    /**
     * Recursively adjusts the tree so that any branches with exactly one
     * child are amalgamated with their child.  This makes navigating
     * the tree less annoying.  The initial branch structure is in any
     * case ad hoc, constructed from examining strings, so modifying it
     * isn't a bad thing to do.
     *
     * @param  parent  node whose descendents are to be potentially amalgamated
     */
    private static void amalgamateSingles( Node parent ) {
        Map<String,Node> children = parent.children_;

        /* Recurse. */
        for ( Node child : children.values() ) {
            amalgamateSingles( child );
        }

        /* Identify single-child children. */
        List<String> singles = new ArrayList<String>();
        for ( Map.Entry<String,Node> entry : children.entrySet() ) {
            if ( entry.getValue().children_.size() == 1 ) {
                singles.add( entry.getKey() );
            }
        }

        /* For each single-(grand)child child, replace it with a node
         * that incorporates its grandchild. */
        for ( String single : singles ) {
            Node child = children.remove( single );
            assert child.children_.size() == 1;
            Map.Entry<String,Node> entry1 =
                child.children_.entrySet().iterator().next();
            children.put( single + "/" + entry1.getKey(), entry1.getValue() );
        }
    }

    /**
     * Sorts map entries for presentation.
     * The output list may contain more items than the input one,
     * since special action is taken to handle the anomalous case
     * of a node which appears to be both a branch and a leaf.
     * 
     * @param  inEntries  input entries
     * @return   sorted entries
     */
    private static Collection<Map.Entry<String,Node>>
            sortEntries( Collection<Map.Entry<String,Node>> inEntries ) {

        /* Prepare a list to be sorted; any node which appears to be
         * both a branch and a leaf is split into two, for ease of
         * processing later.  There aren't many of these, and they are
         * probably not supposed to be present, but I have seen them. */
        List<Map.Entry<String,Node>> outEntries =
            new ArrayList<Map.Entry<String,Node>>();
        for ( Map.Entry<String,Node> cEntry : inEntries ) {
            String cName = cEntry.getKey();
            Node cNode = cEntry.getValue();
            if ( cNode.children_.size() > 0 && cNode.survey_ != null ) {
                Node n1 = new Node();
                n1.children_ = cNode.children_;
                Node n2 = new Node();
                n2.survey_ = cNode.survey_;
                outEntries.add( createEntry( cName, n1 ) );
                outEntries.add( createEntry( cName, n2 ) );
            }
            else {
                outEntries.add( cEntry );
            }
        }

        /* Sort the list.  Use ad hoc rules that seem to look OK.
         * Basically, put branches at the top and otherwise order
         * alphabetically or using other rules as suitable. */
        Collections.sort( outEntries, new Comparator<Map.Entry<String,Node>>() {
            public int compare( Map.Entry<String,Node> e1,
                                Map.Entry<String,Node> e2 ) {
                Node node1 = e1.getValue();
                Node node2 = e2.getValue();
                HipsSurvey hips1 = node1.survey_;
                HipsSurvey hips2 = node2.survey_;
                int nc1 = node1.children_.size();
                int nc2 = node2.children_.size();
                String name1 = e1.getKey();
                String name2 = e2.getKey();
                HipsSurvey.ObsRegime regime1 =
                    HipsSurvey.ObsRegime.fromName( name1 );
                HipsSurvey.ObsRegime regime2 =
                    HipsSurvey.ObsRegime.fromName( name2 );
                if ( nc1 > 0 && nc2 == 0 ) {
                    return -1;
                }
                if ( nc1 == 0 && nc2 > 0 ) {
                    return +1;
                }
                if ( hips1 != null && hips2 != null ) {
                    String skey1 = hips1.getClientSortKey();
                    String skey2 = hips2.getClientSortKey();
                    if ( skey1 != null && skey2 != null ) {
                        return skey1.compareTo( skey2 );
                    }
                    else {
                        return hips1.getShortName()
                              .compareTo( hips2.getShortName() );
                    }
                }
                if ( regime1 != null && regime2 == null ) {
                    return -1;
                }
                if ( regime1 == null && regime2 != null ) {
                    return +1;
                }
                if ( regime1 != null && regime2 != null ) {
                    return regime1.compareTo( regime2 );
                }
                return name1.toLowerCase().compareTo( name2.toLowerCase() );
            }
        } );
        return outEntries;
    }

    /**
     * Returns a survey suitable for use as a default value.
     *
     * @param  surveys   list of available surveys
     * @return   example survey instance, or maybe null
     */
    private static HipsSurvey getExampleSurvey( HipsSurvey[] surveys ) {
        HipsSurvey dss = null;
        HipsSurvey dssColor = null;
        for ( HipsSurvey survey : surveys ) {
            String txt = survey.getShortName();
            if ( txt.startsWith( "DSS2/color" ) ) {
                return survey;
            }
            else if ( txt.startsWith( "DSS2/" ) ) {
                dss = survey;
            }
        }
        if ( dss != null ) {
            return dss;
        }
        else if ( surveys.length > 0 ) {
            return surveys[ 0 ];
        }
        else {
            return null;
        }
    }

    /**
     * Returns a little icon that conveys the impression of fractional
     * coverage of the sky.
     *
     * @param   fraction  fraction, may be NaN
     * @return  icon
     */
    private static Icon createCoverageIcon( final double fraction ) {
        final int size = 10;
        if ( fraction >= 0 && fraction <= 1 ) {
            final Map<RenderingHints.Key,Object> hints =
                new HashMap<RenderingHints.Key,Object>();
            hints.put( RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON );
            hints.put( RenderingHints.KEY_RENDERING,
                       RenderingHints.VALUE_RENDER_QUALITY );
            final int off = 1;
            final int s1 = size - 2 * off;
            final int s2 = s1 / 2;
            return new Icon() {
                public int getIconWidth() {
                    return size;
                }
                public int getIconHeight() {
                    return size;
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    int x0 = x + off;
                    int y0 = y + off;
                    g2.addRenderingHints( hints );
                    g2.drawLine( x0 + s2, y0 + s2, x0 + s2, y0 );
                    g2.fillArc( x0, y0, s1, s1, 90, (int) ( fraction * -360 ) );
                    g2.drawArc( x0, y0, s1, s1, 90, 360 );
                }
            };
        }
        else {
            return IconUtils.emptyIcon( size, size );
        }
    }

    /**
     * Utility method to create a Map.Entry instance.
     * <p>Warning: this is not a general purpose map entry.
     *
     * @param  name  key
     * @param  node  value
     * @return  map entry
     */
    private static Map.Entry<String,Node> createEntry( final String name,
                                                       final Node node ) {
        return new Map.Entry<String,Node>() {
            public String getKey() {
                return name;
            }
            public Node getValue() {
                return node;
            }
            public Node setValue( Node value ) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Tree node.
     * Branch nodes have children, leaf nodes have a survey.
     * Nodes that are both or neither are also possible,
     * though generally they should be one or the other.
     * Not immutable.
     */
    private static class Node {
        Map<String,Node> children_;
        HipsSurvey survey_;

        Node() {
            children_ = new LinkedHashMap<String,Node>();
        }

        /**
         * Returns a child of the given name.
         * Executing this method creates that child if it doesn't already
         * exist.
         *
         * @param  name  child name
         */
        Node getChild( String name ) {
            if ( ! children_.containsKey( name ) ) {
                children_.put( name, new Node() );
            }
            return children_.get( name );
        }
    }
}
