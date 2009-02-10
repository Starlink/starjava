/*
*+
*  Name:
*     CmpMap.c

*  Purpose:
*     JNI implementations of native methods of CmpMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     27-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_CmpMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_CmpMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jMap1,        /* First Mapping object */
   jobject jMap2,        /* Second Mapping object */
   jboolean series       /* In-series flag */
) {
   AstPointer pointer;
   AstPointer map1Pointer;
   AstPointer map2Pointer;

   if ( jniastCheckNotNull( env, jMap1 ) && 
        jniastCheckNotNull( env, jMap2 ) ) {
      map1Pointer = jniastGetPointerField( env, jMap1 );
      map2Pointer = jniastGetPointerField( env, jMap2 );

      THASTCALL( jniastList( 2, map1Pointer.AstObject, map2Pointer.AstObject ),
         pointer.CmpMap = astCmpMap( map1Pointer.Mapping, map2Pointer.Mapping,
                                     series == JNI_TRUE, "" );
      )
      jniastInitObject( env, this, pointer );
   }
}

/* $Id$ */
