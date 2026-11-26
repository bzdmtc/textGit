package com.example.smart_learning.content.repository;

import com.example.smart_learning.content.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByStatusOrderBySortAscIdAsc(String status);

    List<Banner> findAllByOrderBySortAscIdAsc();
}
