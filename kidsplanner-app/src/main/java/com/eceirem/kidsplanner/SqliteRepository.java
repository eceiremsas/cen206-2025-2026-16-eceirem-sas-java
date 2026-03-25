/**
 * @file SqliteRepository.java
 * @brief SQLite {@link IRepository} for {@link Kid} and {@link Activity}.
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
 * SQLite implementation of {@link IRepository} for {@link Kid} and {@link Activity}.
 */
public final class SqliteRepository<T extends HasId<Integer>> implements IRepository<T> {

  /** Row set this repository instance reads and writes. */
  public enum EntityKind {
    /** {@link Kid} table */
    KID,
    /** {@link Activity} table */
    ACTIVITY
  }

  /** JDBC URL (e.g. {@code jdbc:sqlite:kidsplanner.db}). */
  private final String jdbcUrl;
  /** Kid vs activity table binding. */
  private final EntityKind kind;

  /**
   * Ensures SQLite schema and binds to one {@link EntityKind}.
   *
   * @param jdbcUrl e.g. {@code jdbc:sqlite:kidsplanner.db}
   * @param kind kid or activity repository
   */
  public SqliteRepository(String jdbcUrl, EntityKind kind) {
    this.jdbcUrl = jdbcUrl;
    this.kind = kind;
    ensureSchema();
  }

  /** Opens a SQLite connection. */
  private Connection connect() throws SQLException {
    return DriverManager.getConnection(jdbcUrl);
  }

  /** Creates {@code kid} and {@code activity} tables when missing. */
  private void ensureSchema() {
    try (Connection connection = connect(); Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "CREATE TABLE IF NOT EXISTS kid ("
              + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "display_name TEXT NOT NULL,"
              + "birth_year INTEGER,"
              + "notes TEXT)");
      statement.executeUpdate(
          "CREATE TABLE IF NOT EXISTS activity ("
              + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "kid_id INTEGER NOT NULL,"
              + "title TEXT NOT NULL,"
              + "act_date TEXT NOT NULL,"
              + "start_time TEXT NOT NULL,"
              + "end_time TEXT NOT NULL,"
              + "type TEXT NOT NULL,"
              + "location TEXT,"
              + "notes TEXT,"
              + "FOREIGN KEY (kid_id) REFERENCES kid(id))");
    } catch (SQLException e) {
      throw new RepositoryException("SQLite sema olusturulamadi.", e);
    }
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
      throw new RepositoryException("SQLite silme islemi basarisiz.", e);
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
      throw new RepositoryException("SQLite cocuk kaydi eklenemedi.", e);
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
      throw new RepositoryException("SQLite aktivite kaydi eklenemedi.", e);
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
      throw new RepositoryException("SQLite cocuk okunamadi.", e);
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
      throw new RepositoryException("SQLite aktivite okunamadi.", e);
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
      throw new RepositoryException("SQLite cocuk listesi okunamadi.", e);
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
      throw new RepositoryException("SQLite aktivite listesi okunamadi.", e);
    }
    return list;
  }

  /** Updates an existing kid row. */
  private void updateKid(Kid kid) {
    if (kid.getId() == null) {
      throw new RepositoryException("Guncellenecek cocuk kimligi yok.");
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
        throw new RepositoryException("Guncellenecek cocuk bulunamadi.");
      }
    } catch (SQLException e) {
      throw new RepositoryException("SQLite cocuk guncellenemedi.", e);
    }
  }

  /** Updates an existing activity row. */
  private void updateActivity(Activity activity) {
    if (activity.getId() == null) {
      throw new RepositoryException("Guncellenecek aktivite kimligi yok.");
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
        throw new RepositoryException("Guncellenecek aktivite bulunamadi.");
      }
    } catch (SQLException e) {
      throw new RepositoryException("SQLite aktivite guncellenemedi.", e);
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
    activity.setDate(LocalDate.parse(resultSet.getString("act_date")));
    activity.setStartTime(LocalTime.parse(resultSet.getString("start_time")));
    activity.setEndTime(LocalTime.parse(resultSet.getString("end_time")));
    activity.setType(ActivityType.valueOf(resultSet.getString("type")));
    activity.setLocation(resultSet.getString("location"));
    activity.setNotes(resultSet.getString("notes"));
    return activity;
  }
}
