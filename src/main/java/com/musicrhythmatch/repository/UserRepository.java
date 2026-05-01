package com.musicrhythmatch.repository;

import com.musicrhythmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Fetch every user in the database EXCEPT the current user.
     * This is the pool against which we run the matching algorithm.
     */
    List<User> findAllBySpotifyIdNot(String spotifyId);
}