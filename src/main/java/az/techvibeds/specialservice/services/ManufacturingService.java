package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.manufacturing.ManufacturingManagementDto;

public interface ManufacturingService {
    ManufacturingManagementDto getDatasForManufacturingPage(String name);
}
