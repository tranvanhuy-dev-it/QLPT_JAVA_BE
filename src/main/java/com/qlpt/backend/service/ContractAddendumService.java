package com.qlpt.backend.service;

import com.qlpt.backend.dto.contract.ContractAddendumCreateRequest;
import com.qlpt.backend.entity.*;
import java.util.List;
import java.util.UUID;

public interface ContractAddendumService {
    public ContractAddendum createAddendum(UUID contractId, ContractAddendumCreateRequest request, User landlord);

    public List<ContractAddendum> getAddendumsByContract(UUID contractId, User user);

    public List<ContractAddendumExtraFee> getAddendumExtraFees(UUID addendumId, User user);
}
