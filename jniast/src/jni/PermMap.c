/*
*+
*  Name:
*     PermMap.c

*  Purpose:
*     JNI implementations of native methods of PermMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     3-OCT-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_PermMap.h"

/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_PermMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint nin,             /* Number of input coordinates */
   jintArray jInperm,    /* Input coordinate permutations */
   jint nout,            /* Number of output coordinates */
   jintArray jOutperm,   /* Output coordinate permutations */
   jdoubleArray jConstant/* Optional fixed values */
) {
   AstPointer pointer;
   const int *inperm = NULL;
   const int *outperm = NULL;
   const double *constant;

   ENSURE_SAME_TYPE(double,jdouble)
   ENSURE_SAME_TYPE(int,jint)

   if ( jniastCheckArrayLength( env, jInperm, nin ) &&
        jniastCheckArrayLength( env, jOutperm, nout ) ) {

      /* Map the elements of the arrays as required. */
      inperm = (const int *)
               (*env)->GetIntArrayElements( env, jInperm, NULL );
      outperm = (const int *) 
                (*env)->GetIntArrayElements( env, jOutperm, NULL );
      if ( jConstant != NULL ) {
         constant = (const double *) 
                    (*env)->GetDoubleArrayElements( env, jConstant, NULL );
      }
      else {
         constant = NULL;
      }

      /* Call the AST function to do the work. */
      ASTCALL(
         pointer.PermMap = astPermMap( (int) nin, inperm, (int) nout, outperm,
                                       constant, "" );
      )

      /* Release resources. */
      ALWAYS(
         if ( inperm ) {
            (*env)->ReleaseIntArrayElements( env, jInperm, 
                                             (jint *) inperm, JNI_ABORT );
         }
         if ( outperm ) {
            (*env)->ReleaseIntArrayElements( env, jOutperm, 
                                             (jint *) outperm, JNI_ABORT );
         }
         if ( constant != NULL ) {
            (*env)->ReleaseDoubleArrayElements( env, jConstant, 
                                                (jdouble *) constant,
                                                JNI_ABORT );
         }
      )

      /* Set the pointer field of the java object. */
      jniastInitObject( env, this, pointer );
   }
}
/* $Id$ */
