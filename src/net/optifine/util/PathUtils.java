package net.optifine.util;

public class PathUtils {
    public static boolean isRelative(String path) {
        return path == null ? false : path.startsWith("./");
    }

    public static String resolveRelative(String path, String basePath) {
        if (path != null && basePath != null) {
            if (path.startsWith("./")) {
                String s = path.substring(2);
                if (basePath.endsWith("/")) {
                    path = basePath + s;
                } else {
                    path = basePath + "/" + s;
                }
            }

            return path;
        } else {
            return path;
        }
    }

    public static String removeLast(String path) {
        if (path == null) {
            return path;
        } else {
            int i = path.lastIndexOf(47);
            return i >= 0 ? path.substring(0, i) : path;
        }
    }
}