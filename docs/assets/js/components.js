const SIDEBAR_HTML = `
      <div class="sidebar-header">
        <h2>Documentação</h2>
      </div>
      <nav class="sidebar-menu">
        <div class="menu-group">
          <h3>Começando</h3>
          <ul>
            <li><a href="/index.html">Visão Geral</a></li>
            <li><a href="/getting-started.html">Instalação e Setup</a></li>
            <li><a href="/spring-boot-vs-nano-spring.html">Spring Boot vs Nano Spring</a></li>
          </ul>
        </div>

        <div class="menu-group">
          <h3>Core</h3>
          <ul>
            <li><a href="/features/dependency-injection.html">Injeção de Dependência (DI)</a></li>
            <li><a href="/features/rest-api.html">Controllers e REST API</a></li>
            <li><a href="/features/security-validation.html">Validação e Rate Limiting</a></li>
            <li><a href="/features/interceptors-errors.html">Interceptors e Erros</a></li>
            <li><a href="/features/cors.html">CORS e Headers</a></li>
          </ul>
        </div>

        <div class="menu-group">
          <h3>Integrações e Dados</h3>
          <ul>
            <li><a href="/features/sqlite-migrations.html">SQLite e Migrations</a></li>
            <li><a href="/features/files.html">Servidor Estático (Assets/Storage)</a></li>
            <li><a href="/features/templates-static.html">Templates (Mustache)</a></li>
          </ul>
        </div>

        <div class="menu-group">
          <h3>Avançado</h3>
          <ul>
            <li><a href="/features/mdns-discovery.html">mDNS (Service Discovery)</a></li>
            <li><a href="/features/sse.html">Server-Sent Events (SSE)</a></li>
            <li><a href="/features/swagger-openapi.html">Swagger UI e OpenAPI</a></li>
            <li><a href="/features/enterprise-hardening.html">HTTPS e Certificados (TLS)</a></li>
            <li><a href="/features/logging-diagnostics.html">Logging e Actuator</a></li>
          </ul>
        </div>

        <div class="menu-group">
          <h3>Releases</h3>
          <ul>
            <li><a href="/features/v2-0-0.html">v2.0.0 (Modularização e Perfis)</a></li>
          </ul>
        </div>
      </nav>
`;

document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.querySelector('.sidebar');
    if (sidebar) {
        sidebar.innerHTML = SIDEBAR_HTML;
        
        // Fix paths based on current location
        const isFeaturesDir = window.location.pathname.includes('/features/');
        if (isFeaturesDir) {
            const links = sidebar.querySelectorAll('a');
            links.forEach(link => {
                const href = link.getAttribute('href');
                if (href.startsWith('/features/')) {
                    link.setAttribute('href', href.replace('/features/', ''));
                } else if (href.startsWith('/')) {
                    link.setAttribute('href', '.' + href);
                }
            });
        } else {
            const links = sidebar.querySelectorAll('a');
            links.forEach(link => {
                const href = link.getAttribute('href');
                if (href.startsWith('/')) {
                    link.setAttribute('href', href.substring(1));
                }
            });
        }
        
        // Re-run active link highlighting from app.js
        const currentPath = window.location.pathname.replace(/\/$/, '');
        const sidebarLinks = document.querySelectorAll('.sidebar-menu a');
        sidebarLinks.forEach(link => {
          const linkPath = new URL(link.href, window.location.origin).pathname.replace(/\/$/, '');
          if (linkPath === currentPath || (currentPath.endsWith('/docs') && linkPath.endsWith('index.html'))) {
            link.classList.add('active');
          }
        });
    }
});
