package com.asr.grasp.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.asr.grasp.objects.GeneralObject;
import com.asr.grasp.objects.ReconstructionObject;
import com.asr.grasp.utils.Defines;

import org.springframework.stereotype.Repository;

/**
 * Interface with the Postgres Database table Reconstructions.
 *
 * Created by ariane on 13/07/18.
 */
@Repository
public class ReconstructionsModel extends BaseModel {

    final ColumnEntry id = new ColumnEntry(1, "id", Defines.INT);
    final ColumnEntry ownerId = new ColumnEntry(2, "owner_id", Defines.INT);
    final ColumnEntry anscestor = new ColumnEntry(3, "ancestor", Defines
            .STRING);
    final ColumnEntry inferenceType = new ColumnEntry(4, "inference_type",
            Defines.STRING);
    final ColumnEntry jointInferences = new ColumnEntry(5,
            "joint_inferences", Defines.STRING);
    final ColumnEntry label = new ColumnEntry(6, "label", Defines.STRING);
    final ColumnEntry model = new ColumnEntry(7, "model", Defines.STRING);
    final ColumnEntry msa = new ColumnEntry(8, "msa", Defines.STRING);
    final ColumnEntry node = new ColumnEntry(9, "node", Defines.STRING);
    final ColumnEntry numThreads = new ColumnEntry(10, "num_threads",
            Defines.INT);
    final ColumnEntry reconstructedTree = new ColumnEntry
            (11, "reconstructed_tree", Defines.STRING);
    final ColumnEntry sequences = new ColumnEntry(12, "sequences", Defines
            .STRING);
    final ColumnEntry tree = new ColumnEntry(13, "tree", Defines.STRING);

    /**
     * Creates a reconstruction object given a pointer to a results set that
     * contains a reference to the current row.
     *
     * @param rawRecons
     * @return
     * @throws SQLException
     */
    private ReconstructionObject createFromDB(ResultSet rawRecons)
            throws SQLException{
        ReconstructionObject reconstruction = new ReconstructionObject();
        reconstruction.setId(rawRecons.getInt(id.getLabel()));
        reconstruction.setOwnerId(rawRecons.getInt(ownerId
                .getLabel()));
        reconstruction.setJointInferences(rawRecons.getString(jointInferences
                .getLabel()));
        reconstruction.setAncestor(rawRecons.getString
                (anscestor.getLabel()));
        reconstruction.setInferenceType(rawRecons.getString
                (inferenceType.getLabel()));
        reconstruction.setLabel(rawRecons.getString(label
                .getLabel()));
        reconstruction.setModel(rawRecons.getString(model.getLabel()));
        reconstruction.setMsa(rawRecons.getString(msa.getLabel
                ()));
        reconstruction.setNode(rawRecons.getString(node.getLabel()));
        reconstruction.setNumThreads(rawRecons.getInt(numThreads
                .getLabel()
        ));
        reconstruction.setReconTree(rawRecons.getString
                (reconstructedTree.getLabel()));
        reconstruction.setSequences(rawRecons.getString(sequences
                .getLabel()));
        reconstruction.setTree(rawRecons.getString(tree.getLabel()));

        return reconstruction;
    }

    /**
     * Creates the insert statement for a reconstruction.
     */
    private String insertIntoDb(ReconstructionObject recon) {
        String query = "INSERT INTO web.reconstructions(owner_id, ancestor, " +
                "inference_type, joint_inferences, label, model, msa, node, " +
                "num_threads, reconstructed_tree, sequences, tree) VALUES(?," +
                "?,?,?,?,?,?,?,?,?,?,?);";

        try {
            Connection con = DriverManager.getConnection(dbUrl, dbUsername,
                    dbPassword);
            PreparedStatement statement = con.prepareStatement(query);
            // Need to set all the parameters
            // Note the parameter index corrosponds to where in the statement
            // above the value has been assigned FOR consistency try do the
            // same as the model BUT it is up to the programmer to confirm
            // this is all correct.
            statement.setInt(1, recon.getOwnerId());
            statement.setString(2, recon.getAncestor());
            statement.setString(3, recon.getInferenceType());
            statement.setString(4, recon.getJointInferences());
            statement.setString(5, recon.getLabel());
            statement.setString(6, recon.getModel());
            statement.setString(7, recon.getMsa());
            statement.setString(8, recon.getNode());
            statement.setInt(9, recon.getNumThreads());
            statement.setString(10, recon.getReconTree());
            statement.setString(11, recon.getSequences());
            statement.setString(12, recon.getTree());

            // Deletes the record from the model
            statement.executeUpdate();
            return null;
        } catch (Exception e) {
            return "recon.insert.fail";
        }
    }

