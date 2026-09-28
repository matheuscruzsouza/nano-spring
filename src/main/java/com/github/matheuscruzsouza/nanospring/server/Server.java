package com.github.matheuscruzsouza.nanospring.server;

import android.content.Context;
import android.util.Log;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.DeleteMethod;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PostMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PutMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.annotation.Service;
import com.github.matheuscruzsouza.nanospring.annotation.Interceptor;
import com.github.matheuscruzsouza.nanospring.handler.ServerIndexHandler;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexFile;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

public class Server extends RouterNanoHTTPD {
    private Context context;
    private static Context staticContext;
    private Map<Class<?>, Object> services = new HashMap<>();

    public static Context getContext() { return staticContext; }

    public Server(Context context, int port, String basePackage) {
        super(Integer.parseInt(Environment.getProperty("server.port", String.valueOf(port))));
        Environment.init(context);
        setNotFoundHandler(fi.iki.elonen.router.RouterNanoHTTPD.Error404UriHandler.class);
        this.context = context;
        staticContext = context;
        
        initializeServices(basePackage);
        processInterceptors(basePackage);
        processControllerAdvice(basePackage);
        processEndpoints(basePackage);

        try {
            start();
            Log.d("SERVER_OK", "SERVIDOR RODANDO ONLINE NA PORTA " + Environment.getProperty("server.port", String.valueOf(port)) + "!");
        } catch (IOException e) {
            Log.e("SERVER_FAIL", "ERRO AO INICIAR O SOCKET DO SERVIDOR", e);
            e.printStackTrace();
        }
    }

    @Override
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();

