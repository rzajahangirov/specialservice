package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.manufacturing.ManufacturingManagementDto;
import az.techvibeds.specialservice.enums.ProductionOrderStatus;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.repositories.ProductionOrderRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.ManufacturingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManufacturingServiceImpl implements ManufacturingService {

    private final ProductionOrderRepository orderRepository;
    private final CompanyService companyService;

    @Override
    @Transactional(readOnly = true)
    public ManufacturingManagementDto getDatasForManufacturingPage(String userEmail) {
        log.info("Fetching manufacturing data for user: {}", userEmail);

        Company company = companyService.findByUserEmail(userEmail);
        Long companyId = company.getId();

        Integer inProductionCount = orderRepository.countByCompanyIdAndStatus(companyId, ProductionOrderStatus.IN_PRODUCTION);

        Date twentyFourHoursAgo = Date.from(Instant.now().minus(24, ChronoUnit.HOURS));
        Integer last24HoursCount = orderRepository.countByCompanyIdAndStartDateAfter(companyId, twentyFourHoursAgo);

        Integer thisMonthQty = getQuantityForPeriod(companyId, 0);
        Integer lastMonthQty = getQuantityForPeriod(companyId, 1);

        log.debug("Stats calculated - Current Month: {}, Last Month: {}", thisMonthQty, lastMonthQty);

        return ManufacturingManagementDto.builder()
                .inProductionOrderCount(inProductionCount)
                .last24HoursOrderCount(last24HoursCount)
                .monthlyProductionCount(thisMonthQty)
                .relativeToLastMonth(calculateRelativeGrowth(thisMonthQty, lastMonthQty))
                .defectiveProducts(0) //unknown
                .target(0)        //unknown
                .productionsCountsMonthly(new HashMap<>()) //unknown
                .costsCountsMonthly(new HashMap<>()) //unknown
                .build();
    }

    private Integer getQuantityForPeriod(Long companyId, int minusMonths) {
        LocalDate targetMonth = LocalDate.now().minusMonths(minusMonths);
        Date start = java.sql.Date.valueOf(targetMonth.withDayOfMonth(1));
        Date end = java.sql.Date.valueOf(targetMonth.withDayOfMonth(targetMonth.lengthOfMonth()));

        return orderRepository.getTotalQuantityBetweenDates(companyId, start, end);
    }

    private String calculateRelativeGrowth(Integer current, Integer previous) {
        if (previous == null || previous == 0) return "0.00%";
        double growth = ((double) current / previous) * 100;
        return String.format("%.2f%%", growth);
    }
}
