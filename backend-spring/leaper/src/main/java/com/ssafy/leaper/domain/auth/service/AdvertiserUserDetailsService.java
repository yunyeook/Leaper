package com.ssafy.leaper.domain.auth.service;

import com.ssafy.leaper.domain.advertiser.entity.Advertiser;
import com.ssafy.leaper.domain.advertiser.repository.AdvertiserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvertiserUserDetailsService implements UserDetailsService {

    private final AdvertiserRepository advertiserRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        log.info("Loading advertiser by loginId: {}", loginId);

        Advertiser advertiser = advertiserRepository.findByLoginId(loginId)
                .orElseThrow(() -> {
                    log.warn("Advertiser not found with loginId: {}", loginId);
                    return new UsernameNotFoundException("Advertiser not found with loginId: " + loginId);
                });

        // 탈퇴한 사용자 체크
        if (advertiser.getIsDeleted()) {
            log.warn("Advertiser is deleted - loginId: {}", loginId);
            throw new UsernameNotFoundException("Advertiser is deleted: " + loginId);
        }

        log.info("Advertiser found - advertiserId: {}, loginId: {}", advertiser.getId(), loginId);

        // UserDetails 객체 생성
        return User.builder()
                .username(advertiser.getId().toString()) // Principal name을 advertiser ID로 설정
                .password(advertiser.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADVERTISER")))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(advertiser.getIsDeleted())
                .build();
    }
}