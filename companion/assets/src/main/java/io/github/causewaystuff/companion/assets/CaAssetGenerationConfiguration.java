package io.github.causewaystuff.companion.assets;

import org.springframework.context.annotation.Bean;

import io.spring.initializr.generator.io.IndentingWriterFactory;
import io.spring.initializr.generator.io.SimpleIndentStrategy;
import io.spring.initializr.generator.project.ProjectGenerationConfiguration;

/**
 * Project generation configuration for projects using any build system.
 * @see io.spring.initializr.generator.spring.build.BuildProjectGenerationConfiguration
 */
@ProjectGenerationConfiguration
public class CaAssetGenerationConfiguration {
    
    @Bean
    public IndentingWriterFactory indentingWriterFactory() {
        return IndentingWriterFactory.create(new SimpleIndentStrategy("    "),
                (builder) -> builder.indentingStrategy("yaml", new SimpleIndentStrategy("  ")));
    }
    
    @Bean
    public CoProjectContributor makedirProjectContributor(CoProjectDescription projectDescription) {
        return new MakedirProjectContributor(projectDescription);
    }
    
}
