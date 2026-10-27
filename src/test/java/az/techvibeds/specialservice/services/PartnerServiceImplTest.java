package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.partner.PartnerCreateDto;
import az.techvibeds.specialservice.dtos.partner.PartnerResponseDto;
import az.techvibeds.specialservice.dtos.partner.PartnerUpdateDto;
import az.techvibeds.specialservice.enums.PartnerType;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.Partner;
import az.techvibeds.specialservice.repositories.PartnerRepository;
import az.techvibeds.specialservice.services.impl.PartnerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PartnerServiceImplTest {

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private CompanyService companyService;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private PartnerServiceImpl partnerService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");
    }

    @Test
    void createCustomer_WithVoen_ShouldMapAndSaveCorrectly() {
        // Arrange
        PartnerCreateDto dto = new PartnerCreateDto();
        dto.setName("Musteri MMC");
        dto.setContactPerson("Ali");
        dto.setEmail("ali@test.com");
        dto.setPhone("+994501234567");
        dto.setBalance(BigDecimal.ZERO);
        dto.setCurrency("manat");
        dto.setVoen("1234567890");

        when(companyService.findByUserEmail("test@example.com")).thenReturn(company);
        when(partnerRepository.save(any(Partner.class))).thenAnswer(invocation -> {
            Partner p = invocation.getArgument(0);
            p.setId(10L); // simulate DB generated ID
            return p;
        });

        // Act
        PartnerResponseDto response = partnerService.createCustomer(dto, "test@example.com");

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Musteri MMC", response.getName());
        assertEquals("1234567890", response.getVoen());
        assertEquals(PartnerType.CUSTOMER, response.getPartnerType());

        verify(partnerRepository, times(1)).save(any(Partner.class));
    }

    @Test
    void createCustomer_WithoutVoen_ShouldMapAndSaveAsNull() {
        // Arrange
        PartnerCreateDto dto = new PartnerCreateDto();
        dto.setName("Musteri MMC");
        dto.setContactPerson("Ali");
        dto.setEmail("ali@test.com");
        dto.setPhone("+994501234567");
        dto.setBalance(BigDecimal.ZERO);
        dto.setCurrency("manat");
        dto.setVoen(null); // Explicitly null/empty

        when(companyService.findByUserEmail("test@example.com")).thenReturn(company);
        when(partnerRepository.save(any(Partner.class))).thenAnswer(invocation -> {
            Partner p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        // Act
        PartnerResponseDto response = partnerService.createCustomer(dto, "test@example.com");

        // Assert
        assertNotNull(response);
        assertNull(response.getVoen());

        verify(partnerRepository, times(1)).save(any(Partner.class));
    }

    @Test
    void updatePartner_WithVoen_ShouldUpdateCorrectly() throws Exception {
        // Arrange
        Partner partner = new Partner();
        partner.setId(10L);
        partner.setName("Old Name");
        partner.setCompany(company);
        partner.setVoen("old_voen");

        PartnerUpdateDto dto = new PartnerUpdateDto();
        dto.setId(10L);
        dto.setVoen("new_voen");

        when(partnerRepository.findById(10L)).thenReturn(Optional.of(partner));
        when(companyService.findByUserEmail("test@example.com")).thenReturn(company);
        when(partnerRepository.save(any(Partner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PartnerResponseDto response = partnerService.updatePartner(dto, "test@example.com");

        // Assert
        assertNotNull(response);
        assertEquals("new_voen", response.getVoen());
        assertEquals("Old Name", response.getName()); // other fields remain unchanged

        verify(partnerRepository, times(1)).save(partner);
    }
}
