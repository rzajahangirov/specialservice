package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.enums.ActivityTypeWarehouse;
import az.techvibeds.specialservice.models.WarehouseActivity;
import az.techvibeds.specialservice.repositories.WarehouseActivityRepository;
import az.techvibeds.specialservice.services.WarehouseActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseActivityServiceImpl implements WarehouseActivityService {
    private final WarehouseActivityRepository warehouseActivityRepository;

    @Override
    public String getLastTransfer(Long companyId) {
        WarehouseActivity warehouseActivity = warehouseActivityRepository.findTopByTypeAndCompanyIdOrderByDateDesc(ActivityTypeWarehouse.TRANSFER,companyId);
        if(warehouseActivity == null){
            return null;
        }
        String lastTransfer = warehouseActivity.getSourceWarehouse().getName()+"->"+warehouseActivity.getDestinationWarehouse().getName();
        return lastTransfer;
    }
}
