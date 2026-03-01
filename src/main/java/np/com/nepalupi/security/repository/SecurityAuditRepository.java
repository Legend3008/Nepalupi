package np.com.nepalupi.security.repository;

import np.com.nepalupi.security.entity.SecurityAudit;
import np.com.nepalupi.security.enums.AuditStatus;
import np.com.nepalupi.security.enums.AuditType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, UUID> {

    List<SecurityAudit> findByAuditTypeOrderByCreatedAtDesc(AuditType auditType);

    List<SecurityAudit> findByAuditStatusOrderByCreatedAtDesc(AuditStatus status);

    @Query("SELECT a FROM SecurityAudit a WHERE a.nextAuditDue <= :now AND a.auditStatus = 'COMPLETED' ORDER BY a.nextAuditDue")
    List<SecurityAudit> findOverdueAudits(Instant now);

    @Query("SELECT a FROM SecurityAudit a WHERE a.nrbSubmitted = false AND a.auditStatus = 'COMPLETED'")
    List<SecurityAudit> findCompletedButNotSubmittedToNrb();
}
