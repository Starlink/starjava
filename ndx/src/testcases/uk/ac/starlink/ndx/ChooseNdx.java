package uk.ac.starlink.ndx;

import java.io.File;

public class ChooseNdx {

   public static void main( String[] args ) {
       int ndim = -1;
       for ( int i = 0; i < args.length; i++ ) {
           String arg = args[ i ];
           
           if ( new File( arg ).exists() ) {
               System.setProperty( "user.dir", arg );
           }
           else {
               ndim = Integer.parseInt( arg );
           }
       }

       if ( ! NdxChooser.isAvailable() ) {
           System.err.println( "NdxChooser.isAvailable() == false" );
           System.exit( 1 );
       }

       NdxChooser chooser;
       if ( ndim > 0 ) {
           chooser = NdxChooser.newInstance( ndim, ndim );
       }
       else {
           chooser = NdxChooser.newInstance();
       }

       System.out.println( chooser.chooseNdx( null ) );
       System.exit( 0 );
   }
}
