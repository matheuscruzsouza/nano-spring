package com.github.matheuscruzsouza.nanospring.server;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.Repository;
import com.github.matheuscruzsouza.nanospring.annotation.Service;
import com.github.matheuscruzsouza.nanospring.annotation.Value;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConstructorInjectionTest {

    @Before
    @After
    public void cleanup() {
        Environment.reset();
    }

    public static class SampleRepository {
        public String findData() {
            return "db-data";
        }
    }

    public static class SimpleService {
        private final SampleRepository repository;

        public SimpleService(SampleRepository repository) {
            this.repository = repository;
        }

        public SampleRepository getRepository() {
            return repository;
        }
    }

    public static class ChainedService {
        private final SimpleService simpleService;
        private final String appName;

        public ChainedService(SimpleService simpleService, @Value("${app.name:NanoTest}") String appName) {
            this.simpleService = simpleService;
            this.appName = appName;
        }

        public SimpleService getSimpleService() {
            return simpleService;
        }

        public String getAppName() {
            return appName;
        }
    }

    public static class MixedService {
        private final SampleRepository repository;

        @Autowired
        private SimpleService simpleService;

        @Value("${mixed.timeout:5000}")
        private int timeout;

        public MixedService(SampleRepository repository) {
            this.repository = repository;
        }

        public SampleRepository getRepository() {
            return repository;
        }

        public SimpleService getSimpleService() {
            return simpleService;
        }

        public int getTimeout() {
            return timeout;
        }
    }

    public static class MultipleConstructorsService {
        private final SampleRepository repository;
        private final String source;

        public MultipleConstructorsService() {
            this.repository = null;
            this.source = "default";
        }

        @Autowired
        public MultipleConstructorsService(SampleRepository repository) {
            this.repository = repository;
            this.source = "autowired";
        }

        public SampleRepository getRepository() {
            return repository;
        }

        public String getSource() {
            return source;
        }
    }

    @Test
    public void testDirectConstructorInjection() throws Exception {
        Server server = new Server(null, 8080, "test.empty");
        SampleRepository repo = new SampleRepository();
        server.registerSingleton(SampleRepository.class, repo);

        SimpleService service = (SimpleService) server.createInstance(SimpleService.class);
        assertNotNull(service);
        assertSame(repo, service.getRepository());
    }

    @Test
    public void testChainedConstructorInjectionAndValueAnnotation() throws Exception {
        Environment.setProperty("app.name", "CustomApp");

        Server server = new Server(null, 8080, "test.empty");
        SampleRepository repo = new SampleRepository();
        server.registerSingleton(SampleRepository.class, repo);

        SimpleService simpleService = (SimpleService) server.createInstance(SimpleService.class);
        server.registerSingleton(SimpleService.class, simpleService);

        ChainedService chainedService = (ChainedService) server.createInstance(ChainedService.class);
        assertNotNull(chainedService);
        assertSame(simpleService, chainedService.getSimpleService());
        assertEquals("CustomApp", chainedService.getAppName());
    }

    @Test
    public void testMultipleConstructorsPrioritizesAutowired() throws Exception {
        Server server = new Server(null, 8080, "test.empty");
        SampleRepository repo = new SampleRepository();
        server.registerSingleton(SampleRepository.class, repo);

        MultipleConstructorsService service = (MultipleConstructorsService) server.createInstance(MultipleConstructorsService.class);
        assertNotNull(service);
        assertSame(repo, service.getRepository());
        assertEquals("autowired", service.getSource());
    }

    @Test
    public void testReturnsNullWhenDependencyMissing() throws Exception {
        Server server = new Server(null, 8080, "test.empty");
        // SampleRepository not registered
        SimpleService service = (SimpleService) server.createInstance(SimpleService.class);
        assertNull("Deveria retornar null quando a dependência requerida não existe", service);
    }
}
