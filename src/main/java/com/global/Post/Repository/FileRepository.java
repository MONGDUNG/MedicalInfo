package com.global.Post.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.global.Post.Data.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Integer> {

}
