package com.openbake.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Serves local avatar uploads, mirroring main.py's app.mount("/media", StaticFiles(...)). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String mediaDir;

    public WebConfig(@Value("${app.media-dir:./media}") String mediaDir) {
        this.mediaDir = mediaDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(mediaDir).toUri().toString();
        registry.addResourceHandler("/media/**").addResourceLocations(location);
    }
}
