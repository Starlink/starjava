package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.LineBox;

/**
 * Control implementation that has a tabber as its panel.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class TabberControl implements Control {

    private final String label_;
    private final Icon icon_;
    private final JTabbedPane tabber_;
    private final ActionForwarder actionForwarder_;

    /**
     * Constructor.
     *
     * @param  label  control label
     * @param  icon   control icon
     */
    public TabberControl( String label, Icon icon ) {
        label_ = label;
        icon_ = icon;
        tabber_ = new JTabbedPane();
        actionForwarder_ = new ActionForwarder();
    }

    public String getControlLabel() {
        return label_;
    }

    public Icon getControlIcon() {
        return icon_;
    }

    public JComponent getPanel() {
        return tabber_;
    }

    /**
     * Returns this control's panel as a JTabbedPane.
     *
     * @return   panel
     */
    public JTabbedPane getTabber() {
        return tabber_;
    }

    /**
     * Adds a tab to this control's tab pane.
     *
     * <p>The <code>stdPos</code> parameter controls component positioning
     * within the tab.  If it is true, then the component is added in
     * the usual way, positioned at the top and enclosed in a scroll pane.
     * If false, then the component is added in the centre of the panel,
     * and any additional positioning is up to the caller.
     * 
     * @param   name  label of tab to hold the component
     * @param   comp  component to add in a tab
     * @param  stdPos  whether to add in standard position or centered
     */
    public void addControlTab( String name, JComponent comp, boolean stdPos ) {
        JComponent holder = new JPanel( new BorderLayout() );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        if ( stdPos ) {
            JComponent box = new JPanel( new BorderLayout() );
            box.setBorder( gapBorder );
            box.add( comp, BorderLayout.NORTH );
            JScrollPane scroller = new JScrollPane( box );
            holder.add( scroller, BorderLayout.CENTER );
        }
        else {
            holder.setBorder( gapBorder );
            holder.add( comp, BorderLayout.CENTER );
        }
        tabber_.add( name, holder );
    }

    /**
     * Adds a zone selection tab to this control.
     *
     * @param   zsel    zone id specifier, not null
     */
    public void addZoneTab( Specifier<ZoneId> zsel ) {
        addControlTab( "Zone",
                       new LineBox( "Zone", zsel.getComponent() ), true );
        zsel.addActionListener( actionForwarder_ );
    }

    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeActionListener( listener );
    }

    /**
     * Returns an object which will forward actions to listeners registered
     * with this panel.
     *
     * @return  action forwarder
     */
    public ActionListener getActionForwarder() {
        return actionForwarder_;
    }
}
