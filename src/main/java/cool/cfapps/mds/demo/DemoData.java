package cool.cfapps.mds.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DemoData {
    private Long id;
    private String field1;
    private String field2;
}
