package org.example.carebridge.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.carebridge.domain.user.doctor.entity.DoctorLicense;
import org.example.carebridge.domain.user.doctor.entity.Portfolio;
import org.example.carebridge.domain.user.doctor.repository.DoctorLicenseRepository;
import org.example.carebridge.domain.user.doctor.repository.PortfolioRepository;
import org.example.carebridge.domain.user.dto.login.UserLoginRequestDto;
import org.example.carebridge.domain.user.dto.login.UserLoginResponseDto;
import org.example.carebridge.domain.user.dto.signup.UserDoctorSignupRequestDto;
import org.example.carebridge.domain.user.dto.signup.UserPatientSignupRequestDto;
import org.example.carebridge.domain.user.dto.signup.UserSignupRequestDto;
import org.example.carebridge.domain.user.dto.signup.UserSignupResponseDto;
import org.example.carebridge.domain.user.dto.update.UserUpdateRequestDto;
import org.example.carebridge.domain.user.dto.update.UserUpdateResponseDto;
import org.example.carebridge.domain.user.entity.DoctorPortfolioImage;
import org.example.carebridge.domain.user.entity.User;
import org.example.carebridge.domain.user.entity.UserProfileImage;
import org.example.carebridge.domain.user.enums.UserRole;
import org.example.carebridge.domain.user.enums.UserStatus;
import org.example.carebridge.domain.user.repository.DoctorPortfolioRepository;
import org.example.carebridge.domain.user.repository.UserProfileImageRepository;
import org.example.carebridge.domain.user.repository.UserRepository;
import org.example.carebridge.global.entity.RefreshToken;
import org.example.carebridge.global.exception.BadValueException;
import org.example.carebridge.global.exception.CustomException;
import org.example.carebridge.global.exception.ExceptionType;
import org.example.carebridge.global.repository.RefreshTokenRepository;
import org.example.carebridge.global.service.TokenService;
import org.example.carebridge.global.util.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

