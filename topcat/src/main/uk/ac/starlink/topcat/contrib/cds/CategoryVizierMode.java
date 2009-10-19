package uk.ac.starlink.topcat.contrib.cds;

import cds.vizier.VizieRQueryInterface;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * Vizier mode that allows the user to search for catalogues based on 
 * a provided list of known categories.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public class CategoryVizierMode extends SearchVizierMode {

    private final VizieRQueryInterface vqi_;
    private final JList lambdaList_;
    private final JList missionList_;
    private final JList astroList_;

    /**
     * Constructor.
     *
     * @param   vqi  vizier query interface
     * @param   tld  controlling load dialogue
     */
    public CategoryVizierMode( VizieRQueryInterface vqi,
                               VizierTableLoadDialog tld ) {
        super( "By Category", vqi, tld, true );
        vqi_ = vqi;
        lambdaList_ = new JList();
        missionList_ = new JList();
        astroList_ = new JList();
    }

    protected Component createSearchComponent() {
        final JComponent kwPanel = Box.createHorizontalBox();
        kwPanel.add( createListBox( "Wavelength", lambdaList_ ) );
        kwPanel.add( createListBox( "Mission", missionList_ ) );
        kwPanel.add( createListBox( "Astronomy", astroList_ ) );
        kwPanel.addAncestorListener( new AncestorListener() {
            public void ancestorAdded( AncestorEvent evt ) {
                kwPanel.removeAncestorListener( this );
                populateLists();
            }
            public void ancestorMoved( AncestorEvent evt ) {
            }
            public void ancestorRemoved( AncestorEvent evt ) {
            }
        } );
        return kwPanel;
    }

    protected String getSearchArgs() {
        return new StringBuffer()
              .append( getKwArgs( "Wavelength", lambdaList_ ) )
              .append( getKwArgs( "Mission", missionList_ ) )
              .append( getKwArgs( "Astronomy", astroList_ ) )
              .toString();
    }

    /**
     * Fill in the contents of the lists of provided categories.
     * This method can be called from the event dispatch thread.
     */
    private void populateLists() {
        new Thread( "Vizier KW acquisition" ) {
            public void run() {
                final Object[] lambdas;
                final Object[] missions;
                final Object[] astros;
                synchronized ( vqi_ ) {
                    lambdas = vqi_.getWavelengthKW().toArray( new String[ 0 ] );
                    missions = vqi_.getMissionKW().toArray( new String[ 0 ] );
                    astros = vqi_.getAstronomyKW().toArray( new String[ 0 ] );
                }
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        lambdaList_.setListData( lambdas );
                        missionList_.setListData( missions );
                        astroList_.setListData( astros );
                        ((JComponent) getComponent()).revalidate();
                    }
                } );
            }
        }.start();
    }

    /**
     * Returns a VizieR server-friendly argument string which describes
     * the current selections in a JList of categories of a particular
     * kind.
     *
     * @param   name  VizieR server tag for the category type
     * @param   list  JList containing items in category <code>name</code>
     */
    private static String getKwArgs( String name, JList list ) {
        Object[] selections = list.getSelectedValues();
        String key = "-kw." + name;
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < selections.length; i++ ) {
            Object sel = selections[ i ];
            assert sel instanceof String;
            if ( sel instanceof String ) {
                sbuf.append( VizierTableLoadDialog
                            .encodeArg( key, (String) sel ) );
            }
        }
        return sbuf.toString();
    }

    /**
     * Creates a component containing a JList of categories
     * suitable for display in this component.
     *
     * @param  name   human-readable name for the category type
     * @param  list   list object
     * @return   component for display
     */
    private static JComponent createListBox( String name, JList list ) {
        JComponent box = new JPanel( new BorderLayout() );
        box.add( new JLabel( name ), BorderLayout.NORTH );
        list.setVisibleRowCount( 6 );
        box.add( new JScrollPane( list,
                                  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ),
                 BorderLayout.CENTER );
        box.setAlignmentY( Component.TOP_ALIGNMENT );
        return box;
    }
}
