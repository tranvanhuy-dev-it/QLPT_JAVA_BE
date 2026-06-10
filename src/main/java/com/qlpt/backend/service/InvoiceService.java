package com.qlpt.backend.service;

import com.qlpt.backend.dto.invoice.BulkBillingRoomStatus;
import com.qlpt.backend.dto.invoice.BulkInvoiceCreateRequest;
import com.qlpt.backend.dto.invoice.InvoiceCreateRequest;
import com.qlpt.backend.entity.*;
import com.qlpt.backend.enums.*;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface InvoiceService {
    public Invoice createInvoice(InvoiceCreateRequest request, User landlord);
    public Invoice updatePaymentStatus(UUID invoiceId, double paidAmount, User landlord);
    public Page<Invoice> getInvoicesByLandlord(User landlord, Pageable pageable);
    public Page<Invoice> getInvoicesByTenant(User tenant, Pageable pageable);
    public Invoice getInvoiceById(UUID invoiceId, User user);
    public List<InvoiceItem> getInvoiceItems(UUID invoiceId, User user);
    public List<BulkBillingRoomStatus> getBillingStatusForBoardingHouse(UUID bhId, User landlord);
    public void createBulkInvoices(BulkInvoiceCreateRequest request, User landlord);
}
