/*
*+
*  Name:
*     Mapping.c

*  Purpose:
*     JNI implementations of native methods of Mapping class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  Notes:
*     Note the implementation of these functions relies on a jdouble
*     being the same type as a double.

*  History:
*     20-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_Mapping.h"


/* Typedefs. */

typedef struct {
   JNIEnv *env;
   jobject calculator;
   jmethodID method;
} Ukern1Info;



/* Static function prototypes. */

static void initializeIDs( JNIEnv *env );
static void fukern1( double offset, const double params[], int flags, 
                     double *value );

/* Static variables. */
jclass InterpolatorClass = NULL;
jclass SpreaderClass = NULL;
jclass ResampleFlagsClass = NULL;
jfieldID InterpolatorSchemeID = NULL;
jfieldID InterpolatorParamsID = NULL;
jfieldID InterpolatorUkern1erID = NULL;
jfieldID InterpolatorUinterperID = NULL;
jfieldID SpreaderSchemeID = NULL;
jfieldID SpreaderParamsID = NULL;
jmethodID ResampleFlagsGetFlagsIntID = NULL;


/* Static functions. */

static void initializeIDs( JNIEnv *env ) {
/*
*+
*  Name:
*     intializeIDs

*  Purpose:
*     Initialize static field and method ID variables specific to Mapping.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI environment.
*-
*/
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Get global references to classes. */
      ( InterpolatorClass = (jclass) (*env)->NewGlobalRef( env,
           (*env)->FindClass( env, 
                              PACKAGE_PATH "Mapping$Interpolator" ) ) ) &&
      ( SpreaderClass = (jclass) (*env)->NewGlobalRef( env,
           (*env)->FindClass( env,
                              PACKAGE_PATH "Mapping$Spreader" ) ) ) &&
      ( ResampleFlagsClass = (jclass) (*env)->NewGlobalRef( env,
           (*env)->FindClass( env,
                              PACKAGE_PATH "ResampleFlags" ) ) ) &&

      /* Get Field IDs. */
      ( InterpolatorSchemeID = 
           (*env)->GetFieldID( env, InterpolatorClass, "scheme", "I" ) ) &&
      ( InterpolatorParamsID =
           (*env)->GetFieldID( env, InterpolatorClass, "params", "[D" ) ) &&
      ( InterpolatorUkern1erID = 
           (*env)->GetFieldID( env, InterpolatorClass, "ukern1er", 
                               "L" PACKAGE_PATH "Ukern1Calculator;" ) ) &&
      ( InterpolatorUinterperID = 
           (*env)->GetFieldID( env, InterpolatorClass, "uinterper",
                               "L" PACKAGE_PATH "UinterpCalculator;" ) ) &&
      ( SpreaderSchemeID =
           (*env)->GetFieldID( env, SpreaderClass, "scheme_", "I" ) ) &&
      ( SpreaderParamsID =
           (*env)->GetFieldID( env, SpreaderClass, "params_", "[D" ) ) &&

      /* Get Method IDs. */
      ( ResampleFlagsGetFlagsIntID =
           (*env)->GetMethodID( env, ResampleFlagsClass, "getFlagsInt",
                                "()I" ) ) &&
      1;
   }
}


/* Class methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Mapping_nativeInitializeMapping(
   JNIEnv *env,          /* Interface pointer */
   jclass class          /* Class object */
) {
   initializeIDs( env );
}


/* Instance methods. */

JNIEXPORT jobjectArray JNICALL Java_uk_ac_starlink_ast_Mapping_decompose(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jbooleanArray jSeries,/* Array to hold series flag */
   jbooleanArray jInverts/* Array to hold Invert attributes */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstMapping *mapptrs[ 2 ];
   jobjectArray jMaps;
   jobject maps[ 2 ];
   int inverts[ 2 ];
   jboolean jinverts[ 2 ];
   jboolean jseries;
   int series;

   maps[ 0 ] = NULL;
   maps[ 1 ] = NULL;
   jMaps = NULL;

   if ( ( jSeries == NULL || jniastCheckArrayLength( env, jSeries, 1 ) ) &&
        ( jInverts == NULL || jniastCheckArrayLength( env, jInverts, 2 ) ) ) {
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astDecompose( pointer.Mapping, &mapptrs[ 0 ], &mapptrs[ 1 ], &series,
                       &inverts[ 0 ], &inverts[ 1 ] );
      )
      if ( ( ( maps[ 0 ] = 
                  jniastMakeObject( env, (AstObject *) mapptrs[ 0 ] ) ) ) &&
           ( ( mapptrs[ 1 ] == NULL ) ||
             ( maps[ 1 ] = 
                  jniastMakeObject( env, (AstObject *) mapptrs[ 1 ] ) ) ) &&
           ( jMaps = (*env)->NewObjectArray( env, ( maps[ 1 ] == NULL ? 1 : 2 ),
                                             MappingClass, NULL ) ) ) {
         if ( jInverts != NULL ) {
            jinverts[ 0 ] = inverts[ 0 ] ? JNI_TRUE : JNI_FALSE;
            jinverts[ 1 ] = inverts[ 1 ] ? JNI_TRUE : JNI_FALSE;
            (*env)->SetBooleanArrayRegion( env, jInverts, 0, 2, jinverts );
         }
         if ( jSeries != NULL ) {
            jseries = series ? JNI_TRUE : JNI_FALSE;
            (*env)->SetBooleanArrayRegion( env, jSeries, 0, 1, &jseries );
         }
         (*env)->SetObjectArrayElement( env, jMaps, 0, maps[ 0 ] );
         if ( maps[ 1 ] != NULL ) {
            (*env)->SetObjectArrayElement( env, jMaps, 1, maps[ 1 ] );
         }
      }
   }
   return jMaps;
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Mapping_simplify(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstMapping *map;
   jobject newobj;
   
   THASTCALL( jniastList( 1, pointer.AstObject ),
      map = astSimplify( pointer.Mapping );
   )
   return jniastMakeObject( env, (AstObject *) map );
}

