package cool.cfapps.mds.demo;


import cool.cfapps.mds.infrastructure.Password;
import cool.cfapps.mds.infrastructure.UserPasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthTestController {

    private final DemoDataService demoDataService;
    private final UserPasswordService userPasswordService;

    public AuthTestController(DemoDataService demoDataService, UserPasswordService userPasswordService) {
        this.demoDataService = demoDataService;
        this.userPasswordService = userPasswordService;
    }


    @GetMapping
    public String testAuth(Authentication authentication) {
        return "Hello "+authentication.getName()+" from authenticated user!";
    }

    @GetMapping("demo")
    public Iterable<DemoData> testDemoData() {
        return demoDataService.fetchDemoData();

    }

    /*
    curl -u user_one:password_one -X POST http://localhost:8080/auth/password
   -H "Content-Type: application/json"
   -d '{"oldPassword": "password_three", "newPassword": "newSecret"}'

    curl -u user_one:password_one -X POST http://localhost:8080/auth/change-password -H "Content-Type:
    application/json" -d '{"oldPassword": "password_three", "newPassword": "newSecret"}'

     */
    @PostMapping("change-password" )
    public ResponseEntity<Boolean> changePassword(Authentication authentication,@RequestBody Password password) {

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
