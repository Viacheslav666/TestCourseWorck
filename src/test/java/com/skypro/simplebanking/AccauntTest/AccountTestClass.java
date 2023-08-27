package com.skypro.simplebanking.AccauntTest;

import com.skypro.simplebanking.controller.UserController;
import com.skypro.simplebanking.dto.*;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.UserService;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc

public class AccountTestClass {

    @Autowired
    MockMvc mockMvc;
    @Mock
    BankingUserDetails userDetails;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;


    private static List<ListUserDTO> listUserDto() {
        List<ListUserDTO> listUserDTOS = new ArrayList<>();
        listUserDTOS.add(new ListUserDTO(1, "user_one", listAccountDTO()));
        listUserDTOS.add(new ListUserDTO(2, "user_two", listAccountDTO()));
        listUserDTOS.add(new ListUserDTO(3, "user_three", listAccountDTO()));
        return listUserDTOS;
    }

    private String getBasicAuthenticationHeader(String username, String password) {
        return "Basic " + Base64Utils.encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
    @BeforeEach
    void createRepository() {
        userService.createUser("username1","password1" );
        userService.createUser("username2","password2" );
        userService.createUser("username3","password3" );
        userService.createUser("username4","password4" );
        userService.createUser("username5","password5" );
    }
    @AfterEach
    void cleanRepository() {
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }


    private User getUserByName(String username){
        return userRepository.findByUsername(username).orElseThrow();
    }
    private long getUserIdByUserName(String username){
        return getUserByName(username).getId();
    }
    private long getAccountIdByUsername(String username){
        long userId = getUserIdByUserName(username);
        long ost = userId % userRepository.count();
        long count = (userId - ost) / userRepository.count();
        return count * 3 * userRepository.count() + ost*3;
    }

    private static List<ListAccountDTO> listAccountDTO() {
        List<ListAccountDTO> listAccountDTOS = new ArrayList<>();
        listAccountDTOS.add(new ListAccountDTO(1L, AccountCurrency.EUR));
        listAccountDTOS.add(new ListAccountDTO(2L, AccountCurrency.RUB));
        listAccountDTOS.add(new ListAccountDTO(3L, AccountCurrency.USD));
        return listAccountDTOS;
    }

    private static List<AccountDTO> accountDTOList() {
        List<AccountDTO> accountDTOList = new ArrayList<>();
        accountDTOList.add(new AccountDTO(1L, 2020L, AccountCurrency.EUR));
        return accountDTOList;
    }


    private static UserDTO userDTO() {

        return new UserDTO(1L, "User_one", accountDTOList());
    }

    @Test
    void createUser_Ok() throws Exception {
        JSONObject createUserRequest = new JSONObject();
        createUserRequest.put("username", "username");
        createUserRequest.put("password", "password");
        mockMvc.perform(post("/user")
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"), "X-SECURITY-ADMIN-KEY")
                        .contentType(MediaType.APPLICATION_JSON)//создаем запрос
                        .content(createUserRequest.toString()))
                .andExpect(status().isOk());
    }
    @Test
    void createUser_Test_TrowUserAlreadyExistsException() throws Exception {
        JSONObject userRequest = new JSONObject();
        userRequest.put("username", "username1");
        userRequest.put("password", "password1");
        mockMvc.perform(post("/user")
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest.toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void depositToAccount_OK() throws Exception {
        JSONObject balanceChangeRequest = new JSONObject();
        balanceChangeRequest.put("amount", 10000L);
        mockMvc.perform(post("/account/deposit/{id}", getAccountIdByUsername("username1"))
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balanceChangeRequest.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(10001));}

    @Test
    void getAllUsers_Test_Ok() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String listUserDto = objectMapper.writeValueAsString(listUserDto());
        mockMvc.perform(get("/user/list")
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(listUserDto.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5));
    }


    @Test
    void getMyProfile_Test_OK() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("username1"))
                .andExpect(jsonPath("$.accounts.length()").value(3));
    }


    @Test
    void getTranzaction_Test_OK() throws Exception {
        JSONObject transfer = new JSONObject();
        transfer.put("fromAccountId", getAccountIdByUsername("username1"));
        transfer.put("toUserId", getUserIdByUserName("username2"));
        transfer.put("toAccountId", getAccountIdByUsername("username2"));
        transfer.put("amount", 1);

        mockMvc.perform(post("/transfer")
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transfer.toString()))
                .andExpect(status().isOk());
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void givenUsers_AdminNoAccess_Error403() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getTranzaction_noOK_WrongAccountCurrency_Status400() throws Exception {
        JSONObject transfer = new JSONObject();
        transfer.put("fromAccountId", 1);
        transfer.put("toUserId", 2);
        transfer.put("toAccountId", 5);
        transfer.put("amount", 1);

        mockMvc.perform(post("/transfer")
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transfer.toString()))
                .andExpect(status().is4xxClientError());
    }
    @Test
    public void withdrawToAccount_OK() throws Exception {
        JSONObject balanceChangeRequest = new JSONObject();
        balanceChangeRequest.put("amount", 1L);
        mockMvc.perform(post("/account/withdraw/{id}", getAccountIdByUsername("username1"))
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balanceChangeRequest.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(0L));
    }

    @Test
    void withdrawFromAccount_noOK_TrowInsufficientFundsException() throws Exception {
        JSONObject balanceChangeRequest = new JSONObject();
        balanceChangeRequest.put("amount", 100L);

        mockMvc.perform(post("/account/withdraw/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balanceChangeRequest.toString()))
                .andExpect(status().is4xxClientError());

    }
    @Test
    public void getUserAccount_ОK() throws Exception {
        mockMvc.perform(get("/account/{id}", getAccountIdByUsername("username1"))
                .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1")))
                .andExpect(status().isOk());
    }
    @Test
    public void getUserAccount_noOk_WhenAccountNotFound() throws Exception {
        mockMvc.perform(get("/account/{id}", 0L)
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1")))
                .andExpect(status().isNotFound());
    }
}
