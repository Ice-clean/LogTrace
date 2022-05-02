package top.iceclean.logtrace.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author : Ice'Clean
 * @date : 2022-04-28
 */
@Component
public class ResourceConfig implements WebMvcConfigurer {
    private static final String STATIC_RESOURCE_PATTERN = "/static/**";
    private static final String STATIC_RESOURCE_LOCATION = "classpath:/static/";
    private static final String CLASS_RESOURCE_PATTERN = "/**";
    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
            "classpath:/META-INF/resources/", "classpath:/resources/",
            "classpath:/static/", "classpath:/public/" };

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!registry.hasMappingForPattern(STATIC_RESOURCE_PATTERN)) {
            registry.addResourceHandler(STATIC_RESOURCE_PATTERN)
                    .addResourceLocations(STATIC_RESOURCE_LOCATION);
        }
        if (!registry.hasMappingForPattern(CLASS_RESOURCE_PATTERN)) {
            registry.addResourceHandler(CLASS_RESOURCE_PATTERN)
                    .addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);
        }
    }
}
