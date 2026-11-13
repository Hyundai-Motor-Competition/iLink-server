package com.example.pickupservice.common.fixture;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;

public final class MainApplicationYaml {

	private static final String BASE_FILE = "application.yml";

	private MainApplicationYaml() {
	}

	public static Duration profileOverBaseDuration(String profile, String key) {
		Object raw = rawValue("application-" + profile + ".yml", key);
		return toDurationOrNull(raw == null ? rawValue(BASE_FILE, key) : raw);
	}

	public static Duration baseDuration(String key) {
		return toDurationOrNull(rawValue(BASE_FILE, key));
	}

	private static Duration toDurationOrNull(Object raw) {
		return raw == null ? null : ApplicationConversionService.getSharedInstance().convert(raw, Duration.class);
	}

	private static Object rawValue(String file, String key) {
		Resource resource = new ClassPathResource(file);
		if (!resource.exists()) {
			return null;
		}
		try {
			List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(file, resource);
			for (PropertySource<?> source : sources) {
				Object value = source.getProperty(key);
				if (value != null) {
					return value instanceof OriginTrackedValue tracked ? tracked.getValue() : value;
				}
			}
			return null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
