package com.openbake.server.repository;

import com.openbake.server.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findByIsActiveTrue();
    Optional<Category> findByName(String name);
}
