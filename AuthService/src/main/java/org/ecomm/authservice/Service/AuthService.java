package org.ecomm.authservice.Service;

import io.jsonwebtoken.JwtException;
import org.ecomm.authservice.DTO.UserRequest;
import org.ecomm.authservice.Model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.util.JSONPObject;

import java.util.Optional;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil=jwtUtil;
    }

    public Optional<String> authenticate(UserRequest userRequest) {

        Optional<String> use=this.userService.getUser(userRequest)
                .filter(user->passwordEncoder.matches(userRequest.getPassword(), user.getPassword()))
                .map(user->jwtUtil.generateToken(user.getEmail(),user.getRole()));

        return use;
    }

    public boolean validateJwtToken(String token) {
        try {
            this.jwtUtil.validateToken(token);
            return true;
        }
      catch (JwtException e) {
            return false;
      }
    }
}
