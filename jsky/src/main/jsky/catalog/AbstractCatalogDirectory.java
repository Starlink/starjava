/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: AbstractCatalogDirectory.java,v 1.1 2002/08/04 21:48:50 brighton Exp $
 */


package jsky.catalog;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import jsky.coords.CoordinateRadius;
import jsky.util.TclUtil;
import jsky.util.gui.DialogUtil;


/**
 * A generic, abstract base class for catalog directory implementations.
 *
 * @version $Revision: 1.1 $
 * @author Allan Brighton
 */
public abstract class AbstractCatalogDirectory implements CatalogDirectory {

    // A name or title for this catalog directory. 
    private String _name;

    // The short name or id for this catalog directory. 
    private String _id;

    // A URL pointing to the XML file 
    private URL _url;

    // A vector of Catalog objects, one for each catalog in the catalog directory 
    private List _catalogs = new Vector();

    // Optional handler, used to report HTML format errors from servers 
    private HTMLQueryResultHandler _htmlQueryResultHandler;

    // Optional link to the parent catalog directory, or null for the root
    private CatalogDirectory _parent;

    // list of event listeners 
    private EventListenerList _listenerList = new EventListenerList();


    /**
     * Initialize with the name of the catalog directory.
     *
     * @param name the display name for the catalog directory
     */
    public AbstractCatalogDirectory(String name) {
        _name = _id = name; // default id is the same as name
    }


   /**
    * Implementation of the clone method (makes a shallow copy).
    */
    public Object clone() {
	try {
	    return super.clone();
	} 
	catch (CloneNotSupportedException ex) {
	    throw new InternalError(); // won't happen
	}
    }


    /* Return the link to the parent catalog directory, or null for the root or if not known.  */
    public CatalogDirectory getParent() {
        return _parent;
    }

    /* Set the link to the parent catalog directory (null for the root) */
    public void setParent(CatalogDirectory dir) {
        if (dir != this)
            _parent = dir;
    }


    /** Return the handler used to report HTML format errors from servers */
    public HTMLQueryResultHandler getHTMLQueryResultHandler() {
        return _htmlQueryResultHandler;
    }

    /** Optional handler, used to report HTML format errors from servers */
    public void setHTMLQueryResultHandler(HTMLQueryResultHandler handler) {
        _htmlQueryResultHandler = handler;
    }


    /** Return a string representing this object (in this case the name) */
    public String toString() {
        return _name;
    }

    /**
     * Add the given catalog to the catalog list if it is not already there. 
     * If a separate catalog with the same name is in the list, the user is asked 
     * if it should be removed.
     */
    public void addCatalog(int index, Catalog cat) {
        // check for duplicates
        int i = _catalogs.indexOf(cat);
        if (i != -1) {
            return;
        }

        // check if it is a catalog with the same name (maybe from a different config file)
        String name = cat.getName();
        Catalog oldCat = getCatalog(name);
	int size = _catalogs.size();
        if (oldCat != null) {
            int ans = DialogUtil.confirm(name + " already exists. Do you want to replace it?");
            if (ans != JOptionPane.YES_OPTION)
                return;
            removeCatalog(oldCat);
	    size--;
        }

	if (index < 0 || index >= size)
	    _catalogs.add(cat);
	else
	    _catalogs.add(index, cat);

	cat.setParent(this);
	CatalogFactory.registerCatalog(cat, isLocal());
	_fireTreeNodesInserted(_getTreeModelEvent(cat));
    }


    /**
     * Add the given catalog to the catalog list if it is not already there. 
     * If a separate catalog with the same name is in the list, the user is asked 
     * if it should be removed.
     */
    public void addCatalog(Catalog cat) {
	addCatalog(_catalogs.size(), cat);
    }


    /**
     * Remove the given catalog from the catalog list.
     */
    public void removeCatalog(Catalog cat) {
	// event needs state before removing from tree
	TreeModelEvent tme = _getTreeModelEvent(cat);

        _catalogs.remove(cat); 
	_fireTreeNodesRemoved(tme);
	cat.setParent(null);
	CatalogFactory.unregisterCatalog(cat);
    }