// test

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final DoctorLicenseRepository doctorLicenseRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final UserProfileUploadService userProfileUploadService;
    private final UserProfileImageRepository userProfileImageRepository;
    private final DoctorPortfolioUploadService doctorPortfolioUploadService;
    private final DoctorPortfolioRepository doctorPortfolioRepository;

    @Transactional
    public UserSignupResponseDto patientSignup(
            UserPatientSignupRequestDto requestDto) {

        User user = buildUser(requestDto);

        //유효성 검사
        Optional<User> findUser = userRepository.findByEmail(requestDto.getEmail());

        if (findUser.isPresent()) {
            if (findUser.get().getUserStatus() == UserStatus.DELETE
                    || findUser.get().getUserStatus() == UserStatus.ACTIVE) {
                throw new RuntimeException("이미 존재하는 유저 입니다.");
            }
        }

        userRepository.save(user);

        return new UserSignupResponseDto(user.getId(), user.getEmail(), user.getUserRole(), user.getOAuth());

    }

    @Transactional
    public UserSignupResponseDto doctorSignup(
            UserDoctorSignupRequestDto requestDto) {

        //유효성 검사
        Optional<User> findUser = userRepository.findByEmail(requestDto.getEmail());

        if (findUser.isPresent()) {
            if (findUser.get().getUserStatus() == UserStatus.DELETE
                    || findUser.get().getUserStatus() == UserStatus.ACTIVE) {
                throw new RuntimeException("이미 존재하는 유저 입니다.");
            }
        }

        User user = buildUser(requestDto);
        user.updateUserRole(UserRole.DOCTOR);
        User savedUser = userRepository.save(user);

        DoctorLicense doctorLicense =
                new DoctorLicense(savedUser, requestDto.getHospitalName());
        DoctorLicense savedLicense = doctorLicenseRepository.save(doctorLicense);

        Portfolio portfolio = new Portfolio(savedLicense, requestDto.getPortfolio());
        portfolioRepository.save(portfolio);

        return new UserSignupResponseDto(savedUser.getId(), savedUser.getEmail(), savedUser.getUserRole(), savedUser.getOAuth());

    }

    //Login
    @Transactional
    public UserLoginResponseDto login(UserLoginRequestDto requestDto) {

        User user;

        if(requestDto.getId() != null) {
            user = userRepository.findByIdOrElseThrow(requestDto.getId());
        }
        else {
            String email = requestDto.getEmail();

            String password = requestDto.getPassword();

            user = userRepository.findByEmailOrElseThrow(email);

            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new BadCredentialsException("비밀번호 오류");
            }
        }


        String accessToken = jwtUtil.generateAccessToken(user.getId());
        log.info("Access 토큰 생성 : {}", accessToken);

        //RefreshToken Table 에서 해당 사용자가 없다면 발급. 최초 로그인
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        log.info("Refresh 토큰 생성 : {}", refreshToken);

        Optional<RefreshToken> oldRefreshToken = refreshTokenRepository.findByUser(user);
        if (oldRefreshToken.isEmpty()) {
            refreshTokenRepository.save(new RefreshToken(user, refreshToken));
        } else {
            oldRefreshToken.get().updateRefreshToken(refreshToken);
        }
        //RefreshToken Table 에서 해당 사용자의 토큰이 만료되었다면, 재발급.
        return new UserLoginResponseDto(user.getId(), user.getEmail(), accessToken, refreshToken);

    }

    //Update
    @Transactional
    public UserUpdateResponseDto updateUser(UserUpdateRequestDto requestDto, User user) {

        user.updateProfile(requestDto.getAddress(), requestDto.getPhone(), requestDto.getImageUrl());

        return new UserUpdateResponseDto(user);

    }

    //Profile Image Upload
    @Transactional
    public String uploadProfileImage(MultipartFile profileImage, User user) {
        UserProfileImage userProfileImage;
        try {
            userProfileImage = userProfileUploadService.uploadAndSaveMetaData(user, profileImage);
        } catch (IOException e) {
            log.error("파일 업로드 실패 : {} ", e.getMessage());
            throw new RuntimeException("파일 업로드 실패 : " + e.getMessage(), e);
        }
        userProfileImageRepository.save(userProfileImage);
        user.updateImage(userProfileImage.getUrl());

        return userProfileImage.getUrl();

    }

    //Portfolio Upload
    @Transactional
    public String uploadPortfolio(MultipartFile portfolio, User user){
        DoctorPortfolioImage doctorPortfolioImage;
        try {
            doctorPortfolioImage = doctorPortfolioUploadService.DoctorUploadAndSaveMetaData(user, portfolio);
        } catch (IOException e) {
            log.error("파일 업로드 실패 : {} ", e.getMessage());
            throw new RuntimeException("파일 업로드 실패 : " + e.getMessage(), e);
        }
        doctorPortfolioRepository.save(doctorPortfolioImage);
        user.updateImage(doctorPortfolioImage.getUrl());
        return doctorPortfolioImage.getUrl();
    }

    //회원 탈퇴
    @Transactional
    public void deleteUser(User user, String password) {
        //비밀번호 교차 검증 로직
       if(!passwordEncoder.matches(password, user.getPassword())) {
           throw new BadValueException(ExceptionType.WRONG_PASSWORD);
       }

       user.updateStatus(UserStatus.DELETE);
       userRepository.save(user);
    }

    private User buildUser(UserSignupRequestDto userSignupRequestDto) {
        return User.builder()
                .email(userSignupRequestDto.getEmail())
                .password(getEncodingPassword(userSignupRequestDto.getPassword()))
                .userName(userSignupRequestDto.getName())
                .phoneNum(userSignupRequestDto.getPhone())
                .address(userSignupRequestDto.getAddress())
                .birthday(userSignupRequestDto.getBirth())
                .build();
    }

    private String getEncodingPassword(String password) {
        return passwordEncoder.encode(password);
    }


}

