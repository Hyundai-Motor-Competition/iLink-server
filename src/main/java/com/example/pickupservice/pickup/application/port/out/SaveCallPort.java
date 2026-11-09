package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.pickup.domain.Call;

public interface SaveCallPort {

	Call saveCall(Call call);

}
