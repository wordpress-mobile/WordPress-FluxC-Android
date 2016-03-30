package org.wordpress.android.stores.processor;

public class CodeGenerationUtils {

    public static String getActionBuilderMethodName(AnnotatedAction annotatedAction) {
        return "generate" + CodeGenerationUtils.toCamelCase(annotatedAction.getActionName()) + "Action";
    }

    public static String toCamelCase(String string){
        String[] parts = string.split("_");
        String camelCaseString = "";
        for (String part : parts){
            camelCaseString = camelCaseString + capitalize(part);
        }
        return camelCaseString;
    }

    public static String capitalize(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }
}
