package com.openbake.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;

/**
 * Serves the Next.js static export (copied into classpath:/static at build time) and
 * replicates the extension-less URL + SPA-fallback behavior a static host like Nginx
 * would normally provide via `try_files $uri $uri.html $uri/ =404`:
 *
 *  1. Exact static file match (JS/CSS/images/_next/** chunks).
 *  2. path + ".html" (Next's flat-file output: "/about" -> "about.html").
 *  3. For the 3 catch-all dynamic routes, the single pre-rendered shell Next
 *     generated regardless of the actual id in the URL (see
 *     web/app/menu/[...id]/page.tsx and its siblings) — the client component
 *     reads the real id from the browser URL once it hydrates.
 *  4. index.html for any other unmatched client-side route.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final List<String> CATCH_ALL_SHELLS = List.of(
            "menu/", "menu/placeholder.html",
            "orders/", "orders/placeholder.html",
            "admin/orders/", "admin/orders/placeholder.html"
    );

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResourceResolver());
    }

    private static class SpaFallbackResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }

            Resource withHtmlSuffix = location.createRelative(resourcePath + ".html");
            if (withHtmlSuffix.exists() && withHtmlSuffix.isReadable()) {
                return withHtmlSuffix;
            }

            for (int i = 0; i < CATCH_ALL_SHELLS.size(); i += 2) {
                if (resourcePath.startsWith(CATCH_ALL_SHELLS.get(i))) {
                    Resource shell = location.createRelative(CATCH_ALL_SHELLS.get(i + 1));
                    if (shell.exists()) {
                        return shell;
                    }
                }
            }

            if (!resourcePath.contains(".")) {
                return location.createRelative("index.html");
            }

            return null;
        }
    }
}
