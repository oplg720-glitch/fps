package net.opl.fpsstabilizer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reflective adapter to keep the option bridge tolerant of small 26.x option API changes. */
public final class OptionAccess {
    private static final String[][] OPTION_FIELDS = {
        {"renderDistance"},
        {"simulationDistance"},
        {"graphicsMode"},
        {"particles"},
        {"renderClouds", "cloudStatus", "clouds"},
        {"entityShadows"},
        {"entityDistanceScaling"},
        {"biomeBlendRadius"},
        {"mipmapLevels"},
        {"ao", "ambientOcclusion"},
        {"framerateLimit", "maxFps"},
        {"enableVsync", "vsync"}
    };

    private OptionAccess() {}

    public static Map<String, Object> capture(Object client) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (String[] aliases : OPTION_FIELDS) {
            String key = aliases[0];
            Object value = get(client, aliases);
            if (value != Missing.VALUE) snapshot.put(key, value);
        }
        return snapshot;
    }

    /** Applies low-risk client options without saving them permanently. */
    public static int applyAutomaticBaseline(Object client, int renderDistance, int simulationDistance) {
        int changed = 0;
        changed += set(client, renderDistance, "renderDistance");
        changed += set(client, simulationDistance, "simulationDistance");
        changed += set(client, "FAST", "graphicsMode");
        changed += set(client, "DECREASED", "particles");
        changed += set(client, "OFF", "renderClouds", "cloudStatus", "clouds");
        changed += set(client, false, "entityShadows");
        changed += set(client, 0.75D, "entityDistanceScaling");
        changed += set(client, 1, "biomeBlendRadius");
        changed += set(client, false, "ao", "ambientOcclusion");
        return changed;
    }

    public static boolean restore(Object client, Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return false;
        boolean restored = false;
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            restored |= set(client, entry.getValue(), aliasesFor(entry.getKey())) > 0;
        }
        save(client);
        return restored;
    }

    public static int getInt(Object client, String key, int fallback) {
        Object value = get(client, new String[]{key});
        return value instanceof Number number ? number.intValue() : fallback;
    }

    public static boolean getBoolean(Object client, String key, boolean fallback) {
        Object value = get(client, new String[]{key});
        return value instanceof Boolean bool ? bool : fallback;
    }

    public static int getWindowRefreshRate(Object client) {
        Object window = getFieldValue(client, "window");
        if (window == null || window == Missing.VALUE) {
            window = invokeNoArgResult(client, "getWindow");
        }
        Object refreshRate = invokeNoArgResult(window, "getRefreshRate");
        return refreshRate instanceof Number number ? number.intValue() : 0;
    }

    public static void setRenderDistance(Object client, int value) {
        set(client, value, "renderDistance");
    }

    public static void setSimulationDistance(Object client, int value) {
        set(client, value, "simulationDistance");
    }

    private static String[] aliasesFor(String key) {
        for (String[] aliases : OPTION_FIELDS) {
            if (aliases[0].equals(key)) return aliases;
        }
        return new String[]{key};
    }

    public static boolean isInWorld(Object client) {
        Object player = getFieldValue(client, "player");
        Object level = getFieldValue(client, "level");
        return player != null && player != Missing.VALUE && level != null && level != Missing.VALUE;
    }

    public static void save(Object client) {
        Object options = getOptions(client);
        if (options == null) return;
        invokeNoArg(options, "save");
    }

    private static int set(Object client, Object requested, String... aliases) {
        Object options = getOptions(client);
        if (options == null) return 0;
        Field field = findField(options.getClass(), aliases);
        if (field == null) return 0;
        try {
            field.setAccessible(true);
            Object raw = field.get(options);
            Method getter = findMethod(raw, "get", 0);
            if (getter != null) {
                Object current = getter.invoke(raw);
                Object converted = convert(requested, current, getter.getReturnType());
                Method setter = findMethod(raw, "set", 1);
                if (setter == null) return 0;
                setter.invoke(raw, converted);
            } else {
                Object current = raw;
                Object converted = convert(requested, current, field.getType());
                if (Modifier.isFinal(field.getModifiers())) return 0;
                field.set(options, converted);
            }
            return 1;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static Object get(Object client, String... aliases) {
        Object options = getOptions(client);
        if (options == null) return Missing.VALUE;
        Field field = findField(options.getClass(), aliases);
        if (field == null) return Missing.VALUE;
        try {
            field.setAccessible(true);
            Object raw = field.get(options);
            Method getter = findMethod(raw, "get", 0);
            return getter == null ? raw : getter.invoke(raw);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Missing.VALUE;
        }
    }

    private static Object getOptions(Object client) {
        if (client == null) return null;
        Object options = getFieldValue(client, "options");
        if (options != null && options != Missing.VALUE) return options;
        Method method = findMethod(client, "options", 0);
        if (method != null) {
            try {
                return method.invoke(client);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object getFieldValue(Object target, String name) {
        if (target == null) return Missing.VALUE;
        Field field = findField(target.getClass(), name);
        if (field == null) return Missing.VALUE;
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Missing.VALUE;
        }
    }

    private static Field findField(Class<?> type, String... names) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (String name : names) {
                try {
                    return current.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                    // Continue through aliases and superclasses.
                }
            }
        }
        return null;
    }

    private static Method findMethod(Object target, String name, int parameterCount) {
        if (target == null) return null;
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    try {
                        method.setAccessible(true);
                        return method;
                    } catch (RuntimeException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Object requested, Object current, Class<?> declaredType) {
        if (current instanceof Enum<?> enumValue) {
            String wanted = String.valueOf(requested).toUpperCase();
            Class<? extends Enum> enumClass = (Class<? extends Enum>) enumValue.getDeclaringClass();
            for (Enum constant : enumClass.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(wanted)) return constant;
            }
            return current;
        }
        if (current instanceof Boolean) return Boolean.parseBoolean(String.valueOf(requested));
        if (current instanceof Integer || declaredType == int.class || declaredType == Integer.class) {
            return requested instanceof Number n ? n.intValue() : numericSetting(String.valueOf(requested));
        }
        if (current instanceof Long || declaredType == long.class || declaredType == Long.class) {
            return requested instanceof Number n ? n.longValue() : numericSetting(String.valueOf(requested));
        }
        if (current instanceof Float || declaredType == float.class || declaredType == Float.class) {
            return requested instanceof Number n ? n.floatValue() : (float) numericSetting(String.valueOf(requested));
        }
        if (current instanceof Double || declaredType == double.class || declaredType == Double.class) {
            return requested instanceof Number n ? n.doubleValue() : (double) numericSetting(String.valueOf(requested));
        }
        if (current instanceof String) return String.valueOf(requested).toLowerCase();
        if (declaredType == boolean.class || declaredType == Boolean.class) {
            return Boolean.parseBoolean(String.valueOf(requested));
        }
        return requested;
    }

    private static int numericSetting(String name) {
        return switch (name.toUpperCase()) {
            case "FAST", "ALL" -> 0;
            case "FANCY", "DECREASED" -> 1;
            case "FABULOUS", "MINIMAL" -> 2;
            default -> Integer.parseInt(name);
        };
    }

    private static Object invokeNoArgResult(Object target, String name) {
        Method method = findMethod(target, name, 0);
        if (method == null) return Missing.VALUE;
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Missing.VALUE;
        }
    }

    private static void invokeNoArg(Object target, String name) {
        invokeNoArgResult(target, name);
    }

    private enum Missing { VALUE }
}
