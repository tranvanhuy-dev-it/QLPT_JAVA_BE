package com.qlpt.backend.service;

import com.qlpt.backend.dto.invoice.BulkBillingRoomStatus;
import com.qlpt.backend.dto.invoice.BulkInvoiceCreateRequest;
import com.qlpt.backend.dto.invoice.InvoiceCreateRequest;
import com.qlpt.backend.entity.*;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
