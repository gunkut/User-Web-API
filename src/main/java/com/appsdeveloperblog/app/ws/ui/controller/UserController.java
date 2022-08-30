package com.appsdeveloperblog.app.ws.ui.controller;


import com.appsdeveloperblog.app.ws.exceptions.UserServiceException;
import com.appsdeveloperblog.app.ws.service.AddressService;
import com.appsdeveloperblog.app.ws.shared.dto.AddressDto;
import com.appsdeveloperblog.app.ws.ui.model.response.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.BeanUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.appsdeveloperblog.app.ws.service.UserService;
import com.appsdeveloperblog.app.ws.shared.dto.UserDto;
import com.appsdeveloperblog.app.ws.ui.model.request.UserDetailsRequestModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



@RestController // to receive HTTP requests
@RequestMapping("/users") // request mapping for this user controller to start with users.
// http://localhost:8080/users
public class UserController {

    @Autowired
    UserService userService;
    @Autowired
    AddressService addressService;

    @GetMapping(path="/{userId}",
            produces = { MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    public UserRest getUser(@PathVariable String userId) {

        ModelMapper modelMapper = new ModelMapper();
        UserDto userDto = userService.getUserByUserId(userId);

        UserRest returnValue = modelMapper.map(userDto, UserRest.class);

        return returnValue;
    }

    @PostMapping(
            consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public UserRest createUser(@RequestBody UserDetailsRequestModel userDetails) {

        if(userDetails.getFirstName().isEmpty()) throw new UserServiceException(ErrorMessages.INTERNAL_SERVER_ERROR.getErrorMessage());

        ModelMapper modelMapper = new ModelMapper();

        UserDto userDto = modelMapper.map(userDetails, UserDto.class);

        UserDto createdUser = userService.createUser(userDto);
        UserRest returnValue = modelMapper.map(createdUser, UserRest.class);

        return returnValue;
    }

    @PutMapping(path="/{userId}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public UserRest updateUser(@RequestBody UserDetailsRequestModel userDetails,
                             @PathVariable String userId) {

        UserRest returnValue = new UserRest();

        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(userDetails, userDto);

        UserDto updatedUser = userService.updateUser(userDto, userId);
        BeanUtils.copyProperties(updatedUser, returnValue);

        return returnValue;
    }

    @DeleteMapping(path="/{id}",
            produces = {MediaType.APPLICATION_XML_VALUE,
                    MediaType.APPLICATION_JSON_VALUE})
    public OperationStatusModel deleteUser(@PathVariable String id) {

        OperationStatusModel returnValue = new OperationStatusModel();
        returnValue.setOperationName(RequestOperationName.DELETE.name());

        userService.deleteUser(id);
        returnValue.setOperationResult(RequestOperationStatus.SUCCESS.name());

        return returnValue;
    }

    @GetMapping(produces = {MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    public List<UserRest> getUsers(@RequestParam(value = "page", defaultValue = "0") int page,
                                   @RequestParam(value = "limit", defaultValue = "2") int limit){

        List<UserRest> returnValue = new ArrayList<>();

        List<UserDto> users = userService.getUsers(page, limit);

        for(UserDto userDto : users) {
            /*UserRest userModel = new UserRest();
            BeanUtils.copyProperties(userDto, userModel);

            returnValue.add(userModel);*/

            UserRest userModel = new ModelMapper().map(userDto, UserRest.class);
            returnValue.add(userModel);

        }
        return returnValue;
    }

    //http://localhost:8080/mobile-app-ws/users/user_id/addresses/

    @GetMapping(path = "/{userId}/addresses",
            produces = {MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_XML_VALUE})
    public CollectionModel<AddressRest> getUserAddresses(@PathVariable String userId){

        List<AddressRest> returnValue = new ArrayList<>();

        List<AddressDto> addressDto = addressService.getAddresses(userId);

        if(addressDto != null && !addressDto.isEmpty()){
            Type listType = new TypeToken<List<AddressRest>>() {}.getType();
            returnValue = new ModelMapper().map(addressDto, listType);

            for (AddressRest addressRest : returnValue) {
                Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class)
                        .getUserAddress(userId, addressRest.getAddressId()))
                        .withSelfRel();

                addressRest.add(selfLink);
            }
        }

        Link userLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class).getUser(userId)).withRel("user");
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class).getUserAddresses(userId)).withSelfRel();

        return CollectionModel.of(returnValue, userLink, selfLink);
    }

    //http://localhost:8080/mobile-app-ws/users/user_id/addresses/address_id

    @GetMapping(path = "/{userId}/addresses/{addressId}",
            produces = {MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_XML_VALUE})
    public EntityModel<AddressRest> getUserAddress(@PathVariable String userId, @PathVariable String addressId){

        AddressDto addressDto = addressService.getAddress(addressId);

        ModelMapper modelMapper = new ModelMapper();
        AddressRest returnValue = modelMapper.map(addressDto, AddressRest.class);

        // http://localhost:8080/mobile-app-ws/users/<userId>
        Link userLink = WebMvcLinkBuilder.linkTo(UserController.class).slash(userId).withRel("user");

        // http://localhost:8080/mobile-app-ws/users/<userId>/addresses
        Link userAddressesLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class).getUserAddresses(userId))
                /*.slash(userId)
                .slash("addresses")*/
                .withRel("addresses");

        // http://localhost:8080/mobile-app-ws/users/<userId>/addresses/<addressId>
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class).getUserAddress(userId, addressId))
                /*.slash(userId)
                .slash("addresses")
                .slash(addressId)*/
                .withSelfRel();

        /*//RepresentationModel class provides add() method
        returnValue.add(userLink);
        returnValue.add(userAddressesLink);
        returnValue.add(selfLink);
        Altta EntityModel ile return ettiğimiz için gerek kalmadı
        */

        return EntityModel.of(returnValue, Arrays.asList(userLink, userAddressesLink, selfLink));
    }

    // http://localhost:8080/mobile-app-ws/users/email-verification?token=<token>
    @GetMapping(path = "/email-verification",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public OperationStatusModel verifyEmailToken(@RequestParam(name = "token") String token) {

        OperationStatusModel returnValue = new OperationStatusModel();
        returnValue.setOperationName(RequestOperationName.VERIFY_EMAIL.name());

        boolean isVerified = userService.verifyEmailToken(token);

        if(isVerified){
            returnValue.setOperationResult(RequestOperationStatus.SUCCESS.name());
        } else {
            returnValue.setOperationResult((RequestOperationStatus.ERROR.name()));
        }

        return returnValue;
    }
}