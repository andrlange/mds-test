package cool.cfapps.mds.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserPasswordService {

    private final DbUserDetailsService dbUserDetailsService;

    public UserPasswordService(DbUserDetailsService dbUserDetailsService) {
        this.dbUserDetailsService = dbUserDetailsService;
    }

    public boolean changeUserPassword(String username, String oldPassword, String newPassword) {
        return dbUserDetailsService.changeUserPassword(username, oldPassword, newPassword);
    }

    public void logOut(String username) {
        dbUserDetailsService.logOut(username);
    }

}
