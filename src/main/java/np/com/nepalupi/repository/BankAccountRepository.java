package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    Optional<BankAccount> findByBankCodeAndAccountNumber(String bankCode, String accountNumber);

    List<BankAccount> findByUserId(UUID userId);

    Optional<BankAccount> findByUserIdAndIsPrimary(UUID userId, Boolean isPrimary);

    @Query("SELECT ba FROM BankAccount ba JOIN User u ON ba.userId = u.id " +
           "WHERE u.mobileNumber = :mobile AND ba.bankCode = :bankCode")
    List<BankAccount> findByUserMobileAndBankCode(@Param("mobile") String mobileNumber,
                                                  @Param("bankCode") String bankCode);
}
