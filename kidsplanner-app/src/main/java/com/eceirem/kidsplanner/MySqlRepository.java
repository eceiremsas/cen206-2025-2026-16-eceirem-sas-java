/**
 * @file MySqlRepository.java
 * @brief MySQL {@link IRepository} for {@link Kid} and {@link Activity}.
 */
package com.eceirem.kidsplanner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL implementation of {@link IRepository} for {@link Kid} and {@link Activity}.
 */
public final class MySqlRepository<T extends HasId<Integer>> implements IRepository<T> {

  /** Row set this repository instance reads and writes. */
  public enum EntityKind {
    /** {@link Kid} table */
    KID,
    /** {@link Activity} table */
    ACTIVITY
  }

  /** JDBC URL including database name. */
  private final String jdbcUrl;
  /** Database user. */
  private final String username;
  /** Database password. */
  private final String password;
  /** Kid vs activity table binding. */
  private final EntityKind kind;

  /**
   * Connects, ensures schema, and binds to one {@link EntityKind}.
   *
   * @param jdbcUrl JDBC URL including database name
   * @param username DB user
   * @param password DB password
   * @param kind kid or activity repository
   */
  public MySqlRepository(String jdbcUrl, String username, String password, EntityKind kind) {
    this.jdbcUrl = jdbcUrl;
    this.username = username;
    this.password = password;
    this.kind = kind;
    ensureSchema();
  }

  /** Opens a connection using {@link #jdbcUrl} and credentials. */
  private Connection connect() throws SQLException {
    return DriverManager.getConnection(jdbcUrl, username, password);
  }

  /** Creates {@code kid} and {@code activity} tables when missing. */
  private void ensureSchema() {
    try {
      ensureDatabaseExistsIfNeeded();
      ensureTables();
    } catch (SQLException e) {
      throw new RepositoryException(buildFriendlySchemaError(e), e);
    }
  }

