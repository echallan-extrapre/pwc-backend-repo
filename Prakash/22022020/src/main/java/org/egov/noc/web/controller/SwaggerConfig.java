package org.egov.noc.web.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import static springfox.documentation.builders.PathSelectors.regex;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
	@Bean
	public Docket productApi() {
		return new Docket(DocumentationType.SWAGGER_2).select()
				.apis(RequestHandlerSelectors.basePackage("org.egov.noc.web.controller")).paths(regex("/noc/.*"))
				.build().apiInfo(metaData());
	}

	private ApiInfo metaData() {
		return new ApiInfo("Spring Boot REST API", "Spring Boot REST API for Online Permission System", "1.0", "Terms of service",
				new Contact("Prakash Hadgal", "", "prakash.hadgal@extrapreneursindia.com"),
				"Apache License Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0");
	}
}