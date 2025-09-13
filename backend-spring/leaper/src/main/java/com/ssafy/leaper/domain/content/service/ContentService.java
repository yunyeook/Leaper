package com.ssafy.leaper.domain.content.service;

import com.ssafy.leaper.domain.content.dto.ContentListResponse;
import com.ssafy.leaper.domain.content.dto.ContentResponse;
import com.ssafy.leaper.domain.content.entity.Content;
import com.ssafy.leaper.domain.content.repository.ContentRepository;
import com.ssafy.leaper.domain.platform.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

}
