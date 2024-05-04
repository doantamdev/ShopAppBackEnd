package com.project.shopapp.services;

import com.project.shopapp.components.JwtTokenUtils;
import com.project.shopapp.dtos.UserDTO;
import com.project.shopapp.exceptions.DataNotFoundException;
import com.project.shopapp.exceptions.PermissionDenyException;
import com.project.shopapp.models.Role;
import com.project.shopapp.models.User;
import com.project.shopapp.repositories.RoleRepository;
import com.project.shopapp.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService implements IUserService{

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtils jwtTokenUtil;
    private final AuthenticationManager authenticationManager;

     @Override
    public User createUser(UserDTO userDTO) throws Exception {
         String phoneNumber = userDTO.getPhoneNumber();
         if(userRepository.existsByPhoneNumber(phoneNumber))
         {
             throw new DataIntegrityViolationException("Phone number already exists");
         }
         Role role = roleRepository.findById(userDTO.getRoleId())
                 .orElseThrow(() -> new DataNotFoundException("Role not null"));
        if(role.getName().toUpperCase().equals(Role.ADMIN)){
            throw new PermissionDenyException("You cannot create ADMIN account");
        }
         User newUser = User.builder()
                 .fullName(userDTO.getFullName())
                 .phoneNumber(userDTO.getPhoneNumber())
                 .password(userDTO.getPassword())
                 .address(userDTO.getAddress())
                 .dateOfBirth(userDTO.getDateOfBirth())
                 .facebookAccountId(userDTO.getFacebookAccountId())
                 .googleAccountId(userDTO.getGoogleAccountId())
                 .active(true)
                 .build();
         newUser.setRole(role);
         if (userDTO.getFacebookAccountId() == 0 && userDTO.getGoogleAccountId() == 0) {
             String password = userDTO.getPassword();
             String encodedPassword = passwordEncoder.encode(password);
             newUser.setPassword(encodedPassword);
         }

         return userRepository.save(newUser);
    }

    @Override
    public String login(String phoneNumber, String password,  Long roleId
    ) throws Exception {
         Optional<User> optionalUser = userRepository.findByPhoneNumber(phoneNumber);
         if(optionalUser.isEmpty())
         {
             throw new DataNotFoundException("Invalid Phone number or password");
         }
         User existingUser = optionalUser.get();
        if (existingUser.getFacebookAccountId() == 0 && existingUser.getGoogleAccountId() == 0) {
           if(!passwordEncoder.matches(password,existingUser.getPassword()))
           {
               throw new BadCredentialsException("Wrong phone number or password");
           }
        }
         UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(phoneNumber,password, existingUser.getAuthorities());
         authenticationManager.authenticate(authenticationToken);
         return jwtTokenUtil.generateToken(existingUser);
    }

    @Override
    public User getUserDetailsFromToken(String token) throws Exception {
        if(jwtTokenUtil.isTokenExpired(token)) {
            throw new Exception("Token is expired");
        }
        String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);
        Optional<User> user = userRepository.findByPhoneNumber(phoneNumber);

        if (user.isPresent()) {
            return user.get();
        } else {
            throw new Exception("User not found");
        }
    }

}
