package cool.cfapps.mds.jpa;

import cool.cfapps.mds.demo.DemoData;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Profile;

import java.io.Serializable;

@Profile("jpa")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "demo_data")
public class JpaDemoData implements DemoData, Serializable {
    @Id
    private Long id;
    private String field1;
    private String field2;
}
