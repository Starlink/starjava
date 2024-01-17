package uk.ac.starlink.hapi;

import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretListener;

/**
 * ListModel that allows filtering the content by matching to the content
 * of a text field.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2024
 */
public class FilterListModel<T> extends AbstractListModel<T> {

    private final BiPredicate<T,String> matcher_;
    private final JTextField filterField_;
    private final JLabel countLabel_;
    private final JComponent filterPanel_;
    private List<T> items_;
    private List<T> visibleItems_;

    /**
     * Default constructor.
     */
    public FilterListModel() {
        this( "Filter",
              (e, txt) -> e.toString().toLowerCase()
                           .indexOf( txt.toLowerCase() ) >= 0 );
    }

    /**
     * Constructor with custom list element matching.
     *
     * @param  title   title text
     * @param  matcher  indicates whether a list element is to be included
     *                  given string content of this model's text field
     */
    public FilterListModel( String title,
                            BiPredicate<T,String> matcher ) {
        matcher_ = matcher;
        filterField_ = new JTextField( 12 );
        filterField_.addCaretListener( evt -> updateState() );
        countLabel_ = new JLabel();
        filterPanel_ = Box.createVerticalBox();
        Box titleLine = Box.createHorizontalBox();
        titleLine.add( new JLabel( title ) );
        titleLine.add( Box.createHorizontalGlue() );
        Box countLine = Box.createHorizontalBox();
        countLine.add( Box.createHorizontalGlue() );
        countLine.add( countLabel_ );
        if ( title != null ) {
            filterPanel_.add( titleLine );
        }
        filterPanel_.add( filterField_ );
        filterPanel_.add( countLine );
        visibleItems_ = Collections.emptyList();
        setItems( Collections.emptyList() );
    }

    /**
     * Sets the unfiltered content of this model.
     *
     * @param  items  list items
     */
    public void setItems( List<T> items ) {
        int oldSize = items.size();
        items_ = Collections.emptyList();
        fireIntervalRemoved( this, 0, oldSize );
        items_ = items;
        fireIntervalAdded( this, 0, items_.size() );
        updateState();
    }

    /**
     * Returns the text field used for filtering.
     *
     * @return  text field
     */
    public JTextField getFilterField() {
        return filterField_;
    }

    /**
     * Returns a panel containing the text field and some surrounding
     * components.
     */
    public JComponent getFilterPanel() {
        return filterPanel_;
    }

    public int getSize() {
        return visibleItems_.size();
    }

    public T getElementAt( int i ) {
        return visibleItems_.get( i );
    }

    /**
     * Called if state has changed.
     */
    private void updateState() {
        String txt0 = filterField_.getText();
        String txt = txt0 == null ? "" : txt0.trim();
        Predicate<T> filter =
              txt.length() == 0
            ? item -> true
            : item -> item != null && matcher_.test( item, txt );
        int nvis = visibleItems_ == null ? 0 : visibleItems_.size();
        List<T> newVisibleItems = items_.stream()
                                 .filter( filter )
                                 .collect( Collectors.toList() );
        if ( newVisibleItems.size() != nvis ) {
            visibleItems_ = Collections.emptyList();
            fireIntervalRemoved( this, 0, nvis );
            visibleItems_ = newVisibleItems;
            fireIntervalAdded( this, 0, visibleItems_.size() );
            countLabel_.setText( Integer.toString( visibleItems_.size() )
                               + " / " + Integer.toString( items_.size() ) );
        }
    }
}
