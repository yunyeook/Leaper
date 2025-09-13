package com.ssafy.leaper.domain.file.repository;

import com.ssafy.leaper.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<File, Integer> {
}
