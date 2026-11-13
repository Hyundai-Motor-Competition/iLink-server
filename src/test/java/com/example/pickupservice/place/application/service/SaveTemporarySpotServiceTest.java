package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.error.ErrorResponse.FieldError;
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
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaveTemporarySpotServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long ACADEMY_ID = 55L;
	private static final long SAVED_SPOT_ID = 77L;

	private static final LatLng STOP = new LatLng(37.501400, 127.039950);
	private static final LatLng MEETING = new LatLng(37.501301, 127.039901);
	private static final LatLng FAR_STOP = new LatLng(37.510234, 127.039876);
	private static final LatLng FAR_MEETING = new LatLng(37.510300, 127.039900);

	@Mock
	private SaveSpotPort saveSpotPort;

	private SaveTemporarySpotService saveTemporarySpotService;

	@BeforeEach
	void setUp() {
		saveTemporarySpotService = new SaveTemporarySpotService(saveSpotPort);
	}

	@Test
	@DisplayName("임시 장소 저장은 받은 familyId · academyId · name · stopPoint · meetingPoint 그대로 temporary true · isDefault false인 PickupSpot을 저장하고 저장 결과의 spotId를 돌려준다")
	void saveTemporarySpotSavesTemporaryNonDefaultSpotWithGivenValuesAndReturnsSavedSpotId() {
		givenSaveAssignsSpotId();

		long spotId = saveTemporarySpotService.saveTemporarySpot(
				new SaveTemporarySpotCommand(FAMILY_ID, ACADEMY_ID, "정문 앞 추천", STOP, MEETING));

		assertThat(spotId).as("저장 결과의 spotId").isEqualTo(SAVED_SPOT_ID);
		PickupSpot saved = captureSavedSpot();
		assertThat(saved.getSpotId()).as("새로 저장하는 장소").isNull();
		assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(saved.getAcademyId()).isEqualTo(ACADEMY_ID);
		assertThat(saved.getName()).isEqualTo("정문 앞 추천");
		assertThat(saved.getStopPoint()).isEqualTo(STOP);
		assertThat(saved.getMeetingPoint()).isEqualTo(MEETING);
		assertThat(saved.isTemporary()).as("temporary").isTrue();
		assertThat(saved.isDefault()).as("isDefault").isFalse();
		assertThat(saved.getMemo()).as("memo").isNull();
		assertThat(saved.getCreatedBy()).as("차량 토큰엔 사용자가 없어 createdBy 0").isZero();
	}

	@Test
	@DisplayName("임시 장소 저장은 정차 · 만남 지점이 학원에서 500m를 넘어도 거절하지 않고 그대로 저장한다")
	void saveTemporarySpotSavesSpotEvenWhenPointsAreFartherThan500mFromAcademy() {
		givenSaveAssignsSpotId();

		long spotId = saveTemporarySpotService.saveTemporarySpot(
				new SaveTemporarySpotCommand(FAMILY_ID, ACADEMY_ID, "여기로 부르기", FAR_STOP, FAR_MEETING));

		assertThat(spotId).isEqualTo(SAVED_SPOT_ID);
		PickupSpot saved = captureSavedSpot();
		assertThat(saved.getStopPoint()).isEqualTo(FAR_STOP);
		assertThat(saved.getMeetingPoint()).isEqualTo(FAR_MEETING);
		assertThat(saved.isTemporary()).isTrue();
		assertThat(saved.isDefault()).isFalse();
	}

	@Test
	@DisplayName("임시 장소 저장은 name이 정확히 50자면 저장한다")
	void saveTemporarySpotSavesWhenNameIsExactly50Chars() {
		givenSaveAssignsSpotId();
		String name = "가".repeat(50);

		long spotId = saveTemporarySpotService.saveTemporarySpot(
				new SaveTemporarySpotCommand(FAMILY_ID, ACADEMY_ID, name, STOP, MEETING));

		assertThat(spotId).isEqualTo(SAVED_SPOT_ID);
		assertThat(captureSavedSpot().getName()).isEqualTo(name);
	}

	@Test
	@DisplayName("임시 장소 저장은 name이 51자면 VALIDATION_FAILED(fields name)로 거절하고 저장하지 않는다")
	void saveTemporarySpotRejectsWithValidationFailedWhenNameIs51Chars() {
		assertRejectedWithNameField("가".repeat(51));
	}

	@Test
	@DisplayName("임시 장소 저장은 name이 null이면 VALIDATION_FAILED(fields name)로 거절하고 저장하지 않는다")
	void saveTemporarySpotRejectsWithValidationFailedWhenNameIsNull() {
		assertRejectedWithNameField(null);
	}

	@Test
	@DisplayName("임시 장소 저장은 name이 빈 문자열이면 VALIDATION_FAILED(fields name)로 거절하고 저장하지 않는다")
	void saveTemporarySpotRejectsWithValidationFailedWhenNameIsEmpty() {
		assertRejectedWithNameField("");
	}

	@Test
	@DisplayName("임시 장소 저장은 name이 공백뿐이면 VALIDATION_FAILED(fields name)로 거절하고 저장하지 않는다")
	void saveTemporarySpotRejectsWithValidationFailedWhenNameIsBlank() {
		assertRejectedWithNameField("   ");
	}

	private void assertRejectedWithNameField(String name) {
		Throwable thrown = catchThrowable(() -> saveTemporarySpotService.saveTemporarySpot(
				new SaveTemporarySpotCommand(FAMILY_ID, ACADEMY_ID, name, STOP, MEETING)));

		assertThat(thrown).isInstanceOf(BusinessException.class);
		BusinessException exception = (BusinessException) thrown;
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
		assertThat(exception.fields()).isNotNull();
		assertThat(exception.fields()).extracting(FieldError::field).containsExactly("name");
		verify(saveSpotPort, never()).saveSpot(any());
	}

	private void givenSaveAssignsSpotId() {
		willAnswer(invocation -> {
			PickupSpot spot = invocation.getArgument(0);
			return PickupSpot.restore(SAVED_SPOT_ID, spot.getFamilyId(), spot.getAcademyId(), spot.getName(),
					spot.getMemo(), spot.getStopPoint(), spot.getMeetingPoint(), spot.isDefault(),
					spot.getCreatedBy());
		}).given(saveSpotPort).saveSpot(any(PickupSpot.class));
	}

	private PickupSpot captureSavedSpot() {
		ArgumentCaptor<PickupSpot> captor = ArgumentCaptor.forClass(PickupSpot.class);
		verify(saveSpotPort, times(1)).saveSpot(captor.capture());
		return captor.getValue();
	}
}
