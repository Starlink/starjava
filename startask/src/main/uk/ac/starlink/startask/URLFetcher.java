package uk.ac.starlink.startask;

import java.io.*;
import java.net.*;

/** A class to fetch a file specified by a URL
*/
public class URLFetcher {

private URL thisURL;
   
/** Construct a URLFetcher given the URL as a String
*   @param url the URL as a String
*/
public URLFetcher( String url ) throws MalformedURLException {
   thisURL = new URL( url );
}   
   
/** Construct a URLFetcher given a URL
*   @param url The URL
*/
public URLFetcher( URL url ) {
   thisURL = url;
}   

/** Fetch the file specified by the URL
*/
public boolean fetch() {
   return fetch( thisURL.getFile() );
}

/* Fetch the file specified by the URL into a specified file.
*  @param filename The name of the file to be created.
*/
public boolean fetch( String filename ) {

   try {
      InputStream is = thisURL.openStream();
      FileOutputStream file = new FileOutputStream( new File( filename ) );
      byte[] b = new byte[512];
      int nbytes;

      while( ( nbytes = is.read( b ) ) >= 0 ) {
         file.write( b, 0, nbytes );
      }
   
      is.close();
      file.close();
      return true;
      
   } catch( Exception e ) {
      e.printStackTrace();
      return false;
   }
}

public static void main( String[] args ) {

   try {
      URLFetcher urlFetcher = new URLFetcher( args[0] );
      urlFetcher.fetch( args[1] );
      System.exit( 0 );
   } catch( Throwable e ) {
      e.printStackTrace();
      System.exit( 1 );
   }
   
}

} 