JNIEXPORT jdoubleArray JNICALL Java_uk_ac_starlink_ast_Mapping_mapBox(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jLbnd_in,/* Lower input coordinates */
   jdoubleArray jUbnd_in,/* Upper input coordinates */
   jboolean forward,     /* Use forward transformation? */
   jint coord_out,       /* Coordinate index */
   jdoubleArray jXl,     /* Lower input point returned */
   jdoubleArray jXu      /* Upper input point returned */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   jdoubleArray jResult = NULL;
   double *result = NULL;
   const double *lbnd_in = NULL;
   const double *ubnd_in = NULL;
   double *xl;
   double *xu;
   int nin;
   int nout;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Validate parameters. */
   THASTCALL( jniastList( 1, pointer.AstObject ),
      if ( forward == JNI_TRUE ) {
         nin = astGetI( pointer.Frame, "Nin" );
         nout = astGetI( pointer.Frame, "Nout" );
      }
      else {
         nin = astGetI( pointer.Frame, "Nout" );
         nout = astGetI( pointer.Frame, "Nin" );
      }
   )
   if ( jniastCheckArrayLength( env, jLbnd_in, nin ) &&
        jniastCheckArrayLength( env, jUbnd_in, nin ) &&
        ( jXl == NULL || jniastCheckArrayLength( env, jXl, nin ) ) &&
        ( jXu == NULL || jniastCheckArrayLength( env, jXu, nin ) ) ) {

      /* Prepare array for return. */
      jResult = (*env)->NewDoubleArray( env, 2 );
      result = (double *) (*env)->GetDoubleArrayElements( env, jResult, NULL );

      /* Get C data from java data. */
      lbnd_in = (const double *) (*env)->GetDoubleArrayElements( env, jLbnd_in,
                                                                 NULL );
      ubnd_in = (const double *) (*env)->GetDoubleArrayElements( env, jUbnd_in,
                                                                 NULL );
      xl = ( jXl == NULL ) 
         ? NULL 
         : (double *) (*env)->GetDoubleArrayElements( env, jXl, NULL );
      xu = ( jXu == NULL ) 
         ? NULL
         : (double *) (*env)->GetDoubleArrayElements( env, jXu, NULL );

      /* Call the C function to do the work. */
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astMapBox( pointer.Mapping, lbnd_in, ubnd_in, forward == JNI_TRUE,
                    (int) coord_out, &result[ 0 ], &result[ 1 ], xl, xu );
      )

      /* Release resources and copy data back. */
      ALWAYS(
         if ( lbnd_in ) {
            (*env)->ReleaseDoubleArrayElements( env, jLbnd_in, 
                                                (jdouble *) lbnd_in,
                                                JNI_ABORT );
         }
         if ( ubnd_in ) {
            (*env)->ReleaseDoubleArrayElements( env, jUbnd_in,
                                                (jdouble *) ubnd_in,
                                                JNI_ABORT );
         }
         if ( xl ) {
            (*env)->ReleaseDoubleArrayElements( env, jXl, (jdouble *) xl, 0 );
         }
         if ( xu ) {
            (*env)->ReleaseDoubleArrayElements( env, jXu, (jdouble *) xu, 0 );
         }
         if ( result ) {
            (*env)->ReleaseDoubleArrayElements( env, jResult, 
                                                (jdouble *) result, 0 );
         }
      )
   }
   return jResult;
}


JNIEXPORT jdoubleArray JNICALL Java_uk_ac_starlink_ast_Mapping_tran1(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint npoint,          /* Number of points to transform */
   jdoubleArray jXin,    /* Input data coordinates */
   jboolean forward      /* Sense of the transformation */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   jdoubleArray jXout = NULL;
   double *xin = NULL;
   double *xout = NULL;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Check the input array is long enough. */
   if ( jniastCheckArrayLength( env, jXin, npoint ) ) {

      /* Construct a java array to hold the output. */
      ( jXout = (*env)->NewDoubleArray( env, npoint ) ) &&

      /* Map the elements of the input and output arrays. */
      ( xout = (*env)->GetDoubleArrayElements( env, jXout, NULL ) ) &&
      ( xin = (*env)->GetDoubleArrayElements( env, jXin, NULL ) );
   
      /* Call the AST routine to do the work. */
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astTran1( pointer.Mapping, npoint, xin, forward == JNI_TRUE, xout );
      )

      /* Unmap the arrays. */
      ALWAYS(
         if ( xout ) {
            (*env)->ReleaseDoubleArrayElements( env, jXout, xout, 0 );
         }
         if ( xin ) {
            (*env)->ReleaseDoubleArrayElements( env, jXin, xin, JNI_ABORT );
         }
      )
   }

   /* Return the new array. */
   return jXout;
}

JNIEXPORT jdoubleArray JNICALL Java_uk_ac_starlink_ast_Mapping_tranN(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint npoint,          /* Number of points to transform */
   jint ncoord_in,       /* Dimensionality of the input space */
   jdoubleArray jIn,     /* Input points */
   jboolean forward,     /* Sense of transformation */
   jint ncoord_out       /* Dimensionality of the output space */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   jdoubleArray jOut = NULL;
   int indim;
   int outdim;
   double *in = NULL;
   double *out = NULL;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Perform validation of things which may cause trouble before getting
    * caught by the more exhaustive validation of the AST routine. */
   if ( ncoord_in == 0 ) {
      jniastThrowIllegalArgumentException( env, 
                                           "tranN: illegal ncoord_in == 0" );
   }
   else if ( jniastCheckArrayLength( env, jIn, ncoord_in * npoint ) ) {
  
      /* Assume that the input array is the right shape for the data. */
      indim = npoint;

      /* Construct a java array the right shape to hold the output. */
      outdim = npoint;
      ( jOut = (*env)->NewDoubleArray( env, npoint * ncoord_out ) ) &&

      /* Map the elements of the input and output arrays. */
      ( in = (*env)->GetDoubleArrayElements( env, jIn, NULL ) ) &&
      ( out = (*env)->GetDoubleArrayElements( env, jOut, NULL ) );

      /* Call the AST routine to do the work. */
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astTranN( pointer.Mapping, npoint, ncoord_in, indim, in,
                   forward == JNI_TRUE, ncoord_out, outdim, out );
      )

      /* Unmap the arrays. */
      ALWAYS(
         if ( in ) {
            (*env)->ReleaseDoubleArrayElements( env, jIn, in, JNI_ABORT );
         }
         if ( out ) {
            (*env)->ReleaseDoubleArrayElements( env, jOut, out, 0 );
         }
      )
   }

   /* Return the new array. */
   return jOut;
}

JNIEXPORT jobjectArray JNICALL Java_uk_ac_starlink_ast_Mapping_tran2(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint npoint,          /* Number of points to transform */
   jdoubleArray jXin,    /* Input X coordinates */
   jdoubleArray jYin,    /* Input Y coordinates */
   jboolean forward      /* Sense of transformation */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   double *xin = NULL;
   double *yin = NULL;
   double *xout = NULL;
   double *yout = NULL;
   int nx;
   int ny;
   jdoubleArray jXout;
   jdoubleArray jYout;
   jobjectArray result = NULL;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Perform validation of things which may not get caught by the 
    * validation of the AST routine. */
   if ( jniastCheckArrayLength( env, jXin, npoint ) &&
        jniastCheckArrayLength( env, jYin, npoint ) ) {

      /* Construct java arrays to hold the output. */
      ( ! (*env)->ExceptionCheck( env ) ) &&
      ( jXout = (*env)->NewDoubleArray( env, npoint ) ) &&
      ( jYout = (*env)->NewDoubleArray( env, npoint ) ) && 
      ( result = (*env)->NewObjectArray( env, 2, DoubleArrayClass, NULL ) );
      if ( ! (*env)->ExceptionCheck( env ) ) {
         (*env)->SetObjectArrayElement( env, result, 0, jXout );
         (*env)->SetObjectArrayElement( env, result, 1, jYout );
      }

      /* Map the elements of the input and output arrays. */
      ( ! (*env)->ExceptionCheck( env ) ) &&
      ( xin = (*env)->GetDoubleArrayElements( env, jXin, NULL ) ) &&
      ( yin = (*env)->GetDoubleArrayElements( env, jYin, NULL ) ) &&
      ( xout = (*env)->GetDoubleArrayElements( env, jXout, NULL ) ) &&
      ( yout = (*env)->GetDoubleArrayElements( env, jYout, NULL ) );

      /* Call the AST routine to do the work. */
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astTran2( pointer.Mapping, npoint, xin, yin, forward == JNI_TRUE,
                   xout, yout );
      )

      /* Unmap the arrays. */
      ALWAYS(
         if ( xin != NULL ) {
            (*env)->ReleaseDoubleArrayElements( env, jXin, xin, JNI_ABORT );
         }
         if ( yin != NULL ) {
            (*env)->ReleaseDoubleArrayElements( env, jYin, yin, JNI_ABORT );
         }
         if ( xout != NULL ) {
            (*env)->ReleaseDoubleArrayElements( env, jXout, xout, 0 );
         }
         if ( yout != NULL ) {
            (*env)->ReleaseDoubleArrayElements( env, jYout, yout, 0 );
         }
      )
   }

   /* Return the array of arrays. */
   return result;
}