  private void ensureTables() throws SQLException {
    try (Connection connection = connect(); Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "CREATE TABLE IF NOT EXISTS kid ("
              + "id INT AUTO_INCREMENT PRIMARY KEY,"
              + "display_name VARCHAR(120) NOT NULL,"
              + "birth_year INT NULL,"
              + "notes VARCHAR(512) NULL)"
              + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
      statement.executeUpdate(
          "CREATE TABLE IF NOT EXISTS activity ("
              + "id INT AUTO_INCREMENT PRIMARY KEY,"
              + "kid_id INT NOT NULL,"
              + "title VARCHAR(200) NOT NULL,"
              + "act_date DATE NOT NULL,"
              + "start_time TIME NOT NULL,"
              + "end_time TIME NOT NULL,"
              + "type VARCHAR(32) NOT NULL,"
              + "location VARCHAR(200) NULL,"
              + "notes VARCHAR(512) NULL,"
              + "CONSTRAINT fk_activity_kid FOREIGN KEY (kid_id) REFERENCES kid(id))"
              + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
  }

  /**
   * If the JDBC URL targets a database that does not exist yet, create it.
   * This keeps the app working with the provided docker-compose default (MYSQL_DATABASE=kidsplanner)
   * and also helps when the database was manually deleted.
   */
  private void ensureDatabaseExistsIfNeeded() throws SQLException {
    // Fast path: if connect works, db exists.
    try (Connection ignored = connect()) {
      return;
    } catch (SQLException e) {
      // MySQL error 1049 = Unknown database
      if (e.getErrorCode() != 1049) {
        throw e;
      }
      String dbName = parseDatabaseName(jdbcUrl);
      if (dbName == null || dbName.trim().isEmpty()) {
        throw e;
      }
      String serverUrl = stripDatabaseFromJdbcUrl(jdbcUrl);
      try (Connection serverConn = DriverManager.getConnection(serverUrl, username, password);
          Statement st = serverConn.createStatement()) {
        st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " DEFAULT CHARACTER SET utf8mb4");
      }
    }
  }

  private static String parseDatabaseName(String url) {
    // Expected: jdbc:mysql://host:port/dbname?params...
    int scheme = url.indexOf("://");
    if (scheme < 0) {
      return null;
    }
    int firstSlash = url.indexOf('/', scheme + 3);
    if (firstSlash < 0) {
      return null;
    }
    int q = url.indexOf('?', firstSlash + 1);
    String db = q < 0 ? url.substring(firstSlash + 1) : url.substring(firstSlash + 1, q);
    return db.trim().isEmpty() ? null : db.trim();
  }

  private static String stripDatabaseFromJdbcUrl(String url) {
    // Convert jdbc:mysql://host:port/db?x=y -> jdbc:mysql://host:port/?x=y
    int scheme = url.indexOf("://");
    if (scheme < 0) {
      return url;
    }
    int firstSlash = url.indexOf('/', scheme + 3);
    if (firstSlash < 0) {
      return url;
    }
    int q = url.indexOf('?', firstSlash + 1);
    String params = q < 0 ? "" : url.substring(q);
    return url.substring(0, firstSlash + 1) + params;
  }

  private String buildFriendlySchemaError(SQLException e) {
    // 08xxx: connection issues
    String state = e.getSQLState();
    if (state != null && state.startsWith("08")) {
      return "Could not connect to MySQL. Is Docker MySQL running? (docker compose up -d)";
    }
    if (e.getErrorCode() == 1049) {
      String db = parseDatabaseName(jdbcUrl);
      return "MySQL database not found: " + (db == null ? "(unknown)" : db)
          + ". Create the DB with Docker Compose (MYSQL_DATABASE).";
    }
    if (e.getErrorCode() == 1045) {
      return "MySQL authentication failed (user/password). Default: root / kidsplanner.";
    }
    return "Could not create MySQL schema. Is Docker MySQL running?";
  }

  /**
   * Routes to {@link #saveKid} or {@link #saveActivity} based on {@link #kind}.
   *
   * @param entity row to insert
   */
  @SuppressWarnings("unchecked")
  @Override public void save(T entity) {
    if (kind == EntityKind.KID) {
      saveKid((Kid) entity);
    } else {
      saveActivity((Activity) entity);
    }
  }

  /**
   * @param id primary key
   * @return entity or {@code null}
   */
  @SuppressWarnings("unchecked")
  @Override public T findById(int id) {
    if (kind == EntityKind.KID) {
      return (T) findKidById(id);
    }
    return (T) findActivityById(id);
  }

  /** @return all rows for this repository kind */
  @SuppressWarnings("unchecked")
  @Override public List<T> findAll() {
    if (kind == EntityKind.KID) {
      return (List<T>) findAllKids();
    }
    return (List<T>) findAllActivities();
  }

  /**
   * Routes to {@link #updateKid} or {@link #updateActivity}.
   *
   * @param entity row with non-null id
   */
  @SuppressWarnings("unchecked")
  @Override public void update(T entity) {
    if (kind == EntityKind.KID) {
      updateKid((Kid) entity);
    } else {
      updateActivity((Activity) entity);
    }
  }

  /**
   * Deletes by id; for {@link EntityKind#KID}, removes linked activities first.
   *
   * @param id primary key
   */
  @Override public void delete(int id) {
    try (Connection connection = connect()) {
      if (kind == EntityKind.KID) {
        try (PreparedStatement deleteActivities = connection.prepareStatement("DELETE FROM activity WHERE kid_id = ?")) {
          deleteActivities.setInt(1, id);
          deleteActivities.executeUpdate();
        }
        try (PreparedStatement deleteKid = connection.prepareStatement("DELETE FROM kid WHERE id = ?")) {
          deleteKid.setInt(1, id);
          deleteKid.executeUpdate();
        }
      } else {
        try (PreparedStatement deleteActivity = connection.prepareStatement("DELETE FROM activity WHERE id = ?")) {
          deleteActivity.setInt(1, id);
          deleteActivity.executeUpdate();
        }
      }
    } catch (SQLException e) {
      throw new RepositoryException("MySQL delete operation failed.", e);
    }
  }

  /** Inserts a {@link Kid} row and sets generated id. */
  private void saveKid(Kid kid) {
    String sql = "INSERT INTO kid (display_name, birth_year, notes) VALUES (?,?,?)";
    try (Connection connection = connect();
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, kid.getDisplayName());
      if (kid.getBirthYear() == null) {
        statement.setNull(2, java.sql.Types.INTEGER);
      } else {
        statement.setInt(2, kid.getBirthYear().intValue());
      }
      statement.setString(3, kid.getNotes());
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          kid.setId(keys.getInt(1));
        }
      }
    } catch (SQLException e) {
      throw new RepositoryException("Could not insert kid row in MySQL.", e);
    }
  }

  /** Inserts an {@link Activity} row and sets generated id. */
  private void saveActivity(Activity activity) {
    String sql = "INSERT INTO activity (kid_id,title,act_date,start_time,end_time,type,location,notes) VALUES (?,?,?,?,?,?,?,?)";
    try (Connection connection = connect();
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setInt(1, activity.getKidId().intValue());
      statement.setString(2, activity.getTitle());
      statement.setString(3, activity.getDate().toString());
      statement.setString(4, activity.getStartTime().toString());
      statement.setString(5, activity.getEndTime().toString());
      statement.setString(6, activity.getType().name());
      statement.setString(7, activity.getLocation());
      statement.setString(8, activity.getNotes());
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          activity.setId(keys.getInt(1));
        }
      }
    } catch (SQLException e) {
      throw new RepositoryException("Could not insert activity row in MySQL.", e);
    }
  }

  /** Loads one kid by primary key. */
  private Kid findKidById(int id) {
    String sql = "SELECT id, display_name, birth_year, notes FROM kid WHERE id = ?";
    try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }
        return mapKid(resultSet);
      }
    } catch (SQLException e) {
      throw new RepositoryException("Could not read kid from MySQL.", e);
    }
  }

  /** Loads one activity by primary key. */
  private Activity findActivityById(int id) {
    String sql =
        "SELECT id, kid_id, title, act_date, start_time, end_time, type, location, notes FROM activity WHERE id = ?";
    try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }
        return mapActivity(resultSet);
      }
    } catch (SQLException e) {
      throw new RepositoryException("Could not read activity from MySQL.", e);
    }
  }

  /** Returns all kids ordered by id. */
  private List<Kid> findAllKids() {
    List<Kid> list = new ArrayList<Kid>();
    String sql = "SELECT id, display_name, birth_year, notes FROM kid ORDER BY id";
    try (Connection connection = connect();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      while (resultSet.next()) {
        list.add(mapKid(resultSet));
      }
    } catch (SQLException e) {
      throw new RepositoryException("Could not list kids from MySQL.", e);
    }
    return list;
  }

  /** Returns all activities ordered by date and time. */
  private List<Activity> findAllActivities() {
    List<Activity> list = new ArrayList<Activity>();
    String sql =
        "SELECT id, kid_id, title, act_date, start_time, end_time, type, location, notes FROM activity ORDER BY act_date, start_time, id";
    try (Connection connection = connect();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      while (resultSet.next()) {
        list.add(mapActivity(resultSet));
      }
    } catch (SQLException e) {
      throw new RepositoryException("Could not list activities from MySQL.", e);
    }
    return list;
  }

  /** Updates an existing kid row. */
  private void updateKid(Kid kid) {
    if (kid.getId() == null) {
      throw new RepositoryException("Kid id missing for update.");
    }
    String sql = "UPDATE kid SET display_name=?, birth_year=?, notes=? WHERE id=?";
    try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, kid.getDisplayName());
      if (kid.getBirthYear() == null) {
        statement.setNull(2, java.sql.Types.INTEGER);
      } else {
        statement.setInt(2, kid.getBirthYear().intValue());
      }
      statement.setString(3, kid.getNotes());
      statement.setInt(4, kid.getId().intValue());
      if (statement.executeUpdate() == 0) {
        throw new RepositoryException("Kid to update not found.");
      }
    } catch (SQLException e) {
      throw new RepositoryException("Could not update kid in MySQL.", e);
    }
  }

  /** Updates an existing activity row. */
  private void updateActivity(Activity activity) {
    if (activity.getId() == null) {
      throw new RepositoryException("Activity id missing for update.");
    }
    String sql =
        "UPDATE activity SET kid_id=?, title=?, act_date=?, start_time=?, end_time=?, type=?, location=?, notes=? WHERE id=?";
    try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, activity.getKidId().intValue());
      statement.setString(2, activity.getTitle());
      statement.setString(3, activity.getDate().toString());
      statement.setString(4, activity.getStartTime().toString());
      statement.setString(5, activity.getEndTime().toString());
      statement.setString(6, activity.getType().name());
      statement.setString(7, activity.getLocation());
      statement.setString(8, activity.getNotes());
      statement.setInt(9, activity.getId().intValue());
      if (statement.executeUpdate() == 0) {
        throw new RepositoryException("Activity to update not found.");
      }
    } catch (SQLException e) {
      throw new RepositoryException("Could not update activity in MySQL.", e);
    }
  }

  /** Maps a {@code kid} row to a {@link Kid}. */
  private Kid mapKid(ResultSet resultSet) throws SQLException {
    Kid kid = new Kid();
    kid.setId(resultSet.getInt("id"));
    kid.setDisplayName(resultSet.getString("display_name"));
    int birth = resultSet.getInt("birth_year");
    if (resultSet.wasNull()) {
      kid.setBirthYear(null);
    } else {
      kid.setBirthYear(Integer.valueOf(birth));
    }
    kid.setNotes(resultSet.getString("notes"));
    return kid;
  }

  /** Maps an {@code activity} row to an {@link Activity}. */
  private Activity mapActivity(ResultSet resultSet) throws SQLException {
    Activity activity = new Activity();
    activity.setId(resultSet.getInt("id"));
    activity.setKidId(resultSet.getInt("kid_id"));
    activity.setTitle(resultSet.getString("title"));
    activity.setDate(resultSet.getDate("act_date").toLocalDate());
    activity.setStartTime(resultSet.getTime("start_time").toLocalTime());
    activity.setEndTime(resultSet.getTime("end_time").toLocalTime());
    activity.setType(ActivityType.valueOf(resultSet.getString("type")));
    activity.setLocation(resultSet.getString("location"));
    activity.setNotes(resultSet.getString("notes"));
    return activity;
  }
}
