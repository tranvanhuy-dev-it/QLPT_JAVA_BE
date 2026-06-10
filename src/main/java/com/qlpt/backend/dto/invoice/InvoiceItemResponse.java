package com.qlpt.backend.dto.invoice;

import com.qlpt.backend.entity.InvoiceItem;
import java.util.UUID;

public record InvoiceItemResponse(
    UUID id,
    String name,
    double price,
    double quantity,
    double subtotal
) {
    public static InvoiceItemResponse fromEntity(InvoiceItem item) {
        if (item == null) return null;
        try {
            item.getName();
        } catch (org.hibernate.LazyInitializationException e) {
            return null;
        }
        return new InvoiceItemResponse(
            item.getId(),
            item.getName(),
            item.getPrice(),
            item.getQuantity(),
            item.getSubtotal()
        );
    }
}
