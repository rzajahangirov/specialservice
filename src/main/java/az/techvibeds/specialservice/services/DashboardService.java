package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.dashboard.DashboardSummaryDto;
import az.techvibeds.specialservice.dtos.dashboard.RecentActivityDto;
import az.techvibeds.specialservice.dtos.dashboard.SalesTrendDto;
import az.techvibeds.specialservice.dtos.dashboard.WarehouseLevelDto;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    DashboardSummaryDto getSummary(String userEmail, String filterType, LocalDate startDate, LocalDate endDate);
    List<SalesTrendDto> getSalesTrend(String userEmail, String filterType, LocalDate startDate, LocalDate endDate);
    List<WarehouseLevelDto> getWarehouseLevels(String userEmail);
    List<az.techvibeds.specialservice.dtos.dashboard.TopSellingCategoryDto> getTopSellingCategories(String userEmail);
    List<RecentActivityDto> getRecentActivities(String userEmail);
}
