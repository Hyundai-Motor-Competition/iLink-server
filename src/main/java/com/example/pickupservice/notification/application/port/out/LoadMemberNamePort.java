package com.example.pickupservice.notification.application.port.out;

import java.util.List;
import java.util.Map;

public interface LoadMemberNamePort {

	Map<Long, String> loadNames(List<Long> userIds);
}