JNIEXPORT jobjectArray JNICALL Java_uk_ac_starlink_ast_Mapping_tranP(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint npoint,          /* Number of points to transform */
   jint ncoord_in,       /* Dimensionality of the input space */
   jobjectArray jIn,     /* double[][] array of coordinates */
   jboolean forward,     /* Sense of transformation */
   jint ncoord_out       /* Dimensionality of the output space */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   jobjectArray jOut = NULL;
   jdoubleArray *jPtr_in = NULL;
   jdoubleArray *jPtr_out = NULL;
   const double **ptr_in = NULL;
   double **ptr_out = NULL;
   jsize i;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Allocate pointer arrays to reference the input and output points. */
   ptr_in = jniastMalloc( env, ncoord_in * sizeof( double * ) );
   jPtr_in = jniastMalloc( env, ncoord_in * sizeof( jdoubleArray * ) );
   ptr_out = jniastMalloc( env, ncoord_out * sizeof( double * ) );
   jPtr_out = jniastMalloc( env, ncoord_out * sizeof( jdoubleArray * ) );
 
   /* Construct and map java arrays to hold the output points. */
   if ( ! (*env)->ExceptionCheck( env ) ) {
      jOut = (*env)->NewObjectArray( env, ncoord_out, DoubleArrayClass, NULL );
   }
   for ( i = 0; i < ncoord_out; i++ ) {
      if ( ( ! (*env)->ExceptionCheck( env ) ) &&
           ( jPtr_out[ i ] = (*env)->NewDoubleArray( env, npoint ) ) &&
           ( ptr_out[ i ] = (*env)->GetDoubleArrayElements( env, jPtr_out[ i ], 
                                                            NULL ) ) ) {
         (*env)->SetObjectArrayElement( env, jOut, i, jPtr_out[ i ] );
      }
      else {
         if ( jPtr_out != NULL ) {
            jPtr_out[ i ] = NULL;
         }
      }
   }

   /* Map java arrays which reference the input points. */
   for ( i = 0; i < ncoord_in; i++ ) {
      if ( ! (*env)->ExceptionCheck( env ) &&
           ( jPtr_in[ i ] = (*env)->GetObjectArrayElement( env, jIn, i ) ) &&
           jniastCheckArrayLength( env, jPtr_in[ i ], npoint ) &&
           ( ptr_in[ i ] = (*env)->GetDoubleArrayElements( env, jPtr_in[ i ], 
                                                           NULL ) ) ) {
      }
      else {
         if ( jPtr_in != NULL ) {
            jPtr_in[ i ] = NULL;
         }
      }
   }

   /* Call the AST routine to do the work. */
   THASTCALL( jniastList( 1, pointer.AstObject ),
      astTranP( pointer.Mapping, npoint, ncoord_in, ptr_in, 
                forward == JNI_TRUE, ncoord_out, ptr_out );
   )

   /* Unmap the arrays and free allocated memory. */
   ALWAYS(
      for ( i = 0; i < ncoord_in; i++ ) {
         if ( jPtr_in[ i ] != NULL ) {
            (*env)->ReleaseDoubleArrayElements( env, jPtr_in[ i ], 
                                                (jdouble *) ptr_in[ i ],
                                                JNI_ABORT );
         }
      }
      for ( i = 0; i < ncoord_out; i++ ) {
         if ( jPtr_out[ i ] != NULL ) {
            (*env)->ReleaseDoubleArrayElements( env, jPtr_out[ i ],
                                                (jdouble *) ptr_out[ i ], 0 );
         }
      }
   )
   free( ptr_in );
   free( jPtr_in );
   free( ptr_out );
   free( jPtr_out );

   /* Return the result. */
   return jOut;
}

JNIEXPORT jdoubleArray JNICALL Java_uk_ac_starlink_ast_Mapping_tranGrid(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint ncoord_in,       /* Dimensionality of input space */
   jintArray jLbnd,      /* Lower bound array */
   jintArray jUbnd,      /* Upper bound array */
   jdouble tol,          /* Tolerance */
   jint maxpix,          /* Initial scale size for adaptive algorithm */
   jboolean forward,     /* Forward/reverse flag */
   jint ncoord_out       /* Dimensionality of output space */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   double *out = NULL;
   jdoubleArray *jPtr_out = NULL;
   jobjectArray jOut = NULL;
   const int *lbnd;
   const int *ubnd;
   int i;
   int npoint;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Validate input. */
   if ( jniastCheckArrayLength( env, jLbnd, ncoord_in ) &&
        jniastCheckArrayLength( env, jUbnd, ncoord_in ) ) {
      lbnd = (*env)->GetIntArrayElements( env, jLbnd, NULL );
      ubnd = (*env)->GetIntArrayElements( env, jUbnd, NULL );

      /* Calculate the number of points in the output grid. */
      npoint = 1;
      for ( i = 0; i < ncoord_in; i++ ) {
         npoint *= ( abs( ubnd[ i ] - lbnd[ i ] ) + 1 );
      }

      /* Allocate pointer arrays to reference the output points. */
      out = jniastMalloc( env, ncoord_out * npoint * sizeof( double ) );
      jPtr_out = jniastMalloc( env, ncoord_out * sizeof( jdoubleArray ) );

      /* Construct and map java arrays to hold the output points. */
      if ( ! (*env)->ExceptionCheck( env ) ) {
         jOut = (*env)->NewObjectArray( env, ncoord_out, DoubleArrayClass,
                                        NULL );
      }
      for ( i = 0; i < ncoord_out; i++ ) {
         if ( ( ! (*env)->ExceptionCheck( env ) ) &&
              ( jPtr_out[ i ] = (*env)->NewDoubleArray( env, npoint ) ) ) {
            (*env)->SetObjectArrayElement( env, jOut, i, jPtr_out[ i ] );
         }
         else {
            if ( jPtr_out != NULL ) {
               jPtr_out[ i ] = NULL;
            }
         }
      }

      /* Call the AST routine to do the work. */
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astTranGrid( pointer.Mapping, ncoord_in, lbnd, ubnd, (double) tol,
                      (int) maxpix, forward == JNI_TRUE, ncoord_out, npoint,
                      out );
      )

      /* Copy the output to the java arrays. */
      for ( i = 0; i < ncoord_out; i++ ) {
         if ( ! (*env)->ExceptionCheck( env ) ) {
            (*env)->SetDoubleArrayRegion( env, jPtr_out[ i ],
                                          (jsize) 0, (jsize) npoint,
                                          (jdouble *) (out + i * npoint) );
         }
      }

      /* Release resources. */
      ALWAYS(
         (*env)->ReleaseIntArrayElements( env, jLbnd, (int *) lbnd, JNI_ABORT );
         (*env)->ReleaseIntArrayElements( env, jUbnd, (int *) ubnd, JNI_ABORT );
         free( out );
      )
   }

   /* Return the result. */
   return jOut;
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_Mapping_rate(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jAt,     /* Position array. */
   jint ax1,             /* Axis for which rate is to be calculated. */
   jint ax2              /* Axis to vary. */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   double rate = AST__BAD;
   int nin;
   double *at = NULL;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Validate parameters. */
   THASTCALL( jniastList( 1, pointer.AstObject ),
      nin = astGetI( pointer.Frame, "Nin" );
   )

   if ( jniastCheckArrayLength( env, jAt, nin ) ) {

      /* Get C arrays from java data. */
      at = (*env)->GetDoubleArrayElements( env, jAt, NULL );

      /* Call the AST routine to do the work. */
      THASTCALL( jniastList( 1, pointer.AstObject ),
         rate = astRate( pointer.Mapping, at, (int) ax1, (int) ax2 );
      )

      /* Release resources and copy data back. */
      ALWAYS(
         if ( at ) {
            (*env)->ReleaseDoubleArrayElements( env, jAt, at, JNI_ABORT );
         }
      )
   }

   /* Return the result. */
   return (jdouble) rate;
}

