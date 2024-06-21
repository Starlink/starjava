package uk.ac.starlink.datanode.nodes;

import java.util.Iterator;
import javax.swing.Icon;
import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.datanode.factory.DataNodeFactory;

/** 
 * This interface represents a node suitable for use in a tree viewer.
 * From a data point of view, it represents a node which may have zero
 * or more children - the node must be able to supply its own children
 * on request.  From a GUI point of view it supplies 
 * methods which can be used to represent the node, such as a name, 
 * an icon and an opportunity to do custom configuration of an
 * object which provides more detailed (graphical) description of the node.
 * <p>
 * Implementing classes will normally also supply one or more constructors
 * based on other classes (for instance <code>String</code>, 
 * <code>File</code> or other types of <code>DataNode</code>) -
 * such constructors should throw a <code>NoSuchDataException</code>
 * if the construction fails.
 * When a <code>DataNodeFactory</code> instance is informed of the 
 * existence of this new <code>DataNode</code> implementation, it can
 * try to construct a new object of this type from child nodes generated
 * by this or other classes.  In this way a <code>DataNode</code> of
 * a given type does not need to know all about the kinds of child
 * nodes it can have.
 * <p>
 * For smooth working of the node building machinery, 
 * DataNode implementations should make
 * every effort to observe the following rules about resource usage:
 * As far as possible, 
 * internal state of a DataNode should be constructed just-in-time 
 * as it is required rather than up front in the constructor.
 * This is because in many cases a large number of nodes will be 
 * constructed, most of which will only be called upon to supply their
 * name, icon, description etc; if many allocate large amounts of 
 * memory, memory is liable to be exhausted.
 * The only actions which should be permitted to cause a (potentially) 
 * large memory allocation are:
 * <ul>
 * <li>Invocation of {@link #getChildIterator}
 * <li>The user selecting one of the non-default panels in the DetailViewer
 * </ul>
 * In particular the following actions should <em>not</em> cause large resource
 * allocation:
 * <ul>
 * <li>DataNode construction
 * <li>Invocation of {@link #configureDetail}
 * </ul>
 * <p>
 * Although you can implement this interface directly and everything 
 * should work fine, it is probably a good idea to inherit from 
 * {@link DefaultDataNode}.  The separation of interface and implementation
 * is as much for clarity of documentation as anything.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public interface DataNode {

    /**
     * Indicates whether the node can in principle have child nodes.
     * Note this does not actually mean that it has any children.
     *
     * @return   <code>true</code> if the node is of a type which can have
     *           child nodes, <code>false</code> otherwise
     */
    boolean allowsChildren();

    /**
     * Sets a label for this object.  This is like a name but is not
     * intrinsic to the object, so users of implementing classes may call
     * this method to impose a label of their own.
     *
     * @param  label  the label to be given to this object
     */
    void setLabel( String label );

    /**
     * Gets an Iterator over the children of the object, each of which 
     * should itself be a <code>DataNode</code>.  Should only be
     * called if <code>hasChildren</code> returns <code>true</code>.
     * It is preferred that this method completes quickly, so if
     * constructing the whole list of children may be time-consuming,
     * implementing classes should avoid constructing the whole list
     * before returning the iterator.
     * <p>
     * Implementing classes should in general follow this strategy when
     * generating children: the class should get the list of children
     * in whatever way is appropriate for the type of node in question.
     * It should then make use of its childMaker (the 
     * {@link DataNodeFactory} returned by the
     * <code>getChildMaker</code> method) 
     * to turn these into <code>DataNode</code> objects to return as
     * the children rather than using a particular constructor, such
     * as one of its own, to generate them.  In this way, children
     * may turn out to be more specific objects of a type known about
     * by <code>DataNodeFactory</code> but not by the implementing class.
     *
     * @return  an <code>Iterator</code> over the children.  Each object
     *          iterated over should be a <code>DataNode</code>.
     *          Behaviour is undefined if this method is called on an
     *          object for which <code>allowsChildren</code> returns 
     *          <code>false</code>.
     */
    Iterator getChildIterator();

    /**
     * Returns an object which is in some sense the parent of the one
     * this node is based on.
     * The parent is <em>not</em> a <code>DataNode</code>,
     * it is something which may get fed to a <code>DataNodeFactory</code> 
     * to create <code>DataNode</code>.  If no such object exists, which 
     * may well be the case, <code>null</code> should be returned.
     *
     * @return  an object which is the parent of this one, or <code>null</code>
     */
    Object getParentObject();

    /**
     * Sets the object which is in some sense the parent of the one
     * this node is based on.
     * The parent is <em>not</em> a <code>DataNode</code>,
     * it is something which may get fed to a <code>DataNodeFactory</code>
     * to create <code>DataNode</code>.
     * 
     * @param parentObj  an object which is the parent of this one
     */
    void setParentObject( Object parentObj );

    /**
     * Gets the label of this object.  This ought to return the same value
     * <code>getName</code> unless the user of the implementing class
     * has previously called <code>setLabel</code> to change it.
     *
     * @return  the label of the object
     */
    String getLabel();

    /**
     * Gets the name of this object.  This is an intrinsic property of the
     * object.
     *
     * @return  the name of the object
     */
    String getName();

    /**
     * Returns a short string indicating what kind of node this is.
     * The return value should preferably be a Three Letter Acronym.
     * As a rough guideline the return value should indicate what the
     * implementing class is.
     *
     * @return  an abbreviated description of the type of this 
     *          <code>DataNode</code>
     */
    String getNodeTLA();

    /**
     * Returns a short sentence indicating what kind of node this is.
     * The return value should be just a few words.  As a rough
     * guideline it should indicate what the implementing class is.
     * 
     * @return  a short description of the type of this <code>DataNode</code>
     */
    String getNodeType();

    /**
     * Gets a concise description of this object.  The form of the
     * description will depend on
     * the type of node, but it might detail the shape or type of data
     * represented, or otherwise give some information additional to the
     * name.  It should not include the return value of the 
     * <code>getName</code> method, since they may be presented together.
     * It should be on one line, and preferably no longer than around 
     * 70 characters.  The <code>null</code> value may be returned if
     * there is nothing to say.
     *
     * @return  a short string describing this object
     */
    String getDescription();

    /**
     * Gets an <code>Icon</code> which can be used when displaying this node.
     * This should return an icon suitable for displaying in a JTree,
     * ideally about 16x16 pixels.  It should give some indication of
     * the type of node.  
     * <p>
     * Implementations are encouraged to construct this icon lazily 
     * (i.e. not do it until this method is called), since using Icons
     * usually causes a large number of Swing classes to be loaded,
     * and in some circumstances (such as treeview -text) these may
     * never be used.
     *
     * @return  an <code>Icon</code> for display
     */
    Icon getIcon();

    /**
     * Gets the contribution of this node to a pathname.
     * The return value should be the name of this node as it forms part
     * of a path name.  This may or may not be equal to the <code>name</code>
     * member variable.  Can be null to indicate the no pathname can
     * be formed from this node.
     *
     * @return   pathname name of this node
     */
    String getPathElement();

    /**
     * Gets the delimiter string which separates the name of this node from
     * the name of one of its children when constructing a pathname.
     * If <code>null</code> is returned it indicates that no pathname 
     * can be formed from this node and one of its children (for instance
     * if it has no children).
     *
     * @return  short delimiter string
     */
    String getPathSeparator();

    /**
     * Indicates whether this node can, on request, supply a data object
     * of a particular type.  The idea is that if this node corresponds
     * to a generic object such as a table, array, etc then components
     * can ascertain that and present it in ways appropriate to that fact.
     * If a DataNode claims to have an object for one of the defined 
     * {@link DataType}s, it can in general forget about any special
     * presentation specific to that type, on the grounds that generic
     * node processing will take care of it.
     *
     * <p>Invoking this method ought not to create such a data object
     * if that is an expensive process, merely to indicate whether 
     * a subsequent invocation of {@link #getDataObject} using the same
     * <code>type</code> is likely to be successful.
     *
     * @param   type  data object type of interest
     * @return  true iff a subsequent call of
     *          <code>getDataObject(type)</code> is likely to be successful
     */
    boolean hasDataObject( DataType type );

    /**
     * Returns a data object of a given type which corresponds to this node.
     * This method should only be invoked if a prior invocation of
     * {@link #hasDataObject} using the same <code>type</code> has returned true
     * (otherwise an <code>IllegalArgumentException</code> may be thrown).
     * The returned object must be in instance of the class returned by
     * <code>type.getDataClass()</code>.
     *
     * @param  type  data object type of interest
     * @return  data object of type <code>type</code>
     */
    Object getDataObject( DataType type ) throws DataObjectException;

    /**
     * Configures a DetailViewer object to show additional class-specific 
     * details associated with this node.  Implementing classes should 
     * call various methods on the given <code>DetailViewer</code> object
     * to customise it to contain information about the node in question.
     * This will
     * consist of populating the main panel with basic and compact
     * information about this node, and possibly adding further tabbed
     * panes giving alternative views of the data attached to the node.
     * See the documentation for {@link DetailViewer} for more information.
     * <p>
     * Nodes which don't have much to say about themselves may implement
     * this method as a no-op, though it will generally be a good idea
     * to invoke the superclass's implementation if there is a superclass.
     * 
     * @param   dv  the detail viewer which this node is given an opportunity
     *          to configure
     */
    void configureDetail( DetailViewer dv );

    /**
     * Sets the factory which should in general be used to generate 
     * child nodes.
     * This is not necessarily the factory which is used for generating
     * children of this node, since this node may have special procedures
     * for generating children.  However it is the factory which should 
     * in general be used for creating descendants of this node.
     * <p>
     * This method should only be used by applications which wish to
     * restrict the type of node which can appear in a whole subtree
     * of the node hierarchy.  The childMaker is normally inherited
     * from parent to child, so for instance customising the childMaker
     * of the tree root by removing certain builders will prevent 
     * such nodes from appearing anywhere in the tree.
     *
     * @param  factory  the factory to use for generating children
     */
    void setChildMaker( DataNodeFactory factory );

    /**
     * Gets the factory which should in general be used to generate 
     * descendant nodes.
     *
     * @return  the factory used for generating children
     */
    DataNodeFactory getChildMaker();

    /**
     * Stores information about how this node was created.
     * 
     * @param  state  an object encapsulating the means by which this node
     *                was created
     */
    void setCreator( CreationState state );

    /**
     * Retrieves information about how this node was created.
     *
     * @return  an object encapsulating the means by which this node was
     *          created.  May be null if no information is available
     */
    CreationState getCreator();
}
