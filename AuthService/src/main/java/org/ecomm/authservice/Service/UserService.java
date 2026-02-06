package org.ecomm.authservice.Service;


import org.ecomm.authservice.DTO.UserRegistrationRequest;
import org.ecomm.authservice.DTO.UserRegistrationResponse;
import org.ecomm.authservice.DTO.UserRequest;
import org.ecomm.authservice.Exceptions.UserAlreadyExists;
import org.ecomm.authservice.Exceptions.UserDoesNotExist;
import org.ecomm.authservice.Model.User;
import org.ecomm.authservice.Repository.AuthRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AuthRepository authRepository) {
        this.authRepository = authRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public UserRegistrationResponse register(UserRegistrationRequest userRegistrationRequest) {

        Optional<User> users= this.authRepository.findByEmail(userRegistrationRequest.email);

        if(users.isPresent()){
            throw new UserAlreadyExists("User already present");
        }

           User user = User.builder()
                   .email(userRegistrationRequest.email)
                   .password(passwordEncoder.encode(userRegistrationRequest.password))
                   .role(userRegistrationRequest.role)
                   .build();

        User saved = this.authRepository.save(user);
       return UserRegistrationResponse
                .builder()
                .email(saved.getEmail())
                .message("User registered successfully!")
                .build();

    }

    public Optional<User> getUser(UserRequest userRequest) {
        Optional<User> byEmail = this.authRepository.findByEmail(userRequest.getEmail());
        if(byEmail.isEmpty()){
            throw new UserDoesNotExist("User doesn't exist");
        }
        return byEmail;
    }
}
