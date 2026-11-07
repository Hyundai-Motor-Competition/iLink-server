package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.IssueLinkCodeCommand;
import com.example.pickupservice.auth.application.port.in.LinkCodeResult;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.application.port.out.SaveLinkCodePort;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.common.error.BusinessException;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueLinkCodeServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-07T03:00:00Z");
	private static final long DEVICE_ID = 601L;
	private static final long VEHICLE_ID = 77L;
	private static final long OTHER_VEHICLE_ID = 78L;
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
		lenient().when(saveLinkCodePort.saveLinkCode(any(LinkCode.class))).thenAnswer(invocation -> {
			LinkCode code = invocation.getArgument(0);
			LinkCode saved = code.getCodeId() != null ? code
					: LinkCode.restore(nextCodeId.getAndIncrement(), code.getVehicleId(), code.getCode(),
							code.getStatus(), code.getExpiresAt(), code.getUsedAt(), code.getMode(),
							code.getTokenDeliveredAt());
			savedCodes.add(saved);
			return saved;
		});
	}

	@Test
	@DisplayName("연결 코드 발급은 그 차량의 이전 ISSUED 코드를 EXPIRED로 바꾼다")
	void issueLinkCodeExpiresPreviousIssuedCodesOfVehicle() {
		LinkCode previousFirst = LinkCode.restore(5001L, VEHICLE_ID, "AAAA1111", LinkCodeStatus.ISSUED,
				NOW.plusSeconds(120), null, null, null);
		LinkCode previousSecond = LinkCode.restore(5002L, VEHICLE_ID, "BBBB2222", LinkCodeStatus.ISSUED,
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
	@DisplayName("연결 코드 발급 횟수는 차량마다 따로 센다")
	void issueLinkCodeCountsIssuesPerVehicle() {
		for (int i = 0; i < 5; i++) {
			issueLinkCodeService.issueLinkCode(command(VEHICLE_ID));
		}

		LinkCodeResult otherVehicleResult = issueLinkCodeService.issueLinkCode(command(OTHER_VEHICLE_ID));

		assertThat(otherVehicleResult.reissuesLeft()).isEqualTo(4);
	}

	private static IssueLinkCodeCommand command(long vehicleId) {
		return new IssueLinkCodeCommand(DEVICE_ID, vehicleId, null);
	}

}
