package com.ssafy.leaper.domain.file.repository;

import com.ssafy.leaper.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Integer> {
    Optional<File> findByAccessKey(String accessKey);
}
