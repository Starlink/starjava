package uk.ac.starlink.topcat.plot2;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import uk.ac.starlink.ttools.plot2.Gesture;

/**
 * Provides a description of the available navigation gestures.
 *
 * @author   Mark Taylor
 * @since    3 Feb 2014
 */
public class NavigationHelpPanel extends JPanel {

    private final JComponent helpBox_;
    private final JComponent legendBox_;
    private final Map<Gesture,String> optMap_;
    private final Map<Gesture,JLabel> labelMap_;
    private final Map<Gesture,Dimension> sizeMap_;
    private static final int PAD = 16;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public NavigationHelpPanel() {
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        helpBox_ = Box.createHorizontalBox();
        legendBox_ = Box.createHorizontalBox();
        optMap_ = new HashMap<Gesture,String>();
        labelMap_ = new LinkedHashMap<Gesture,JLabel>();
        sizeMap_ = new HashMap<Gesture,Dimension>();
        add( helpBox_ );

        /* If the mouse enters this component, its display switches to a
         * description of the icons used here.  This idea was pinched
         * from the GAIA UI. */
        addMouseListener( new MouseAdapter() {
            public void mouseEntered( MouseEvent evt ) {
                removeAll();
                add( legendBox_ );
                revalidate();
                repaint();
            }
            public void mouseExited( MouseEvent evt ) {
                removeAll();
                add( helpBox_ );
                revalidate();
                repaint();
            }
        } );
    }

    /**
     * Sets the list of known gestures and their meanings.
     *
     * @param  optMap  ordered mapping of gestures to behaviour descriptions
     */
    public void setOptions( Map<Gesture,String> optMap ) {

        /* If the map is just the same as last time, there is no need to
         * update the appearance. */
        if ( optMap_.equals( optMap ) ) {
            return;
        }
        else {
            optMap_.clear();
            optMap_.putAll( optMap );
        }

        /* If the gesture list is the same as last time, change the text
         * but try to keep the placements unchanged, since flickering
         * repositioning of the icons and text is visually distracting.
         * Only resize if any of the components does not have enough
         * room to display (i.e. resize if too big, but not if too small). */
        Set<Gesture> gestures = optMap.keySet();
        if ( gestures.equals( labelMap_.keySet() ) ) {
            boolean resize = false;
            for ( Gesture gesture : gestures ) {
                JLabel label = labelMap_.get( gesture );
                label.setPreferredSize( null );
                label.setText( optMap.get( gesture ) );
                Dimension pSize = label.getPreferredSize();
                Dimension maxSize = sizeMap_.get( gesture );
                if ( pSize.width > maxSize.width ||
                     pSize.height > maxSize.height ) {
                    sizeMap_.put( gesture, new Dimension( pSize ) );
                    resize = true;
                }
                Dimension size = new Dimension( sizeMap_.get( gesture ) );
                label.setMinimumSize( new Dimension( size ) );
                label.setPreferredSize( new Dimension( size ) );
            }
            if ( resize ) {
                helpBox_.revalidate();
            }
        }

        /* Otherwise, all change.  Create a new component to display the
         * gesture meanings, and another (legend) to display the meanings
         * of the icons. */
        else {
            helpBox_.removeAll();
            legendBox_.removeAll();
            legendBox_.add( new JLabel( "Mouse Help: " ) );
            labelMap_.clear();
            sizeMap_.clear();
            for ( Gesture gesture : gestures ) {
                String txt = optMap.get( gesture );
                JLabel label =
                    new JLabel( txt, gesture.getIcon(), SwingConstants.LEFT );
                helpBox_.add( label );
                Dimension size = label.getPreferredSize();
                sizeMap_.put( gesture, new Dimension( size ) );
                label.setMinimumSize( new Dimension( size ) );
                label.setPreferredSize( new Dimension( size ) );
                labelMap_.put( gesture, label );
                helpBox_.add( Box.createHorizontalStrut( PAD ) );
                legendBox_.add( new JLabel( ": " + gesture.getDescription(),
                                            gesture.getIcon(),
                                            SwingConstants.LEFT ) );
                legendBox_.add( Box.createHorizontalStrut( PAD ) );
            }
            helpBox_.add( Box.createHorizontalGlue() );
            helpBox_.revalidate();
            legendBox_.add( Box.createHorizontalGlue() );
            legendBox_.revalidate();
        }
        repaint();
    }

    /**
     * Disabling the component greys it out as usual.
     * The semantics of this are a bit different from usual; user interaction
     * is not really possible anyway, but if you want to grey it out,
     * this method works anyway.
     */
    @Override
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        for ( JLabel label : labelMap_.values() ) {
            label.setEnabled( enabled );
        }
    }
}
