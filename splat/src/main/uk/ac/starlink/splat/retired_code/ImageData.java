package uk.ac.starlink.splat.imagedata;

import java.util.Vector;

/**
 * Abstract base class for accessing image data from various sources.
 **/

public abstract class ImageData {

    /**
     *  Create ImageData object, without specifying container file.
     **/
    public ImageData() {
    }

    /**
     *  Create ImageData object, accessing the container file.
     **/
    public ImageData( String filename ) {
	open( filename );
    }

    /**
     *  Open a data container using its filename.
     *
     *  @param  filename Name of the file containing the image data
     *
     *  @return true if file opened, false otherwise
     **/
    public boolean open( String filename ) {

	//  If container file is already opened then close it.
	if ( dataRef != null ) {
	    closeFile();
	}

	//  Open the container.
	openFile( filename );
    }

    /**
     *  Really open the container file.
     *
     *  <p>This method is implemenation specific. It should check if
     *  the file exists and open it if appropriate to the data format
     *  in use.</p>
     *
     *  @param filename Name of the container file
     *
     *  @return true if the file exists and can be opened.
     **/
    protected abstract boolean openFile( String container );

    /**
     *  Access a component in a container file. 
     *
     *  @param component The name of the component to access.
     *
     *  @return Integer handle to use when refering to this
     *          component. Set to -1 if component not available.
     **/
    public int component( String name ) {
	
	//  See if the component is already available.
	int handle = lookup( name );
	if ( handle == -1 ) {
	    handle = getComponent( name );
	}
	return handle;
    }

    /**
     *  Access a component in a container file. 
     *
     *  <p>This method is implementation specific. It should make the
     *  specified component (i.e. an image data array of some kind)
     *  available. </p>
     *
     *  <p>The format of component specifications is also
     *  implementation specific, but should follow the rules {1}
     *  signifies the first data component {2} the second etc.  How
     *  these map into actual data components is the responsibility of
     *  the implementor.</p>
     *
     *  <p>The assignHandle method should be used to register the
     *   component when accessed.</p>
     *
     *  @param component The name of the component to access.
     *
     *  @return Handle to use when refering to this component. 
     *
     **/
    protected abstract int getComponent( String name );

    /**
     *  Lookup a data component returning its handle if available.
     *
     *  @param component Name of the component to check.
     *
     *  @return The component handle or -1 if component not known.
     **/
    protected int lookup( String name ) {
	int index = -1;
	if ( handles.size() > 0 ) {
	    index = handles.indexOf( name );
	}
	return index;
    }

    /**
     *  Vector of data component names indexed by their handle.
     **/
    protected Vector handles = new Vector();

}
