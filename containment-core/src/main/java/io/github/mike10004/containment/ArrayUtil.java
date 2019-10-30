package io.github.mike10004.containment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

class ArrayUtil {

    private ArrayUtil() {}

    public static String[] prepend(String[] array, String item) {
        return Iterables.toArray(Lists.asList(item, array), String.class);
    }

    public static String[] nullToEmpty(String[] array) {
        return array == null ? new String[0] : array;
    }
}
