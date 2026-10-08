package com.github.matheuscruzsouza.nanospring.openapi;

import com.github.matheuscruzsouza.nanospring.annotation.PathVariable;
import com.github.matheuscruzsouza.nanospring.annotation.RequestBody;
import com.github.matheuscruzsouza.nanospring.annotation.RequestHeader;
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam;
import com.github.matheuscruzsouza.nanospring.annotation.ResponseStatus;
import com.github.matheuscruzsouza.nanospring.handler.ServerIndexHandler;
import com.github.matheuscruzsouza.nanospring.openapi.annotation.ApiResponse;
import com.github.matheuscruzsouza.nanospring.openapi.annotation.ApiResponses;
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Operation;
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Parameter;
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Tag;
import com.github.matheuscruzsouza.nanospring.server.Environment;
import com.github.matheuscruzsouza.nanospring.validation.Email;
import com.github.matheuscruzsouza.nanospring.validation.Max;
import com.github.matheuscruzsouza.nanospring.validation.Min;
import com.github.matheuscruzsouza.nanospring.validation.NotBlank;
import com.github.matheuscruzsouza.nanospring.validation.NotEmpty;
import com.github.matheuscruzsouza.nanospring.validation.NotNull;
import com.github.matheuscruzsouza.nanospring.validation.Size;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Gerador dinâmico de especificação OpenAPI 3.0.1 a partir das rotas e anotações do Nano-Spring.
 */
public class OpenApiGenerator {

    public static Map<String, Object> generateSpec() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("openapi", "3.0.1");

        // Info
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", Environment.getProperty("nano.swagger.title", "Nano-Spring REST API"));
        info.put("version", Environment.getProperty("nano.swagger.version", "1.0.0"));
        info.put("description", Environment.getProperty("nano.swagger.description", "Embedded REST API documentation powered by Nano-Spring"));
        root.put("info", info);

        // Servers
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("url", Environment.getProperty("nano.swagger.server-url", "/"));
        server.put("description", "Default server");
        servers.add(server);
        root.put("servers", servers);

        // Components & Schemas
        Map<String, Object> components = new LinkedHashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();
        components.put("schemas", schemas);

        // Security configuration
        boolean securityEnabled = "true".equalsIgnoreCase(Environment.getProperty("nano.swagger.security.enabled", "false"))
                || Environment.getProperty("nano.swagger.security.type") != null;

        if (securityEnabled) {
            String secType = Environment.getProperty("nano.swagger.security.type", "bearer").toLowerCase();
            String secName = Environment.getProperty("nano.swagger.security.name", "Authorization");
            String secScheme = Environment.getProperty("nano.swagger.security.scheme", "bearer");
            String bearerFormat = Environment.getProperty("nano.swagger.security.bearer-format", "JWT");

            Map<String, Object> securitySchemes = new LinkedHashMap<>();
            Map<String, Object> schemeDef = new LinkedHashMap<>();

            String schemeKey;
            if ("apikey".equals(secType) || "api-key".equals(secType)) {
                schemeKey = "apiKeyAuth";
                schemeDef.put("type", "apiKey");
                schemeDef.put("name", secName);
                schemeDef.put("in", Environment.getProperty("nano.swagger.security.in", "header"));
            } else if ("basic".equals(secType)) {
                schemeKey = "basicAuth";
                schemeDef.put("type", "http");
                schemeDef.put("scheme", "basic");
            } else {
                schemeKey = "bearerAuth";
                schemeDef.put("type", "http");
                schemeDef.put("scheme", secScheme);
                if (bearerFormat != null && !bearerFormat.isEmpty()) {
                    schemeDef.put("bearerFormat", bearerFormat);
                }
            }

            securitySchemes.put(schemeKey, schemeDef);
            components.put("securitySchemes", securitySchemes);

            List<Map<String, Object>> security = new ArrayList<>();
            Map<String, Object> secReq = new LinkedHashMap<>();
            secReq.put(schemeKey, Collections.emptyList());
            security.add(secReq);
            root.put("security", security);
        }

        root.put("components", components);

        // Paths
        Map<String, Object> paths = new LinkedHashMap<>();
        root.put("paths", paths);

        Map<String, Map<NanoHTTPD.Method, Map<Object, Method>>> instances = ServerIndexHandler.getInstances();
        List<String> sortedPaths = new ArrayList<>(instances.keySet());
        Collections.sort(sortedPaths);

