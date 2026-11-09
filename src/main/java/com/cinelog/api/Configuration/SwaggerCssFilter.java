package com.cinelog.api.Configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SwaggerCssFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (uri != null && uri.contains("/swagger-ui/index.html")) {
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(request, responseWrapper);

            byte[] content = responseWrapper.getContentAsByteArray();
            if (content.length > 0) {
                String html = new String(content, StandardCharsets.UTF_8);

                // Injeção do CSS e do Botão/Script
                if (html.contains("</head>") && !html.contains("custom.css")) {

                    // Caminho ABSOLUTO para o CSS (essencial para não dar 404)
                    String cssLink = "<link rel=\"stylesheet\" type=\"text/css\" href=\"/custom.css\">";

                    // HTML do botão e o JavaScript para alternar o tema
                    String themeToggleHtml = """
                        <style>
                            #theme-toggle-btn {
                                position: fixed; bottom: 20px; right: 20px; z-index: 10000;
                                padding: 12px 18px; border-radius: 50px;
                                border: 1px solid #6366f1; background: #1e293b;
                                color: #f3f4f6; cursor: pointer; font-size: 16px;
                                box-shadow: 0 4px 15px rgba(0,0,0,0.3);
                                display: flex; align-items: center; gap: 8px;
                                transition: all 0.2s ease;
                            }
                            #theme-toggle-btn:hover { transform: scale(1.05); background: #334155; }
                            html.dark-mode #theme-toggle-btn { background: #f8fafc; color: #1e293b; }
                        </style>
                        <button id="theme-toggle-btn" onclick="toggleSwaggerTheme()">
                            🌓 Alternar Tema
                        </button>
                        <script>
                            (function() {
                                const html = document.documentElement;
                                // Verifica preferência salva
                                const savedTheme = localStorage.getItem('swagger-theme');
                                if (savedTheme === 'dark') {
                                    html.classList.add('dark-mode');
                                } else if (savedTheme === 'light') {
                                    html.classList.remove('dark-mode');
                                } else {
                                    // Se não houver preferência, segue a do sistema
                                    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
                                        html.classList.add('dark-mode');
                                    }
                                }
                            })();

                            function toggleSwaggerTheme() {
                                const html = document.documentElement;
                                const isNowDark = html.classList.toggle('dark-mode');
                                localStorage.setItem('swagger-theme', isNowDark ? 'dark' : 'light');
                            }
                        </script>
                    """;

                    // Injeta o CSS no <head> e o botão/script no final do <body>
                    html = html.replace("</head>", cssLink + "</head>");
                    html = html.replace("</body>", themeToggleHtml + "</body>");
                }

                // Escreve a resposta final modificada
                response.setContentType("text/html;charset=UTF-8");
                byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
                response.setContentLength(htmlBytes.length);
                response.getOutputStream().write(htmlBytes);

                // IMPORTANTE: Retorna aqui. Não use copyBodyToResponse() após escrever manualmente.
                return;
            }
            // Só copia o body se não houver conteúdo modificado (fallback)
            responseWrapper.copyBodyToResponse();
        } else {
            filterChain.doFilter(request, response);
        }
    }
}