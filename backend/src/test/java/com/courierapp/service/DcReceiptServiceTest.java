package com.courierapp.service;

import com.courierapp.dto.dcreceipt.DcReceiptRequest;
import com.courierapp.dto.dcreceipt.DcReceiptResponse;
import com.courierapp.entity.Company;
import com.courierapp.entity.DcReceipt;
import com.courierapp.entity.DeliveryChallan;
import com.courierapp.entity.Unit;
import com.courierapp.enums.DcStatus;
import com.courierapp.enums.DcType;
import com.courierapp.exception.BusinessException;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.mapper.DcMapper;
import com.courierapp.mapper.DcReceiptMapper;
import com.courierapp.repository.CompanySettingsRepository;
import com.courierapp.repository.DcReceiptRepository;
import com.courierapp.repository.DeliveryChallanRepository;
import com.courierapp.service.impl.DcReceiptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DcReceiptService — unit tests")
class DcReceiptServiceTest {

    @Mock DcReceiptRepository dcReceiptRepository;
    @Mock DeliveryChallanRepository deliveryChallanRepository;
    @Mock CompanySettingsRepository companySettingsRepository;
    @Mock DcReceiptNumberGenerator dcReceiptNumberGenerator;
    @Mock DcReceiptMapper dcReceiptMapper;
    @Mock DcMapper dcMapper;
    @Mock AuditLogService auditLogService;
    @Mock com.courierapp.security.CurrentUserService currentUserService;

    @InjectMocks
    DcReceiptServiceImpl dcReceiptService;

    private DeliveryChallan issuedReturnableDc;
    private DcReceiptResponse dummyResponse;

    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserService.requireCompanyId()).thenReturn(COMPANY_ID);
        Company company = Company.builder().id(COMPANY_ID).companyCode("1").name("Test Co").build();
        Unit unit = Unit.builder().id(1L).unitName("Coimbatore Factory").active(true).company(company).build();
        issuedReturnableDc = DeliveryChallan.builder()
                .id(20L)
                .dcNumber("C1-DC-2026-00001")
                .dcType(DcType.RETURNABLE)
                .unit(unit)
                .status(DcStatus.ISSUED)
                .build();

        dummyResponse = mock(DcReceiptResponse.class);
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("confirms receipt for an ISSUED Returnable DC and moves it to RETURNED")
        void confirmSuccess() {
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(issuedReturnableDc));
            when(dcReceiptRepository.existsByDeliveryChallanId(20L)).thenReturn(false);
            when(dcReceiptNumberGenerator.next(any(), any())).thenReturn("C1-RC-2026-00001");
            when(dcReceiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(dcReceiptMapper.toResponse(any())).thenReturn(dummyResponse);

            DcReceiptResponse result = dcReceiptService.create(new DcReceiptRequest(20L, "Good condition"));

            assertThat(result).isSameAs(dummyResponse);
            assertThat(issuedReturnableDc.getStatus()).isEqualTo(DcStatus.RETURNED);

            ArgumentCaptor<DcReceipt> captor = ArgumentCaptor.forClass(DcReceipt.class);
            verify(dcReceiptRepository).save(captor.capture());
            assertThat(captor.getValue().getPreviousDcStatus()).isEqualTo(DcStatus.ISSUED);
            assertThat(captor.getValue().getDeliveryChallan()).isSameAs(issuedReturnableDc);

            verify(deliveryChallanRepository).save(issuedReturnableDc);
        }

        @Test
        @DisplayName("confirms receipt for a DELIVERED Returnable DC too")
        void confirmDeliveredSuccess() {
            issuedReturnableDc.setStatus(DcStatus.DELIVERED);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(issuedReturnableDc));
            when(dcReceiptRepository.existsByDeliveryChallanId(20L)).thenReturn(false);
            when(dcReceiptNumberGenerator.next(any(), any())).thenReturn("C1-RC-2026-00002");
            when(dcReceiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(dcReceiptMapper.toResponse(any())).thenReturn(dummyResponse);

            dcReceiptService.create(new DcReceiptRequest(20L, null));

            assertThat(issuedReturnableDc.getStatus()).isEqualTo(DcStatus.RETURNED);
        }

        @Test
        @DisplayName("rejects a Non-Returnable DC")
        void rejectsNonReturnable() {
            issuedReturnableDc.setDcType(DcType.NON_RETURNABLE);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(issuedReturnableDc));

            assertThatThrownBy(() -> dcReceiptService.create(new DcReceiptRequest(20L, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Returnable");
        }

        @Test
        @DisplayName("rejects a DC that is not yet ISSUED or DELIVERED")
        void rejectsWrongStatus() {
            issuedReturnableDc.setStatus(DcStatus.APPROVED);
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(issuedReturnableDc));

            assertThatThrownBy(() -> dcReceiptService.create(new DcReceiptRequest(20L, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ISSUED or DELIVERED");
        }

        @Test
        @DisplayName("rejects a DC that already has a receipt")
        void rejectsDuplicateReceipt() {
            when(deliveryChallanRepository.findById(20L)).thenReturn(Optional.of(issuedReturnableDc));
            when(dcReceiptRepository.existsByDeliveryChallanId(20L)).thenReturn(true);

            assertThatThrownBy(() -> dcReceiptService.create(new DcReceiptRequest(20L, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already has a receipt");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for an unknown DC")
        void unknownDc() {
            when(deliveryChallanRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dcReceiptService.create(new DcReceiptRequest(99L, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── delete (undo) ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("restores the DC's prior status and removes the receipt")
        void undoRestoresPriorStatus() {
            DcReceipt receipt = DcReceipt.builder()
                    .id(30L)
                    .receiptNumber("C1-RC-2026-00001")
                    .deliveryChallan(issuedReturnableDc)
                    .previousDcStatus(DcStatus.ISSUED)
                    .build();
            issuedReturnableDc.setStatus(DcStatus.RETURNED);
            when(dcReceiptRepository.findById(30L)).thenReturn(Optional.of(receipt));

            dcReceiptService.delete(30L);

            assertThat(issuedReturnableDc.getStatus()).isEqualTo(DcStatus.ISSUED);
            verify(deliveryChallanRepository).save(issuedReturnableDc);
            verify(dcReceiptRepository).delete(receipt);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for an unknown receipt")
        void unknownReceipt() {
            when(dcReceiptRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dcReceiptService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
