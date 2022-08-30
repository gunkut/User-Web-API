package com.appsdeveloperblog.app.ws.service.impl;

import com.appsdeveloperblog.app.ws.exceptions.UserServiceException;
import com.appsdeveloperblog.app.ws.io.repositories.UserRepository;
import com.appsdeveloperblog.app.ws.io.entity.UserEntity;
import com.appsdeveloperblog.app.ws.shared.AmazonSES;
import com.appsdeveloperblog.app.ws.shared.Utils;
import com.appsdeveloperblog.app.ws.shared.dto.AddressDto;
import com.appsdeveloperblog.app.ws.ui.model.response.ErrorMessages;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.appsdeveloperblog.app.ws.service.UserService;
import com.appsdeveloperblog.app.ws.shared.dto.UserDto;

import java.util.ArrayList;
import java.util.List;


@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    Utils utils;
    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    AmazonSES amazonSES;

    @Override
    public UserDto createUser(UserDto user) {

        if (userRepository.findByEmail(user.getEmail()) != null)
            throw new UserServiceException("Record already exists");

        for(int i = 0; i < user.getAddresses().size(); i++) {
            AddressDto address = user.getAddresses().get(i);
            address.setUserDetails(user);
            address.setAddressId(utils.generateAddressId(30));
            user.getAddresses().set(i, address);
        }

        ModelMapper modelMapper = new ModelMapper();

        UserEntity userEntity = modelMapper.map(user, UserEntity.class);

        String publicUserId = utils.generateUserId(30);
        userEntity.setUserId(publicUserId);
        userEntity.setEncryptedPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        userEntity.setEmailVerificationToken(utils.generateEmailVerificationToken(publicUserId));

        UserEntity storedUserDetails = userRepository.save(userEntity);

        UserDto returnValue = modelMapper.map(storedUserDetails, UserDto.class);

        // Send an email message to user to verify their address
        amazonSES.verifyEmail(returnValue);

        return returnValue;
    }

    @Override
    public UserDto getUser(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);
        if(userEntity == null) throw new UsernameNotFoundException(email);

        UserDto returnValue = new UserDto();
        BeanUtils.copyProperties(userEntity, returnValue);

        return returnValue;
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        /*UserDto returnValue = new UserDto();*/
        UserEntity userEntity = userRepository.findByUserId(userId);

        if(userEntity == null)
            throw new UsernameNotFoundException("User with id: " + userId + " not found");

        ModelMapper modelMapper = new ModelMapper();
        /*BeanUtils.copyProperties(userEntity, returnValue);*/
        UserDto returnValue = modelMapper.map(userEntity, UserDto.class);

        return returnValue;
    }

    @Override
    public UserDto updateUser(UserDto user, String userId) {
        UserDto returnValue = new UserDto();
        UserEntity userEntity = userRepository.findByUserId(userId);

        if(userEntity == null)
            throw new UserServiceException(ErrorMessages.COULD_NOT_UPDATE_RECORD.getErrorMessage());

        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());

        UserEntity updatedUserDetails = userRepository.save(userEntity);

        BeanUtils.copyProperties(updatedUserDetails, returnValue);

        return returnValue;
    }

    @Override
    public void deleteUser(String userId){
        UserEntity userEntity = userRepository.findByUserId(userId);

        if(userEntity == null)
            throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());

        userRepository.delete(userEntity);
    }

    @Override
    public List<UserDto> getUsers(int page, int limit){

        List<UserDto> returnValue = new ArrayList<>();

        if(page > 0) page -= 1;
        Pageable pageRequest = PageRequest.of(page, limit);

        Page<UserEntity> usersPage = userRepository.findAll(pageRequest);
        List<UserEntity> users = usersPage.getContent();

        for(UserEntity userEntity : users) {
            /*UserDto userDto = new UserDto();
            BeanUtils.copyProperties(userEntity, userDto);

            returnValue.add(userDto);*/

            UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);
            returnValue.add(userDto);
        }

        return returnValue;
    }

    @Override
    public boolean verifyEmailToken(String token) {
        boolean returnValue = false;

        // Find user by token
        UserEntity userEntity = userRepository.findUserByEmailVerificationToken(token);

        if (userEntity != null) {
           boolean hasTokenExpired = Utils.hasTokenExpired(token);

           if (!hasTokenExpired) {
               userEntity.setEmailVerificationToken(null);
               userEntity.setEmailVerificationStatus(Boolean.TRUE);
               userRepository.save(userEntity);
               returnValue = true;
           }
        }

        return returnValue;
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email);

        if (userEntity == null)
            throw new UsernameNotFoundException(email);

        return new User(userEntity.getEmail(), userEntity.getEncryptedPassword(),
                userEntity.getEmailVerificationStatus(), true, true,
                true, new ArrayList<>());
        /*return new User(userEntity.getEmail(), userEntity.getEncryptedPassword(), new ArrayList<>());*/
    }
}