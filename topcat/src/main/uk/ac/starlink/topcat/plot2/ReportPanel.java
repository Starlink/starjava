package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Displays plot reports.
 *
 * @author   Mark Taylor
 * @since    11 Dec 2014
 */
public class ReportPanel extends JPanel {

    private final JComboBox subsetSelector_;
    private final Map<ReportKey,JComponent> boxMap_;
    private final DefaultComboBoxModel subsetSelModel_;
    private final JPanel reportHolder_;
    private Map<RowSubset,ReportMap> reports_;

    /**
     * Constructor.
     */
    public ReportPanel() {
        super( new BorderLayout() );
        subsetSelModel_ = new DefaultComboBoxModel();
        subsetSelector_ = new JComboBox( subsetSelModel_ );
        subsetSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                updateDisplay();
            }
        } );
        add( new LineBox( "Subset", new ShrinkWrapper( subsetSelector_ ),
                          true ),
             BorderLayout.NORTH );
        reportHolder_ = new JPanel( new BorderLayout() );
        add( reportHolder_, BorderLayout.CENTER );
        boxMap_ = new LinkedHashMap<ReportKey,JComponent>();
        reports_ = new HashMap<RowSubset,ReportMap>();
    }

    /**
     * Updates the reports that can be displayed by this component.
     * They are supplied as a map keyed by RowSubset.
     *
     * @param  reports   map of subsets to plot reports
     */
    public void submitReports( Map<RowSubset,ReportMap> reports ) {
        reports_ = reports;
        Object selected = subsetSelModel_.getSelectedItem();
        subsetSelModel_.removeAllElements();
        boolean hasSelected = false;
        for ( RowSubset rset : reports.keySet() ) {
            subsetSelModel_.addElement( rset );
            if ( rset.equals( selected ) ) {
                subsetSelModel_.setSelectedItem( rset );
                hasSelected = true;
            }
        }
        if ( ! hasSelected ) {
            subsetSelModel_.setSelectedItem( subsetSelModel_.getSize() > 0
                                           ? subsetSelModel_.getElementAt( 0 )
                                           : null );
        }
        updateDisplay();
    }

    /**
     * Updates this component's appearance to match its current state.
     */
    private void updateDisplay() {
        displayReport( reports_.get( subsetSelector_.getSelectedItem() ) );
    }

    /** 
     * Displays the content of a particular report map.
     *
     * @param  report  plot report to display
     */
    private void displayReport( ReportMap report ) {

        /* Assemble a list of key-string pairs to display. */
        Map<ReportKey,JComponent> componentMap =
            new LinkedHashMap<ReportKey,JComponent>();
        if ( report != null ) {
            for ( ReportKey key : report.keySet() ) {
                if ( key.isGeneralInterest() ) {
                    componentMap.put( key,
                                      createReportComponent( key, report ) );
                }
            }
        }

        /* Ensure that the display component has slots for each of the
         * items we are going to display.  If not, throw it out and
         * position a new one that does. */
        if ( ! boxMap_.keySet().containsAll( componentMap.keySet() ) ) {
            boxMap_.clear();
            LabelledComponentStack stack = new LabelledComponentStack();
            for ( ReportKey key : componentMap.keySet() ) {
                JComponent box = Box.createHorizontalBox();
                stack.addLine( key.getMeta().getLongName(), null, box, true );
                boxMap_.put( key, box );
            }
            reportHolder_.removeAll();
            reportHolder_.add( stack );
            reportHolder_.revalidate();
        }

        /* Display the report items in the display component. */
        for ( Map.Entry<ReportKey,JComponent> entry : boxMap_.entrySet() ) {
            ReportKey key = entry.getKey();
            JComponent box = entry.getValue();
            box.removeAll();
            JComponent cmp = componentMap.get( key );
            if ( cmp != null ) {
                box.add( cmp );
            }
            box.revalidate();
        }
    }

    /**
     * Returns a GUI component that presents an entry from a report map
     * to the user.
     *
     * @param  key  key
     * @param  map   report map containing key
     * @return  component
     */
    private static <T> JComponent createReportComponent( ReportKey<T> key,
                                                         ReportMap map ) {
        T value = map.get( key );
        JTextField field = new JTextField();
        field.setText( value == null ? null : value.toString() );
        field.setEditable( false );
        return field;
    }
}
