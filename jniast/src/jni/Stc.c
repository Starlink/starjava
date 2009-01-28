/*
*+
*  Name:
*     Stc.c

*  Purpose:
*     JNI implementations of native methods of Stc class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     1-AUG-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"

JNIEXPORT jobject Java_uk_ac_starlink_ast_Stc_getStcCoordKeyMap(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint index            /* Index of coord keymap to return */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstKeyMap *keymap;

   THASTCALL( jniastList( 1, pointer.AstObject ),
      keymap = astGetStcCoord( pointer.Stc, (int) index );
   )
   return jniastMakeObject( env, (AstObject *) keymap );
}

JNIEXPORT jobject Java_uk_ac_starlink_ast_Stc_getStcRegion(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstRegion *region;
   THASTCALL( jniastList( 1, pointer.AstObject ),
      region = astGetStcRegion( pointer.Stc );
   )
   return jniastMakeObject( env, (AstObject *) region );
}

JNIEXPORT jint Java_uk_ac_starlink_ast_Stc_getStcNCoord(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   int ncoord;
   THASTCALL( jniastList( 1, pointer.AstObject ),
      ncoord = astGetStcNCoord( pointer.Stc );
   )
   return ncoord;
}

