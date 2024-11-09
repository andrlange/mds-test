package cool.cfapps.mds.jdbc;

import cool.cfapps.mds.demo.DemoData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;

@Profile("jdbc")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "demo_data")
public class JdbcDemoData implements DemoData, Serializable {
    private Long id;
    private String field1;
    private String field2;
}
