package com.ssafy.leaper.domain.influencer.service;

import com.ssafy.leaper.domain.influencer.repository.InfluencerRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class InfluencerCommandService {
    private final InfluencerRepository influencerRepository;

    public ServiceResult<Void> deleteAccountByEmail(String email) {
        influencerRepository.deleteByEmail(email);
        return ServiceResult.ok();
    }
}
