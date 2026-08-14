package me.qscbm.inlayx.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {
    public static Object getFieldValue(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    public static Method findMethod(Object target, String name, Class<?>... paramTypes) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(name, paramTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
