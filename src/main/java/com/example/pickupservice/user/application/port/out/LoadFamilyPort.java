package com.example.pickupservice.user.application.port.out;

import com.example.pickupservice.user.domain.Family;

import java.util.Optional;

public interface LoadFamilyPort {

	Optional<Family> loadFamily(long familyId);
}
