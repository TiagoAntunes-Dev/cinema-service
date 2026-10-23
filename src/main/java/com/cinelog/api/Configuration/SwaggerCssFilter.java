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

        // Intercepta exclusivamente a página principal do Swagger UI
        if (uri != null && uri.contains("/swagger-ui/index.html")) {
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(request, responseWrapper);

            byte[] content = responseWrapper.getContentAsByteArray();
            if (content.length > 0) {
                String html = new String(content, StandardCharsets.UTF_8);

                // Injeta o link para o teu custom.css antes de fechar a tag </head>
                if (html.contains("</head>") && !html.contains("custom.css")) {
                    String cssLink = "<link rel=\"stylesheet\" type=\"text/css\" href=\"/custom.css\">";
                    html = html.replace("</head>", cssLink + "</head>");
                }

                response.setContentType("text/html;charset=UTF-8");
                response.setContentLength(html.getBytes(StandardCharsets.UTF_8).length);
                response.getOutputStream().write(html.getBytes(StandardCharsets.UTF_8));
                responseWrapper.copyBodyToResponse();
                return;
            }
            responseWrapper.copyBodyToResponse();
        } else {
            filterChain.doFilter(request, response);
        }
    }
}