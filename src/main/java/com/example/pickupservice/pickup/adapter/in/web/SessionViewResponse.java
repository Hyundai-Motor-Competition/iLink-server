package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.pickup.application.port.in.CallGuardView;
import com.example.pickupservice.pickup.application.port.in.CallProgressView;
import com.example.pickupservice.pickup.application.port.in.ChildLocationView;
import com.example.pickupservice.pickup.application.port.in.SessionBoardingView;
import com.example.pickupservice.pickup.application.port.in.SessionCallView;
import com.example.pickupservice.pickup.application.port.in.SessionChildView;
import com.example.pickupservice.pickup.application.port.in.SessionDriverView;
import com.example.pickupservice.pickup.application.port.in.SessionEtaView;
import com.example.pickupservice.pickup.application.port.in.SessionIncidentView;
import com.example.pickupservice.pickup.application.port.in.SessionRouteView;
import com.example.pickupservice.pickup.application.port.in.SessionSpotView;
import com.example.pickupservice.pickup.application.port.in.SessionStatusView;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.in.VehicleLocationView;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.user.domain.JoinCallMode;

import java.util.List;
import java.util.function.Function;

import static com.example.pickupservice.pickup.adapter.in.web.KstTime.kst;

public record SessionViewResponse(
		long sessionId,
		int stateVersion,
		SessionStatusResponse status,
		SessionSpotResponse spot,
		SessionEtaResponse eta,
		SessionCallResponse call,
		List<SessionChildResponse> children,
		VehicleLocationResponse vehicleLocation,
		SessionRouteResponse route,
		SessionBoardingResponse boarding,
		SessionIncidentResponse incident,
		SessionDriverResponse driver
) {

	public static SessionViewResponse from(SessionView view) {
		return new SessionViewResponse(
				view.sessionId(),
				view.stateVersion(),
				mapOrNull(view.status(), SessionStatusResponse::from),
				mapOrNull(view.spot(), SessionSpotResponse::from),
				mapOrNull(view.eta(), SessionEtaResponse::from),
				mapOrNull(view.call(), SessionCallResponse::from),
				mapOrNull(view.children(), children -> children.stream().map(SessionChildResponse::from).toList()),
				mapOrNull(view.vehicleLocation(), VehicleLocationResponse::from),
				mapOrNull(view.route(), SessionRouteResponse::from),
				mapOrNull(view.boarding(), SessionBoardingResponse::from),
				mapOrNull(view.incident(), SessionIncidentResponse::from),
				mapOrNull(view.driver(), SessionDriverResponse::from));
	}

	public record SessionStatusResponse(SessionStatus session, VehiclePhase vehicle, String call, SpotStatus spot) {

		static SessionStatusResponse from(SessionStatusView status) {
			return new SessionStatusResponse(status.session(), status.vehicle(), status.call(), status.spot());
		}
	}

	public record SessionSpotResponse(long spotId, String name, int spotVersion, boolean locked) {

		static SessionSpotResponse from(SessionSpotView spot) {
			return new SessionSpotResponse(spot.spotId(), spot.name(), spot.spotVersion(), spot.locked());
		}
	}

	public record SessionEtaResponse(String stopEtaAt, Integer remainingSeconds, String updatedAt, boolean stale) {

		static SessionEtaResponse from(SessionEtaView eta) {
			return new SessionEtaResponse(kst(eta.stopEtaAt()), eta.remainingSeconds(), kst(eta.updatedAt()),
					eta.stale());
		}
	}

	public record SessionCallResponse(
			JoinCallMode mode,
			Long callId,
			boolean held,
			String scheduledAt,
			CallGuardResponse guard,
			CallProgressResponse progress
	) {

		static SessionCallResponse from(SessionCallView call) {
			return new SessionCallResponse(call.mode(), call.callId(), call.held(), kst(call.scheduledAt()),
					mapOrNull(call.guard(), CallGuardResponse::from),
					mapOrNull(call.progress(), CallProgressResponse::from));
		}
	}

	public record CallGuardResponse(boolean met, List<String> unmet) {

		static CallGuardResponse from(CallGuardView guard) {
			return new CallGuardResponse(guard.met(), guard.unmet());
		}
	}

	public record CallProgressResponse(String acceptedAt, String sentAt, String deliveredAt, String acknowledgedAt) {

		static CallProgressResponse from(CallProgressView progress) {
			return new CallProgressResponse(kst(progress.acceptedAt()), kst(progress.sentAt()),
					kst(progress.deliveredAt()), kst(progress.acknowledgedAt()));
		}
	}

	public record SessionChildResponse(
			long childUserId,
			String name,
			ChildPhase phase,
			int extraPrepMinutes,
			String callNotBefore,
			String acknowledgedAt,
			String boardedAt,
			ChildLocationResponse location,
			String readyAt,
			String profileImageUrl
	) {

		static SessionChildResponse from(SessionChildView child) {
			return new SessionChildResponse(child.childUserId(), child.name(), child.phase(),
					child.extraPrepMinutes(), kst(child.callNotBefore()), kst(child.acknowledgedAt()),
					kst(child.boardedAt()), mapOrNull(child.location(), ChildLocationResponse::from),
					kst(child.readyAt()), child.profileImageUrl());
		}
	}

	public record ChildLocationResponse(double lat, double lng, String at) {

		static ChildLocationResponse from(ChildLocationView location) {
			return new ChildLocationResponse(location.lat(), location.lng(), kst(location.at()));
		}
	}

	public record VehicleLocationResponse(double lat, double lng, String at) {

		static VehicleLocationResponse from(VehicleLocationView location) {
			return new VehicleLocationResponse(location.lat(), location.lng(), kst(location.at()));
		}
	}

	public record SessionRouteResponse(int routeVersion, Integer remainingMeters, Integer totalMeters) {

		static SessionRouteResponse from(SessionRouteView route) {
			return new SessionRouteResponse(route.routeVersion(), route.remainingMeters(), route.totalMeters());
		}
	}

	public record SessionBoardingResponse(String detectedAt, String cancelableUntil, String door) {

		static SessionBoardingResponse from(SessionBoardingView boarding) {
			return new SessionBoardingResponse(kst(boarding.detectedAt()), kst(boarding.cancelableUntil()),
					boarding.door());
		}
	}

	public record SessionIncidentResponse(String type, long childUserId, String openedAt) {

		static SessionIncidentResponse from(SessionIncidentView incident) {
			return new SessionIncidentResponse(incident.type(), incident.childUserId(), kst(incident.openedAt()));
		}
	}

	public record SessionDriverResponse(String name, String profileImageUrl) {

		static SessionDriverResponse from(SessionDriverView driver) {
			return new SessionDriverResponse(driver.name(), driver.profileImageUrl());
		}
	}

	private static <T, R> R mapOrNull(T value, Function<T, R> mapper) {
		return value == null ? null : mapper.apply(value);
	}

}
