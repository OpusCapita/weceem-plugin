package org.weceem.event

public enum WeceemEvent {
     /* boolean */ contentShouldBeCreated /* (WcmContent parentNodeOrNull) */, 
     /* void */ contentDidGetCreated /* () */,

     /* boolean */ contentShouldBeDeleted /* () */,
     /* void */ contentWillBeDeleted,
     /* void */ contentDidGetDeleted,

     /* void */ contentDidChangeTitle /* (String previousTitle) */,
     /* void */ contentDidGetUpdated /* () */,

     /* boolean */ contentShouldMove /* (WcmContent targetParent) */,
     /* void */ contentDidMove /* () */,
     
     /* boolean */ contentShouldAcceptChildren /* () */,
     /* boolean */ contentShouldAcceptChild /* (WcmContent possibleChild) */
}