    /**
     * Gets the reconstructions for a current user.
     *
     * Returns all the reconstructions that the user owns and has access to.
     *
     * ToDo: also get the reconstructions that a group is part of.
     *
     * @return Set<Reconstruction>
     */
    public HashSet<ReconstructionObject> getAllForUser(int userId) {
        ResultSet rawRecons = queryOnId("SELECT * FROM " +
                "web.reconstructions AS r" +
                " LEFT JOIN web.share_users AS su ON su.r_id = " +
                "r.id WHERE " +
                "su.u_id=?;", userId);
        // Need to convert each of the sets to a Reconstruction object
        if (rawRecons == null) {
            return null;
        }

        // Try format the data and create reconstruction objects to return to
        // the user.
        HashSet<ReconstructionObject> reconstructions = new HashSet<>();
        try {
            /**
             * Raw reconstructions maintains a pointer to each row. Each time
             * we call next() we move along the data structure. When we are
             * calling 'getInt' or 'getString' we are calling it on that row.
             */
            while (rawRecons.next()) {
                // add to our set
                reconstructions.add(createFromDB(rawRecons));
            }
        }
        catch (Exception e) {
            System.err.println("Was not able to get reconstructions.");
            System.err.println(e);
        }
        return reconstructions;
    }

    /**
     * Gets the reconstruction Ids for a user.
     *
     * Returns all the reconstruction IDs that the user owns and has access to.
     * We use this so we don't store all the reconstructions for a user in
     * memory as it is unlikely that they will choose everyone and loading
     * them will waste IO/slow down the machiene.
     *
     * Returns them segregated into reconstructions the user owns and
     * reconstructions they just have access to.
     *
     * ToDo: also get the reconstructions that a group is part of.
     *
     * @return Set<Reconstruction>
     */
    public HashMap<Integer, ArrayList<GeneralObject>> getReconsForUser(int
                                                                            userId) {
        ResultSet rawRecons = queryOnId(
                "SELECT r.label as label, " +
                " r.owner_id as owner_id, " +
                "r.id as r_id," +
                        "pg_size_pretty(CAST (pg_column_size(r.ancestor) + " +
                        "pg_column_size(r.reconstructed_tree) + " +
                        "pg_column_size" +
                        "(r.joint_inferences) as BIGINT)) as size," +
                        "TO_CHAR(r.updated_at, 'Dy DD Mon YYYY') as " +
                        "updated_at " +
                " FROM web.reconstructions AS r" +
                " LEFT JOIN web.share_users AS su ON su.r_id = " +
                "r.id " +
                        "WHERE su.u_id=?;", userId);
        // Need to convert each of the sets to a Reconstruction object
        if (rawRecons == null) {
            return null;
        }

        // Try format the data and create reconstruction objects to return to
        // the user.
        ArrayList<GeneralObject> ownedRecons = new ArrayList<>();
        ArrayList<GeneralObject> sharedWithRecons = new ArrayList<>();

        try {
            /**
             * Raw reconstructions maintains a pointer to each row. Each time
             * we call next() we move along the data structure. When we are
             * calling 'getInt' or 'getString' we are calling it on that row.
             */
            while (rawRecons.next()) {
                int reconOwnerId = rawRecons.getInt("owner_id");
                int reconId = rawRecons.getInt("r_id");
                String reconLabel = rawRecons.getString("label");
                String updatedAt = rawRecons.getString("updated_at");
                String size = rawRecons.getString("size");
                // Check if the user owns this reconstruction
                if (reconOwnerId == userId) {
                    ownedRecons.add(new GeneralObject(reconId, reconLabel,
                            size, updatedAt));
                } else {
                    sharedWithRecons.add(new GeneralObject(reconId,
                            reconLabel, size, updatedAt));
                }
            }
        }
        catch (Exception e) {
            System.err.println("Was not able to get reconstructions.");
            System.err.println(e);
            return null;
        }
        HashMap<Integer, ArrayList<GeneralObject>> recons = new HashMap<>();
        recons.put(Defines.OWNER_ACCESS, ownedRecons);
        recons.put(Defines.MEMBER_ACCESS, sharedWithRecons);

        return recons;
    }

