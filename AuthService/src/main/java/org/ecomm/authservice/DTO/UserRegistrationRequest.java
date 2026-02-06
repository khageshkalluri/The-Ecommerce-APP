package org.ecomm.authservice.DTO;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserRegistrationRequest {

    @NotNull(message = "email Id cannot be null or blank")
    @Email(message = "please enter a valid email Id")
    public String email;

    @NotNull(message = "password cannot be null or blank")
    public String password;

    @NotNull(message = "role cannot be null")
    public String role;
}
