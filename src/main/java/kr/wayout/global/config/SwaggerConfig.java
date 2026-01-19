package kr.wayout.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(info = @Info(title = "Way Out WAS", description = "Way Out 서비스의 API 명세입니다.", version = "v1"))
@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi openApi() {
        String[] path = {"/**"};

        return GroupedOpenApi.builder()
                .group("Way Out API v1")
                .pathsToMatch(path)
                .build();
    }
}
