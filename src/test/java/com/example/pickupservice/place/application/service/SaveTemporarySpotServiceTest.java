package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.SaveTemporarySpotCommand;
import com.example.pickupservice.place.application.port.out.SaveSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaveTemporarySpotServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long ACADEMY_ID = 55L;
	private static final long DRIVER_USER_ID = 2001L;
	private static final long NEW_SPOT_ID = 77L;
	private static final String NAME = "차량 정차 위치";
	private static final LatLng STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng MEETING_POINT = new LatLng(37.501301, 127.039901);

	@Mock
	private SaveSpotPort saveSpotPort;

	private SaveTemporarySpotService saveTemporarySpotService;

	@BeforeEach
	void setUp() {
		saveTemporarySpotService = new SaveTemporarySpotService(saveSpotPort);
	}

	@Test
	@DisplayName("임시 장소 저장은 요청한 가족 · 학원 · 작성자 · 이름 · 정차 지점 · 만남 지점으로 temporary=true이고 기본 장소가 아니며 memo가 없는 PickupSpot 하나를 저장한다")
	void saveTemporarySpotSavesOneTemporaryNonDefaultSpotWithRequestedValues() {
		givenSaveAssignsNewSpotId();

		saveTemporarySpotService.saveTemporarySpot(command());

		ArgumentCaptor<PickupSpot> captor = ArgumentCaptor.forClass(PickupSpot.class);
		verify(saveSpotPort).saveSpot(captor.capture());
		PickupSpot saved = captor.getValue();
		assertThat(saved.getSpotId()).isNull();
		assertThat(saved.isTemporary()).isTrue();
		assertThat(saved.isDefault()).isFalse();
		assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(saved.getAcademyId()).isEqualTo(ACADEMY_ID);
		assertThat(saved.getName()).isEqualTo(NAME);
		assertThat(saved.getStopPoint()).isEqualTo(STOP_POINT);
		assertThat(saved.getMeetingPoint()).isEqualTo(MEETING_POINT);
		assertThat(saved.getCreatedBy()).isEqualTo(DRIVER_USER_ID);
		assertThat(saved.getMemo()).isNull();
	}

	@Test
	@DisplayName("임시 장소 저장은 저장소가 매긴 spotId를 돌려준다")
	void saveTemporarySpotReturnsSpotIdAssignedBySavePort() {
		givenSaveAssignsNewSpotId();

		long spotId = saveTemporarySpotService.saveTemporarySpot(command());

		assertThat(spotId).isEqualTo(NEW_SPOT_ID);
	}

	private void givenSaveAssignsNewSpotId() {
		willAnswer(invocation -> {
			PickupSpot spot = invocation.getArgument(0);
			return PickupSpot.restore(NEW_SPOT_ID, spot.getFamilyId(), spot.getAcademyId(), spot.getName(),
					spot.getMemo(), spot.getStopPoint(), spot.getMeetingPoint(), spot.isDefault(), spot.getCreatedBy());
		}).given(saveSpotPort).saveSpot(any(PickupSpot.class));
	}

	private static SaveTemporarySpotCommand command() {
		return new SaveTemporarySpotCommand(FAMILY_ID, ACADEMY_ID, DRIVER_USER_ID, NAME, STOP_POINT, MEETING_POINT);
	}
}
