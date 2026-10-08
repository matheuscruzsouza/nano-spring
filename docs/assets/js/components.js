const SIDEBAR_HTML = `
      <div class="sidebar-section">
        <div class="sidebar-title">Começando</div>
        <ul class="sidebar-menu">
          <li><a href="/index.html">Visão Geral</a></li>
          <li><a href="/getting-started.html">Instalação & Setup</a></li>
          <li><a href="/spring-boot-vs-nano-spring.html">⚖️ Spring Boot vs Nano-Spring</a></li>
          <li><a href="/features/v2-0-0.html">✨ Novidades da v2.0.0</a></li>
        </ul>
      </div>

      <div class="sidebar-section">
        <div class="sidebar-title">Core Web</div>
        <ul class="sidebar-menu">
          <li><a href="/features/rest-api.html">APIs RESTful</a></li>
          <li><a href="/features/swagger-openapi.html">📑 Swagger & OpenAPI 3.0</a></li>
          <li><a href="/features/dependency-injection.html">Injeção de Dependências</a></li>
          <li><a href="/features/sse.html">Server-Sent Events (SSE)</a></li>
          <li><a href="/features/files.html">Upload & Download</a></li>
          <li><a href="/features/interceptors-errors.html">Interceptadores & Erros</a></li>
        </ul>
      </div>

      <div class="sidebar-section">
        <div class="sidebar-title">Enterprise & Hardening</div>
        <ul class="sidebar-menu">
          <li><a href="/features/logging-diagnostics.html">📜 Diagnósticos & Perfis</a></li>
          <li><a href="/features/security-validation.html">🔒 Segurança & Validação</a></li>
          <li><a href="/features/enterprise-hardening.html">🛡️ Enterprise & Resiliência</a></li>
        </ul>
      </div>

      <div class="sidebar-section">
        <div class="sidebar-title">Infraestrutura Local</div>
        <ul class="sidebar-menu">
          <li><a href="/features/sqlite-migrations.html">SQLite & Migrações</a></li>
          <li><a href="/features/mdns-discovery.html">Descoberta mDNS (.local)</a></li>
          <li><a href="/features/cors.html">CORS Nativo</a></li>
          <li><a href="/features/templates-static.html">Templates & Estáticos</a></li>
        </ul>
      </div>
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
                    link.setAttribute('href', '../' + href.substring(1));
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
