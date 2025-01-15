package org.example.carebridge.domain.user.controller;

//import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.carebridge.domain.user.dto.login.UserLoginRequestDto;
import org.example.carebridge.domain.user.dto.login.UserLoginResponseDto;
import org.example.carebridge.domain.user.dto.signup.UserDoctorSignupRequestDto;
import org.example.carebridge.domain.user.dto.signup.UserPatientSignupRequestDto;
import org.example.carebridge.domain.user.dto.signup.UserSignupResponseDto;
import org.example.carebridge.domain.user.dto.update.UserDeleteRequestDto;
import org.example.carebridge.domain.user.dto.update.UserUpdateRequestDto;
import org.example.carebridge.domain.user.dto.update.UserUpdateResponseDto;
import org.example.carebridge.domain.user.entity.User;
import org.example.carebridge.domain.user.service.UserService;
import org.example.carebridge.global.auth.UserDetailsImple;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    //환자 회원가입
    @PostMapping("/signup-patient")
    public ResponseEntity<UserSignupResponseDto> createPatient(
            @RequestBody UserPatientSignupRequestDto userPatientSignupRequestDto) {

        UserSignupResponseDto userSignupResponseDto = userService.patientSignup(userPatientSignupRequestDto);

        return new ResponseEntity<>(userSignupResponseDto, HttpStatus.CREATED);
    }

    //의사 회원가입
    @PostMapping("/signup-doctor")
    public ResponseEntity<UserSignupResponseDto> createDoctor(
            @RequestBody UserDoctorSignupRequestDto userDoctorSignupRequestDto) {

        UserSignupResponseDto userSignupResponseDto = userService.doctorSignup(userDoctorSignupRequestDto);

        return new ResponseEntity<>(userSignupResponseDto, HttpStatus.CREATED);
    }

    //로그인
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(
            @RequestBody UserLoginRequestDto userLoginRequestDto,
            HttpServletResponse response) {

        UserLoginResponseDto userLoginResponseDto = userService.login(userLoginRequestDto);

        //언젠간 삭제됨(배포시)
        ResponseCookie cookie = ResponseCookie.from("Authorization", userLoginResponseDto.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return new ResponseEntity<>(userLoginResponseDto, HttpStatus.OK);
    }

    //프로필 사진 등록
    @PostMapping("/upload/profile-image")
    public ResponseEntity<String> uploadProfileImage(@RequestParam("profile-image")MultipartFile profileImage,
                                                     @AuthenticationPrincipal UserDetailsImple userDetails) {
        User user = userDetails.getUser();
        String profileImageUrl = userService.uploadProfileImage(profileImage, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(profileImageUrl);
    }

    //의사 면허증 등록
    @PostMapping("/upload/doctor-portfolio")
    public ResponseEntity<String> uploadDoctorPortfolio(@RequestParam("doctor-portfolio")MultipartFile portfolio,
                                                        @AuthenticationPrincipal UserDetailsImple userDetails) {
        User user = userDetails.getUser();
        String portfolioUrl = userService.uploadPortfolio(portfolio, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioUrl);
    }

    //회원 탈퇴
    @DeleteMapping
    public void deleteUser(@RequestBody UserDeleteRequestDto userDeleteRequestDto,
                           @AuthenticationPrincipal UserDetailsImple userDetails) {
        User user = userDetails.getUser();
        String password = userDeleteRequestDto.getPassword();
        userService.deleteUser(user, password);
    }

    //사용자 정보 수정
    @PatchMapping("/update-profile")
    public ResponseEntity<UserUpdateResponseDto> update(
            @RequestBody UserUpdateRequestDto userUpdateRequestDto,
            @AuthenticationPrincipal UserDetailsImple userDetailsImple) {

        User user = userDetailsImple.getUser();
        UserUpdateResponseDto userUpdateResponseDto = userService.updateUser(userUpdateRequestDto,user);

        return new ResponseEntity<>(userUpdateResponseDto, HttpStatus.OK);
    }
}
