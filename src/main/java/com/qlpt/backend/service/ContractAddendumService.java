package com.qlpt.backend.service;

import com.qlpt.backend.dto.contract.ContractAddendumCreateRequest;
import com.qlpt.backend.entity.*;
import com.qlpt.backend.enums.*;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface ContractAddendumService {
    public ContractAddendum createAddendum(UUID contractId, ContractAddendumCreateRequest request, User landlord);
    public List<ContractAddendum> getAddendumsByContract(UUID contractId, User user);
    public List<ContractAddendumExtraFee> getAddendumExtraFees(UUID addendumId, User user);
}
