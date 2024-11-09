package cool.cfapps.mds.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("jpa")
public interface JpaDemoDataRepository extends JpaRepository<JpaDemoData, Long> {
}
