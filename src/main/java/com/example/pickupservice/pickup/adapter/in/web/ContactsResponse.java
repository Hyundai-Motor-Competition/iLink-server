package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.pickup.application.port.in.ContactResult;

import java.util.List;

public record ContactsResponse(
		List<ContactResponse> items
) {

	public static ContactsResponse from(List<ContactResult> contacts) {
		return new ContactsResponse(contacts.stream()
				.map(contact -> new ContactResponse(contact.name(), contact.role(), contact.phone()))
				.toList());
	}

	public record ContactResponse(
			String name,
			String role,
			String phone
	) {
	}

}
