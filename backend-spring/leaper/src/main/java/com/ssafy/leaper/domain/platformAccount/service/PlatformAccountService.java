package com.ssafy.leaper.domain.platformAccount.service;

import com.ssafy.leaper.domain.platformAccount.dto.request.PlatformAccountCreateRequest;
import com.ssafy.leaper.domain.platformAccount.dto.response.PlatformAccountResponse;

import java.util.List;

public interface PlatformAccountService {

    void createPlatformAccounts(Integer influencerId, List<PlatformAccountCreateRequest> requests);

    List<PlatformAccountResponse> getPlatformAccountsByInfluencer(Integer influencerId);

    PlatformAccountResponse getPlatformAccountById(Integer platformAccountId);

    void deletePlatformAccount(Integer influencerId, Integer platformAccountId);
}