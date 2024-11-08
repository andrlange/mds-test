package cool.cfapps.mds.infrastructure;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.context.annotation.ApplicationScope;

@Data
@ApplicationScope
@NoArgsConstructor
public class Password {
    String oldPassword;
    String newPassword;
}
