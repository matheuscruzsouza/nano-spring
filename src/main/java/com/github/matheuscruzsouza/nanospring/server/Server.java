package com.github.matheuscruzsouza.nanospring.server;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.DeleteMethod;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PostMethod;
import com.github.matheuscruzsouza.nanospring.annotation.PutMethod;
import com.github.matheuscruzsouza.nanospring.annotation.Repository;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.annotation.Service;
import com.github.matheuscruzsouza.nanospring.annotation.Interceptor;
import com.github.matheuscruzsouza.nanospring.actuator.ActuatorHealthHandler;
import com.github.matheuscruzsouza.nanospring.cors.CorsConfiguration;
import com.github.matheuscruzsouza.nanospring.database.SqliteMigrator;
import com.github.matheuscruzsouza.nanospring.discovery.NetworkWatcher;
import com.github.matheuscruzsouza.nanospring.discovery.NsdServiceManager;
import com.github.matheuscruzsouza.nanospring.handler.ServerIndexHandler;
import com.github.matheuscruzsouza.nanospring.openapi.SwaggerUiHandler;
import com.github.matheuscruzsouza.nanospring.security.SslConfiguration;
import com.github.matheuscruzsouza.nanospring.security.TokenBucketRateLimiter;

import java.io.IOException;
import javax.net.ssl.SSLServerSocketFactory;
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
    private NsdServiceManager nsdServiceManager = new NsdServiceManager();
    private CorsConfiguration corsConfig;
    private ThreadPoolAsyncRunner threadPoolAsyncRunner;
    private NetworkWatcher networkWatcher;
    private SslConfiguration sslConfig;
    private TokenBucketRateLimiter rateLimiter;

    public static Context getContext() { return staticContext; }

    public <T> void registerSingleton(Class<T> type, T instance) {
        this.services.put(type, instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        return (T) this.services.get(type);
    }

    public SQLiteDatabase getDatabase() {
        return getBean(SQLiteDatabase.class);
    }

    public CorsConfiguration getCorsConfiguration() {
        return corsConfig;
    }

    public void setCorsConfiguration(CorsConfiguration corsConfig) {
        this.corsConfig = corsConfig != null ? corsConfig : new CorsConfiguration();
    }

    public NsdServiceManager getNsdServiceManager() {
        return nsdServiceManager;
    }

    public ThreadPoolAsyncRunner getThreadPoolAsyncRunner() {
        return threadPoolAsyncRunner;
    }

    public NetworkWatcher getNetworkWatcher() {
        return networkWatcher;
    }

    public SslConfiguration getSslConfiguration() {
        return sslConfig;
    }

    public TokenBucketRateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public void enableNsd(String serviceName) {
        enableNsd(serviceName, true);
    }

    public void enableNsd(String serviceName, boolean enableHostResolution) {
        if (this.context == null) return;
        int activePort = getListeningPort();
        if (activePort <= 0) {
            activePort = Integer.parseInt(Environment.getProperty("server.port", "8080"));
        }
        String nsdType = Environment.getProperty("nano.nsd.type", NsdServiceManager.DEFAULT_SERVICE_TYPE);
        nsdServiceManager.register(this.context, serviceName, nsdType, activePort, enableHostResolution);
    }

    public void disableNsd() {
        if (nsdServiceManager != null) {
            nsdServiceManager.unregister();
        }
    }

    public Server(Context context, int port, String basePackage) {
        super(Integer.parseInt(Environment.getProperty("server.port", String.valueOf(port))));
        Environment.init(context);
        this.threadPoolAsyncRunner = ThreadPoolAsyncRunner.fromEnvironment();
        setAsyncRunner(this.threadPoolAsyncRunner);
        setNotFoundHandler(fi.iki.elonen.router.RouterNanoHTTPD.Error404UriHandler.class);
        this.context = context;
        staticContext = context;
        this.corsConfig = new CorsConfiguration(Environment.getProperties());
        this.services.put(CorsConfiguration.class, this.corsConfig);
        if (context != null) {
            this.services.put(Context.class, context);
            this.services.put(NsdServiceManager.class, nsdServiceManager);
            initializeDatabase(context);
        }
        
        initializeServices(basePackage);
        processInterceptors(basePackage);
        processControllerAdvice(basePackage);
        processEndpoints(basePackage);

        this.sslConfig = new SslConfiguration();
        if (this.sslConfig.isEnabled()) {
            SSLServerSocketFactory sslFactory = this.sslConfig.createSslSocketFactory(context);
            if (sslFactory != null) {
                makeSecure(sslFactory, this.sslConfig.getProtocols());
                Log.i("SERVER_SSL", "HTTPS / TLS habilitado com sucesso.");
            }
        }

        this.rateLimiter = new TokenBucketRateLimiter();
        ServerIndexHandler.setRateLimiter(this.rateLimiter);

        try {
            int readTimeout = Integer.parseInt(Environment.getProperty("nano.server.read-timeout", "5000"));
            start(readTimeout);
            int serverPort = Integer.parseInt(Environment.getProperty("server.port", String.valueOf(port)));
            Log.d("SERVER_OK", "SERVIDOR RODANDO ONLINE NA PORTA " + serverPort + " (timeout=" + readTimeout + "ms)!");

            boolean nsdEnabled = Boolean.parseBoolean(Environment.getProperty("nano.nsd.enabled", "false"));
            if (nsdEnabled && context != null) {
                String nsdName = Environment.getProperty("nano.nsd.name", NsdServiceManager.DEFAULT_SERVICE_NAME);
                String nsdType = Environment.getProperty("nano.nsd.type", NsdServiceManager.DEFAULT_SERVICE_TYPE);
                boolean hostResolution = Boolean.parseBoolean(Environment.getProperty("nano.nsd.host-resolution", "true"));
                nsdServiceManager.register(context, nsdName, nsdType, serverPort, hostResolution);
            }

            boolean networkWatcherEnabled = Boolean.parseBoolean(Environment.getProperty("nano.network.watcher.enabled", "true"));
            if (networkWatcherEnabled && context != null) {
                this.networkWatcher = new NetworkWatcher(context);
                this.networkWatcher.start(new NetworkWatcher.NetworkChangeListener() {
                    @Override
                    public void onNetworkChanged(String newIp) {
                        Log.i("SERVER", "IP de rede alterado para " + newIp + ". Re-anunciando serviços mDNS...");
                        if (nsdServiceManager != null && nsdServiceManager.isRegistered()) {
                            nsdServiceManager.rebind();
                        }
                    }

                    @Override
                    public void onNetworkLost() {
                        Log.w("SERVER", "Conexão de rede perdida.");
                    }
                });
            }
        } catch (IOException e) {
            Log.e("SERVER_FAIL", "ERRO AO INICIAR O SOCKET DO SERVIDOR", e);
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (networkWatcher != null) {
            networkWatcher.stop();
            networkWatcher = null;
        }
        if (nsdServiceManager != null) {
            nsdServiceManager.unregister();
        }
        if (threadPoolAsyncRunner != null) {
            threadPoolAsyncRunner.closeAll();
        }
        super.stop();
    }

    @Override
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        if (corsConfig != null && corsConfig.isEnabled() && session.getMethod() == NanoHTTPD.Method.OPTIONS) {
            NanoHTTPD.Response preflight = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", "");
            applyCorsHeaders(preflight);
            return preflight;
        }

        NanoHTTPD.Response response = handleInternal(session);
        if (corsConfig != null && corsConfig.isEnabled() && response != null) {
            applyCorsHeaders(response);
        }
        return response;
    }

    protected NanoHTTPD.Response handleInternal(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();

        if (rateLimiter != null && rateLimiter.isGlobalEnabled()) {
            String clientIp = session.getRemoteIpAddress();
            if (!rateLimiter.checkGlobal(clientIp)) {
                Map<String, Object> err = new HashMap<>();
                err.put("status", 429);
                err.put("error", "Too Many Requests");
                err.put("message", "Global rate limit exceeded");
                NanoHTTPD.Response r = NanoHTTPD.newFixedLengthResponse(
                        com.github.matheuscruzsouza.nanospring.http.HttpStatus.custom(429, "Too Many Requests"),
                        "application/json",
                        new com.google.gson.Gson().toJson(err)
                );
                r.addHeader("Retry-After", "1");
                return r;
            }
        }

        for (com.github.matheuscruzsouza.nanospring.handler.HandlerInterceptor interceptor : ServerIndexHandler.getInterceptors()) {
            if (!interceptor.preHandle(session, uri)) {
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.UNAUTHORIZED, "text/plain", "Unauthorized by Interceptor");
            }
        }

        boolean actuatorEnabled = Boolean.parseBoolean(Environment.getProperty("nano.actuator.enabled", "true"));
        if (actuatorEnabled) {
            if ("/actuator/health".equals(uri)) {
                return ActuatorHealthHandler.handleHealth(this, this.context);
            }
            if ("/actuator/info".equals(uri)) {
                return ActuatorHealthHandler.handleInfo(this.context);
            }
            if ("/actuator/logfile".equals(uri)) {
                return ActuatorHealthHandler.handleLogfile(session);
            }
        }

        boolean swaggerEnabled = Boolean.parseBoolean(Environment.getProperty("nano.swagger.enabled", "true"));
        if (swaggerEnabled) {
            if ("/v3/api-docs".equals(uri)) {
                return SwaggerUiHandler.handleApiDocs();
            }
            if ("/swagger-ui".equals(uri) || "/swagger-ui/".equals(uri) || "/swagger-ui.html".equals(uri)) {
                return SwaggerUiHandler.handleUi();
            }
        }

        if (uri.startsWith("/static/")) {
            return serveStaticFile(uri);
        }

        NanoHTTPD.Response response = ServerIndexHandler.process(session);
        if (response != null) return response;
        return super.serve(session);
    }

    public void applyCorsHeaders(NanoHTTPD.Response response) {
        if (response == null || corsConfig == null || !corsConfig.isEnabled()) return;
        response.addHeader("Access-Control-Allow-Origin", corsConfig.getAllowedOrigins());
        response.addHeader("Access-Control-Allow-Methods", corsConfig.getAllowedMethods());
        response.addHeader("Access-Control-Allow-Headers", corsConfig.getAllowedHeaders());
        response.addHeader("Access-Control-Max-Age", corsConfig.getMaxAge());
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

    private void initializeDatabase(Context context) {
        boolean migrationEnabled = Boolean.parseBoolean(Environment.getProperty("nano.datasource.migration.enabled", "true"));
        if (!migrationEnabled) return;

        String dbName = Environment.getProperty("nano.datasource.name", "nanospring.db");
        try {
            SQLiteDatabase db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);
            boolean walEnabled = Boolean.parseBoolean(Environment.getProperty("nano.datasource.wal.enabled", "true"));
            if (walEnabled) {
                try {
                    boolean walSuccess = db.enableWriteAheadLogging();
                    Log.d("DATABASE_WAL", "SQLite WAL mode habilitado: " + walSuccess);
                } catch (Exception e) {
                    Log.w("DATABASE_WAL", "Nao foi possivel ativar WAL mode: " + e.getMessage());
                }
            }
            SqliteMigrator.migrate(context, db);
            this.services.put(SQLiteDatabase.class, db);
            Log.d("DATABASE_OK", "Banco de dados SQLite inicializado com sucesso: " + dbName);
        } catch (Exception e) {
            Log.e("DATABASE_FAIL", "Erro ao inicializar banco de dados SQLite ou executar migrações", e);
            e.printStackTrace();
        }
    }

    private void initializeServices(String packageName) {
        List<Class<?>> serviceClasses = getAnnotatedClasses(this.context, packageName, Service.class);
        List<Class<?>> repoClasses = getAnnotatedClasses(this.context, packageName, Repository.class);
        List<Class<?>> allComponents = new ArrayList<>(serviceClasses);
        for (Class<?> klass : repoClasses) {
            if (!allComponents.contains(klass)) {
                allComponents.add(klass);
            }
        }

        for (Class<?> klass : allComponents) {
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
