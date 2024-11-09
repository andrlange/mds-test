package cool.cfapps.mds.demo;

import cool.cfapps.mds.infrastructure.Password;
import cool.cfapps.mds.infrastructure.UserPasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
public class DemoDataController {

    private final DemoDataService service;
    private final UserPasswordService userPasswordService;


    public DemoDataController(DemoDataService service, UserPasswordService userPasswordService) {
        this.service = service;
        this.userPasswordService = userPasswordService;
    }

    @GetMapping
    public String testAuth(Authentication authentication) {
        return "Hello " + authentication.getName() + " from authenticated user for " + service.type() + "!";
    }

    @GetMapping("demo")
    public List<DemoData> testDemoData() {
        return service.fetchDemoData();
    }

    @PostMapping("change-password" )
    public ResponseEntity<Boolean> changePassword(Authentication authentication, @RequestBody Password password) {

        log.info("Changing password for user: {} with: {}", authentication.getName(), password);
        boolean result = userPasswordService.changeUserPassword(authentication.getName(), password.getOldPassword().trim(),
                password.getNewPassword().trim());
        return result ? ResponseEntity.ok(true) : ResponseEntity.badRequest().body(false);

    }

    @GetMapping("logout")
    public void logout(Authentication authentication) {
        userPasswordService.logOut(authentication.getName());
    }
}
