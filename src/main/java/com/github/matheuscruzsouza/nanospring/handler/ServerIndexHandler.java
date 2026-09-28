package com.github.matheuscruzsouza.nanospring.handler;

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PostMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PutMethod;
import com.github.matheuscruzsouza.nanospring.annotation.DeleteMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable;
import com.github.matheuscruzsouza.nanospring.annotation.RequestBody;
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam;
import com.github.matheuscruzsouza.nanospring.ui.ModelAndView;
import com.github.matheuscruzsouza.nanospring.server.Server;
import com.google.gson.Gson;
import com.samskivert.mustache.Mustache;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class ServerIndexHandler {

    // path -> HTTP Method -> (Controller Instance -> Method)
    private final static Map<String, Map<NanoHTTPD.Method, Map<Object, Method>>> INSTANCES = new ConcurrentHashMap<>();
    private final static Map<Class<? extends Throwable>, Map.Entry<Object, Method>> EXCEPTION_HANDLERS = new ConcurrentHashMap<>();

    public static void registerExceptionHandler(Class<? extends Throwable> exceptionClass, Object instance, Method method) {
        EXCEPTION_HANDLERS.put(exceptionClass, new java.util.AbstractMap.SimpleEntry<>(instance, method));
    }

    private static java.util.List<HandlerInterceptor> interceptors = new java.util.ArrayList<>();

    public static void setInterceptors(java.util.List<HandlerInterceptor> list) {
        interceptors = list;
    }

    public static java.util.List<HandlerInterceptor> getInterceptors() {
        return interceptors;
    }

    private final static String DEFAULT_MIMETYPE = "application/json";

    public static NanoHTTPD.Response process(NanoHTTPD.IHTTPSession session) {
        NanoHTTPD.Method httpMethod = session.getMethod();
        String uri = session.getUri();
        System.out.println("REQUEST URI: " + uri);

        for (Map.Entry<String, Map<NanoHTTPD.Method, Map<Object, Method>>> pathEntry : INSTANCES.entrySet()) {
            String registeredPath = pathEntry.getKey();
            
            // Convert /index/profile/:id to regex /index/profile/([^/]+)
            String regex = registeredPath.replaceAll(":[a-zA-Z0-9_]+", "([^/]+)");
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("^" + regex + "$");
            java.util.regex.Matcher m = p.matcher(uri);

            if (m.matches()) {
                Map<NanoHTTPD.Method, Map<Object, Method>> methodsMap = pathEntry.getValue();
                Map<Object, Method> pathMap = methodsMap.get(httpMethod);
                

                if (pathMap == null) {
                    return newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, "text/plain", "Method not allowed");
                }

                Optional<Map.Entry<Object, Method>> entry = pathMap.entrySet().stream().findFirst();
                if (entry.isEmpty()) return null;

                Object instance = entry.get().getKey();
                Method method = entry.get().getValue();

                // Extract path variables explicitly by group index
                Map<String, String> pathVariables = new HashMap<>();
                System.out.println("Registered path = " + registeredPath);
                java.util.regex.Matcher varMatcher = java.util.regex.Pattern.compile(":([a-zA-Z0-9_]+)").matcher(registeredPath);
                int groupIdx = 1;
                while (varMatcher.find()) {
                    System.out.println("Encontrou var: " + varMatcher.group(1) + " = " + m.group(groupIdx));
                    pathVariables.put(varMatcher.group(1), m.group(groupIdx++));
                }

                return executeMethod(instance, method, httpMethod, session, pathVariables);
            }
        }
        return null;
    }

    private static NanoHTTPD.Response executeMethod(Object instance, Method method, NanoHTTPD.Method httpMethod, NanoHTTPD.IHTTPSession session, Map<String, String> pathVariables) {
        Gson gson = new Gson();
        try {
            String mimeType = DEFAULT_MIMETYPE;
            if (httpMethod == NanoHTTPD.Method.GET && method.isAnnotationPresent(GetMethod.class)) {
                mimeType = method.getAnnotation(GetMethod.class).mimeType();
            } else if (httpMethod == NanoHTTPD.Method.POST && method.isAnnotationPresent(PostMethod.class)) {
                mimeType = method.getAnnotation(PostMethod.class).mimeType();
            } else if (httpMethod == NanoHTTPD.Method.PUT && method.isAnnotationPresent(PutMethod.class)) {
                mimeType = method.getAnnotation(PutMethod.class).mimeType();
            } else if (httpMethod == NanoHTTPD.Method.DELETE && method.isAnnotationPresent(DeleteMethod.class)) {
                mimeType = method.getAnnotation(DeleteMethod.class).mimeType();
            }

            Map<String, String> files = new HashMap<>();
            if (httpMethod == NanoHTTPD.Method.POST || httpMethod == NanoHTTPD.Method.PUT) {
                try {
                    session.parseBody(files);
                android.util.Log.d("UPLOAD_DEBUG", "Files map: " + files.toString());
                android.util.Log.d("UPLOAD_DEBUG", "Parms map: " + session.getParms().toString());
                } catch (IOException | NanoHTTPD.ResponseException e) {
                    e.printStackTrace();
                }
            }

            Object[] convertedArgs = new Object[method.getParameterCount()];
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            Class<?>[] parameterTypes = method.getParameterTypes();

            for (int i = 0; i < method.getParameterCount(); i++) {
                Annotation[] annotations = parameterAnnotations[i];
                Class<?> targetType = parameterTypes[i];
                boolean matched = false;

                for (Annotation ann : annotations) {
                    if (ann instanceof PathVariable) {
                        PathVariable pv = (PathVariable) ann;
                        String value = pathVariables.get(pv.value());
                        convertedArgs[i] = convertStringToType(value, targetType);
                        matched = true;
                        break;
                    } else if (ann instanceof RequestParam) {
                        RequestParam rp = (RequestParam) ann;
                        Map<String, String> queryParams = session.getParms();
                        String value = queryParams.get(rp.value());
                        convertedArgs[i] = convertStringToType(value, targetType);
                        matched = true;
                        break;
                    } else if (ann instanceof com.github.matheuscruzsouza.nanospring.annotation.UploadedFile) {
                        com.github.matheuscruzsouza.nanospring.annotation.UploadedFile uf = (com.github.matheuscruzsouza.nanospring.annotation.UploadedFile) ann;
                        String tempFilePath = files.get(uf.value());
                        if (tempFilePath != null) {
                            if (targetType == java.io.File.class) {
                                convertedArgs[i] = new java.io.File(tempFilePath);
                            } else {
                                convertedArgs[i] = tempFilePath;
                            }
                        } else {
                            convertedArgs[i] = null;
                        }
                        matched = true;
                        break;
                    } else if (ann instanceof RequestBody) {
                        String postData = files.get("postData");
                        if (postData != null) {
                            convertedArgs[i] = gson.fromJson(postData, targetType);
                        } else {
                            convertedArgs[i] = null;
                        }
                        matched = true;
                        break;
                    }
                }
                
                if (!matched) {
                    convertedArgs[i] = getPrimitiveDefault(targetType);
                }
            }

            Object result = null;
            try {
                result = method.invoke(instance, convertedArgs);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                Map.Entry<Object, Method> handler = findExceptionHandler(cause.getClass());
                if (handler != null) {
                    try {
                        result = handler.getValue().invoke(handler.getKey(), cause);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "Erro no ExceptionHandler: " + e.getMessage());
                    }
                } else {
                    throw ite;
                }
            }
            
            if (result instanceof java.io.File) {
                java.io.File f = (java.io.File) result;
                try {
                    NanoHTTPD.Response r = NanoHTTPD.newChunkedResponse(Status.OK, mimeType, new java.io.FileInputStream(f));
                    r.addHeader("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"");
                    return r;
                } catch (Exception e) {
                    return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "Erro lendo arquivo: " + e.getMessage());
                }
            }

            if (result instanceof com.github.matheuscruzsouza.nanospring.sse.SseEmitter) {
                com.github.matheuscruzsouza.nanospring.sse.SseEmitter emitter = (com.github.matheuscruzsouza.nanospring.sse.SseEmitter) result;
                NanoHTTPD.Response response = NanoHTTPD.newChunkedResponse(Status.OK, "text/event-stream", emitter.getInputStream());
                response.addHeader("Cache-Control", "no-cache");
                response.addHeader("Connection", "keep-alive");
                response.addHeader("Access-Control-Allow-Origin", "*");
                return response;
            }

            if (result instanceof ModelAndView) {
                ModelAndView mav = (ModelAndView) result;
                String templatePath = "templates/" + mav.getViewName() + ".html";
                try (Reader reader = new InputStreamReader(Server.getContext().getAssets().open(templatePath), "UTF-8")) {
                    String html = Mustache.compiler().compile(reader).execute(mav.getModel());
                    return newFixedLengthResponse(Status.OK, "text/html", html);
                } catch (Exception e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "Template não encontrado: " + templatePath);
                }
            }

            String response = "";
            if (result != null) {
                if (result instanceof String && !mimeType.equals("application/json")) {
                    response = (String) result;
                } else {
                    response = gson.toJson(result);
                }
            }

            return newFixedLengthResponse(Status.OK, mimeType, response);
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "Erro interno: " + e.getMessage());
        }
    }

    public static void register(NanoHTTPD.Method httpMethod, String path, Object klass, Method method) {
        INSTANCES.putIfAbsent(path, new ConcurrentHashMap<>());
        INSTANCES.get(path).put(httpMethod, Map.of(klass, method));
    }

    private static Map.Entry<Object, Method> findExceptionHandler(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) return null;
        if (EXCEPTION_HANDLERS.containsKey(clazz)) return EXCEPTION_HANDLERS.get(clazz);
        return findExceptionHandler(clazz.getSuperclass());
    }

    private static Object convertStringToType(String value, Class<?> targetType) {
        if (value == null) {
            return getPrimitiveDefault(targetType);
        }

        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(value);
        if (targetType == short.class || targetType == Short.class) return Short.parseShort(value);
        if (targetType == byte.class || targetType == Byte.class) return Byte.parseByte(value);
        if (targetType == char.class || targetType == Character.class) return value.isEmpty() ? '\0' : value.charAt(0);

        throw new IllegalArgumentException("Tipo não suportado para conversão automática: " + targetType.getName());
    }

    private static Object getPrimitiveDefault(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == boolean.class) return false;
        if (type == double.class) return 0.0d;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null; 
    }
}
