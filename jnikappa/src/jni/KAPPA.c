#include <jni.h>
#include <f77.h>
#include "uk_ac_starlink_kappa_KAPPA.h" 

#ifdef DEBUG
 #define DEB(a) printf(a);
 #define DEB1(a,b) printf(a,b);
 #define DEB2(a,b,c) printf(a,b,c);
#else
 #define DEB(a) ;
 #define DEB1(a,b) ;
 #define DEB2(a,b,c) ;
#endif

JNIEXPORT jint JNICALL 
   Java_uk_ac_starlink_kappa_KAPPA_jnicontour(
     JNIEnv *env, jobject obj, jobject pl, jobject tmsg ) {

   int status;
   
   status = 0;

DEB("Activating SUBPAR\n");   
   subpar_activ( env, obj, pl, tmsg, &status );

   ndfBegin();   

DEB("Calling contour\n");
   F77_CALL(contour)( INTEGER_ARG(&status) );

DEB("Close NDF and HDS\n");
   ndfEnd( &status);
   
DEB("De-activating SUBPAR\n");   
   subpar_deact( &status );

   return (jint)status;
   
}   

JNIEXPORT jint JNICALL 
   Java_uk_ac_starlink_kappa_KAPPA_jnidisplay(
     JNIEnv *env, jobject obj, jobject pl, jobject tmsg ) {

   int status;
   
   status = 0;

DEB("Activating SUBPAR\n");   
   subpar_activ( env, obj, pl, tmsg, &status );

   ndfBegin();   

DEB("Calling adamtest\n");
   F77_CALL(display)( INTEGER_ARG(&status) );

DEB("Close NDF and HDS\n");
   ndfEnd( &status);
/*   F77_CALL(hds_stop)( INTEGER_ARG(&status) );*/
   
DEB("De-activating SUBPAR\n");   
   subpar_deact( &status );

   return (jint)status;
   
}   

JNIEXPORT jint JNICALL 
   Java_uk_ac_starlink_kappa_KAPPA_jnistats(
     JNIEnv *env, jobject obj, jobject pl, jobject tmsg ) {

   int status;
   
   status = 0;

DEB("Activating SUBPAR\n");   
   subpar_activ( env, obj, pl, tmsg, &status );

   ndfBegin();   

DEB("Calling stats\n");
   F77_CALL(stats)( INTEGER_ARG(&status) );

DEB("Close NDF and HDS\n");
   ndfEnd( &status );
/*   F77_CALL(hds_show)("LOCATORS",&status,8);*/
/*   F77_CALL(hds_stop)( INTEGER_ARG(&status) );*/
   
DEB("De-activating SUBPAR\n");   
   subpar_deact( &status );
   
   return (jint)status;
   
}   
   
