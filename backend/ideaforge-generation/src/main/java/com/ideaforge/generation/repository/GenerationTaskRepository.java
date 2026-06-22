package com.ideaforge.generation.repository;

import com.ideaforge.generation.entity.GenerationTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationTaskRepository extends JpaRepository<GenerationTask, Long> {
}