    /**
     * Gets a reconstruction by ID. A user ID is also passed so we need to
     * confirm that the user has access to this reconstruction.
     *
     * @return null if no reconstruction matches those configs
     */
    public ReconstructionObject getById(int reconId, int
            userId) {
        String query = "SELECT r.id, r.owner_id, " +
                "r.ancestor, r" +
                ".inference_type, r.joint_inferences, r.label, " +
                "r.model, r.msa, r.node," +
                " r.num_threads, r.reconstructed_tree, " +
                "r.sequences, r.tree" +
                " FROM " +
                "web.reconstructions AS r LEFT JOIN web.share_users AS su ON " +
                "su.r_id=r.id WHERE " +
                "r.id=? AND su.u_id=?;";
        try {
            Connection con = DriverManager.getConnection(dbUrl, dbUsername,
                    dbPassword);
            PreparedStatement statement = con.prepareStatement(query);
            statement.setInt(1, reconId);
            statement.setInt(2, userId);

            ResultSet rawRecons = statement.executeQuery();
            // If we have an entry convert it to the correct format.
            if (rawRecons.next()) {
                return createFromDB(rawRecons);
            }
            return null;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }


    /**
     * Gets a reconstruction by ID. A user ID is also passed so we need to
     * confirm that the user has access to this reconstruction.
     *
     * @return null if no reconstruction matches those configs
     */
    public int getIdByLabel(String reconLabel, int
            userId) {
        String query = "SELECT r.id FROM " +
                "web.reconstructions AS r " +
                " LEFT JOIN web.share_users AS su ON su.r_id = " +
                "r.id WHERE " +
                "r.label=? AND su.u_id=?;";
        try {
            Connection con = DriverManager.getConnection(dbUrl, dbUsername,
                    dbPassword);
            PreparedStatement statement = con.prepareStatement(query);
            statement.setString(1, reconLabel);
            statement.setInt(2, userId);

            return getId(statement.executeQuery());
        } catch (Exception e) {
            System.out.println(e);
        }
        return Defines.ERROR;
    }

    /**
     * Checks whether a user has access to a reconstruction or not.
     *
     * Will return False if either the reconstruction doesn't exist or the
     * user hasn't been granted access.
     *
     * @return null if no reconstruction matches those configs
     */
    public int getUsersAccess(int reconId, int
            userId) {
        String query = "SELECT r.owner_id as owner_id, " +
                "su.u_id as user_id " +
                "FROM web.reconstructions AS r " +
                "LEFT JOIN web.share_users AS su ON su.r_id = " +
                "r.id" +
                " WHERE su.u_id=? AND r.id=?;";

         ResultSet rawRecons = runTwoIdQuery(query, reconId, userId, 2,
                    1);
         try {
            // Get the ownerId and userId if the reconstruction exists
            if (rawRecons.next()) {
                int reconOwnerId = rawRecons.getInt("owner_id");
                int reconUserId = rawRecons.getInt("user_id");
                if (reconOwnerId == reconUserId) {
                    return Defines.OWNER_ACCESS;
                }
                return Defines.MEMBER_ACCESS;
            }
            return Defines.NO_ACCESS;
        } catch (Exception e) {
            System.out.println(e);
            return Defines.ERROR;
        }
    }

    /**
     * Checks if a label exists.
     * @param label
     * @return
     */
    public String doesExist(String label) {
        int reconId = getIdOnUniqueString("SELECT id FROM " +
                "web.reconstructions WHERE label=?;", label);
        if (reconId != Defines.FALSE) {
            return "Reconstructions must have a unique label, a " +
                    "reconstruction already exists with that label.";
        }
        return null;
    }

    /**
     * Save a reconstruction to the model. The owner should already be
     * stored in the owner_id attribute of the reconstruction.
     *
     * Note: We want to return the error message so that upstream we don't
     * have to find out what casued the error and do duplicate effort (e.g.
     * did it fail because there was already a reconstruction with that name).
     *
     * Look in the messages.properties files for the error messages or to add
     * new ones.
     *
     * @return  null on success or an
     */
    public String save(ReconstructionObject reconstruction) {
        // Check that the reconstruction name is unique
        int reconId = getIdOnUniqueString("SELECT id FROM " +
                "web.reconstructions WHERE label=?;", reconstruction.getLabel
                ());
        if (reconId != Defines.FALSE) {
            return "recon.label.duplicate";
        }
        // Save the reconstruction to both the recon table and to the user
        // share table.
        String err = insertIntoDb(reconstruction);
        if (err != null) {
            return err;
        }
        // Check we have this reconstruction in the model also update the
        // reconstruction ID now it has been assigned an ID.
        reconId = getId(queryOnString("SELECT id FROM web.reconstructions " +
                "WHERE label=?;", reconstruction.getLabel()));
        // If there is an error here it wasn't able to be inserted. Let the
        // user know to try again.
        if (reconId == Defines.ERROR) {
            return "fail";
        }
        // Otherwise we can set the reconstruction ID and add the owner to
        // the share_users table.
        reconstruction.setId(reconId);
        err = shareWithUser(reconId, reconstruction.getOwnerId
                ());
        if (err != null) {
            return "fail";
        }
        return null;
    }


    /**
     * Deletes a reconstruction and all it's data.
     *
     * Also deletes the reconstruction from ther user shares and group shares
     * table.
     *
     * @param reconId
     * @return
     */
    public String delete(int reconId) {
        // Delete the reconstruction from the share_users table
        String query = "DELETE FROM web.share_users WHERE r_id=?;";
        deleteOnId(query, reconId);
        // Delete from the group users table
        query = "DELETE FROM web.share_groups WHERE r_id=?;";
        deleteOnId(query, reconId);
        // Delete the reconstruction
        query = "DELETE FROM web.reconstructions WHERE id=?;";

        if (deleteOnId(query, reconId) == false) {
            return "fail";
        }
        return null;
    }

    /**
     * Deletes a reconstruction and all it's data.
     *
     * Also deletes the reconstruction from ther user shares and group shares
     * table.
     *
     * @param
     * @return
     */
    public String deleteByIdList(ArrayList<Integer> reconIds) {
        // Delete the reconstruction from the share_users table
        String query = "DELETE FROM web.share_users WHERE r_id IN ?;";
        queryOnIds(query, reconIds);
        // Delete from the group users table
        query = "DELETE FROM web.share_groups WHERE r_id IN ?;";
        queryOnIds(query, reconIds);
        // Delete the reconstruction
        query = "DELETE FROM web.reconstructions WHERE id IN ?;";
        if (deleteOnIds(query, reconIds) == null) {
            return "fail";
        }
        return null;
    }

    /**
     * Gets all reconstruction IDs that have not been updated in over a month.
     * @param date
     * @return
     */
    public ArrayList<Integer> findByDateOlder(Long date) {
        String query = "SELECT id FROM web.reconstructions WHERE updated_at >" +
                " ?;";
        return getIdList(query(query));
    }

    /**
     * ShareObject a reconstruction with a user.
     *
     * This assumes that the user and reconstruction ID exist.
     * @param reconId
     * @param userId
     * @return
     */

    public String shareWithUser(int reconId, int userId) {
        String query = "INSERT INTO web.share_users(r_id, u_id) VALUES(?, ?);";
        String err = runTwoUpdateQuery(query, reconId, userId, 1, 2);
        if(err != null) {
            return err;
        }
        return null;
    }

    /**
     * Remove a users' access to a reconstruction.
     * @param reconId
     * @param userId
     * @return
     */
    public String removeUsersAccess(int reconId, int userId) {
        String query = "DELETE FROM web.share_users WHERE r_id = ? AND u_id =" +
                " ?;";
        if (runTwoUpdateQuery(query, reconId, userId, 1, 2) == null) {
            return "fail";
        }
        return null;
    }

}
