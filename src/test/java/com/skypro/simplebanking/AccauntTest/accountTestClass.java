package com.skypro.simplebanking.AccauntTest;

import com.skypro.simplebanking.controller.UserController;
import com.skypro.simplebanking.dto.*;
import com.skypro.simplebanking.entity.AccountCurrency;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.plaf.ListUI;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc

public class accountTestClass {
    @Autowired
    UserController userControllerTest;
    @Autowired
    MockMvc mockMvc;

    private static  List<ListUserDTO> listUserDto() {
        List<ListUserDTO> listUserDTOS = new ArrayList<>();
        listUserDTOS.add(new ListUserDTO(1,"user_one", listAccountDTO()));
        listUserDTOS.add(new ListUserDTO(2,"user_two", listAccountDTO()));
        listUserDTOS.add(new ListUserDTO(3,"user_three", listAccountDTO()));
        return listUserDTOS;
    }

    private static  List<ListAccountDTO> listAccountDTO() {
        List<ListAccountDTO> listAccountDTOS = new ArrayList<>();
        listAccountDTOS.add(new ListAccountDTO(1L, AccountCurrency.EUR));
        listAccountDTOS.add(new ListAccountDTO(2L, AccountCurrency.RUB));
        listAccountDTOS.add(new ListAccountDTO(3L, AccountCurrency.USD));
        return listAccountDTOS;
    }

    private static List<AccountDTO> accountDTOList() {
        List<AccountDTO> accountDTOList = new ArrayList<>();
        accountDTOList.add(new AccountDTO(1L,2020L,AccountCurrency.EUR));
        return accountDTOList;
    }


    private static  UserDTO userDTO() {

    return new UserDTO(1L, "User_one", accountDTOList());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_Ok() throws Exception {
        JSONObject createUserRequest = new JSONObject();
        createUserRequest.put("username", "username");
        createUserRequest.put("password", "password");
        mockMvc.perform(post("/user")//создаем запрос
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_Test_Ok() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String listUserDto = objectMapper.writeValueAsString(listUserDto());
        mockMvc.perform(get("/user/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(listUserDto.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyProfile_Test_OK() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        String myProfile = objectMapper.writeValueAsString(userDTO());
        mockMvc.perform(get("/user/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(myProfile.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$username").value("User_one"))
    }

}
