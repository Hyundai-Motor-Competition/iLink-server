package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.IssueLinkCodeCommand;
import com.example.pickupservice.auth.application.port.in.LinkCodeResult;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.application.port.out.SaveLinkCodePort;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueLinkCodeServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-07T03:00:00Z");
	private static final long DEVICE_ID = 601L;
	private static final long OTHER_DEVICE_ID = 602L;
	private static final long OTHER_VEHICLE_DEVICE_ID = 701L;
	private static final long VEHICLE_ID = 77L;
	private static final long OTHER_VEHICLE_ID = 78L;
	private static final long FAMILY_ID = 12L;
	private static final String NEW_CODE = "K7P2QX9M";

	@Mock
	private LoadLinkCodePort loadLinkCodePort;

	@Mock
	private SaveLinkCodePort saveLinkCodePort;

	@Mock
	private LinkCodeGenerator linkCodeGenerator;

	@Spy
	private LinkCodeIssueCounter linkCodeIssueCounter = new LinkCodeIssueCounter();

	@Mock
	private LoadVehicleUseCase loadVehicleUseCase;

	@Mock
	private Clock clock;

	@InjectMocks
	private IssueLinkCodeService issueLinkCodeService;

	private final AtomicLong nextCodeId = new AtomicLong(5012L);
	private final List<LinkCode> savedCodes = new ArrayList<>();

	@BeforeEach
	void setUp() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		lenient().when(linkCodeGenerator.generate()).thenReturn(NEW_CODE);
		lenient().when(loadVehicleUseCase.findVehicle(anyLong()))
				.thenAnswer(invocation -> Optional.of(pendingVehicle(invocation.getArgument(0))));
		lenient().when(saveLinkCodePort.saveLinkCode(any(LinkCode.class))).thenAnswer(invocation -> {
			LinkCode code = invocation.getArgument(0);
			LinkCode saved = code.getCodeId() != null ? code
					: LinkCode.restore(nextCodeId.getAndIncrement(), code.getVehicleId(), code.getIssuedDeviceId(), code.isIssuedBeforeLink(),
							code.getCode(), code.getStatus(), code.getExpiresAt(), code.getUsedAt(), code.getMode(),
							code.getTokenDeliveredAt());
			savedCodes.add(saved);
			return saved;
		});
	}

	@Test
	@DisplayName("연결 코드 발급은 같은 기기가 발급한 이전 ISSUED 코드를 EXPIRED로 바꾼다")
	void issueLinkCodeExpiresPreviousIssuedCodesOfSameDevice() {
		LinkCode previousFirst = LinkCode.restore(5001L, VEHICLE_ID, DEVICE_ID, false, "AAAA1111", LinkCodeStatus.ISSUED,
				NOW.plusSeconds(120), null, null, null);
		LinkCode previousSecond = LinkCode.restore(5002L, VEHICLE_ID, DEVICE_ID, false, "BBBB2222", LinkCodeStatus.ISSUED,
				NOW.plusSeconds(240), null, null, null);
		when(loadLinkCodePort.loadIssuedCodes(VEHICLE_ID)).thenReturn(List.of(previousFirst, previousSecond));

		issueLinkCodeService.issueLinkCode(command(VEHICLE_ID));

		ArgumentCaptor<LinkCode> saved = ArgumentCaptor.forClass(LinkCode.class);
		verify(saveLinkCodePort, times(3)).saveLinkCode(saved.capture());
		List<LinkCode> savedValues = saved.getAllValues();

		assertThat(savedValues).filteredOn(code -> code.getCodeId() != null)
				.extracting(LinkCode::getCodeId, LinkCode::getStatus)
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple(5001L, LinkCodeStatus.EXPIRED),
						org.assertj.core.groups.Tuple.tuple(5002L, LinkCodeStatus.EXPIRED));

		assertThat(savedValues).filteredOn(code -> code.getCodeId() == null)
				.singleElement()
				.satisfies(code -> {
					assertThat(code.getVehicleId()).isEqualTo(VEHICLE_ID);
					assertThat(code.getCode()).isEqualTo(NEW_CODE);
					assertThat(code.getStatus()).isEqualTo(LinkCodeStatus.ISSUED);
					assertThat(code.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
				});
	}

	@Test
	@DisplayName("연결 코드 발급은 첫 발급 때 reissuesLeft 4를 돌려준다")
	void issueLinkCodeReturnsFourReissuesLeftOnFirstIssue() {
		LinkCodeResult result = issueLinkCodeService.issueLinkCode(command(VEHICLE_ID));

		assertThat(result.reissuesLeft()).isEqualTo(4);
		assertThat(result.codeId()).isEqualTo(5012L);
		assertThat(result.code()).isEqualTo(NEW_CODE);
		assertThat(result.qrPayload()).isEqualTo("pickup://link?code=" + NEW_CODE);
		assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
	}

	@Test
	@DisplayName("연결 코드 발급은 발급할 때마다 reissuesLeft가 1씩 줄어 다섯 번째에 0이 된다")
	void issueLinkCodeDecreasesReissuesLeftOnEachIssue() {
		List<Integer> reissuesLeft = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			reissuesLeft.add(issueLinkCodeService.issueLinkCode(command(VEHICLE_ID)).reissuesLeft());
		}

		assertThat(reissuesLeft).containsExactly(4, 3, 2, 1, 0);
	}

	@Test
	@DisplayName("연결 코드를 연속 6번째로 발급하면 LINK_CODE_REISSUE_LIMIT으로 거절한다")
	void issueLinkCodeRejectsSixthConsecutiveIssue() {
		for (int i = 0; i < 5; i++) {
			issueLinkCodeService.issueLinkCode(command(VEHICLE_ID));
		}
		int savedBeforeSixth = savedCodes.size();

		assertThatThrownBy(() -> issueLinkCodeService.issueLinkCode(command(VEHICLE_ID)))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
					assertThat(e.code()).isEqualTo("LINK_CODE_REISSUE_LIMIT");
				});

		assertThat(savedCodes).hasSize(savedBeforeSixth);
	}

	@Test
	@DisplayName("연결 코드 발급 횟수는 기기(deviceId) 단위로 센다")
	void issueLinkCodeCountsIssuesPerDevice() {
		for (int i = 0; i < 5; i++) {
			issueLinkCodeService.issueLinkCode(new IssueLinkCodeCommand(OTHER_DEVICE_ID, VEHICLE_ID, null));
		}

		LinkCodeResult ownerDeviceResult = issueLinkCodeService.issueLinkCode(
				new IssueLinkCodeCommand(DEVICE_ID, VEHICLE_ID, null));

		assertThat(ownerDeviceResult.reissuesLeft()).isEqualTo(4);
		assertThatThrownBy(() -> issueLinkCodeService.issueLinkCode(
				new IssueLinkCodeCommand(OTHER_DEVICE_ID, VEHICLE_ID, null)))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.code()).isEqualTo("LINK_CODE_REISSUE_LIMIT"));
	}

	@Test
	@DisplayName("연결 코드 발급 횟수는 다른 차량의 다른 기기와 따로 센다")
	void issueLinkCodeCountsIssuesSeparatelyForDeviceOfOtherVehicle() {
		for (int i = 0; i < 5; i++) {
			issueLinkCodeService.issueLinkCode(command(VEHICLE_ID));
		}

		LinkCodeResult otherVehicleResult = issueLinkCodeService.issueLinkCode(
				new IssueLinkCodeCommand(OTHER_VEHICLE_DEVICE_ID, OTHER_VEHICLE_ID, null));

		assertThat(otherVehicleResult.reissuesLeft()).isEqualTo(4);
	}

	@Test
	@DisplayName("연결 코드 발급은 같은 기기가 발급한 이전 ISSUED 코드만 EXPIRED로 바꾼다")
	void issueLinkCodeExpiresOnlyPreviousIssuedCodesOfSameDevice() {
		LinkCode sameDeviceCode = LinkCode.restore(5001L, VEHICLE_ID, DEVICE_ID, false, "AAAA1111",
				LinkCodeStatus.ISSUED, NOW.plusSeconds(120), null, null, null);
		LinkCode otherDeviceCode = LinkCode.restore(5002L, VEHICLE_ID, OTHER_DEVICE_ID, true, "BBBB2222",
				LinkCodeStatus.ISSUED, NOW.plusSeconds(240), null, null, null);
		when(loadLinkCodePort.loadIssuedCodes(VEHICLE_ID)).thenReturn(List.of(sameDeviceCode, otherDeviceCode));

		issueLinkCodeService.issueLinkCode(new IssueLinkCodeCommand(DEVICE_ID, VEHICLE_ID, FAMILY_ID));

		assertThat(sameDeviceCode.getStatus()).isEqualTo(LinkCodeStatus.EXPIRED);
		assertThat(otherDeviceCode.getStatus()).as("다른 기기가 발급한 코드는 그대로").isEqualTo(LinkCodeStatus.ISSUED);
		ArgumentCaptor<LinkCode> saved = ArgumentCaptor.forClass(LinkCode.class);
		verify(saveLinkCodePort, times(2)).saveLinkCode(saved.capture());
		assertThat(saved.getAllValues()).filteredOn(code -> code.getCodeId() != null)
				.extracting(LinkCode::getCodeId)
				.containsExactly(5001L);
		assertThat(saved.getAllValues()).filteredOn(code -> code.getCodeId() == null)
				.singleElement()
				.satisfies(code -> assertThat(code.getIssuedDeviceId()).isEqualTo(DEVICE_ID));
	}

	@Test
	@DisplayName("연결 전 토큰으로 발급한 코드는 issuedBeforeLink true로 저장한다")
	void issueLinkCodeWithPreLinkTokenSavesCodeIssuedBeforeLink() {
		issueLinkCodeService.issueLinkCode(new IssueLinkCodeCommand(DEVICE_ID, VEHICLE_ID, null));

		ArgumentCaptor<LinkCode> saved = ArgumentCaptor.forClass(LinkCode.class);
		verify(saveLinkCodePort).saveLinkCode(saved.capture());
		assertThat(saved.getValue().getCodeId()).isNull();
		assertThat(saved.getValue().isIssuedBeforeLink()).isTrue();
	}

	@Test
	@DisplayName("LINK-01: 발급 기기 정보가 없는 옛 ISSUED 코드(issuedDeviceId null)는 같은 차량에서 새 코드를 발급할 때 함께 EXPIRED가 된다")
	void issueLinkCodeAlsoExpiresLegacyIssuedCodeWithoutIssuedDeviceId() {
		LinkCode legacyCode = LinkCode.restore(5000L, VEHICLE_ID, null, false, "LEGACY00",
				LinkCodeStatus.ISSUED, NOW.plusSeconds(60), null, null, null);
		LinkCode sameDeviceCode = LinkCode.restore(5001L, VEHICLE_ID, DEVICE_ID, false, "AAAA1111",
				LinkCodeStatus.ISSUED, NOW.plusSeconds(120), null, null, null);
		LinkCode otherDeviceCode = LinkCode.restore(5002L, VEHICLE_ID, OTHER_DEVICE_ID, true, "BBBB2222",
				LinkCodeStatus.ISSUED, NOW.plusSeconds(240), null, null, null);
		when(loadLinkCodePort.loadIssuedCodes(VEHICLE_ID))
				.thenReturn(List.of(legacyCode, sameDeviceCode, otherDeviceCode));

		issueLinkCodeService.issueLinkCode(new IssueLinkCodeCommand(DEVICE_ID, VEHICLE_ID, FAMILY_ID));

		assertThat(legacyCode.getStatus()).as("발급 기기 정보가 없는 옛 코드도 만료").isEqualTo(LinkCodeStatus.EXPIRED);
		assertThat(sameDeviceCode.getStatus()).isEqualTo(LinkCodeStatus.EXPIRED);
		assertThat(otherDeviceCode.getStatus()).as("다른 기기가 발급한 코드는 그대로").isEqualTo(LinkCodeStatus.ISSUED);
		ArgumentCaptor<LinkCode> saved = ArgumentCaptor.forClass(LinkCode.class);
		verify(saveLinkCodePort, times(3)).saveLinkCode(saved.capture());
		assertThat(saved.getAllValues()).filteredOn(code -> code.getCodeId() != null)
				.extracting(LinkCode::getCodeId, LinkCode::getStatus)
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple(5000L, LinkCodeStatus.EXPIRED),
						org.assertj.core.groups.Tuple.tuple(5001L, LinkCodeStatus.EXPIRED));
		assertThat(saved.getAllValues()).filteredOn(code -> code.getCodeId() == null)
				.singleElement()
				.satisfies(code -> {
					assertThat(code.getIssuedDeviceId()).isEqualTo(DEVICE_ID);
					assertThat(code.getStatus()).isEqualTo(LinkCodeStatus.ISSUED);
				});
	}

	@Test
	@DisplayName("연결된 토큰(familyId 있음)으로 발급한 코드는 issuedBeforeLink false로 저장한다")
	void issueLinkCodeWithLinkedTokenSavesCodeNotIssuedBeforeLink() {
		lenient().when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(linkedVehicle(VEHICLE_ID)));

		issueLinkCodeService.issueLinkCode(new IssueLinkCodeCommand(DEVICE_ID, VEHICLE_ID, FAMILY_ID));

		ArgumentCaptor<LinkCode> saved = ArgumentCaptor.forClass(LinkCode.class);
		verify(saveLinkCodePort).saveLinkCode(saved.capture());
		assertThat(saved.getValue().getCodeId()).isNull();
		assertThat(saved.getValue().isIssuedBeforeLink()).isFalse();
	}

	@Test
	@DisplayName("연결 코드 발급은 연결 전 토큰(familyId 없음)인데 그 차량이 이미 가족에 연결돼 있으면 VEHICLE_ALREADY_LINKED로 거절한다")
	void issueLinkCodeWithPreLinkTokenRejectsVehicleAlreadyLinkedToFamily() {
		lenient().when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(linkedVehicle(VEHICLE_ID)));

		assertThatThrownBy(() -> issueLinkCodeService.issueLinkCode(command(VEHICLE_ID)))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(e.code()).isEqualTo("VEHICLE_ALREADY_LINKED");
				});

		verify(loadVehicleUseCase).findVehicle(VEHICLE_ID);
		verify(saveLinkCodePort, never()).saveLinkCode(any());
		verify(linkCodeIssueCounter, never()).increment(anyLong());
		assertThat(savedCodes).isEmpty();
		assertThat(linkCodeIssueCounter.remaining(DEVICE_ID)).isEqualTo(5);
	}

	@Test
	@DisplayName("연결 코드 발급은 연결 전 토큰이고 차량도 연결 대기(familyId 없음)면 정상 발급한다")
	void issueLinkCodeWithPreLinkTokenAndPendingVehicleIssuesCode() {
		lenient().when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(pendingVehicle(VEHICLE_ID)));

		LinkCodeResult result = issueLinkCodeService.issueLinkCode(command(VEHICLE_ID));

		assertThat(result.code()).isEqualTo(NEW_CODE);
		assertThat(result.reissuesLeft()).isEqualTo(4);
		assertThat(savedCodes).singleElement().satisfies(code -> {
			assertThat(code.getVehicleId()).isEqualTo(VEHICLE_ID);
			assertThat(code.getStatus()).isEqualTo(LinkCodeStatus.ISSUED);
		});
	}

	@Test
	@DisplayName("연결 코드 발급은 연결된 토큰(familyId 있음)이면 연결된 차량이어도 RELINK용 코드를 정상 발급한다")
	void issueLinkCodeWithLinkedTokenAndLinkedVehicleIssuesCode() {
		lenient().when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(linkedVehicle(VEHICLE_ID)));

		LinkCodeResult result = issueLinkCodeService.issueLinkCode(
				new IssueLinkCodeCommand(DEVICE_ID, VEHICLE_ID, FAMILY_ID));

		assertThat(result.code()).isEqualTo(NEW_CODE);
		assertThat(result.reissuesLeft()).isEqualTo(4);
		assertThat(savedCodes).singleElement().satisfies(code -> {
			assertThat(code.getVehicleId()).isEqualTo(VEHICLE_ID);
			assertThat(code.getStatus()).isEqualTo(LinkCodeStatus.ISSUED);
		});
	}

	@Test
	@DisplayName("연결 코드 발급은 저장하는 코드에 명령의 deviceId를 issuedDeviceId로 기록한다")
	void issueLinkCodeRecordsCommandDeviceIdAsIssuedDeviceId() {
		issueLinkCodeService.issueLinkCode(command(VEHICLE_ID));

		ArgumentCaptor<LinkCode> saved = ArgumentCaptor.forClass(LinkCode.class);
		verify(saveLinkCodePort).saveLinkCode(saved.capture());
		assertThat(saved.getValue().getCodeId()).isNull();
		assertThat(saved.getValue().getIssuedDeviceId()).isEqualTo(DEVICE_ID);
		assertThat(savedCodes).singleElement()
				.satisfies(code -> assertThat(code.getIssuedDeviceId()).isEqualTo(DEVICE_ID));
	}

	@Test
	@DisplayName("LINK-01: 연결이 해제된 차량 행(unlinkedAt 있음)의 기기가 연결 전 토큰(familyId 없음)으로 코드를 발급하면 409 VEHICLE_UNLINKED로 거절한다")
	void issueLinkCodeWithPreLinkTokenRejectsUnlinkedVehicle() {
		lenient().when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(unlinkedVehicle(VEHICLE_ID)));

		assertThatThrownBy(() -> issueLinkCodeService.issueLinkCode(new IssueLinkCodeCommand(DEVICE_ID, VEHICLE_ID, null)))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(e.code()).isEqualTo("VEHICLE_UNLINKED");
				});

		verify(loadVehicleUseCase).findVehicle(VEHICLE_ID);
		verify(saveLinkCodePort, never()).saveLinkCode(any());
		verify(linkCodeIssueCounter, never()).increment(anyLong());
		assertThat(savedCodes).isEmpty();
		assertThat(linkCodeIssueCounter.remaining(DEVICE_ID)).isEqualTo(5);
	}

	@Test
	@DisplayName("LINK-01: 연결이 해제된 차량 행(unlinkedAt 있음)의 기기가 연결된 토큰(familyId 있음)으로 코드를 발급하면 409 VEHICLE_UNLINKED로 거절한다")
	void issueLinkCodeWithLinkedTokenRejectsUnlinkedVehicle() {
		lenient().when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(unlinkedVehicle(VEHICLE_ID)));
		LinkCode previousCode = LinkCode.restore(5001L, VEHICLE_ID, DEVICE_ID, false, "AAAA1111",
				LinkCodeStatus.ISSUED, NOW.plusSeconds(120), null, null, null);
		lenient().when(loadLinkCodePort.loadIssuedCodes(VEHICLE_ID)).thenReturn(List.of(previousCode));

		assertThatThrownBy(() -> issueLinkCodeService.issueLinkCode(new IssueLinkCodeCommand(DEVICE_ID, VEHICLE_ID, FAMILY_ID)))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(e.code()).isEqualTo("VEHICLE_UNLINKED");
				});

		verify(loadVehicleUseCase).findVehicle(VEHICLE_ID);
		verify(saveLinkCodePort, never()).saveLinkCode(any());
		verify(linkCodeIssueCounter, never()).increment(anyLong());
		assertThat(savedCodes).isEmpty();
		assertThat(linkCodeIssueCounter.remaining(DEVICE_ID)).isEqualTo(5);
		assertThat(previousCode.getStatus()).as("거절되면 이전 코드도 그대로").isEqualTo(LinkCodeStatus.ISSUED);
	}

	private static IssueLinkCodeCommand command(long vehicleId) {
		return new IssueLinkCodeCommand(DEVICE_ID, vehicleId, null);
	}

	private static VehicleInfo pendingVehicle(long vehicleId) {
		return new VehicleInfo(vehicleId, null, "KMHJ381ABNU123456", "아이오닉 5", null, true);
	}

	private static VehicleInfo linkedVehicle(long vehicleId) {
		return new VehicleInfo(vehicleId, FAMILY_ID, "KMHJ381ABNU123456", "아이오닉 5", "12가3456", true);
	}

	private static VehicleInfo unlinkedVehicle(long vehicleId) {
		return new VehicleInfo(vehicleId, FAMILY_ID, "KMHJ381ABNU123456", "아이오닉 5", "12가3456", false);
	}

}