JNIEXPORT jdoubleArray JNICALL Java_uk_ac_starlink_ast_Mapping_linearApprox(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jLbnd,   /* Lower bounds */
   jdoubleArray jUbnd,   /* Upper bounds */
   jdouble tol           /* Tolerance */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   jdoubleArray jFit = NULL;
   double *fit = NULL;
   const double *lbnd = NULL;
   const double *ubnd = NULL;
   int nin;
   int nout;
   int success = 0;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Get mapping characteristics. */
   THASTCALL( jniastList( 1, pointer.AstObject ),
      nin = astGetI( pointer.Frame, "Nin" );
      nout = astGetI( pointer.Frame, "Nout" );
   )

   /* Validate parameters. */
   if ( jniastCheckArrayLength( env, jLbnd, nin ) &&
        jniastCheckArrayLength( env, jUbnd, nin ) ) {

      /* Get C arrays from java data. */
      lbnd = (*env)->GetDoubleArrayElements( env, jLbnd, NULL );
      ubnd = (*env)->GetDoubleArrayElements( env, jUbnd, NULL );

      /* Construct array for result. */
      jFit = (*env)->NewDoubleArray( env, ( nin + 1 ) * nout );
      fit = (double *) (*env)->GetDoubleArrayElements( env, jFit, NULL );

      /* Call AST routine to do the work. */
      THASTCALL( jniastList( 1, pointer.AstObject ),
         success = astLinearApprox( pointer.Mapping, lbnd, ubnd, tol, fit );
      )

      /* Release resources. */
      ALWAYS(
         if ( lbnd ) {
            (*env)->ReleaseDoubleArrayElements( env, jLbnd, (jdouble *) lbnd,
                                                JNI_ABORT );
         }
         if ( ubnd ) {
            (*env)->ReleaseDoubleArrayElements( env, jUbnd, (jdouble *) ubnd,
                                                JNI_ABORT );
         }
         if ( fit ) {
            (*env)->ReleaseDoubleArrayElements( env, jFit, (jdouble *) fit, 0 );
         }
      )

      /* According to status, return the result or null. */
      return success ? jFit : NULL;
   }
   return NULL;
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_Mapping_mapSplit(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jintArray jIn,        /* Array of input indices */
   jintArray jOut        /* Array to store output indices */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstMapping *outMap;
   int nin;
   int maxNout;
   int *in;
   int *out;

   ENSURE_SAME_TYPE(int, jint)

   /* Validate parameters. */
   THASTCALL( jniastList( 1, pointer.AstObject ),
      maxNout = astGetI( pointer.Mapping, "Nout" );
   )
   if ( jniastCheckNotNull( env, jIn ) &&
        jniastCheckArrayLength( env, jOut, maxNout ) ) {

      /* Prepare array for return. */
      nin = (*env)->GetArrayLength( env, jIn );
      in = (*env)->GetIntArrayElements( env, jIn, NULL );
      out = (int *) (*env)->GetIntArrayElements( env, jOut, NULL );

      /* Call the C function to do the work. */
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astMapSplit( pointer.Mapping, nin, in, out, &outMap );
      )

      /* Release resources. */
      ALWAYS(
         (*env)->ReleaseIntArrayElements( env, jIn, in, JNI_ABORT );
         (*env)->ReleaseIntArrayElements( env, jOut, out, 0 );
      )

      /* Return the created map. */
      return outMap == NULL ? NULL
                            : jniastMakeObject( env, (AstObject *) outMap );
   }
   return NULL;
}


#define MAKE_RESAMPLEX(Xletter,Xtype,Xjtype,XJtype,Xjsign) \
 \
static void fuinterp##Xletter( int ndim_in, const int lbnd_in[], \
                               const int ubnd_in[], const Xtype in[], \
                               const Xtype in_var[], int npoint, \
                               const int offset[], \
                               const double *const coords[], \
                               const double params[], int flags, Xtype badval, \
                               Xtype out[], Xtype out_var[], int *nbad ); \
 \
typedef struct { \
   JNIEnv *env; \
   jobject calculator; \
   jmethodID method; \
   jobject jFlags; \
   int *lbnd_in; \
   jintArray jLbnd_in; \
   int *ubnd_in; \
   jintArray jUbnd_in; \
   Xtype *in; \
   jarray jIn; \
   Xtype *in_var; \
   jarray jIn_var; \
   Xtype *out; \
   jarray jOut; \
   Xtype *out_var; \
   jarray jOut_var; \
} UinterpInfo##Xletter; \
 \
JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_Mapping_resample##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jint ndim_in,         /* Dimensionality of the input grid */ \
   jintArray jLbnd_in,   /* Lower input bounds */ \
   jintArray jUbnd_in,   /* Upper input bounds */ \
   Xjtype##Array jIn,    /* Input data grid */ \
   Xjtype##Array jIn_var,/* Input variance grid */ \
   jobject interpObj,    /* Interpolator object */ \
   jobject jFlags,       /* Bad value flag */ \
   jdouble tol,          /* Tolerance */ \
   jint maxpix,          /* Initial scale size */ \
   Xjtype badval,        /* Bad value */ \
   jint ndim_out,        /* Dimensionality of the output grid */ \
   jintArray jLbnd_out,  /* Lower output bounds of array */ \
   jintArray jUbnd_out,  /* Upper output bounds of array */ \
   jintArray jLbnd,      /* Lower output bounds for calculation */ \
   jintArray jUbnd,      /* Upper output bounds for calculation */ \
   Xjtype##Array jOut,   /* Output data grid */ \
   Xjtype##Array jOut_var/* Output variance grid */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   int *lbnd = NULL; \
   int *lbnd_in = NULL; \
   int *lbnd_out = NULL; \
   int *ubnd = NULL; \
   int *ubnd_in = NULL; \
   int *ubnd_out = NULL; \
   Xtype *in = NULL; \
   Xtype *out = NULL; \
   Xtype *in_var = NULL; \
   Xtype *out_var = NULL; \
   int interp; \
   int flags; \
   int nbad; \
   jdoubleArray jParams; \
   double *params; \
   void (*finterp)(); \
   Ukern1Info *infoUk1; \
   UinterpInfo##Xletter *infoUi; \
   jobject calc; \
   jmethodID method; \
   jclass calcClass; \
 \
   ENSURE_SAME_TYPE(Xtype,Xjtype) \
   ENSURE_SAME_TYPE(int,jint) \
   ENSURE_SAME_TYPE(double,jdouble) \
 \
   /* Decode flags. */ \
   flags = (int) (*env)->CallIntMethod( env, jFlags, \
                                        ResampleFlagsGetFlagsIntID ); \
 \
   /* Map array elements from java arrays. */ \
   ( lbnd = (int *) (*env)->GetIntArrayElements( env, jLbnd, NULL ) ) && \
   ( ubnd = (int *) (*env)->GetIntArrayElements( env, jUbnd, NULL ) ) && \
   ( lbnd_in = (int *) (*env)->GetIntArrayElements( env, jLbnd_in, \
                                                    NULL ) ) && \
   ( ubnd_in = (int *) (*env)->GetIntArrayElements( env, jUbnd_in, \
                                                    NULL ) ) && \
   ( lbnd_out = (int *) (*env)->GetIntArrayElements( env, jLbnd_out, \
                                                     NULL ) ) && \
   ( ubnd_out = (int *) (*env)->GetIntArrayElements( env, jUbnd_out, \
                                                     NULL ) ) && \
   ( in = (Xtype *) (*env)->Get##XJtype##ArrayElements( env, jIn, \
                                                        NULL ) ) && \
   ( out = (Xtype *) (*env)->Get##XJtype##ArrayElements( env, jOut, \
                                                         NULL ) ); \
   if ( jIn_var != NULL && ! (*env)->ExceptionCheck( env ) ) { \
      ( in_var = (Xtype *) \
           (*env)->Get##XJtype##ArrayElements( env, jIn_var, NULL ) ) && \
      ( out_var = (Xtype *) \
           (*env)->Get##XJtype##ArrayElements( env, jOut_var, NULL ) ); \
   } \
 \
   /* Check that the arrays are all long enough. */ \
   if ( ! (*env)->ExceptionCheck( env ) ) { \
      if ( (*env)->GetArrayLength( env, jLbnd_in ) < ndim_in || \
           (*env)->GetArrayLength( env, jUbnd_in ) < ndim_in || \
           (*env)->GetArrayLength( env, jLbnd ) < ndim_out || \
           (*env)->GetArrayLength( env, jUbnd ) < ndim_out || \
           (*env)->GetArrayLength( env, jLbnd_out ) < ndim_out || \
           (*env)->GetArrayLength( env, jUbnd_out ) < ndim_out ) { \
         jniastThrowIllegalArgumentException( env, "resample" #Xletter ": " \
                                              "bound arrays too short" ); \
      } \
      else { \
         int i; \
         int nin = 1; \
         int nout = 1; \
         for ( i = 0; i < ndim_in; i++ ) { \
            nin *= ( ubnd_in[ i ] - lbnd_in[ i ] + 1 ); \
         } \
         for ( i = 0; i < ndim_out; i++ ) { \
            nout *= ( ubnd_out[ i ] - lbnd_out[ i ] + 1 ); \
         } \
         if ( (*env)->GetArrayLength( env, jIn ) < nin || \
              (*env)->GetArrayLength( env, jOut ) < nout || \
              ( in_var != NULL && \
                (*env)->GetArrayLength( env, jIn_var ) < nin ) || \
              ( out_var != NULL && \
                (*env)->GetArrayLength( env, jOut_var ) < nout ) ) { \
            jniastThrowIllegalArgumentException( env, "resample" #Xletter ": " \
                                                 "data/variance arrays " \
                                                 "too short" ); \
         } \
      } \
   } \
 \
   /* Interrogate the interpolator object to find out how the sub-pixel \
    * interpolation is to be done. */ \
   ( ! (*env)->ExceptionCheck( env ) ) && \
   ( interp = (int) (*env)->GetIntField( env, interpObj, \
                                         InterpolatorSchemeID ) ) && \
   ( jParams = (*env)->GetObjectField( env, interpObj, \
                                       InterpolatorParamsID ) ) && \
   ( params = (double *) (*env)->GetDoubleArrayElements( env, \
                                                         jParams, NULL ) ); \
   if ( ! (*env)->ExceptionCheck( env ) ) { \
      switch ( interp ) { \
         case AST__UKERN1: \
            finterp = (void (*)()) fukern1; \
            calc = (*env)->GetObjectField( env, interpObj,  \
                                           InterpolatorUkern1erID ); \
            calcClass = (*env)->GetObjectClass( env, calc ); \
            infoUk1 = jniastMalloc( env, sizeof( Ukern1Info ) ); \
            *((Ukern1Info **) (params + 1)) = infoUk1; \
            infoUk1->env = env; \
            infoUk1->calculator = calc; \
            infoUk1->method = (*env)->GetMethodID( env, calcClass, \
                                                   "ukern1", "(D)D" ); \
            break; \
         case AST__UINTERP: \
            finterp = (void (*)()) fuinterp##Xletter; \
            calc = (*env)->GetObjectField( env, interpObj, \
                                           InterpolatorUinterperID ); \
            calcClass = (*env)->GetObjectClass( env, calc ); \
            infoUi = jniastMalloc( env, sizeof( UinterpInfo##Xletter ) ); \
            *((UinterpInfo##Xletter **) params) = infoUi; \
            infoUi->env = env; \
            infoUi->calculator = calc; \
            infoUi->method =  \
               (*env)->GetMethodID( env, calcClass, \
                                    "uinterp" #Xletter, "(" \
                                    "I"          /* ndim_in */ \
                                    "[I"         /* lbnd_in */ \
                                    "[I"         /* ubnd_in */ \
                                    "[" #Xjsign  /* in */ \
                                    "[" #Xjsign  /* in_var */ \
                                    "I"          /* npoint */ \
                                    "[I"         /* offset */ \
                                    "[[D"        /* coords */ \
                                    #Xjsign      /* badval */ \
                                    "[" #Xjsign  /* out */ \
                                    "[" #Xjsign  /* out_var */ \
                                    ")I" ); \
            infoUi->jFlags = jFlags; \
            infoUi->lbnd_in = lbnd_in; \
            infoUi->jLbnd_in = jLbnd_in; \
            infoUi->ubnd_in = ubnd_in; \
            infoUi->jUbnd_in = jUbnd_in; \
            infoUi->in = in; \
            infoUi->jIn = jIn; \
            infoUi->in_var = in_var; \
            infoUi->jIn_var = jIn_var; \
            infoUi->out = out; \
            infoUi->jOut = jOut; \
            infoUi->out_var = out_var; \
            infoUi->jOut_var = jOut_var; \
            break; \
         default: \
            finterp = (void (*)()) NULL; \
      } \
   } \
 \
   /* Call the AST routine to do the work. */ \
   THASTCALL( jniastList( 1, pointer.AstObject ), \
      nbad = astResample##Xletter( pointer.Mapping, ndim_in, lbnd_in, \
                                   ubnd_in, in, in_var, interp, finterp, \
                                   params, flags, tol, maxpix, badval, \
                                   ndim_out, lbnd_out, ubnd_out, lbnd, ubnd, \
                                   out, out_var ); \
   ) \
 \
   /* Release resources. */ \
   switch ( interp ) { \
      case AST__UKERN1: \
         free( infoUk1 ); \
         break; \
      case AST__UINTERP: \
         free( infoUi ); \
         break; \
      default: \
         ; \
   } \
   ALWAYS( \
      if ( lbnd ) { \
         (*env)->ReleaseIntArrayElements( env, jLbnd, (jint *) lbnd, \
                                          JNI_ABORT ); \
      } \
      if ( ubnd ) { \
         (*env)->ReleaseIntArrayElements( env, jUbnd, (jint *) ubnd, \
                                          JNI_ABORT ); \
      } \
      if ( lbnd_in ) { \
         (*env)->ReleaseIntArrayElements( env, jLbnd_in, (jint *) lbnd_in, \
                                          JNI_ABORT ); \
      } \
      if ( ubnd_in ) { \
         (*env)->ReleaseIntArrayElements( env, jUbnd_in, (jint *) ubnd_in, \
                                          JNI_ABORT ); \
      } \
      if ( lbnd_out ) { \
         (*env)->ReleaseIntArrayElements( env, jLbnd_out, (jint *) lbnd_out, \
                                          JNI_ABORT ); \
      } \
      if ( ubnd_out ) { \
         (*env)->ReleaseIntArrayElements( env, jUbnd_out, (jint *) ubnd_out, \
                                          JNI_ABORT ); \
      } \
      if ( in ) { \
         (*env)->Release##XJtype##ArrayElements( env, jIn, (Xjtype *) in, \
                                                 JNI_ABORT ); \
      } \
      if ( out ) { \
         (*env)->Release##XJtype##ArrayElements( env, jOut, \
                                                 (Xjtype *) out, 0 ); \
      } \
      if ( jIn_var != NULL ) { \
         if ( in_var ) { \
            (*env)->Release##XJtype##ArrayElements( env, jIn_var, \
                                                    (Xjtype *) in_var, \
                                                    JNI_ABORT ); \
         } \
         if ( out_var ) { \
            (*env)->Release##XJtype##ArrayElements( env, jOut_var, \
                                                    (Xjtype *) out_var, 0 ); \
         } \
      } \
   ) \
 \
   /* Return the number of bad pixels. */ \
   return (jint) nbad; \
} \
 \
