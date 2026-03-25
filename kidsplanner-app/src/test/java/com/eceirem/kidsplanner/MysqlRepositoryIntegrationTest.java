package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MysqlRepositoryIntegrationTest {

  @AfterEach
  void tearDown() {
    RepositoryFactory.resetConfigurationForTests();
  }

  private void configureFactory() {
    RepositoryFactory.configureMysql(MysqlTestFixture.jdbcUrl(), MysqlTestFixture.user(), MysqlTestFixture.password());
  }

  @Test
  void kidAndActivityFullCrud() {
    configureFactory();
    IRepository<Kid> kids = new MysqlRepository<Kid>(MysqlTestFixture.jdbcUrl(), MysqlTestFixture.user(),
        MysqlTestFixture.password(), MysqlRepository.EntityKind.KID);
    IRepository<Activity> acts = new MysqlRepository<Activity>(MysqlTestFixture.jdbcUrl(), MysqlTestFixture.user(),
        MysqlTestFixture.password(), MysqlRepository.EntityKind.ACTIVITY);

    Kid k = new Kid("MySQLKid", 2014, "n");
    kids.save(k);
    assertNotNull(k.getId());

    Activity a = new Activity("Lesson", LocalDate.of(2026, 4, 1), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, "Lab", "x", k.getId());
    acts.save(a);
    assertNotNull(a.getId());

    assertEquals(1, kids.findAll().size());
    Activity got = acts.findById(a.getId());
    assertEquals("Lesson", got.getTitle());

    Activity upd = new Activity("Lesson2", LocalDate.of(2026, 4, 2), LocalTime.of(11, 0), LocalTime.of(12, 0),
        ActivityType.PLAY, "P", "y", k.getId());
    upd.setId(a.getId());
    acts.update(upd);
    assertEquals("Lesson2", acts.findById(a.getId()).getTitle());

    acts.delete(a.getId());
    kids.delete(k.getId());
    assertTrue(acts.findAll().isEmpty());
    assertTrue(kids.findAll().isEmpty());
  }

  @Test
  void createsDatabaseWhenNameMissingInUrl() throws Exception {
    String base = MysqlTestFixture.jdbcUrl();
    int q = base.indexOf('?');
    String prefix = base.substring(0, base.lastIndexOf('/'));
    String params = q < 0 ? "" : base.substring(q);
    String newDbUrl = prefix + "/brandnewdb_it" + params;

    IRepository<Kid> kids = new MysqlRepository<Kid>(newDbUrl, MysqlTestFixture.user(), MysqlTestFixture.password(),
        MysqlRepository.EntityKind.KID);
    Kid k = new Kid("Boot", null, "");
    kids.save(k);
    assertNotNull(k.getId());

    try (Connection c = DriverManager.getConnection(MysqlTestFixture.jdbcUrl(), MysqlTestFixture.user(),
        MysqlTestFixture.password());
        Statement st = c.createStatement()) {
      st.executeUpdate("DROP DATABASE IF EXISTS brandnewdb_it");
    }
  }

  @Test
  void wrongPasswordThrowsFriendlyMessage() {
    RepositoryException ex = assertThrows(RepositoryException.class, () ->
        new MysqlRepository<Kid>(MysqlTestFixture.jdbcUrl(), MysqlTestFixture.user(), "wrong-password-xyz",
            MysqlRepository.EntityKind.KID));
    assertTrue(ex.getMessage().contains("MySQL") || ex.getMessage().toLowerCase().contains("auth"));
  }

  @Test
  void connectionRefusedUsesConnectivityMessage() {
    RepositoryException ex = assertThrows(RepositoryException.class, () ->
        new MysqlRepository<Kid>("jdbc:mysql://127.0.0.1:17/nope?useSSL=false&allowPublicKeyRetrieval=true",
            "root", "x", MysqlRepository.EntityKind.KID));
    assertTrue(ex.getMessage().contains("MySQL") || ex.getMessage().contains("connect"));
  }

  @Test
  void updateKidMissingIdThrows() {
    configureFactory();
    IRepository<Kid> kids = new MysqlRepository<Kid>(MysqlTestFixture.jdbcUrl(), MysqlTestFixture.user(),
        MysqlTestFixture.password(), MysqlRepository.EntityKind.KID);
    Kid k = new Kid("a", null, "");
    RepositoryException ex = assertThrows(RepositoryException.class, () -> kids.update(k));
    assertTrue(ex.getMessage().contains("id missing"));
  }

  @Test
  void updateKidNotFoundThrows() {
    configureFactory();
    IRepository<Kid> kids = new MysqlRepository<Kid>(MysqlTestFixture.jdbcUrl(), MysqlTestFixture.user(),
        MysqlTestFixture.password(), MysqlRepository.EntityKind.KID);
    Kid k = new Kid("a", null, "");
    k.setId(999999);
    RepositoryException ex = assertThrows(RepositoryException.class, () -> kids.update(k));
    assertTrue(ex.getMessage().contains("not found"));
  }

  @Test
  void findByIdMissingReturnsNull() {
    configureFactory();
    IRepository<Kid> kids = new MysqlRepository<Kid>(MysqlTestFixture.jdbcUrl(), MysqlTestFixture.user(),
        MysqlTestFixture.password(), MysqlRepository.EntityKind.KID);
    assertNull(kids.findById(42424242));
  }
}