    /** Replace the given old catalog with the given new catalog in the catalog list. */
    public void replaceCatalog(Catalog oldCat, Catalog newCat) {
	int i = _catalogs.indexOf(oldCat);
	if (i != -1) {
	    _catalogs.set(i, newCat);
	    newCat.setParent(this);
	    //System.out.println("XXX newcat path = " + TclUtil.makeList(newCat.getPath()));
	    _fireTreeNodesChanged(_getTreeModelEvent(newCat));
	}
    }


    /** Move the the given catalog up or down in the tree. */
    public void moveCatalog(Catalog cat, boolean up) {
        int i = _catalogs.indexOf(cat);
        if (i == -1 || up && i == 0 || !up && i == _catalogs.size() - 1)
            return;

        removeCatalog(cat);
        addCatalog(i + (up ? -1 : 1), cat);
    }


    /** Move the the given catalog all the way up or down in the tree, as far as possible. */
    public void moveCatalogToEnd(Catalog cat, boolean up) {
        int i = _catalogs.indexOf(cat);
        if (i == -1 || up && i == 0 || !up && i == _catalogs.size() - 1)
            return;

        removeCatalog(cat);
        if (up)
            addCatalog(0, cat);
        else
            addCatalog(cat);
    }


    /** Return the named catalog, if found in this directory */
    public Catalog getCatalog(String catalogName) {
        int n = getNumCatalogs();
        for (int i = 0; i < n; i++) {
            Catalog cat = getCatalog(i);
            if (catalogName.equals(cat.getName()) || catalogName.equals(cat.getId()))
                return cat;
        }
        return null;
    }


    // Methods to implement the CatalogDirectory interface

    /** Return the number of catalogs in this directory */
    public int getNumCatalogs() {
        return _catalogs.size();
    }

    /** Return the ith catalog in the directory */
    public Catalog getCatalog(int i) {
        return (Catalog)(_catalogs.get(i));
    }

    /** Return the index of the given catalog in the directory */
    public int indexOf(Catalog cat) {
	return _catalogs.indexOf(cat);
    }


    /** Set the list of catalogs in this catalog directory. */
    public void setCatalogs(List catalogs) {
        _catalogs = catalogs;
	int n = _catalogs.size();
	for(int i = 0; i < n; i++) {
	    Catalog cat = (Catalog)_catalogs.get(i);
	    cat.setParent(this);
	    CatalogFactory.registerCatalog(cat, isLocal());
	}
	_fireTreeStructureChanged(_getTreeModelEvent(this));
    }

    /** Return a copy of the list of catalogs in this catalog directory. */
    public List getCatalogs() {
        return new Vector(_catalogs);
    }


    /** Return a memory catalog describing the list of catalogs in the directory */
    public TableQueryResult getCatalogList() {
        // column headings
        FieldDescAdapter[] columns = new FieldDescAdapter[1];
        columns[0] = new FieldDescAdapter("Title");

        // data rows
        int numCatalogs = getNumCatalogs();
        Vector rows = new Vector(numCatalogs, 1);
        for (int i = 0; i < numCatalogs; i++) {
            Vector cols = new Vector(1, 1);
            cols.add(getCatalog(i));
            rows.add(cols);
        }

        // create a memory catalog
        MemoryCatalog result = new MemoryCatalog(columns, rows);
        result.setName(_name);
        result.setTitle(_name);
        result.setDescription(_name);

        return result;
    }


    /** Return the URL of the file describing this catalog directory. */
    public URL getURL() {
        return _url;
    }

    /** Set the URL of the file describing this catalog directory. */
    public void setURL(URL url) {
        _url = url;
    }



    
    // -- Inplement the Catalog interface --
    


    /** Return the name of the catalog directory */
    public String getName() {
        return _name;
    }

    /** Return the name of the catalog directory */
    public void setName(String name) {
        _name = name;
    }

    /** Return a string to display as a title for the catalog directory in a user interface */
    public String getTitle() {
        return getName();
    }

    /** Return the Id or short name of the catalog directory */
    public String getId() {
        return _id;
    }
    /** Set the Id or short name of the catalog directory */
    public void setId(String id) {
        _id = id;
    }


    /** Return a description of the catalog, or null if not available */
    public String getDescription() {
	if (_url != null)
	    return _name + " [" + _url.toString() + "]";
        return _name;
    }


    /** Return a URL pointing to documentation for the catalog, or null if not available */
    public URL getDocURL() {
        return null;
    }

    /** Return the number of query parameters that this catalog accepts */
    public int getNumParams() {
        return 0;
    }

