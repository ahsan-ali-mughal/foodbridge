package com.foodbridge.document.repository;

import com.foodbridge.document.document.Receipt;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReceiptRepository extends MongoRepository<Receipt, String> {

    Optional<Receipt> findByClaimId(Long claimId);
}