static void fuinterp##Xletter( int ndim_in, const int *lbnd_in,  \
                               const int *ubnd_in, const Xtype *in, \
                               const Xtype *in_var, int npoint,  \
                               const int *offset,  \
                               const double *const *coords, \
                               const double *params, int flags, \
                               Xtype badval, Xtype *out, Xtype *out_var, \
                               int *nbad ) { \
/* \
*+ \
*  Name: \
*     fuinterpX \
 \
*  Purpose: \
*     Invoke instance method to do user generic subpixel interpolation. \
 \
*  Description: \
*     This function is called by astResampleX when the interpolation \
*     scheme is AST__UINTERP.  It in turn calls the uinterpX method \
*     of the appropriate UinterpCalculator object so that AST can \
*     use the results of that call.  The form and parameters of the \
*     function are as documented in the entry for the (dummy)  \
*     astUinterp function in SUN/211. \
*- \
*/ \
   UinterpInfo##Xletter *info = *((UinterpInfo##Xletter **) (params)); \
   JNIEnv *env = info->env; \
   jobject calc = info->calculator; \
   jmethodID method = info->method; \
   jobject jFlags = info->jFlags; \
   jintArray jLbnd_in; \
   jintArray jUbnd_in; \
   Xjtype##Array jIn; \
   Xjtype##Array jIn_var; \
   jintArray jOffset; \
   jobjectArray jCoords; \
   Xjtype##Array jOut; \
   Xjtype##Array jOut_var; \
   jdoubleArray jArr; \
   int nin; \
   int nout; \
   register int i; \
 \
   /* Get the size of the input and output grids if we will need them. */ \
   nin = 1; \
   nout = 1; \
   if ( in != info->in || in_var != info->in_var ) { \
      for ( i = 0; i < ndim_in; i++ ) { \
         nin *= ( ubnd_in[ i ] - lbnd_in[ i ] + 1 ); \
      } \
   } \
   if ( out != info->out || out_var != info->out_var ) { \
      register int maxoff = 0; \
      for ( i = 0; i < npoint; i++ ) { \
         if ( offset[ i ] > maxoff ) { \
            maxoff = offset[ i ]; \
         } \
      } \
      nout = maxoff + 1; \
   } \
 \
   /* Get java arrays to pass to the java method.  I expect these to be \
    * the same arrays which were passed to astResampleX in the first \
    * place, in which place those arrays can be passed directly to the \
    * uinterp method of the UinterpCalculator object.  However, that  \
    * does depend on the details of the AST implementation, so check \
    * each one, and construct a new array in each case if we don't  \
    * already have the one we are expecting.  This is a bit tedious, \
    * but better safe than sorry. */ \
   if ( lbnd_in == info->lbnd_in ) { \
      jLbnd_in = info->jLbnd_in; \
   } \
   else { \
      if ( ! (*env)->ExceptionCheck( env ) && \
           ( jLbnd_in = (*env)->NewIntArray( env, ndim_in ) ) ) { \
         (*env)->SetIntArrayRegion( env, jLbnd_in, 0, ndim_in, \
                                    (jint *) lbnd_in ); \
      } \
   } \
   if ( ubnd_in == info->ubnd_in ) { \
      jUbnd_in = info->jUbnd_in; \
   } \
   else { \
      if ( ! (*env)->ExceptionCheck( env ) && \
           ( jUbnd_in = (*env)->NewIntArray( env, ndim_in ) ) ) { \
         (*env)->SetIntArrayRegion( env, jUbnd_in, 0, ndim_in, \
                                    (jint *) ubnd_in ); \
      } \
   } \
   if ( in == info->in ) { \
      jIn = info->jIn; \
   } \
   else { \
      if ( ! (*env)->ExceptionCheck( env ) && \
           ( jIn = (*env)->New##XJtype##Array( env, nin ) ) ) { \
         (*env)->Set##XJtype##ArrayRegion( env, jIn, 0, nin, \
                                           (Xjtype *) in ); \
      } \
   } \
   if ( in_var == info->in_var ) { \
      jIn_var = info->jIn_var; \
   } \
   else { \
      if ( ! (*env)->ExceptionCheck( env ) && \
           ( jIn_var = (*env)->New##XJtype##Array( env, nin ) ) ) { \
         (*env)->Set##XJtype##ArrayRegion( env, jIn_var, 0, nin, \
                                           (Xjtype *) in_var ); \
      } \
   } \
   if ( out == info->out ) { \
      jOut = info->jOut; \
   } \
   else { \
      if ( ! (*env)->ExceptionCheck( env ) && \
           ( jOut = (*env)->New##XJtype##Array( env, nout ) ) ) { \
         (*env)->Set##XJtype##ArrayRegion( env, jOut, 0, nout, \
                                           (Xjtype *) out ); \
      } \
   } \
   if ( out_var == info->out_var ) { \
      jOut_var = info->jOut_var; \
   } \
   else { \
      if ( ! (*env)->ExceptionCheck( env ) && \
           ( jOut_var = (*env)->New##XJtype##Array( env, nout ) ) ) { \
         (*env)->Set##XJtype##ArrayRegion( env, jOut_var, 0, nout, \
                                           (Xjtype *) out_var ); \
      } \
   } \
 \
   /* Construct new java arrays to hold the offset and coords information. */ \
   if ( ! (*env)->ExceptionCheck( env ) ) { \
      if ( ( jOffset = (*env)->NewIntArray( env, npoint ) ) ) { \
         (*env)->SetIntArrayRegion( env, jOffset, 0, npoint, \
                                    (jint *) offset ); \
      } \
      if ( ! (*env)->ExceptionCheck( env ) && \
           ( jCoords = (*env)->NewObjectArray( env, ndim_in, \
                                               DoubleArrayClass, NULL ) ) ) { \
         for ( i = 0; i < ndim_in; i++ ) { \
            if ( ( jArr = (*env)->NewDoubleArray( env, npoint ) ) ) { \
               (*env)->SetDoubleArrayRegion( env, jArr, 0, npoint, \
                                             (jdouble *) coords[ i ] ); \
               (*env)->SetObjectArrayElement( env, jCoords, i, jArr ); \
            } \
            else { \
               break; \
            } \
         } \
      } \
   } \
 \
   if ( ! (*env)->ExceptionCheck( env ) ) { \
      /* Call the uinterp method of the UinterpCalculator object. */ \
      *nbad = (*env)->CallIntMethod( env, calc, method, (jint) ndim_in, \
                                     jLbnd_in, jUbnd_in, jIn, jIn_var, \
                                     (jint) npoint, jOffset, jCoords, jFlags, \
                                     (Xjtype) badval, jOut, jOut_var ); \
   } \
 \
   /* And tidy up.  Newly created arrays are local references and will \
    * disappear of their own accord.  We just need to make sure that \
    * the results of the calculations make it back to where AST thinks \
    * they are. */ \
   if ( ! (*env)->ExceptionCheck( env ) ) { \
      if ( out != (Xtype *) info->out ) { \
         (*env)->Get##XJtype##ArrayRegion( env, jOut, 0, nout, \
                                           (Xjtype *) out ); \
      } \
      if ( out_var != (Xtype *) info->out_var ) { \
         (*env)->Get##XJtype##ArrayRegion( env, jOut_var, 0, nout, \
                                           (Xjtype *) out_var ); \
      } \
   } \
 \
   /* Finally inform AST if any exceptions have been thrown. */ \
   if ( (*env)->ExceptionCheck( env ) ) { \
      astSetStatus( AST__UINER ); \
   } \
}
MAKE_RESAMPLEX(D,double,jdouble,Double,D)
MAKE_RESAMPLEX(F,float,jfloat,Float,F)
MAKE_RESAMPLEX(L,long,jlong,Long,J)
MAKE_RESAMPLEX(I,int,jint,Int,I)
MAKE_RESAMPLEX(S,short,jshort,Short,S)
MAKE_RESAMPLEX(B,signed char,jbyte,Byte,B)
/* There is no reason that you can't define the corresponding unsigned
 * methods, since the underlying AST library supports this, but they 
 * cannot be handled properly using standard Java data types. 
 *
 * MAKE_RESAMPLEX(UL,unsigned long,jlong,Long,J)
 * MAKE_RESAMPLEX(UI,unsigned int,jint,Int,I)
 * MAKE_RESAMPLEX(US,unsigned short,jshort,Short,S)
 * MAKE_RESAMPLEX(UB,unsigned char,jbyte,Byte,B)
 */
