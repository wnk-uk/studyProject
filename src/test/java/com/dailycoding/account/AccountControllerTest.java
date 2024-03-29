package com.dailycoding.account;

import com.dailycoding.domain.Account;
import com.dailycoding.mail.EmailMessage;
import com.dailycoding.mail.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.web.reactive.result.view.CsrfRequestDataValueProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public class AccountControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired AccountRepository accountRepository;

    @MockBean
    EmailService emailService;

    @DisplayName("인증 메일 확인 - 입력값 오류")
    @Test
    void checkEmainToken_with_wrong_input() throws Exception {
        mockMvc.perform(get("/check-email-token")
                .param("token", "sdkldoekd")
                .param("email", "email@email.com"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("account/checked-email"))
                .andExpect(unauthenticated()); //인증된 사용자인것인가
    }

    @DisplayName("인증 메일 확인 - 입력값 정상")
    @Test
    void checkedEmainToken() throws Exception {
        Account account = Account.builder()
                            .email("test@email.com")
                            .password("12345678")
                            .nickname("wnkee")
                            .build();
        Account newAccount = accountRepository.save(account);
        newAccount.generateEmailCheckToken();

        mockMvc.perform(get("/check-email-token")
                            .param("token", newAccount.getEmailCheckToken())
                            .param("email", newAccount.getEmail()))
                            .andExpect(status().isOk())
                            .andExpect(model().attributeDoesNotExist("error"))
                            .andExpect(model().attributeExists("nickname"))
                            .andExpect(model().attributeExists("numberOfUser"))
                            .andExpect(view().name("account/checked-email"))
                            .andExpect(authenticated().withUsername("wnkee"));
    }

    @DisplayName("회원 가입 화면 보이는지 테스트")
    @Test
    void signUpForm() throws Exception {
        mockMvc.perform(get("/sign-up"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().attributeExists("signUpForm"))
                .andExpect(unauthenticated());
    }

    @DisplayName("회원 가입 처리 - 입력값 정상")
    @Test
    void signUpSubmit_with_correct_input() throws Exception {
        mockMvc.perform(post("/sign-up")
                            .param("nickname", "uke")
                            .param("email", "dailycodnig0211@naver.com")
                            .param("password", "12345678")
                            .with(csrf()))
                            .andExpect(status().is3xxRedirection())
                            .andExpect(view().name("redirect:/"))
                            .andExpect(authenticated().withUsername("uke"));

        Account account = accountRepository.findByEmail("dailycodnig0211@naver.com");
        assertNotNull(account);
        assertNotEquals(account.getPassword(), "12345678");
        assertNotNull(account.getEmailCheckToken());
        //assertTrue(accountRepository.existsByEmail("rndwh11@naver.com"));
        then(emailService).should().sendEmail(any(EmailMessage.class));

    }
}