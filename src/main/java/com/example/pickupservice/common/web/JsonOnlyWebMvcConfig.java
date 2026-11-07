package com.example.pickupservice.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class JsonOnlyWebMvcConfig implements WebMvcConfigurer {

	@Override
	public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
		builder.configureMessageConvertersList(converters -> converters.removeIf(converter ->
				converter.getSupportedMediaTypes().stream().anyMatch(MediaType.APPLICATION_XML::equalsTypeAndSubtype)));
	}

}
