package uk.ac.starlink.startask;

import java.io.File;
import java.io.Serializable;

import java.net.URL;

/** Class to transfer a URL in a distributed processing system
*/
class StarTaskURL implements StarTaskDataPacket, Serializable {

/* The URL */
URL url;

/* The name of the file to create on the server */
String localName;

/* The disposal method */
int dispose;


/** Construct a StarTaskURL from a given URL. The pathname of the file created
*   on the server will be the same as the pathname of the URL. The disposal
*   method will be 'DELETE'.
*   @param urlString The name of the original file
*/
StarTaskURL( String urlString ) throws Exception {
   this( urlString, null, StarTaskDataPacket.DELETE );
}

/** Construct a StarTaskURL from a given URL. The pathname of the file created
*   on the server will be the same as the pathname of the URL.
*   It will be disposed of according to the <code>disp</code> parameter.
*   @param urlString The name of the original file
*   @param disp The disposal method string
*/
StarTaskURL( String urlString, String disp ) throws Exception {
   this( urlString, null, disp );
}

/** Construct a StarTaskURL from a given URL specifying the pathname of the file
*   to be created on the server and the disposal method. If the
*   <code>newName</code> parameter is null, the file created will have the
*   pathname of the URL.
*   @param urlString The name of the original file 
*   @param newName The name of the file to be created.
*   @int dispose The disposal method code (see {@link StarTaskDataPacket}).
*/
StarTaskURL( String urlString, String newName, int disp )
 throws Exception {
    url = new URL( urlString );
    if( newName != null ) {
       localName = newName;
    } else {
//       File localFile = new File( url.getPath() );
//       localName = localFile.getName();
        localName = url.getPath().substring(1);
    }
    dispose = disp;
}

/** Construct a StarTaskURL from a given file specifying the pathname of the
*   file to be created on the server and the disposal method. If the
*   <code>newName</code> parameter is null, the file created will have the
*   pathname of the URL.
*   @param urlString The name of the original file 
*   @param newName The name of the file to be created.
*   @param disp The disposal method string
*/
StarTaskURL( String urlString, String newName, String disp )
 throws Exception {

/* Construct a StarTaskFile with disposal method DELETE */
   this( urlString, newName, StarTaskDataPacket.DELETE );
   
/* and overwrite the disposal method as required */
   String ldisp = disp.toLowerCase();
   if( ldisp.equals( "delete" ) || ldisp.equals( "d" ) ) {
      dispose = StarTaskDataPacket.DELETE;
   } else if( ldisp.equals( "keep" ) || ldisp.equals( "k" ) ) {
      dispose = StarTaskDataPacket.KEEP;
   } else if( ldisp.equals( "return" ) || ldisp.equals( "r" ) ) {
      dispose = StarTaskDataPacket.RETURN;
   } else if( ldisp.equals( "ftp" ) || ldisp.equals( "f" ) ) {
      dispose = StarTaskDataPacket.FTP;
   } else {
      throw new StarTaskException(
       "StarTaskFile: Invalid dispose string " + disp );
   }
}

/** Make the local file for this StarTaskURL.
*/
public void makeLocal() throws Exception {
   URLFetcher urlf = new URLFetcher( url );
   File localFile = new File( localName );
   File directory = localFile.getParentFile();
   if( !directory.exists() ) {
      if( !directory.mkdirs() ) {
         throw new StarTaskException(
          "StarTaskFile: failed to create container directory for " + localName );
      }
   } 
   System.out.println( "Creating " + localName + " from URL " + url );
   urlf.fetch( localName );
}


/** Dispose of the local file for this StarTaskURL
*/
public StarTaskDataPacket dispose() {
   StarTaskDataPacket dataPacket = null;
   if( dispose == StarTaskDataPacket.DELETE ) {
      File file = new File( localName );
      file.delete();
   }
   
   return dataPacket;
}

/** Get the return version of this StarTaskURL
*/
public StarTaskDataPacket getReturn() {
   return null;
}

}
   
