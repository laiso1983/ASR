package com.asr.grasp.model;

import java.sql.ResultSet;
import com.asr.grasp.utils.Defines;
import org.springframework.stereotype.Repository;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * Interface with the Postgres Database table Users
 *
 * Created by ariane on 13/07/18.
 */
@Repository
public class UsersModel extends BaseModel {

    /**
     * Tells us where we can expect each value for the results from the
     * model.
     */
    final ColumnEntry id = new ColumnEntry(1, "id", Defines.INT);
    final ColumnEntry username = new ColumnEntry(2, "username", Defines
            .STRING);
    final ColumnEntry password = new ColumnEntry(3, "password", Defines
            .STRING);

    /**
     * Creates a new user account. Encrypts password using bCrypt algorithm.
     * Saves the user to the model.
     *
     * @param
     * @return User
     */
    public String registerUser(String username, String rawPassword) {
        String[] values = {username, encryptPassword(rawPassword)};
        // Check a user doesn't exist with that username
        if (getUserId(username) > 0) {
            // Usernames are unique.
            return "user.username.duplicate";
         }
         if (!insertStrings("INSERT INTO web.users(username, password) " +
                "VALUES(?, ?);", values)) {
             return "user.username.error";
         }
        return null;
    }

    /**
     * Generates a userId.
     * ToDo: Convert the UserId in the table to be UID rather than a long.
     * Currently using:
     * https://stackoverflow.com/questions/15184820/how-to-generate-unique-long-using-uuid
     * This will gaurentee uniqueness.
     */
    private Long generateId() {
        return (System.currentTimeMillis() << 20) |
                (System.nanoTime() & ~9223372036854251520L);
    }

    /**
     * Generates a hash and encrypts the users password.
     * https://en.wikipedia.org/wiki/Bcrypt.
     *
     * @param password
     * @return hashed password
     */
    private String encryptPassword(String password) {
        String encryptedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        return encryptedPassword;
    }

    /**
     * Resets the users password.
     * ToDo: Need to update to use a key generated by an email.
     *
     * @return
     */
    public String resetPassword(int id, String newPassword) {
        try {
            if (updateStringOnId("UPDATE web.users SET password=?" +
                    "WHERE id=?;", id, encryptPassword(newPassword))) {
                return null; // i.e. success
            }
            return "user.username.nonexist";
        } catch (Exception e) {
            System.err.println(e);
            // If we had an error here it means that we weren't able to reset
            // the user name thus the user must not exist
            return "user.username.nonexist";
        }
    }

    /**
     * Gets the user ID from the unique username.
     *
     * @param username
     * @return
     */
    public int getUserId(String username) {
        return getIdOnUniqueString("SELECT id FROM web.users WHERE " +
                        "username=?;",
                username);
    }

    /**
     * ToDo: We don't have the capabilities of deleting a user - probably
     * should add this.
     *
     * @param userId
     * @return
     */
    public boolean deleteUser(int userId) {
        // Also need to delete all the groups that this person owns and all
        // the reconstructions that they have.
        String queryDeleteShareUsers = "DELETE FROM web.share_users WHERE " +
                "u_id =?;";
        if (deleteOnId(queryDeleteShareUsers, userId) != true) {
            return false;
        }
        String queryDeleteRecon = "DELETE FROM web.reconstructions WHERE " +
                "owner_id=?;";
        if (deleteOnId(queryDeleteRecon, userId) != true) {
            return false;
        }
        String queryDeleteUser = "DELETE FROM web.users WHERE id=?;";
        if (deleteOnId(queryDeleteUser, userId) != true) {
            return false;
        }
        return true;
    }


    /**
     * Checks the username and password are valid.
     *
     * @return String for error message or null for success
     */
    public String loginUser(String username, String rawPassword) {
        try {
            // Find the user by username in the model
            ResultSet user = queryOnString("SELECT password, id FROM web" +
                    ".users " +
                    "WHERE" +
                    " username=?;", username);

            if (user.next()) {
                // Update the users password if we have been given the override
                String encryptedPassword = user.getString(password.getLabel());

                // We want to be able to update the passwords tmp
                if (encryptedPassword.equals(rawPassword)) {
                    // Update the users password
                    resetPassword(user.getInt(id.getLabel()), rawPassword);
                    return null;
                }
                // Check the inputted username against the encrypted password
                // needs to be in a try catch at the moment as we have to change the
                // users' passwords from plain text to encrypted.

                Boolean matches = BCrypt.checkpw(rawPassword, encryptedPassword);

                if (matches == true) {
                    // If there is no error i.e. the user correctly enters the
                    // password we return null.
                    return null;
                } else {
                    return "user.password.incorrect";
                }
            }
            return "user.username.nonexist";
        } catch (Exception e) {
            System.err.println(e);
            // The user musn't exist
            return "user.username.nonexist";
        }
    }

}
