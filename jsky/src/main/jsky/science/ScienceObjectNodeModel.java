//=== File Prolog =============================================================
//	This code was adapted by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Development History -----------------------------------------------------
//
//	05/03/00	S. Grosvenor / 588 Booz-Allen
//		Original implementation, part of breakdown of old ScienceObject class
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//=== End File Prolog====================================================================

package jsky.science;

/**
 * Extends the basic ScienceObjectModel (SOM) interface to handle some slightly more
 * complex capabilities not needed for many "SOM's".
 * The two main additional features that implementers of this interface must support is:
 * <br>a) support for a data source containing a full set of data of which the science
 *      object instance has only a slice.
 * <br>b) support for having additional ScienceObjectModels as "children".
 *
 * <P>This code was originally developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 */
public interface ScienceObjectNodeModel extends ScienceObjectModel {

    /** Bound property name - should fired when setMoreDataAvailable() is run */
    public static final String MORE_DATA_PROPERTY = "MoreDataAvailable".intern();

    /** Bound property name - should fired when setDataSource() is run */
    public static final String DATA_SOURCE_PROPERTY = "DataSource".intern();

    /**
     * Requests that the science object retrieve any extra data that it has
     * available from its data source.
     **/
    void requestMoreData();

    /**
     * Returns true if more data is available for this science object from its
     * ScienceObjectNodeModel.
     **/
    boolean isMoreDataAvailable();

    /**
     * Returns an object that describes the source of the ScienceObjectModel's data.
     **/
    ScienceObjectNodeModel getDataSource();

    /**
     * Sets the source of the ScienceObjectModel's data.
     **/
    void setDataSource(ScienceObjectNodeModel s);

    /**
     * Sets whether or not more data is available for this science object from its
     * DataSource.
     **/
    void setMoreDataAvailable(boolean more);

    /**
     * Attempts to retrieve more information for the specified ScienceObjectModel and
     * populate the ScienceObjectModel with that extra data.  This only makes sense
     * for ScienceObjects where isMoreDataAvailable() is true.
     *
     * @param	forObject	retrieve more data for this science object
     **/
    void retrieveMoreData(ScienceObjectNodeModel forObject);

    /**
     * returns a List of the children of this node
     */
    java.util.List getChildren();

    /**
     * Removes all occurrences of a ScienceObjectModel as a "child" of the current object
     * automatically will handle removing listening
     *
     * @param so    ScienceObjectModel to be added
     * @return the ScienceObjectModel just removed, or null if so not a existing child
     *
     **/
    ScienceObjectModel removeChild(ScienceObjectModel so);

    /**
     * Replaces all occurrence of the "old" child with the new child
     * in the propertychange listings.
     *
     * @param so1    ScienceObjectModel to be replaced
     * @param so2    new ScienceObjectModel to be "added"
     * @return the ScienceObjectModel just removed, or null if so1 not a valid child
     *
     **/
    ScienceObjectModel replaceChild(ScienceObjectModel so1, ScienceObjectModel so2);

    /**
     * Adds a ScienceObject as a "child" of the current object.
     * Implementers should automatically will handle propertyChange listening,
     * cloning and loading/writing of all official children
     *
     * @param so    ScienceObjectModel to be added
     *
     **/
    void addChild(ScienceObjectModel so);

    /**
     * Removes all children from the ScienceObjectModel.
     **/
    void removeAllChildren();

    /**
     * Returns true when the object is in the process of performing an update.
     * Provides a means of allowing delayed updating.
     * See SEA's gov.nasa.gsfc.util.gui.JPanelTogglePending for an example.
     **/
    public boolean isPending();

}
