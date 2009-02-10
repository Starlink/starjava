/*
*+
*  Name:
*     MathMap.c

*  Purpose:
*     JNI implmentation of native methods of MathMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     3-OCT-2001 (MBT):
*        Original version.
*     21-FEB-2003 (PWD):
*        Corrected memory allocation for input string arrays.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_MathMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_MathMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint nin,             /* Number of input coords */
   jint nout,            /* Number of output coords */
   jobjectArray jFwd,    /* Array of forward transformation strings */
   jobjectArray jInv     /* Array of inverse transformation strings */
) {
   AstPointer pointer;
   jstring *jFwdEl;
   jstring *jInvEl;
   const char **fwd;
   const char **inv;
   jint nfwd;
   jint ninv;
   int i;

   if ( jniastCheckNotNull( env, jFwd ) &&
        jniastCheckNotNull( env, jInv ) ) {

      /* Get sizes of java arrays. */
      nfwd = (*env)->GetArrayLength( env, jFwd );
      ninv = (*env)->GetArrayLength( env, jInv );

      /* Copy java arrays into C arrays. */
      fwd = jniastMalloc( env, nfwd * sizeof( char * ) );
      inv = jniastMalloc( env, ninv * sizeof( char * ) );
      jFwdEl = jniastMalloc( env, nfwd * sizeof( jstring ) );
      jInvEl = jniastMalloc( env, ninv * sizeof( jstring ) );
      if ( ! (*env)->ExceptionCheck( env ) ) {
         for ( i = 0; i < nfwd; i++ ) {
            jFwdEl[ i ] = (*env)->GetObjectArrayElement( env, jFwd, i );
            if ( ! jniastCheckNotNull( env,
                      (jobject) 
                      ( fwd[ i ] = (const char *) 
                                   jniastGetUTF( env, jFwdEl[ i ] ) ) ) ) {
               for ( ; i < nfwd; i++ ) {
                  fwd[ i ] = NULL;
               }
               break;
            }
         }
      }
      if ( ! (*env)->ExceptionCheck( env ) ) {
         for ( i = 0; i < ninv; i++ ) {
            jInvEl[ i ] = (*env)->GetObjectArrayElement( env, jInv, i );
            if ( ! jniastCheckNotNull( env,
                      (jobject) 
                      ( inv[ i ] = (const char *) 
                                   jniastGetUTF( env, jInvEl[ i ] ) ) ) ) {
               for ( ; i < ninv; i++ ) {
                  inv[ i ] = NULL;
               }
               break;
            }
         }
      }

      /* Call the AST routine to do the work. */
      ASTCALL(
         pointer.MathMap = astMathMap( (int) nin, (int) nout, nfwd, 
                                       fwd, ninv, inv, "" );
      )

      /* Release resources. */
      for ( i = 0; i < nfwd; i++ ) {
         jniastReleaseUTF( env, jFwdEl[ i ], fwd[ i ] );
      }
      for ( i = 0; i < ninv; i++ ) {
         jniastReleaseUTF( env, jInvEl[ i ], inv[ i ] );
      }
      free( fwd );
      free( inv );
      free( jFwdEl );
      free( jInvEl );
   
      /* Set the AstObject pointer to the new AST object. */
      jniastInitObject( env, this, pointer );
   }
}
/* $Id$ */
