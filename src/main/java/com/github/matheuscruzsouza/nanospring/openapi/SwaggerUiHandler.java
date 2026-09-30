package com.github.matheuscruzsouza.nanospring.openapi;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

/**
 * Handler responsável por servir a especificação OpenAPI (/v3/api-docs)
 * e a interface interativa Nano-Swagger (/swagger-ui).
 */
public class SwaggerUiHandler {

    public static NanoHTTPD.Response handleApiDocs() {
        String json = OpenApiGenerator.generateSpecJson(true);
        NanoHTTPD.Response response = newFixedLengthResponse(Status.OK, "application/json; charset=UTF-8", json);
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }

    public static NanoHTTPD.Response handleUi() {
        NanoHTTPD.Response response = newFixedLengthResponse(Status.OK, "text/html; charset=UTF-8", getSwaggerHtml());
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }

    private static String getSwaggerHtml() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"pt-BR\" class=\"dark\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Nano-Spring Swagger UI</title>\n" +
                "  <style>\n" +
                "    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n" +
                "    body { font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0b0f19; color: #e2e8f0; line-height: 1.5; }\n" +
                "    header { background-color: #111827; border-bottom: 1px solid #1f2937; padding: 1rem 1.5rem; position: sticky; top: 0; z-index: 50; display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 1rem; }\n" +
                "    .brand { display: flex; align-items: center; gap: 0.75rem; }\n" +
                "    .brand svg { width: 28px; height: 28px; color: #10b981; }\n" +
                "    .brand h1 { font-size: 1.25rem; font-weight: 700; color: #f9fafb; letter-spacing: -0.025em; }\n" +
                "    .badge-openapi { background: rgba(16, 185, 129, 0.15); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.3); font-size: 0.7rem; font-weight: 600; padding: 0.15rem 0.5rem; border-radius: 9999px; text-transform: uppercase; }\n" +
                "    .search-box { flex: 1; max-width: 400px; position: relative; }\n" +
                "    .search-box input { width: 100%; background: #1f2937; border: 1px solid #374151; color: #f9fafb; padding: 0.45rem 0.85rem; border-radius: 0.5rem; font-size: 0.875rem; outline: none; transition: border-color 0.2s; }\n" +
                "    .search-box input:focus { border-color: #10b981; }\n" +
                "    .actions { display: flex; align-items: center; gap: 0.75rem; }\n" +
                "    .btn-link { background: #1f2937; color: #9ca3af; border: 1px solid #374151; padding: 0.45rem 0.85rem; border-radius: 0.5rem; font-size: 0.825rem; font-weight: 500; text-decoration: none; display: inline-flex; align-items: center; gap: 0.4rem; transition: all 0.2s; }\n" +
                "    .btn-link:hover { color: #f9fafb; border-color: #4b5563; background: #374151; }\n" +
                "    main { max-width: 1200px; margin: 0 auto; padding: 1.5rem; }\n" +
                "    .api-info { background: #111827; border: 1px solid #1f2937; border-radius: 0.75rem; padding: 1.5rem; margin-bottom: 2rem; }\n" +
                "    .api-info h2 { font-size: 1.5rem; font-weight: 700; color: #f9fafb; margin-bottom: 0.5rem; }\n" +
                "    .api-info p { color: #9ca3af; font-size: 0.95rem; margin-bottom: 1rem; }\n" +
                "    .api-meta { display: flex; gap: 1rem; font-size: 0.8rem; color: #6b7280; font-family: ui-monospace, monospace; }\n" +
                "    .tag-section { margin-bottom: 2rem; }\n" +
                "    .tag-header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #1f2937; padding-bottom: 0.75rem; margin-bottom: 1rem; }\n" +
                "    .tag-title { font-size: 1.25rem; font-weight: 700; color: #f3f4f6; display: flex; align-items: center; gap: 0.5rem; }\n" +
                "    .endpoint-card { background: #111827; border: 1px solid #1f2937; border-radius: 0.5rem; margin-bottom: 0.75rem; overflow: hidden; transition: border-color 0.2s; }\n" +
                "    .endpoint-header { padding: 0.75rem 1rem; display: flex; align-items: center; gap: 1rem; cursor: pointer; user-select: none; }\n" +
                "    .endpoint-header:hover { background: #162032; }\n" +
                "    .method { font-size: 0.75rem; font-weight: 700; padding: 0.25rem 0.6rem; border-radius: 0.375rem; min-width: 65px; text-align: center; text-transform: uppercase; font-family: ui-monospace, monospace; }\n" +
                "    .method.get { background: #1e3a8a; color: #93c5fd; border: 1px solid #2563eb; }\n" +
                "    .method.post { background: #064e3b; color: #6ee7b7; border: 1px solid #059669; }\n" +
                "    .method.put { background: #78350f; color: #fcd34d; border: 1px solid #d97706; }\n" +
                "    .method.delete { background: #7f1d1d; color: #fca5a5; border: 1px solid #dc2626; }\n" +
                "    .route-path { font-family: ui-monospace, monospace; font-size: 0.95rem; font-weight: 600; color: #f9fafb; flex: 1; }\n" +
                "    .route-path .param { color: #38bdf8; font-weight: 700; }\n" +
                "    .route-summary { color: #9ca3af; font-size: 0.875rem; }\n" +
                "    .chevron { transition: transform 0.2s; color: #6b7280; }\n" +
                "    .endpoint-card.open .chevron { transform: rotate(180deg); }\n" +
                "    .endpoint-body { display: none; padding: 1.25rem; border-top: 1px solid #1f2937; background: #0d131f; }\n" +
                "    .endpoint-card.open .endpoint-body { display: block; }\n" +
                "    .section-subtitle { font-size: 0.8rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: #9ca3af; margin-bottom: 0.75rem; }\n" +
                "    .param-group { margin-bottom: 1.25rem; }\n" +
                "    .param-row { display: grid; grid-template-columns: 180px 100px 1fr; gap: 1rem; align-items: center; margin-bottom: 0.5rem; }\n" +
                "    .param-name { font-family: ui-monospace, monospace; font-size: 0.85rem; color: #f3f4f6; font-weight: 600; }\n" +
                "    .param-name .req { color: #ef4444; margin-left: 2px; }\n" +
                "    .param-in { font-size: 0.75rem; color: #6b7280; font-family: ui-monospace, monospace; }\n" +
                "    .param-input { background: #1f2937; border: 1px solid #374151; color: #f9fafb; padding: 0.4rem 0.65rem; border-radius: 0.375rem; font-size: 0.85rem; font-family: ui-monospace, monospace; outline: none; }\n" +
                "    .param-input:focus { border-color: #10b981; }\n" +
                "    .body-textarea { width: 100%; min-height: 120px; background: #1f2937; border: 1px solid #374151; color: #f9fafb; padding: 0.75rem; border-radius: 0.375rem; font-size: 0.85rem; font-family: ui-monospace, monospace; outline: none; resize: vertical; margin-bottom: 1rem; }\n" +
                "    .body-textarea:focus { border-color: #10b981; }\n" +
                "    .btn-exec { background: #10b981; color: #ffffff; border: none; padding: 0.5rem 1.25rem; border-radius: 0.375rem; font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: background 0.2s; display: inline-flex; align-items: center; gap: 0.5rem; }\n" +
                "    .btn-exec:hover { background: #059669; }\n" +
                "    .btn-exec:disabled { opacity: 0.5; cursor: not-allowed; }\n" +
                "    .response-box { margin-top: 1.25rem; border-top: 1px dashed #374151; padding-top: 1rem; }\n" +
                "    .res-meta { display: flex; align-items: center; gap: 1rem; margin-bottom: 0.5rem; font-size: 0.8rem; }\n" +
                "    .status-badge { font-weight: 700; padding: 0.15rem 0.5rem; border-radius: 0.25rem; font-family: ui-monospace, monospace; }\n" +
                "    .status-2xx { background: #064e3b; color: #6ee7b7; }\n" +
                "    .status-4xx { background: #78350f; color: #fcd34d; }\n" +
                "    .status-5xx { background: #7f1d1d; color: #fca5a5; }\n" +
                "    .res-body { background: #020617; border: 1px solid #1e293b; border-radius: 0.375rem; padding: 0.75rem; font-family: ui-monospace, monospace; font-size: 0.8rem; color: #38bdf8; overflow-x: auto; white-space: pre-wrap; word-break: break-all; max-height: 350px; }\n" +
                "    .empty-placeholder { color: #6b7280; font-style: italic; font-size: 0.85rem; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <header>\n" +
                "    <div class=\"brand\">\n" +
                "      <svg fill=\"none\" viewBox=\"0 0 24 24\" stroke=\"currentColor\"><path stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"2\" d=\"M13 10V3L4 14h7v7l9-11h-7z\"/></svg>\n" +
                "      <h1>Nano-Swagger</h1>\n" +
                "      <span class=\"badge-openapi\">OpenAPI 3.0</span>\n" +
                "    </div>\n" +
                "    <div class=\"search-box\">\n" +
                "      <input type=\"text\" id=\"filterInput\" placeholder=\"Filtrar rotas, tags ou métodos...\" oninput=\"filterEndpoints()\">\n" +
                "    </div>\n" +
                "    <div class=\"actions\">\n" +
                "      <a href=\"/v3/api-docs\" target=\"_blank\" class=\"btn-link\">\n" +
                "        <svg width=\"14\" height=\"14\" fill=\"currentColor\" viewBox=\"0 0 16 16\"><path d=\"M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5h-2z\"/></svg>\n" +
                "        JSON Spec\n" +
                "      </a>\n" +
                "    </div>\n" +
                "  </header>\n" +
                "\n" +
                "  <main>\n" +
                "    <div class=\"api-info\" id=\"apiInfo\">\n" +
                "      <h2 id=\"docTitle\">Carregando API...</h2>\n" +
                "      <p id=\"docDesc\">Consultando /v3/api-docs...</p>\n" +
                "      <div class=\"api-meta\">\n" +
                "        <span>Versão: <strong id=\"docVersion\">-</strong></span>\n" +
                "        <span>Servidor: <strong id=\"docServer\">/</strong></span>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div id=\"endpointsContainer\"></div>\n" +
                "  </main>\n" +
                "\n" +
                "  <script>\n" +
                "    let apiSpec = null;\n" +
                "\n" +
                "    async function loadSpec() {\n" +
                "      try {\n" +
                "        const res = await fetch('/v3/api-docs');\n" +
                "        apiSpec = await res.json();\n" +
                "        renderApiInfo(apiSpec);\n" +
                "        renderEndpoints(apiSpec);\n" +
                "      } catch (err) {\n" +
                "        document.getElementById('docTitle').innerText = 'Erro ao carregar especificação OpenAPI';\n" +
                "        document.getElementById('docDesc').innerText = err.message;\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function renderApiInfo(spec) {\n" +
                "      if (spec.info) {\n" +
                "        document.getElementById('docTitle').innerText = spec.info.title || 'Nano-Spring API';\n" +
                "        document.getElementById('docDesc').innerText = spec.info.description || '';\n" +
                "        document.getElementById('docVersion').innerText = spec.info.version || '1.0.0';\n" +
                "      }\n" +
                "      if (spec.servers && spec.servers.length > 0) {\n" +
                "        document.getElementById('docServer').innerText = spec.servers[0].url || '/';\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function renderEndpoints(spec) {\n" +
                "      const container = document.getElementById('endpointsContainer');\n" +
                "      container.innerHTML = '';\n" +
                "\n" +
                "      const tagsMap = {};\n" +
                "      if (!spec.paths) return;\n" +
                "\n" +
                "      Object.entries(spec.paths).forEach(([path, methods]) => {\n" +
                "        Object.entries(methods).forEach(([method, op]) => {\n" +
                "          const tag = (op.tags && op.tags.length > 0) ? op.tags[0] : 'Geral';\n" +
                "          if (!tagsMap[tag]) tagsMap[tag] = [];\n" +
                "          tagsMap[tag].push({ path, method, op });\n" +
                "        });\n" +
                "      });\n" +
                "\n" +
                "      Object.entries(tagsMap).forEach(([tag, items], tagIdx) => {\n" +
                "        const sec = document.createElement('div');\n" +
                "        sec.className = 'tag-section';\n" +
                "        sec.innerHTML = `<div class=\"tag-header\"><h3 class=\"tag-title\">${tag} <span style=\"font-size: 0.8rem; color: #6b7280; font-weight: normal;\">(${items.length})</span></h3></div>`;\n" +
                "        \n" +
                "        items.forEach((item, itemIdx) => {\n" +
                "          const card = createEndpointCard(item.path, item.method, item.op, `${tagIdx}_${itemIdx}`);\n" +
                "          sec.appendChild(card);\n" +
                "        });\n" +
                "        container.appendChild(sec);\n" +
                "      });\n" +
                "    }\n" +
                "\n" +
                "    function createEndpointCard(path, method, op, id) {\n" +
                "      const card = document.createElement('div');\n" +
                "      card.className = 'endpoint-card';\n" +
                "      card.dataset.path = path;\n" +
                "      card.dataset.method = method;\n" +
                "      card.dataset.tags = (op.tags || []).join(' ');\n" +
                "\n" +
                "      const highlightedPath = path.replace(/\\{([^}]+)\\}/g, '<span class=\"param\">{$1}</span>');\n" +
                "      const summary = op.summary || '';\n" +
                "\n" +
                "      card.innerHTML = `\n" +
                "        <div class=\"endpoint-header\" onclick=\"toggleCard('${id}')\">\n" +
                "          <span class=\"method ${method}\">${method}</span>\n" +
                "          <span class=\"route-path\">${highlightedPath}</span>\n" +
                "          <span class=\"route-summary\">${summary}</span>\n" +
                "          <svg class=\"chevron\" id=\"chev_${id}\" width=\"16\" height=\"16\" fill=\"none\" viewBox=\"0 0 24 24\" stroke=\"currentColor\"><path stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"2\" d=\"M19 9l-7 7-7-7\"/></svg>\n" +
                "        </div>\n" +
                "        <div class=\"endpoint-body\" id=\"body_${id}\">\n" +
                "          ${renderParametersForm(op.parameters, id)}\n" +
                "          ${renderRequestBodyForm(op.requestBody, id)}\n" +
                "          <div style=\"margin-top: 1rem;\">\n" +
                "            <button class=\"btn-exec\" id=\"btn_${id}\" onclick=\"executeRequest('${id}', '${path}', '${method}')\">\n" +
                "              <svg width=\"14\" height=\"14\" fill=\"currentColor\" viewBox=\"0 0 16 16\"><path d=\"m11.596 8.697-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z\"/></svg>\n" +
                "              Executar (Try it out)\n" +
                "            </button>\n" +
                "          </div>\n" +
                "          <div class=\"response-box\" id=\"res_${id}\" style=\"display: none;\">\n" +
                "            <div class=\"res-meta\">\n" +
                "              <span>Status: <strong class=\"status-badge\" id=\"status_${id}\"></strong></span>\n" +
                "              <span>Tempo: <strong id=\"time_${id}\"></strong></span>\n" +
                "            </div>\n" +
                "            <pre class=\"res-body\"><code id=\"rescode_${id}\"></code></pre>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      `;\n" +
                "      return card;\n" +
                "    }\n" +
                "\n" +
                "    function toggleCard(id) {\n" +
                "      const body = document.getElementById('body_' + id);\n" +
                "      const card = body.closest('.endpoint-card');\n" +
                "      card.classList.toggle('open');\n" +
                "    }\n" +
                "\n" +
                "    function renderParametersForm(params, id) {\n" +
                "      if (!params || params.length === 0) return '';\n" +
                "      let html = '<div class=\"param-group\"><h4 class=\"section-subtitle\">Parâmetros</h4>';\n" +
                "      params.forEach(p => {\n" +
                "        const req = p.required ? '<span class=\"req\">*</span>' : '';\n" +
                "        html += `\n" +
                "          <div class=\"param-row\">\n" +
                "            <div class=\"param-name\">${p.name}${req}</div>\n" +
                "            <div class=\"param-in\">(${p.in})</div>\n" +
                "            <input class=\"param-input\" data-in=\"${p.in}\" data-name=\"${p.name}\" placeholder=\"${p.example || ''}\" id=\"param_${id}_${p.name}\">\n" +
                "          </div>\n" +
                "        `;\n" +
                "      });\n" +
                "      html += '</div>';\n" +
                "      return html;\n" +
                "    }\n" +
                "\n" +
                "    function renderRequestBodyForm(body, id) {\n" +
                "      if (!body || !body.content || !body.content['application/json']) return '';\n" +
                "      return `\n" +
                "        <div class=\"param-group\">\n" +
                "          <h4 class=\"section-subtitle\">Corpo da Requisição (JSON)</h4>\n" +
                "          <textarea class=\"body-textarea\" id=\"body_val_${id}\" placeholder=\"{\\n  \\\"key\\\": \\\"value\\\"\\n}\"></textarea>\n" +
                "        </div>\n" +
                "      `;\n" +
                "    }\n" +
                "\n" +
                "    async function executeRequest(id, pathTemplate, method) {\n" +
                "      const btn = document.getElementById('btn_' + id);\n" +
                "      const resBox = document.getElementById('res_' + id);\n" +
                "      const statusBadge = document.getElementById('status_' + id);\n" +
                "      const timeBadge = document.getElementById('time_' + id);\n" +
                "      const codeEl = document.getElementById('rescode_' + id);\n" +
                "\n" +
                "      btn.disabled = true;\n" +
                "      resBox.style.display = 'block';\n" +
                "      codeEl.innerText = 'Enviando requisição...';\n" +
                "\n" +
                "      let finalUrl = pathTemplate;\n" +
                "      const queryParams = new URLSearchParams();\n" +
                "      const headers = { 'Accept': 'application/json' };\n" +
                "\n" +
                "      const inputs = document.querySelectorAll(`[id^=\"param_${id}_\"]`);\n" +
                "      inputs.forEach(inp => {\n" +
                "        const location = inp.dataset.in;\n" +
                "        const name = inp.dataset.name;\n" +
                "        const val = inp.value.trim();\n" +
                "        if (location === 'path') {\n" +
                "          finalUrl = finalUrl.replace(`{${name}}`, encodeURIComponent(val || name));\n" +
                "        } else if (location === 'query' && val) {\n" +
                "          queryParams.append(name, val);\n" +
                "        } else if (location === 'header' && val) {\n" +
                "          headers[name] = val;\n" +
                "        }\n" +
                "      });\n" +
                "\n" +
                "      if (queryParams.toString()) {\n" +
                "        finalUrl += '?' + queryParams.toString();\n" +
                "      }\n" +
                "\n" +
                "      let bodyContent = null;\n" +
                "      const bodyEl = document.getElementById('body_val_' + id);\n" +
                "      if (bodyEl && bodyEl.value.trim() && method.toUpperCase() !== 'GET') {\n" +
                "        bodyContent = bodyEl.value.trim();\n" +
                "        headers['Content-Type'] = 'application/json';\n" +
                "      }\n" +
                "\n" +
                "      const startTime = performance.now();\n" +
                "      try {\n" +
                "        const response = await fetch(finalUrl, {\n" +
                "          method: method.toUpperCase(),\n" +
                "          headers: headers,\n" +
                "          body: bodyContent\n" +
                "        });\n" +
                "        const duration = Math.round(performance.now() - startTime);\n" +
                "        timeBadge.innerText = duration + ' ms';\n" +
                "\n" +
                "        const statusCode = response.status;\n" +
                "        statusBadge.innerText = statusCode + ' ' + response.statusText;\n" +
                "        statusBadge.className = 'status-badge ' + (statusCode < 300 ? 'status-2xx' : (statusCode < 500 ? 'status-4xx' : 'status-5xx'));\n" +
                "\n" +
                "        const text = await response.text();\n" +
                "        try {\n" +
                "          const obj = JSON.parse(text);\n" +
                "          codeEl.innerText = JSON.stringify(obj, null, 2);\n" +
                "        } catch (e) {\n" +
                "          codeEl.innerText = text || '(Resposta vazia)';\n" +
                "        }\n" +
                "      } catch (err) {\n" +
                "        const duration = Math.round(performance.now() - startTime);\n" +
                "        timeBadge.innerText = duration + ' ms';\n" +
                "        statusBadge.innerText = 'Falha de Conexão';\n" +
                "        statusBadge.className = 'status-badge status-5xx';\n" +
                "        codeEl.innerText = 'Erro ao executar fetch(): ' + err.message;\n" +
                "      } finally {\n" +
                "        btn.disabled = false;\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function filterEndpoints() {\n" +
                "      const query = document.getElementById('filterInput').value.toLowerCase().trim();\n" +
                "      const cards = document.querySelectorAll('.endpoint-card');\n" +
                "      cards.forEach(card => {\n" +
                "        const path = card.dataset.path.toLowerCase();\n" +
                "        const method = card.dataset.method.toLowerCase();\n" +
                "        const tags = card.dataset.tags.toLowerCase();\n" +
                "        const match = path.includes(query) || method.includes(query) || tags.includes(query);\n" +
                "        card.style.display = match ? 'block' : 'none';\n" +
                "      });\n" +
                "    }\n" +
                "\n" +
                "    window.addEventListener('DOMContentLoaded', loadSpec);\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