        for (com.github.matheuscruzsouza.nanospring.handler.HandlerInterceptor interceptor : ServerIndexHandler.getInterceptors()) {
            if (!interceptor.preHandle(session, uri)) {
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.UNAUTHORIZED, "text/plain", "Unauthorized by Interceptor");
            }
        }

        if (uri.startsWith("/static/")) {
            return serveStaticFile(uri);
        }

        NanoHTTPD.Response response = ServerIndexHandler.process(session);
        if (response != null) return response;
        return super.serve(session);
    }

    private NanoHTTPD.Response serveStaticFile(String uri) {
        try {
            String assetPath = uri.substring(1); 
            java.io.InputStream is = context.getAssets().open(assetPath);
            String mimeType = detectMimeType(uri);
            return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, mimeType, is);
        } catch (java.io.IOException e) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "File Not Found: " + uri);
        }
    }

    private String detectMimeType(String uri) {
        if (uri.endsWith(".css")) return "text/css";
        if (uri.endsWith(".js")) return "application/javascript";
        if (uri.endsWith(".png")) return "image/png";
        if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
        if (uri.endsWith(".svg")) return "image/svg+xml";
        if (uri.endsWith(".ico")) return "image/x-icon";
        if (uri.endsWith(".html")) return "text/html";
        return "application/octet-stream";
    }

    private List<Class<?>> getAnnotatedClasses(Context context, String packageName, Class<? extends Annotation> annotation) {
        List<Class<?>> annotatedClasses = new ArrayList<>();
        try {
            DexFile df = new DexFile(context.getPackageCodePath());
            for (Enumeration<String> iter = df.entries(); iter.hasMoreElements();) {
                String className = iter.nextElement();

                if (className.startsWith(packageName)) {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(annotation)) {
                        annotatedClasses.add(clazz);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return annotatedClasses;
    }

    private void initializeServices(String packageName) {
        List<Class<?>> serviceClasses = getAnnotatedClasses(this.context, packageName, Service.class);
        for (Class<?> klass : serviceClasses) {
            try {
                Constructor<?> ctor = klass.getConstructor();
                Object instance = ctor.newInstance();
                services.put(klass, instance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        for (Object serviceInstance : services.values()) {
            injectDependencies(serviceInstance);
        }
    }

    private String resolveExpression(String expr) {
        if (expr != null && expr.startsWith("${") && expr.endsWith("}")) {
            String inner = expr.substring(2, expr.length() - 1);
            String key = inner;
            String defaultValue = null;
            if (inner.contains(":")) {
                int colonIdx = inner.indexOf(":");
                key = inner.substring(0, colonIdx);
                defaultValue = inner.substring(colonIdx + 1);
            }
            String prop = Environment.getProperty(key);
            if (prop != null) return prop;
            return defaultValue;
        }
        return expr;
    }

    private Object convertStringToType(String value, Class<?> targetType) {
        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(value);
        return value;
    }

    private void injectDependencies(Object instance) {
        Class<?> clazz = instance.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Object dependency = services.get(field.getType());
                if (dependency != null) {
                    try {
                        field.setAccessible(true);
                        field.set(instance, dependency);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            } else if (field.isAnnotationPresent(com.github.matheuscruzsouza.nanospring.annotation.Value.class)) {
                String expression = field.getAnnotation(com.github.matheuscruzsouza.nanospring.annotation.Value.class).value();
                String resolvedValue = resolveExpression(expression);
                if (resolvedValue != null) {
                    try {
                        field.setAccessible(true);
                        Object converted = convertStringToType(resolvedValue, field.getType());
                        field.set(instance, converted);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void processControllerAdvice(String packageName) {
        List<Class<?>> adviceClasses = getAnnotatedClasses(this.context, packageName, com.github.matheuscruzsouza.nanospring.annotation.ControllerAdvice.class);
        for (Class<?> klass : adviceClasses) {
            try {
                java.lang.reflect.Constructor<?> ctor = klass.getConstructor();
                Object instance = ctor.newInstance();
                injectDependencies(instance);
                for (java.lang.reflect.Method m : klass.getMethods()) {
                    if (m.isAnnotationPresent(com.github.matheuscruzsouza.nanospring.annotation.ExceptionHandler.class)) {
                        Class<? extends Throwable> exceptionClass = m.getAnnotation(com.github.matheuscruzsouza.nanospring.annotation.ExceptionHandler.class).value();
                        ServerIndexHandler.registerExceptionHandler(exceptionClass, instance, m);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processInterceptors(String packageName) {
        List<Class<?>> interceptorClasses = getAnnotatedClasses(this.context, packageName, Interceptor.class);
        List<com.github.matheuscruzsouza.nanospring.handler.HandlerInterceptor> interceptors = new ArrayList<>();
        
        for (Class<?> klass : interceptorClasses) {
            try {
                if (com.github.matheuscruzsouza.nanospring.handler.HandlerInterceptor.class.isAssignableFrom(klass)) {
                    java.lang.reflect.Constructor<?> ctor = klass.getConstructor();
                    Object instance = ctor.newInstance();
                    injectDependencies(instance);
                    interceptors.add((com.github.matheuscruzsouza.nanospring.handler.HandlerInterceptor) instance);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        interceptors.sort((a, b) -> {
            com.github.matheuscruzsouza.nanospring.annotation.Order orderA = a.getClass().getAnnotation(com.github.matheuscruzsouza.nanospring.annotation.Order.class);
            com.github.matheuscruzsouza.nanospring.annotation.Order orderB = b.getClass().getAnnotation(com.github.matheuscruzsouza.nanospring.annotation.Order.class);
            int valA = orderA != null ? orderA.value() : 0;
            int valB = orderB != null ? orderB.value() : 0;
            return Integer.compare(valA, valB);
        });
        
        ServerIndexHandler.setInterceptors(interceptors);
    }

    private void processEndpoints(String packageName) {
        List<Class<?>> classes = getAnnotatedClasses(this.context, packageName, RestController.class);

        for (Class<?> klass : classes) {
            if (klass.isAnnotationPresent(RestController.class)) {
                String path = klass.getAnnotationsByType(RestController.class)[0].value();

                try {
                    Constructor<?> ctor = klass.getConstructor();
                    Object instance = ctor.newInstance();
                    
                    injectDependencies(instance);

                    for (java.lang.reflect.Method m : klass.getMethods()) {
                        if (m.isAnnotationPresent(GetMethod.class)) {
                            String methodPath = m.getDeclaredAnnotationsByType(GetMethod.class)[0].value();
                            ServerIndexHandler.register(NanoHTTPD.Method.GET, path + methodPath, instance, m);
                        } else if (m.isAnnotationPresent(PostMethod.class)) {
                            String methodPath = m.getDeclaredAnnotationsByType(PostMethod.class)[0].value();
                            ServerIndexHandler.register(NanoHTTPD.Method.POST, path + methodPath, instance, m);
                        } else if (m.isAnnotationPresent(PutMethod.class)) {
                            String methodPath = m.getDeclaredAnnotationsByType(PutMethod.class)[0].value();
                            ServerIndexHandler.register(NanoHTTPD.Method.PUT, path + methodPath, instance, m);
                        } else if (m.isAnnotationPresent(DeleteMethod.class)) {
                            String methodPath = m.getDeclaredAnnotationsByType(DeleteMethod.class)[0].value();
                            ServerIndexHandler.register(NanoHTTPD.Method.DELETE, path + methodPath, instance, m);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
