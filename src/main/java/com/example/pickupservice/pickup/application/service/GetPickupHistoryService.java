package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.GetPickupHistoryUseCase;
import com.example.pickupservice.pickup.application.port.in.HistoryItem;
import com.example.pickupservice.pickup.application.port.in.HistoryQuery;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadHistoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetPickupHistoryService implements GetPickupHistoryUseCase {

	private final LoadHistoryPort loadHistoryPort;
	private final LoadFamilyPort loadFamilyPort;

	// 주체 가족의 자녀가 아니면 NOT_FOUND. 범위는 한국 날짜로 그 달 1일 ~ 말일
	@Override
	public List<HistoryItem> history(HistoryQuery query) {
		boolean familyChild = loadFamilyPort.loadMember(query.childId())
				.filter(member -> Objects.equals(member.familyId(), query.principal().familyId()))
				.isPresent();
		if (!familyChild) {
			throw PickupErrors.notFound();
		}
		YearMonth month = query.month();
		return loadHistoryPort.loadHistory(query.childId(), month.atDay(1), month.atEndOfMonth());
	}

}
