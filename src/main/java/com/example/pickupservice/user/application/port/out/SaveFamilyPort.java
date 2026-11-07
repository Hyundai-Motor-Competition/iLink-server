package com.example.pickupservice.user.application.port.out;

import com.example.pickupservice.user.domain.Family;

public interface SaveFamilyPort {

	Family saveFamily(Family family);
}
