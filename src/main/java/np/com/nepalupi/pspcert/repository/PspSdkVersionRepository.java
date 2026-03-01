package np.com.nepalupi.pspcert.repository;

import np.com.nepalupi.pspcert.entity.PspSdkVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PspSdkVersionRepository extends JpaRepository<PspSdkVersion, UUID> {

    List<PspSdkVersion> findByPspIdOrderByCreatedAtDesc(String pspId);

    @Query("SELECT v FROM PspSdkVersion v WHERE v.upgradeRequired = true AND v.transactionsRestricted = false")
    List<PspSdkVersion> findRequiringUpgrade();

    @Query("SELECT v FROM PspSdkVersion v WHERE v.upgradeRequired = true AND v.upgradeDeadline < CURRENT_TIMESTAMP AND v.transactionsRestricted = false")
    List<PspSdkVersion> findPastUpgradeDeadline();

    List<PspSdkVersion> findByIsCurrentFalseOrderByPspId();
}
