package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialCreateDto;
import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialReadDto;
import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialUseDto;
import az.techvibeds.specialservice.enums.ActivityTypeWarehouse;
import az.techvibeds.specialservice.enums.InvoiceStatus;
import az.techvibeds.specialservice.enums.InvoiceType;
import az.techvibeds.specialservice.enums.ProductStatus;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.ConstructionProjectMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConstructionProjectMaterialServiceImpl implements ConstructionProjectMaterialService {
    private final ConstructionProjectMaterialRepository materialRepository;
    private final ConstructionProjectRepository projectRepository;
    private final WarehouseProductRepository warehouseProductRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseActivityRepository warehouseActivityRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceSequenceRepository invoiceSequenceRepository;
    private final PartnerRepository partnerRepository;
    private final CompanyService companyService;

    @Override
    @Transactional
    public ConstructionProjectMaterialReadDto addRequiredMaterial(
            Long projectId, ConstructionProjectMaterialCreateDto dto, String email) {
        Company company = companyService.findByUserEmail(email);
        ConstructionProject project = findProjectAndValidate(projectId, company);
        WarehouseProduct warehouseProduct = findWarehouseProduct(dto.getProductCode(), dto.getWarehouseId(), company);

        ConstructionProjectMaterial material = materialRepository
                .findByProject_IdAndProduct_ProductCodeAndWarehouse_Id(projectId, dto.getProductCode(), dto.getWarehouseId())
                .orElseGet(() -> buildMaterial(project, warehouseProduct, company));

        material.setRequiredQuantity(dto.getRequiredQuantity());
        material.setNote(dto.getNote());

        return mapToReadDto(materialRepository.save(material));
    }

    @Override
    @Transactional
    public ConstructionProjectMaterialReadDto useMaterial(
            Long projectId, ConstructionProjectMaterialUseDto dto, String email) {
        Company company = companyService.findByUserEmail(email);
        ConstructionProject project = findProjectAndValidate(projectId, company);
        WarehouseProduct warehouseProduct = findWarehouseProduct(dto.getProductCode(), dto.getWarehouseId(), company);

        int availableQuantity = warehouseProduct.getQuantity() != null ? warehouseProduct.getQuantity() : 0;
        if (availableQuantity < dto.getQuantity()) {
            throw new BusinessException("Not enough stock. Available: " + availableQuantity);
        }

        ConstructionProjectMaterial material = materialRepository
                .findByProject_IdAndProduct_ProductCodeAndWarehouse_Id(projectId, dto.getProductCode(), dto.getWarehouseId())
                .orElseGet(() -> buildMaterial(project, warehouseProduct, company));

        LocalDate useDate = dto.getUseDate() != null ? dto.getUseDate() : LocalDate.now();
        Invoice invoice = createUsageInvoice(project, warehouseProduct.getProduct(), dto, company, useDate);

        warehouseProduct.setQuantity(availableQuantity - dto.getQuantity());
        warehouseProductRepository.save(warehouseProduct);

        Product product = warehouseProduct.getProduct();
        long currentStock = product.getStock() != null ? product.getStock() : 0L;
        product.setStock(Math.max(0L, currentStock - dto.getQuantity()));
        if (product.getStock() <= 0) {
            product.setProductStatus(ProductStatus.LOW_STOCK);
        }
        productRepository.save(product);

        WarehouseActivity activity = new WarehouseActivity();
        activity.setType(ActivityTypeWarehouse.OUTGOING);
        activity.setProduct(warehouseProduct);
        activity.setQuantity(dto.getQuantity());
        activity.setSourceWarehouse(warehouseProduct.getWarehouse());
        activity.setPurchasePrice(product.getPrice());
        activity.setSalePrice(product.getPrice());
        activity.setReferenceNumber("PROJECT-" + project.getId());
        activity.setSourceName(warehouseProduct.getWarehouse() != null ? warehouseProduct.getWarehouse().getName() : null);
        activity.setDestinationName("Tikinti: " + project.getName());
        activity.setDate(LocalDateTime.now());
        activity.setCompany(company);
        warehouseActivityRepository.save(activity);

        material.setUsedQuantity((material.getUsedQuantity() != null ? material.getUsedQuantity() : 0) + dto.getQuantity());
        material.setLastUsedDate(useDate);
        material.setInvoice(invoice);
        material.setNote(dto.getNote());

        return mapToReadDto(materialRepository.save(material));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConstructionProjectMaterialReadDto> getProjectMaterials(Long projectId, String email) {
        Company company = companyService.findByUserEmail(email);
        findProjectAndValidate(projectId, company);

        return materialRepository.findAllByProject_Id(projectId)
                .stream()
                .map(this::mapToReadDto)
                .collect(Collectors.toList());
    }

    private ConstructionProject findProjectAndValidate(Long projectId, Company company) {
        ConstructionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("Project not found", HttpStatus.NOT_FOUND));

        if (!project.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("You do not have permission to access this project");
        }

        return project;
    }

    private WarehouseProduct findWarehouseProduct(String productCode, Long warehouseId, Company company) {
        Product product = productRepository.findByProductCodeAndCompanyId(productCode, company.getId())
                .orElseThrow(() -> new BusinessException("Product not found or access denied", HttpStatus.NOT_FOUND));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException("Warehouse not found", HttpStatus.NOT_FOUND));

        if (!warehouse.getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Warehouse access denied", HttpStatus.FORBIDDEN);
        }

        WarehouseProduct warehouseProduct = warehouseProductRepository.findByProductAndWarehouse(product, warehouse);
        if (warehouseProduct == null) {
            throw new BusinessException("This product is not present in the specified warehouse");
        }

        return warehouseProduct;
    }

    private ConstructionProjectMaterial buildMaterial(ConstructionProject project, WarehouseProduct warehouseProduct, Company company) {
        ConstructionProjectMaterial material = new ConstructionProjectMaterial();
        material.setProject(project);
        material.setWarehouseProduct(warehouseProduct);
        material.setProduct(warehouseProduct.getProduct());
        material.setWarehouse(warehouseProduct.getWarehouse());
        material.setCompany(company);
        material.setRequiredQuantity(0);
        material.setUsedQuantity(0);
        return material;
    }

    private Invoice createUsageInvoice(
            ConstructionProject project,
            Product product,
            ConstructionProjectMaterialUseDto dto,
            Company company,
            LocalDate useDate) {
        Partner partner = partnerRepository.findById(dto.getPartnerId())
                .orElseThrow(() -> new BusinessException("Partner not found", HttpStatus.NOT_FOUND));

        if (!partner.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("This partner does not belong to your company");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber(company));
        invoice.setInvoiceType(dto.getInvoiceType() != null ? dto.getInvoiceType() : InvoiceType.SALES);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setInvoiceDate(useDate);
        invoice.setDeliveryDate(useDate);
        invoice.setCurrency("AZN");
        invoice.setPartner(partner);
        invoice.setCompany(company);
        invoice.setNotes("Construction project material usage: " + project.getName());

        InvoiceItem item = new InvoiceItem();
        item.setName(product.getName());
        item.setUnit("pcs");
        item.setUnitPrice(resolveProductPrice(product));
        item.setQuantity(dto.getQuantity());
        item.setBarcode(product.getProductCode());
        item.setProduct(product);
        item.calculateLineTotal();

        invoice.addItem(item);
        return invoiceRepository.save(invoice);
    }

    private String generateInvoiceNumber(Company company) {
        int currentYear = LocalDate.now().getYear();

        InvoiceSequence sequence = invoiceSequenceRepository
                .findByCompanyAndYear(company, currentYear)
                .orElseGet(() -> {
                    InvoiceSequence newSeq = new InvoiceSequence();
                    newSeq.setCompany(company);
                    newSeq.setYear(currentYear);
                    newSeq.setLastNumber(0L);
                    return newSeq;
                });

        String invoiceNumber = sequence.generateNextNumber();
        invoiceSequenceRepository.save(sequence);
        return invoiceNumber;
    }

    private BigDecimal resolveProductPrice(Product product) {
        if (product.getPriceWithVat() != null) {
            return product.getPriceWithVat();
        }
        if (product.getPrice() != null) {
            return product.getPrice();
        }
        return BigDecimal.ZERO;
    }

    private ConstructionProjectMaterialReadDto mapToReadDto(ConstructionProjectMaterial material) {
        ConstructionProjectMaterialReadDto dto = new ConstructionProjectMaterialReadDto();
        dto.setId(material.getId());
        dto.setProjectId(material.getProject().getId());
        dto.setProjectName(material.getProject().getName());
        dto.setProductId(material.getProduct().getId());
        dto.setProductCode(material.getProduct().getProductCode());
        dto.setProductName(material.getProduct().getName());
        dto.setWarehouseId(material.getWarehouse().getId());
        dto.setWarehouseName(material.getWarehouse().getName());
        dto.setRequiredQuantity(material.getRequiredQuantity());
        dto.setUsedQuantity(material.getUsedQuantity());
        dto.setAvailableQuantity(material.getWarehouseProduct().getQuantity());
        dto.setLastUsedDate(material.getLastUsedDate());
        dto.setNote(material.getNote());

        if (material.getInvoice() != null) {
            dto.setInvoiceId(material.getInvoice().getId());
            dto.setInvoiceNumber(material.getInvoice().getInvoiceNumber());
        }

        return dto;
    }
}
