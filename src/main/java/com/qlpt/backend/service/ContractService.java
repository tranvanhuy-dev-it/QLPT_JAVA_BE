package com.qlpt.backend.service;

import com.qlpt.backend.dto.contract.ContractCreateRequest;
import com.qlpt.backend.entity.*;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContractService {
    public Contract createContract(ContractCreateRequest request, User landlord);

    public Contract terminateContract(UUID contractId, User landlord);

    public Contract updateContract(UUID id, int numberOfTenants, User landlord);

    public Page<Contract> getContractsByLandlord(User landlord, Boolean showAll, Pageable pageable);

    public Page<Contract> getContractsByRoomAndLandlord(UUID roomId, User landlord, Boolean showAll, Pageable pageable);

    public Contract getContractById(UUID contractId, User user);

    public Page<Contract> getContractsByTenant(User tenant, Boolean showAll, Pageable pageable);

    public List<ContractExtraFee> getContractExtraFees(UUID contractId, User user);
}