        for (String registeredPath : sortedPaths) {
            String openApiPath = registeredPath.replaceAll(":([a-zA-Z0-9_]+)", "{$1}");

            Map<NanoHTTPD.Method, Map<Object, Method>> methodMap = instances.get(registeredPath);
            if (methodMap == null || methodMap.isEmpty()) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(openApiPath, k -> new LinkedHashMap<String, Object>());

            List<NanoHTTPD.Method> sortedMethods = new ArrayList<>(methodMap.keySet());
            sortedMethods.sort((m1, m2) -> m1.name().compareTo(m2.name()));

            for (NanoHTTPD.Method httpMethod : sortedMethods) {
                String methodLower = httpMethod.name().toLowerCase();
                Map<Object, Method> controllerMap = methodMap.get(httpMethod);
                if (controllerMap == null || controllerMap.isEmpty()) continue;

                Map.Entry<Object, Method> entry = controllerMap.entrySet().iterator().next();
                Object controller = entry.getKey();
                Method method = entry.getValue();

                Map<String, Object> operation = new LinkedHashMap<>();

                // Tags
                List<String> tags = new ArrayList<>();
                Tag methodTag = method.getAnnotation(Tag.class);
                Tag classTag = controller != null ? controller.getClass().getAnnotation(Tag.class) : null;
                if (methodTag != null && !methodTag.name().isEmpty()) {
                    tags.add(methodTag.name());
                } else if (classTag != null && !classTag.name().isEmpty()) {
                    tags.add(classTag.name());
                } else if (controller != null) {
                    tags.add(controller.getClass().getSimpleName());
                }
                if (!tags.isEmpty()) {
                    operation.put("tags", tags);
                }

                // Summary & Description
                Operation op = method.getAnnotation(Operation.class);
                String summary = (op != null && !op.summary().isEmpty()) ? op.summary() : method.getName();
                operation.put("summary", summary);
                if (op != null && !op.description().isEmpty()) {
                    operation.put("description", op.description());
                }
                if ((op != null && op.deprecated()) || method.isAnnotationPresent(Deprecated.class)) {
                    operation.put("deprecated", true);
                }

                // Parameters
                List<Map<String, Object>> parameters = new ArrayList<>();
                Class<?>[] paramTypes = method.getParameterTypes();
                Annotation[][] paramAnnotations = method.getParameterAnnotations();

                for (int i = 0; i < paramTypes.length; i++) {
                    Class<?> pType = paramTypes[i];
                    Annotation[] pAnns = paramAnnotations[i];

                    PathVariable pv = null;
                    RequestParam rp = null;
                    RequestHeader rh = null;
                    RequestBody rb = null;
                    Parameter pDoc = null;

                    for (Annotation a : pAnns) {
                        if (a instanceof PathVariable) pv = (PathVariable) a;
                        else if (a instanceof RequestParam) rp = (RequestParam) a;
                        else if (a instanceof RequestHeader) rh = (RequestHeader) a;
                        else if (a instanceof RequestBody) rb = (RequestBody) a;
                        else if (a instanceof Parameter) pDoc = (Parameter) a;
                    }

                    if (pv != null) {
                        String pName = (pv.value() != null && !pv.value().isEmpty()) ? pv.value() : "param" + i;
                        Map<String, Object> pObj = buildParameterMap(pName, "path", true, pType, pDoc, schemas);
                        parameters.add(pObj);
                    } else if (rp != null) {
                        String pName = (rp.value() != null && !rp.value().isEmpty()) ? rp.value() : "param" + i;
                        Map<String, Object> pObj = buildParameterMap(pName, "query", false, pType, pDoc, schemas);
                        parameters.add(pObj);
                    } else if (rh != null) {
                        String pName = !rh.value().isEmpty() ? rh.value() : (!rh.name().isEmpty() ? rh.name() : "param" + i);
                        if (!pName.isEmpty() && pType != Map.class) {
                            Map<String, Object> pObj = buildParameterMap(pName, "header", rh.required(), pType, pDoc, schemas);
                            parameters.add(pObj);
                        }
                    } else if (rb != null) {
                        Map<String, Object> reqBody = new LinkedHashMap<>();
                        reqBody.put("required", true);
                        Map<String, Object> content = new LinkedHashMap<>();
                        Map<String, Object> jsonContent = new LinkedHashMap<>();
                        jsonContent.put("schema", registerAndGetSchema(pType, schemas));
                        content.put("application/json", jsonContent);
                        reqBody.put("content", content);
                        operation.put("requestBody", reqBody);
                    }
                }

                if (!parameters.isEmpty()) {
                    operation.put("parameters", parameters);
                }

                // Responses
                Map<String, Object> responses = new LinkedHashMap<>();
                List<ApiResponse> apiResponses = new ArrayList<>();
                if (method.isAnnotationPresent(ApiResponses.class)) {
                    Collections.addAll(apiResponses, method.getAnnotation(ApiResponses.class).value());
                }
                if (method.isAnnotationPresent(ApiResponse.class)) {
                    apiResponses.add(method.getAnnotation(ApiResponse.class));
                }

                if (!apiResponses.isEmpty()) {
                    for (ApiResponse ar : apiResponses) {
                        Map<String, Object> respObj = new LinkedHashMap<>();
                        respObj.put("description", !ar.description().isEmpty() ? ar.description() : "Response " + ar.responseCode());
                        if (ar.responseClass() != Void.class && ar.responseClass() != void.class) {
                            Map<String, Object> content = new LinkedHashMap<>();
                            Map<String, Object> jsonContent = new LinkedHashMap<>();
                            jsonContent.put("schema", registerAndGetSchema(ar.responseClass(), schemas));
                            content.put("application/json", jsonContent);
                            respObj.put("content", content);
                        }
                        responses.put(String.valueOf(ar.responseCode()), respObj);
                    }
                } else {
                    int statusCode = 200;
                    String reason = "OK";
                    if (method.isAnnotationPresent(ResponseStatus.class)) {
                        ResponseStatus rs = method.getAnnotation(ResponseStatus.class);
                        statusCode = rs.code() > 0 ? rs.code() : rs.value().getRequestStatus();
                        if (!rs.reason().isEmpty()) {
                            reason = rs.reason();
                        } else {
                            reason = statusCode == 201 ? "Created" : (statusCode == 204 ? "No Content" : "OK");
                        }
                    }
                    Map<String, Object> respObj = new LinkedHashMap<>();
                    respObj.put("description", reason);

                    Class<?> returnType = method.getReturnType();
                    Type genericType = method.getGenericReturnType();
                    Class<?> unwrappedType = unwrapType(returnType, genericType);

                    if (unwrappedType != null && unwrappedType != void.class && unwrappedType != Void.class && !NanoHTTPD.Response.class.isAssignableFrom(unwrappedType)) {
                        Map<String, Object> content = new LinkedHashMap<>();
                        Map<String, Object> jsonContent = new LinkedHashMap<>();
                        jsonContent.put("schema", registerAndGetSchema(unwrappedType, schemas));
                        content.put("application/json", jsonContent);
                        respObj.put("content", content);
                    }
                    responses.put(String.valueOf(statusCode), respObj);
                }
                operation.put("responses", responses);

                pathItem.put(methodLower, operation);
            }
        }

