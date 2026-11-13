package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.pickup.application.port.in.CallGuardView;
import com.example.pickupservice.pickup.application.port.in.CallProgressView;
import com.example.pickupservice.pickup.application.port.in.SessionCallView;
import com.example.pickupservice.pickup.application.port.in.SessionChildView;
import com.example.pickupservice.pickup.application.port.in.SessionEtaView;
import com.example.pickupservice.pickup.application.port.in.SessionRouteView;
import com.example.pickupservice.pickup.application.port.in.SessionSpotView;
import com.example.pickupservice.pickup.application.port.in.SessionStatusView;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.user.domain.JoinCallMode;

import java.time.Instant;
import java.util.List;

/**
 * 설계서 「세션 뷰 (SessionView)」 응답 예시와 같은 값. 예시에 없는 children[].callNotBefore · location · readyAt · profileImageUrl · vehicleLocation · driver · route.totalMeters는 필드 표대로 넣었다.
 */
public final class SessionViewFixture {

	public static final long SESSION_ID = 1024L;
	static final int STATE_VERSION = 15;

	static final String SESSION_VIEW_JSON = """
			{
			  "sessionId": 1024, "stateVersion": 15,
			  "status": { "session": "ACTIVE", "vehicle": "APPROACHING", "call": "NONE", "spot": "CONFIRMED" },
			  "spot": { "spotId": 21, "name": "해솔영어학원 정문 앞", "spotVersion": 4, "locked": false },
			  "eta": { "stopEtaAt": "2026-11-05T18:12:00+09:00", "remainingSeconds": 720,
			           "updatedAt": "2026-11-05T18:00:05+09:00", "stale": false },
			  "call": { "mode": "AUTO", "callId": null, "held": false, "scheduledAt": "2026-11-05T18:09:00+09:00",
			            "guard": { "met": true, "unmet": [] },
			            "progress": { "acceptedAt": null, "sentAt": null, "deliveredAt": null, "acknowledgedAt": null } },
			  "children": [
			    { "childUserId": 31, "name": "민준", "phase": "WAITING", "extraPrepMinutes": 10,
			      "callNotBefore": "2026-11-05T18:10:00+09:00", "acknowledgedAt": null, "boardedAt": null, "location": null, "readyAt": null, "profileImageUrl": null },
			    { "childUserId": 32, "name": "서윤", "phase": "WAITING", "extraPrepMinutes": 0,
			      "callNotBefore": null, "acknowledgedAt": null, "boardedAt": null, "location": null, "readyAt": null, "profileImageUrl": null }
			  ],
			  "vehicleLocation": null,
			  "route": { "routeVersion": 2, "remainingMeters": 4200, "totalMeters": null }, "boarding": null, "incident": null,
			  "driver": null
			}
			""";

	private SessionViewFixture() {
	}

	public static SessionView sessionView() {
		return new SessionView(
				SESSION_ID,
				STATE_VERSION,
				new SessionStatusView(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, "NONE", SpotStatus.CONFIRMED),
				new SessionSpotView(21L, "해솔영어학원 정문 앞", 4, false),
				new SessionEtaView(
						Instant.parse("2026-11-05T09:12:00Z"), 720, Instant.parse("2026-11-05T09:00:05Z"), false),
				new SessionCallView(
						JoinCallMode.AUTO, null, false, Instant.parse("2026-11-05T09:09:00Z"),
						new CallGuardView(true, List.of()),
						new CallProgressView(null, null, null, null)),
				List.of(
						new SessionChildView(31L, "민준", ChildPhase.WAITING, 10,
								Instant.parse("2026-11-05T09:10:00Z"), null, null, null),
						new SessionChildView(32L, "서윤", ChildPhase.WAITING, 0, null, null, null, null)),
				new SessionRouteView(2, 4200),
				null,
				null);
	}

}
