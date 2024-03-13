package com.cat2bug;

import com.cat2bug.framework.security.context.AuthenticationContextHolder;
import com.cat2bug.junit.Cat2BugAutoSpringSuite;
import com.cat2bug.junit.annotation.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
@RunWith(Cat2BugAutoSpringSuite.class)
@AutoTestScan(packageName = "com.cat2bug.web")
@PushReport
//@Authentication(name = "admin",password = "cat2bug")
public class Cat2BugApplicationTest {
    @Copy
    public Long ddd = 1L;

    private Long ddd2 = 2L;

    @Copy
    private static final  Long DD_4 = 4L;

    @Copy
    @Autowired
    public void Security(AuthenticationManager authenticationManager) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken("admin", "cat2bug");
        AuthenticationContextHolder.setContext(authenticationToken);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticationManager.authenticate(authenticationToken));
        SecurityContextHolder.setContext(securityContext);
    }
}