package coo.cfapps.mds.repository;

import coo.cfapps.mds.entity.DemoData;
import org.springframework.data.repository.CrudRepository;

public interface DemoDataRepository extends CrudRepository<DemoData, Long> {
}
