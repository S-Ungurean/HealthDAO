package org.dao;

import java.util.Optional;

import org.dao.models.UserDTO;

public interface UserDAO {

    /** Finds a user by their Google subject claim, resolving through the users_by_google_sub lookup table. */
    Optional<UserDTO> findByGoogleSub(String googleSub);

    /** Returns the user record for the given userId, or empty if not found. */
    Optional<UserDTO> findByUserId(String userId);

    /**
     * Creates a new user, guarding the googleSub -> userId lookup with a lightweight transaction
     * so a concurrent first-login race can't create two userIds for one Google account.
     * Returns the user that "won" the race — either the newly created one, or an existing one
     * created by a concurrent request for the same googleSub.
     */
    UserDTO createUser(UserDTO user);
}
