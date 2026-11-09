package com.example.pickupservice.pickup.application.port.in;

import java.util.List;

public interface GetContactsUseCase {

	List<ContactResult> getContacts(SessionQuery query);

}
