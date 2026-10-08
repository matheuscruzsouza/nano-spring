with open('nano-spring-web/src/main/java/com/github/matheuscruzsouza/nanospring/server/internal/ThreadPoolAsyncRunner.java', 'r') as f:
    text = f.read()

replacement = """    public static ThreadPoolAsyncRunner fromEnvironment() {
        boolean isLowMemory = Environment.hasActiveProfile("low-memory");
        
        int defCore = isLowMemory ? 2 : DEFAULT_CORE_POOL_SIZE;
        int defMax = isLowMemory ? 4 : DEFAULT_MAX_POOL_SIZE;
        int defQueue = isLowMemory ? 50 : DEFAULT_QUEUE_CAPACITY;
        
        int core = parsePositiveInt(Environment.getProperty("nano.server.threads.core", String.valueOf(defCore)), defCore);
        int max = parsePositiveInt(Environment.getProperty("nano.server.threads.max", String.valueOf(defMax)), defMax);
        int queue = parsePositiveInt(Environment.getProperty("nano.server.threads.queue-capacity", String.valueOf(defQueue)), defQueue);
        long keepAlive = parsePositiveLong(Environment.getProperty("nano.server.threads.keep-alive", String.valueOf(DEFAULT_KEEP_ALIVE_SECONDS)), DEFAULT_KEEP_ALIVE_SECONDS);

        if (max < core) {
            max = core;
        }

        return new ThreadPoolAsyncRunner(core, max, queue, keepAlive);
    }"""
text = text.replace(text[text.find('    public static ThreadPoolAsyncRunner fromEnvironment() {'):text.find('    private static int parsePositiveInt')], replacement + '\n')

with open('nano-spring-web/src/main/java/com/github/matheuscruzsouza/nanospring/server/internal/ThreadPoolAsyncRunner.java', 'w') as f:
    f.write(text)
