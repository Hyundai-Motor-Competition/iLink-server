package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.user.application.port.in.VehicleInfo;

import java.time.Instant;

final class LinkCodeRules {

	private LinkCodeRules() {
	}

	static boolean isUsable(LinkCode linkCode, Instant now) {
		return linkCode.getStatus() == LinkCodeStatus.ISSUED && !linkCode.isExpired(now);
	}

	static LinkMode actualModeOf(LinkCode linkCode, VehicleInfo vehicle) {
		if (linkCode.isIssuedBeforeLink()) {
			return LinkMode.NEW;
		}
		return vehicle.linked() && vehicle.familyId() != null ? LinkMode.RELINK : LinkMode.NEW;
	}
}
