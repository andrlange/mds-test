package cool.cfapps.mds.jdbc;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("jdbc")
public interface JdbcDemoDataRepository extends CrudRepository<JdbcDemoData, Long> {
}
