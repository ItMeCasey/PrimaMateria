package com.itmecasey.primamateria.script;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * Gives ModJS scripts a global {@code PrimaMateria} (the {@link PrimaMateriaApi}). ModJS is optional, so everything here goes
 * through reflection: ModJS offers {@code ExtraBindings.register(BindingProvider)} and the provider's
 * {@code install(type, cx, scope)} adds globals to every script scope. Without ModJS nothing is loaded and nothing fails. Client
 * scripts get no global.
 *
 * <p>{@link #install()} is what {@code PrimaMateria.init} calls by name. It also sets up the other optional script side (the game
 * state lookup and the KubeJS helper script), because both belong to "scripting" and neither needs its own entry point.
 */
public final class ModJsBridge {
    private static final Logger LOG = Logger.getLogger("primamateria");

    private ModJsBridge() {
    }

    /** Called once at startup: state lookup, KubeJS helper, ModJS global. Never throws. */
    public static void install() {
        callOptional("com.itmecasey.primamateria.compat.GameAccess", "install");
        if (com.itmecasey.primamateria.Dev.ENABLED) {
            callOptional("com.itmecasey.primamateria.dev.DemoRecipes", "install");
        }
        try {
            KubeJsBridge.install();
        } catch (RuntimeException | LinkageError e) {
            LOG.warning("Prima Materia: the KubeJS helper could not be set up: " + e);
        }
        install(PrimaMateriaApi.INSTANCE);
    }

    private static void callOptional(String className, String method) {
        try {
            Class.forName(className).getMethod(method).invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // not part of this build (unit tests): fine
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warning("Prima Materia: " + className + "." + method + " failed: " + e);
        }
    }

    /** Installs the binding when ModJS is on the classpath; returns whether it did. */
    public static boolean install(PrimaMateriaApi api) {
        try {
            Class<?> provider = Class.forName("com.itmecasey.modjs.script.BindingProvider");
            Class<?> extra = Class.forName("com.itmecasey.modjs.script.ExtraBindings");
            Class<?> context = Class.forName("org.mozilla.javascript.Context");
            Class<?> scriptable = Class.forName("org.mozilla.javascript.Scriptable");
            Class<?> scriptableObject = Class.forName("org.mozilla.javascript.ScriptableObject");
            Method javaToJs = context.getMethod("javaToJS", Object.class, scriptable);
            Method put = scriptableObject.getMethod("putProperty", scriptable, String.class, Object.class);
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("install") && args != null && args.length == 3) {
                    if (!"CLIENT".equals(String.valueOf(args[0]))) {
                        put.invoke(null, args[2], "PrimaMateria", javaToJs.invoke(null, api, args[2]));
                    }
                    return null;
                }
                if (method.getName().equals("toString")) {
                    return "PrimaMateria binding";
                }
                if (method.getName().equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
                if (method.getName().equals("equals")) {
                    return proxy == args[0];
                }
                return null;
            };
            Object binding = Proxy.newProxyInstance(provider.getClassLoader(), new Class<?>[] {provider}, handler);
            extra.getMethod("register", provider).invoke(null, binding);
            LOG.info("Prima Materia: ModJS scripts can use the PrimaMateria global");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warning("Prima Materia: could not add the PrimaMateria global to ModJS: " + e);
            return false;
        }
    }
}
