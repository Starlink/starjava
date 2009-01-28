/*
*+
*  Name:
*     FluxFrame.c

*  Purpose:
*     JNI implementations of native methods of FluxFrame class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     13-DEC-2004 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_FluxFrame.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FluxFrame_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdouble specval,      /* Spectral value */
   jobject jSpecfrm      /* SpecFrame */
) {
   AstPointer pointer;
   AstPointer specfrmPointer;

   if ( jSpecfrm == NULL ) {
      specfrmPointer.SpecFrame = NULL;
   }
   else {
      specfrmPointer = jniastGetPointerField( env, jSpecfrm );
   }

   THASTCALL( jniastList( 1, specfrmPointer.AstObject ),
      pointer.FluxFrame = astFluxFrame( (double) specval,
                                        specfrmPointer.SpecFrame, "" );
   )
   jniastInitObject( env, this, pointer );
}
