package com.example.pickupservice.common.time;

import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

	public TimeZoneConfig() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

}
