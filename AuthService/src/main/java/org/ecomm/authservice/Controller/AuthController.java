package org.ecomm.authservice.Controller;

import org.ecomm.authservice.DTO.UserRegistrationRequest;
import org.ecomm.authservice.DTO.UserRegistrationResponse;
import org.ecomm.authservice.DTO.UserRequest;
import org.ecomm.authservice.Service.AuthService;
import org.ecomm.authservice.Service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

   public AuthController(AuthService authService, UserService userService) {
       this.authService = authService;
       this.userService = userService;
   }

   @PostMapping("/auth/register")
   public ResponseEntity<UserRegistrationResponse> registerUser(@RequestBody UserRegistrationRequest userRegistrationRequest) {
       UserRegistrationResponse userRegistrationResponse = this.userService.register(userRegistrationRequest);
       return new ResponseEntity<>(userRegistrationResponse, HttpStatus.CREATED);
   }

   @PostMapping("/auth/login")
   public ResponseEntity<Map<String,String>> loginUser(@RequestBody UserRequest userRequest) {
         Optional<String> token=  this.authService.authenticate(userRequest);
         if(token.isEmpty()){
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
         }
         Map<String,String> map = new HashMap<>();
         map.put("token",token.get());
         return new ResponseEntity<>(map,HttpStatus.OK);
   }


   @PostMapping("/auth/validate")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String token){
        if((!token.startsWith("Bearer ") || token==null) || (token.length()<7)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String tokenValue = token.substring(7);
        if(this.authService.validateJwtToken(tokenValue)){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

   }

}
