package org.wordpress.android.stores.processor;

import org.wordpress.android.stores.annotations.Action;

import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

public class AnnotatedAction {
    private String mActionName;
    private TypeMirror mPayloadType;

    public AnnotatedAction(Element typeElement, Action actionAnnotation) {
        mActionName = typeElement.getSimpleName().toString();
        try {
            actionAnnotation.payloadType();
        } catch (MirroredTypeException e) {
            mPayloadType = e.getTypeMirror();
        }
    }

    public String getActionName() {
        return mActionName;
    }

    public TypeMirror getPayloadType() {
        return mPayloadType;
    }
}
