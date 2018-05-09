package uk.ac.starlink.topcat;

import javax.swing.ListModel;

/**
 * Extends the ListModel interface with a generic type.
 * This interface doesn't (can't) enforce actual type safety in its
 * implementations, it's mainly available as a marker to make the
 * intention clear in method signatures.
 * 
 * @author   Mark Taylor
 * @since    9 May 2018
 */
public interface TypedListModel<T> extends ListModel {
    T getElementAt( int index );
}
