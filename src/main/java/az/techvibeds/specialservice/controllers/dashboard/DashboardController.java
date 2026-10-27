package az.techvibeds.specialservice.controllers.dashboard;

import az.techvibeds.specialservice.dtos.dashboard.DashboardSummaryDto;
import az.techvibeds.specialservice.dtos.dashboard.RecentActivityDto;
import az.techvibeds.specialservice.dtos.dashboard.SalesTrendDto;
import az.techvibeds.specialservice.dtos.dashboard.WarehouseLevelDto;
import az.techvibeds.specialservice.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardSummaryDto> getSummary(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Principal principal) {
        return ResponseEntity.ok(dashboardService.getSummary(principal.getName(), filterType, startDate, endDate));
    }

    @GetMapping("/sales-trend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalesTrendDto>> getSalesTrend(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Principal principal) {
        return ResponseEntity.ok(dashboardService.getSalesTrend(principal.getName(), filterType, startDate, endDate));
    }

    @GetMapping("/warehouse-levels")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WarehouseLevelDto>> getWarehouseLevels(Principal principal) {
        return ResponseEntity.ok(dashboardService.getWarehouseLevels(principal.getName()));
    }

    @GetMapping("/top-selling-categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<az.techvibeds.specialservice.dtos.dashboard.TopSellingCategoryDto>> getTopSellingCategories(Principal principal) {
        return ResponseEntity.ok(dashboardService.getTopSellingCategories(principal.getName()));
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecentActivityDto>> getRecentActivity(Principal principal) {
        return ResponseEntity.ok(dashboardService.getRecentActivities(principal.getName()));
    }
}