        return root;
    }

    public static String generateSpecJson() {
        return generateSpecJson(false);
    }

    public static String generateSpecJson(boolean pretty) {
        Map<String, Object> spec = generateSpec();
        Gson gson = pretty ? new GsonBuilder().setPrettyPrinting().create() : new Gson();
        return gson.toJson(spec);
    }

    private static Map<String, Object> buildParameterMap(String name, String in, boolean required, Class<?> type, Parameter pDoc, Map<String, Object> schemas) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("name", name);
        param.put("in", in);
        param.put("required", required);
        param.put("schema", registerAndGetSchema(type, schemas));
        if (pDoc != null) {
            if (!pDoc.description().isEmpty()) param.put("description", pDoc.description());
            if (!pDoc.example().isEmpty()) param.put("example", pDoc.example());
            if (pDoc.required()) param.put("required", true);
        }
        return param;
    }

    private static Class<?> unwrapType(Class<?> rawType, Type genericType) {
        if (rawType == null || rawType == void.class || rawType == Void.class) return null;
        if (rawType.getName().equals("com.github.matheuscruzsouza.nanospring.http.ResponseEntity") ||
            rawType.getName().equals("java.util.concurrent.CompletableFuture")) {
            if (genericType instanceof ParameterizedType) {
                Type[] args = ((ParameterizedType) genericType).getActualTypeArguments();
                if (args != null && args.length > 0) {
                    if (args[0] instanceof Class) {
                        return unwrapType((Class<?>) args[0], args[0]);
                    } else if (args[0] instanceof ParameterizedType) {
                        Type raw = ((ParameterizedType) args[0]).getRawType();
                        if (raw instanceof Class) {
                            return (Class<?>) raw;
                        }
                    }
                }
            }
        }
        return rawType;
    }

    public static Map<String, Object> registerAndGetSchema(Class<?> type, Map<String, Object> schemas) {
        if (type == null || type == void.class || type == Void.class) {
            return Collections.emptyMap();
        }

        if (type == String.class) {
            return Map.of("type", "string");
        } else if (type == int.class || type == Integer.class || type == short.class || type == Short.class || type == byte.class || type == Byte.class) {
            return Map.of("type", "integer", "format", "int32");
        } else if (type == long.class || type == Long.class) {
            return Map.of("type", "integer", "format", "int64");
        } else if (type == float.class || type == Float.class) {
            return Map.of("type", "number", "format", "float");
        } else if (type == double.class || type == Double.class || Number.class.isAssignableFrom(type)) {
            return Map.of("type", "number", "format", "double");
        } else if (type == boolean.class || type == Boolean.class) {
            return Map.of("type", "boolean");
        } else if (java.util.Date.class.isAssignableFrom(type) || type.getName().startsWith("java.time.")) {
            return Map.of("type", "string", "format", "date-time");
        } else if (type.isArray()) {
            Map<String, Object> arr = new LinkedHashMap<>();
            arr.put("type", "array");
            arr.put("items", registerAndGetSchema(type.getComponentType(), schemas));
            return arr;
        } else if (Collection.class.isAssignableFrom(type)) {
            Map<String, Object> arr = new LinkedHashMap<>();
            arr.put("type", "array");
            arr.put("items", Map.of("type", "object"));
            return arr;
        } else if (Map.class.isAssignableFrom(type)) {
            return Map.of("type", "object");
        }

        // Custom Object / DTO
        String schemaName = type.getSimpleName();
        if (schemas != null) {
            if (!schemas.containsKey(schemaName)) {
                Map<String, Object> schemaObj = new LinkedHashMap<>();
                schemas.put(schemaName, schemaObj);
                schemaObj.put("type", "object");
                Map<String, Object> properties = new LinkedHashMap<>();
                schemaObj.put("properties", properties);
                List<String> requiredFields = new ArrayList<>();

                for (Field field : type.getDeclaredFields()) {
                    int mod = field.getModifiers();
                    if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) continue;

                    String fName = field.getName();
                    if (field.isAnnotationPresent(NotNull.class) ||
                        field.isAnnotationPresent(NotBlank.class) ||
                        field.isAnnotationPresent(NotEmpty.class)) {
                        requiredFields.add(fName);
                    }

                    Map<String, Object> fieldSchema = new LinkedHashMap<>(registerAndGetSchema(field.getType(), schemas));

                    if (field.isAnnotationPresent(Size.class)) {
                        Size sz = field.getAnnotation(Size.class);
                        if (sz.min() > 0) fieldSchema.put("minLength", sz.min());
                        if (sz.max() < Integer.MAX_VALUE) fieldSchema.put("maxLength", sz.max());
                    }
                    if (field.isAnnotationPresent(Min.class)) {
                        fieldSchema.put("minimum", field.getAnnotation(Min.class).value());
                    }
                    if (field.isAnnotationPresent(Max.class)) {
                        fieldSchema.put("maximum", field.getAnnotation(Max.class).value());
                    }
                    if (field.isAnnotationPresent(Email.class)) {
                        fieldSchema.put("format", "email");
                    }
                    if (field.isAnnotationPresent(com.github.matheuscruzsouza.nanospring.validation.Pattern.class)) {
                        fieldSchema.put("pattern", field.getAnnotation(com.github.matheuscruzsouza.nanospring.validation.Pattern.class).regexp());
                    }
                    if (field.isAnnotationPresent(Parameter.class)) {
                        Parameter p = field.getAnnotation(Parameter.class);
                        if (!p.description().isEmpty()) fieldSchema.put("description", p.description());
                        if (!p.example().isEmpty()) fieldSchema.put("example", p.example());
                    }
                    properties.put(fName, fieldSchema);
                }

                if (!requiredFields.isEmpty()) {
                    schemaObj.put("required", requiredFields);
                }
            }
            return Map.of("$ref", "#/components/schemas/" + schemaName);
        }

        return Map.of("type", "object");
    }
}