#undef MAKE_RESAMPLEX

static void fukern1( double offset, const double *params, int flags, 
                     double *value ) {
/*
*+
*  Name:
*     fukern1

*  Purpose:
*     Invoke instance method to do user 1-d kernel interpolation.

*  Description:
*     This function is called by astResampleX when the interpolation 
*     scheme is AST__UKERN1.  It in turn calls the ukern1 method of
*     the appropriate Ukern1Calculator object and returns the result
*     of that call to AST.  The form and parameters of the function 
*     are as documented in the entry for the (dummy) astUkern1 
*     function in SUN/211.
*-
*/
   Ukern1Info *info = *((Ukern1Info **) (params + 1));
   JNIEnv *env = info->env;
   jobject calc = info->calculator;
   jmethodID method = info->method;

   *value = (*env)->CallDoubleMethod( env, calc, method, (jdouble) offset );
   if ( (*env)->ExceptionCheck( env ) ) {
      astSetStatus( AST__UK1ER );
   }
}

#define MAKE_REBINX(Xletter,Xtype,Xjtype,XJtype) \
 \
JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_Mapping_rebin##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jdouble wlim,         /* Weight threshold */ \
   jint ndim_in,         /* Dimensionality of the input grid */ \
   jintArray jLbnd_in,   /* Lower input bounds */ \
   jintArray jUbnd_in,   /* Upper input bounds */ \
   Xjtype##Array jIn,    /* Input data grid */ \
   Xjtype##Array jIn_var,/* Input variance grid */ \
   jobject spreadObj,    /* Spreader object */ \
   jboolean usebad,      /* Bad value flag */ \
   jdouble tol,          /* Tolerance */ \
   jint maxpix,          /* Initial scale size */ \
   Xjtype badval,        /* Bad value */ \
   jint ndim_out,        /* Dimensionality of the output grid */ \
   jintArray jLbnd_out,  /* Lower output bounds of array */ \
   jintArray jUbnd_out,  /* Upper output bounds of array */ \
   jintArray jLbnd,      /* Lower output bounds for calculation */ \
   jintArray jUbnd,      /* Upper output bounds for calculation */ \
   Xjtype##Array jOut,   /* Output data grid */ \
   Xjtype##Array jOut_var/* Output variance grid */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   int *lbnd = NULL; \
   int *lbnd_in = NULL; \
   int *lbnd_out = NULL; \
   int *ubnd = NULL; \
   int *ubnd_in = NULL; \
   int *ubnd_out = NULL; \
   Xtype *in = NULL; \
   Xtype *out = NULL; \
   Xtype *in_var = NULL; \
   Xtype *out_var = NULL; \
   int scheme; \
   int flags; \
   int nbad; \
   jdoubleArray jParams; \
   double *params; \
   int spread; \
 \
   ENSURE_SAME_TYPE(Xtype,Xjtype) \
   ENSURE_SAME_TYPE(int,jint) \
   ENSURE_SAME_TYPE(double,jdouble) \
 \
   /* Map array elements from java arrays. */ \
   ( lbnd = (int *) (*env)->GetIntArrayElements( env, jLbnd, NULL ) ) && \
   ( ubnd = (int *) (*env)->GetIntArrayElements( env, jUbnd, NULL ) ) && \
   ( lbnd_in = (int *) (*env)->GetIntArrayElements( env, jLbnd_in, \
                                                    NULL ) ) && \
   ( ubnd_in = (int *) (*env)->GetIntArrayElements( env, jUbnd_in, \
                                                    NULL ) ) && \
   ( lbnd_out = (int *) (*env)->GetIntArrayElements( env, jLbnd_out, \
                                                     NULL ) ) && \
   ( ubnd_out = (int *) (*env)->GetIntArrayElements( env, jUbnd_out, \
                                                     NULL ) ) && \
   ( in = (Xtype *) (*env)->Get##XJtype##ArrayElements( env, jIn, \
                                                        NULL ) ) && \
   ( out = (Xtype *) (*env)->Get##XJtype##ArrayElements( env, jOut, \
                                                         NULL ) ); \
   if ( jIn_var != NULL && ! (*env)->ExceptionCheck( env ) ) { \
      ( in_var = (Xtype *) \
           (*env)->Get##XJtype##ArrayElements( env, jIn_var, NULL ) ) && \
      ( out_var = (Xtype *) \
           (*env)->Get##XJtype##ArrayElements( env, jOut_var, NULL ) ); \
   } \
 \
   /* Check that the arrays are all long enough. */ \
   if ( ! (*env)->ExceptionCheck( env ) ) { \
      if ( (*env)->GetArrayLength( env, jLbnd_in ) < ndim_in || \
           (*env)->GetArrayLength( env, jUbnd_in ) < ndim_in || \
           (*env)->GetArrayLength( env, jLbnd ) < ndim_out || \
           (*env)->GetArrayLength( env, jUbnd ) < ndim_out || \
           (*env)->GetArrayLength( env, jLbnd_out ) < ndim_out || \
           (*env)->GetArrayLength( env, jUbnd_out ) < ndim_out ) { \
         jniastThrowIllegalArgumentException( env, "rebin" #Xletter ": " \
                                              "bound arrays too short" ); \
      } \
      else { \
         int i; \
         int nin = 1; \
         int nout = 1; \
         for ( i = 0; i < ndim_in; i++ ) { \
            nin *= ( ubnd_in[ i ] - lbnd_in[ i ] + 1 ); \
         } \
         for ( i = 0; i < ndim_out; i++ ) { \
            nout *= ( ubnd_out[ i ] - lbnd_out[ i ] + 1 ); \
         } \
         if ( (*env)->GetArrayLength( env, jIn ) < nin || \
              (*env)->GetArrayLength( env, jOut ) < nout || \
              ( in_var != NULL && \
                (*env)->GetArrayLength( env, jIn_var ) < nin ) || \
              ( out_var != NULL && \
                (*env)->GetArrayLength( env, jOut_var ) < nout ) ) { \
            jniastThrowIllegalArgumentException( env, "resample" #Xletter ": " \
                                                 "data/variance arrays " \
                                                 "too short" ); \
         } \
      } \
   } \
 \
   /* Interrogate the Spreader object to find out how the spreading \
    * is going to be done. */ \
   ( ! (*env)->ExceptionCheck( env ) ) && \
   ( spread = (int) (*env)->GetIntField( env, spreadObj, \
                                         SpreaderSchemeID ) ) && \
   ( jParams = (*env)->GetObjectField( env, spreadObj, \
                                       SpreaderParamsID ) ) && \
   ( params =  (*env)->GetDoubleArrayElements( env, \
                                                         jParams, NULL ) ); \
   flags = ( usebad == JNI_TRUE ) ? AST__USEBAD : 0; \
 \
   /* Call the AST routine to do the work. */ \
   THASTCALL( jniastList( 1, pointer.AstObject ), \
      astRebin##Xletter( pointer.Mapping, wlim, ndim_in, lbnd_in, ubnd_in, \
                         in, in_var, spread, params, flags, tol, maxpix, \
                         badval, ndim_out, lbnd_out, ubnd_out, lbnd, ubnd, \
                         out, out_var ); \
   ) \
 \
   /* Release resources. */ \
   ALWAYS( \
      if ( lbnd ) { \
         (*env)->ReleaseIntArrayElements( env, jLbnd, (jint *) lbnd, \
                                          JNI_ABORT ); \
      } \
      if ( ubnd ) { \
         (*env)->ReleaseIntArrayElements( env, jUbnd, (jint *) ubnd, \
                                          JNI_ABORT ); \
      } \
      if ( lbnd_in ) { \
         (*env)->ReleaseIntArrayElements( env, jLbnd_in, (jint *) lbnd_in, \
                                          JNI_ABORT ); \
      } \
      if ( ubnd_in ) { \
         (*env)->ReleaseIntArrayElements( env, jUbnd_in, (jint *) ubnd_in, \
                                          JNI_ABORT ); \
      } \
      if ( lbnd_out ) { \
         (*env)->ReleaseIntArrayElements( env, jLbnd_out, (jint *) lbnd_out, \
                                          JNI_ABORT ); \
      } \
      if ( ubnd_out ) { \
         (*env)->ReleaseIntArrayElements( env, jUbnd_out, (jint *) ubnd_out, \
                                          JNI_ABORT ); \
      } \
      if ( in ) { \
         (*env)->Release##XJtype##ArrayElements( env, jIn, (Xjtype *) in, \
                                                 JNI_ABORT ); \
      } \
      if ( out ) { \
         (*env)->Release##XJtype##ArrayElements( env, jOut, \
                                                 (Xjtype *) out, 0 ); \
      } \
      if ( jIn_var != NULL ) { \
         if ( in_var ) { \
            (*env)->Release##XJtype##ArrayElements( env, jIn_var, \
                                                    (Xjtype *) in_var, \
                                                    JNI_ABORT ); \
         } \
         if ( out_var ) { \
            (*env)->Release##XJtype##ArrayElements( env, jOut_var, \
                                                    (Xjtype *) out_var, 0 ); \
         } \
      } \
   ) \
}
MAKE_REBINX(I,int,jint,Int)
MAKE_REBINX(F,float,jfloat,Float)
MAKE_REBINX(D,double,jdouble,Double)
#undef MAKE_REBINX


/* $Id$ */
