/*
*+
*  Name:
*     TranMap.c

*  Purpose:
*     JNI implementations of native methods of TranMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     12-FEB-2004 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_TranMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_TranMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jMap1,        /* First mapping. */
   jobject jMap2         /* Second mapping. */
) {
   AstPointer pointer;
   AstPointer map1Pointer;
   AstPointer map2Pointer;

   if ( jniastCheckNotNull( env, jMap1 ) &&
        jniastCheckNotNull( env, jMap2 ) ) {
       map1Pointer = jniastGetPointerField( env, jMap1 );
       map2Pointer = jniastGetPointerField( env, jMap2 );

       THASTCALL( jniastList( 2, map1Pointer.AstObject, map2Pointer.AstObject ),
          pointer.TranMap = astTranMap( map1Pointer.Mapping, 
                                        map2Pointer.Mapping, "" );
       )
       jniastInitObject( env, this, pointer );
   }
}