    /** Return a description of the ith query parameter */
    public FieldDesc getParamDesc(int i) {
        return null;
    }

    /** Return a description of the named query parameter */
    public FieldDesc getParamDesc(String name) {
        return null;
    }

    /**
     * This method is required to implement the Catalog interface, but does nothing here.
     */
    public void setRegionArgs(QueryArgs queryArgs, CoordinateRadius region) {
    }

    /**
     * Return true if this is a local catalog, and false if it requires
     * network access or if a query could hang. A local catalog query is
     * run in the event dispatching thread, while others are done in a
     * separate thread.
     */
    public boolean isLocal() {
        return _url.getProtocol().equals("file");
    }

    /**
     * Return true if this object represents an image server.
     */
    public boolean isImageServer() {
        return false;
    }

    /** Return the catalog type (one of the constants: CATALOG, ARCHIVE, DIRECTORY, LOCAL, IMAGE_SERVER) */
    public String getType() {return Catalog.DIRECTORY;}

    /**
     * This method assumes the catalog directory has no query parameters and
     * just returns "this".
     *
     * @param queryArgs An object describing the query arguments (not used here)
     * @return An object describing the result of the query
     */
    public QueryResult query(QueryArgs queryArgs) throws IOException {
        return this;
    }


    /**
     * Return a list of name servers (Catalogs with serv_type
     * equal to "namesvr") to use to resolve astronomical object names.
     */
    public List getNameServers() {
        return CatalogFactory.getCatalogsByType(Catalog.NAME_SERVER);
    }


    /**
     * Returns the root catalog directory, casted to an AbstractCatalogDirectory.
     * This returns the same value as the getRoot() method.
     */
    public AbstractCatalogDirectory getRootCatalogDirectory() {
	return (AbstractCatalogDirectory)getRoot();
    }


    // -- Implement the TreeModel interface


    /**
     * Returns the root of the tree.  Returns <code>null</code>
     * only if the tree has no nodes.
     *
     * @return  the root of the tree
     */
    public Object getRoot() {
	CatalogDirectory rootDir = null, catDir = this;
	while(catDir != null) {
	    rootDir = catDir;
	    catDir =  catDir.getParent();
	}
	return rootDir;
    }


    /**
     * Returns the child of <code>parent</code> at index <code>index</code>
     * in the parent's
     * child array.  <code>parent</code> must be a node previously obtained
     * from this data source. This should not return <code>null</code>
     * if <code>index</code>
     * is a valid index for <code>parent</code> (that is <code>index >= 0 &&
     * index < getChildCount(parent</code>)).
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the child of <code>parent</code> at index <code>index</code>
     */
    public Object getChild(Object parent, int index) {
	if (parent instanceof CatalogDirectory) {
	    CatalogDirectory catDir = (CatalogDirectory)parent;
	    if (index >= 0 && index < catDir.getNumCatalogs())
		return catDir.getCatalog(index);
	}
	return null;
    }

    
    /**
     * Returns the number of children of <code>parent</code>.
     * Returns 0 if the node
     * is a leaf or if it has no children.  <code>parent</code> must be a node
     * previously obtained from this data source.
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the number of children of the node <code>parent</code>
     */
    public int getChildCount(Object parent) {
	if (parent instanceof CatalogDirectory) {
	    CatalogDirectory catDir = (CatalogDirectory)parent;
	    return catDir.getNumCatalogs();
	}
	return 0;
    }


    /**
     * Returns <code>true</code> if <code>node</code> is a leaf.
     * It is possible for this method to return <code>false</code>
     * even if <code>node</code> has no children.
     * A directory in a filesystem, for example,
     * may contain no files; the node representing
     * the directory is not a leaf, but it also has no children.
     *
     * @param   node  a node in the tree, obtained from this data source
     * @return  true if <code>node</code> is a leaf
     */
    public boolean isLeaf(Object node) {
	if (node instanceof Catalog)
	    return (!((Catalog)node).getType().equals(Catalog.DIRECTORY));
	return false;
    }

    /**
      * Messaged when the user has altered the value for the item identified
      * by <code>path</code> to <code>newValue</code>. 
      * If <code>newValue</code> signifies a truly new value
      * the model should post a <code>treeNodesChanged</code> event.
      *
      * @param path path to the node that the user has altered
      * @param newValue the new value from the TreeCellEditor
      */
    public void valueForPathChanged(TreePath path, Object newValue) {
	_fireTreeNodesChanged(_getTreeModelEvent((Catalog)newValue));
    }


