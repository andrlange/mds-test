package coo.cfapps.mds.repository;

import coo.cfapps.mds.entity.DemoData;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface DemoDataRepository extends CrudRepository<DemoData, Long> {

    @Query(value = "SELECT CONCAT(CURRENT_SCHEMA,' - ',CURRENT_USER)  AS SCHEMA")
        String getSchema();
}