    /**
     * Returns the index of child in parent.  If <code>parent</code>
     * is <code>null</code> or <code>child</code> is <code>null</code>,
     * returns -1.
     *
     * @param parent a note in the tree, obtained from this data source
     * @param child the node we are interested in
     * @return the index of the child in the parent, or -1 if either
     *    <code>child</code> or <code>parent</code> are <code>null</code>
     */
    public int getIndexOfChild(Object parent, Object child) {
	if (parent instanceof CatalogDirectory && child instanceof Catalog) {
	    CatalogDirectory catDir = (CatalogDirectory)parent;
	    Catalog cat = (Catalog)child;
	    return catDir.indexOf(cat);
	}
	return -1;
    }


    /**
     * Adds a listener for the <code>TreeModelEvent</code>
     * posted after the tree changes.
     *
     * @param   l       the listener to add
     * @see     #removeTreeModelListener
     */
    public void addTreeModelListener(TreeModelListener l) {
        _listenerList.add(TreeModelListener.class, l);
    }

    /**
     * Removes a listener previously added with
     * <code>addTreeModelListener</code>.
     *
     * @see     #addTreeModelListener
     * @param   l       the listener to remove
     */  
    public void removeTreeModelListener(TreeModelListener l) {
        _listenerList.remove(TreeModelListener.class, l);
    }


    // Return a tree model event for an operation of the given catalog
    private TreeModelEvent _getTreeModelEvent(Catalog cat) {
	Object source = this;
	CatalogDirectory catDir = cat.getParent();

	Object[] path = null;
	int[] childIndices = null;
	Object[] children = null;
	if (catDir == null) {
	    // must be the tree root
	    path = new Catalog[1];
	    path[0] = cat;
	    childIndices = new int[]{0};
	}
	else {
	    path = getPath(catDir);
	    childIndices = new int[]{catDir.indexOf(cat)};
	}
	children = new Object[]{cat};

	return new TreeModelEvent(source, path, childIndices, children);
    }


    /** Return an array of catalogs describing the path to the given catalog or catalog directory. */
    public Catalog[] getPath(Catalog cat) {
	if (cat == null)
	    return null;
	
	List l = new Vector();
	CatalogDirectory dir;
	if (cat instanceof CatalogDirectory) {
	    dir = (CatalogDirectory)cat;
	}
	else {
	    dir = cat.getParent();
	    l.add(cat);
	}
	while(dir != null) {
	    l.add(dir);
	    dir =  dir.getParent();
	}

	int n = l.size();
	Catalog[] ar = new Catalog[n];
	for(int i = 0; i < n; i++)
	    ar[n-i-1] = (Catalog)l.get(i);

	return ar;
    }

    /** 
     * Return an array of Catalog or CatalogDirectory objects representing the 
     * path from the root catalog directory to this catalog.
     */
    public Catalog[] getPath() {
	return getPath(this);
    }


    // Notify tree model listeners that nodes changed
    private void _fireTreeNodesChanged(TreeModelEvent e) {
	AbstractCatalogDirectory root = getRootCatalogDirectory();
	if (root != null && this != root) {
	    root._fireTreeNodesChanged(e);
	    return;
	}
	    
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {
                ((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
            }
        }
    }

    // Notify tree model listeners that nodes changed
    private void _fireTreeNodesInserted(TreeModelEvent e) {
	AbstractCatalogDirectory root = getRootCatalogDirectory();
	if (root != null && this != root) {
	    root._fireTreeNodesInserted(e);
	    return;
	}
	    
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {
                ((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
            }
        }
    }

    // Notify tree model listeners that nodes changed
    private void _fireTreeNodesRemoved(TreeModelEvent e) {
	AbstractCatalogDirectory root = getRootCatalogDirectory();
	if (root != null && this != root) {
	    root._fireTreeNodesRemoved(e);
	    return;
	}
	    
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {
                ((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
            }
        }
    }

    // Notify tree model listeners that nodes changed
    private void _fireTreeStructureChanged(TreeModelEvent e) {
	AbstractCatalogDirectory root = getRootCatalogDirectory();
	if (root != null && this != root) {
	    root._fireTreeStructureChanged(e);
	    return;
	}
	    
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {
                ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
            }
        }
    }
}
