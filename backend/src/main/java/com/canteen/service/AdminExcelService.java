package com.canteen.service;

import com.canteen.dto.ImportResult;
import com.canteen.entity.MenuItem;
import com.canteen.entity.User;
import com.canteen.repository.MenuItemRepository;
import com.canteen.repository.UserRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Excel round-trip for Admin: export Users / MenuItems to .xlsx,
 * import edited files back with validation + partial-update support.
 *
 * <p>Security: password hashes are NEVER exported. Imports only accept a
 * {@code NewPassword} column (plain text in Excel) which is BCrypt-hashed
 * server-side. Blank NewPassword = keep existing hash.
 */
@Service
public class AdminExcelService {

  private static final Logger log = LoggerFactory.getLogger(AdminExcelService.class);
  private static final DataFormatter FMT = new DataFormatter();

  // Users sheet: last column is write-only input, never exported with a value.
  static final String[] USER_HEADERS = {
      "ID", "StudentID/Username", "Name", "Email", "Role",
      "TabBalance", "Status", "CreatedAt", "UpdatedAt", "NewPassword (optional - blank keeps existing)"
  };
  static final String[] MENU_HEADERS = {
      "ID", "Name", "Description", "Price", "Category",
      "Available", "ImageURL", "MenuDate (yyyy-MM-dd)", "CreatedAt", "UpdatedAt"
  };

  private final UserRepository users;
  private final MenuItemRepository menu;
  private final PasswordEncoder encoder;

  public AdminExcelService(UserRepository users, MenuItemRepository menu, PasswordEncoder encoder) {
    this.users = users;
    this.menu = menu;
    this.encoder = encoder;
  }

  // ---------------- export ----------------

  public byte[] exportUsers() throws IOException {
    List<User> all = users.findAllByOrderById();
    try (Workbook wb = new XSSFWorkbook()) {
      fillUsersSheet(wb.createSheet("Users"), all);
      return toBytes(wb);
    }
  }

  public byte[] exportMenu() throws IOException {
    List<MenuItem> all = menu.findAllByOrderById();
    try (Workbook wb = new XSSFWorkbook()) {
      fillMenuSheet(wb.createSheet("MenuItems"), all);
      return toBytes(wb);
    }
  }

  /** One file with two sheets (preferred by spec). */
  public byte[] exportAll() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      fillUsersSheet(wb.createSheet("Users"), users.findAllByOrderById());
      fillMenuSheet(wb.createSheet("MenuItems"), menu.findAllByOrderById());
      return toBytes(wb);
    }
  }

  private void fillUsersSheet(Sheet sh, List<User> all) {
    header(sh, USER_HEADERS);
    int r = 1;
    for (User u : all) {
      Row row = sh.createRow(r++);
      row.createCell(0).setCellValue(u.getId() == null ? "" : String.valueOf(u.getId()));
      row.createCell(1).setCellValue(nz(u.getStudentId()));
      row.createCell(2).setCellValue(nz(u.getName()));
      row.createCell(3).setCellValue(nz(u.getEmail()));
      row.createCell(4).setCellValue(nz(u.getRole()));
      row.createCell(5).setCellValue(u.getTabBalance());
      row.createCell(6).setCellValue(nz(u.getStatus()));
      row.createCell(7).setCellValue(u.getCreatedAt() == null ? "" : u.getCreatedAt().toString());
      row.createCell(8).setCellValue(u.getUpdatedAt() == null ? "" : u.getUpdatedAt().toString());
      row.createCell(9).setCellValue(""); // NewPassword always blank on export
    }
    autosize(sh, USER_HEADERS.length);
    sh.createFreezePane(0, 1);
  }

  private void fillMenuSheet(Sheet sh, List<MenuItem> all) {
    header(sh, MENU_HEADERS);
    int r = 1;
    for (MenuItem m : all) {
      Row row = sh.createRow(r++);
      row.createCell(0).setCellValue(m.getId() == null ? "" : String.valueOf(m.getId()));
      row.createCell(1).setCellValue(nz(m.getName()));
      row.createCell(2).setCellValue(nz(m.getDescription()));
      row.createCell(3).setCellValue(m.getPrice());
      row.createCell(4).setCellValue(nz(m.getCategory()));
      row.createCell(5).setCellValue(m.isAvailable() ? "TRUE" : "FALSE");
      row.createCell(6).setCellValue(nz(m.getImageUrl()));
      row.createCell(7).setCellValue(m.getMenuDate() == null ? "" : m.getMenuDate().toString());
      row.createCell(8).setCellValue(m.getCreatedAt() == null ? "" : m.getCreatedAt().toString());
      row.createCell(9).setCellValue(m.getUpdatedAt() == null ? "" : m.getUpdatedAt().toString());
    }
    autosize(sh, MENU_HEADERS.length);
    sh.createFreezePane(0, 1);
  }

  private void header(Sheet sh, String[] cols) {
    Workbook wb = sh.getWorkbook();
    CellStyle st = wb.createCellStyle();
    Font f = wb.createFont();
    f.setBold(true);
    f.setColor(IndexedColors.WHITE.getIndex());
    st.setFont(f);
    st.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
    st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    st.setAlignment(HorizontalAlignment.CENTER);
    st.setBorderBottom(BorderStyle.THIN);
    Row row = sh.createRow(0);
    for (int i = 0; i < cols.length; i++) {
      Cell c = row.createCell(i);
      c.setCellValue(cols[i]);
      c.setCellStyle(st);
    }
  }

  private void autosize(Sheet sh, int cols) {
    for (int i = 0; i < cols; i++) {
      sh.autoSizeColumn(i);
      sh.setColumnWidth(i, Math.min(sh.getColumnWidth(i) + 800, 12000));
    }
  }

  private byte[] toBytes(Workbook wb) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      wb.write(out);
      return out.toByteArray();
    }
  }

  private static String nz(String s) { return s == null ? "" : s; }

  // ---------------- import ----------------

  @Transactional
  public ImportResult importUsers(MultipartFile file) throws IOException {
    Sheet sh = openSheet(file, "Users");
    Map<String, Integer> col = headerIndex(sh, USER_HEADERS, true);
    int created = 0, updated = 0, skipped = 0;
    List<String> errors = new ArrayList<>();

    for (int i = 1; i <= sh.getLastRowNum(); i++) {
      Row row = sh.getRow(i);
      if (row == null || isBlankRow(row, col.size())) continue;
      int excelRow = i + 1;
      try {
        String idStr = cell(col, row, "ID");
        String studentId = cell(col, row, "StudentID/Username").trim();
        String name = cell(col, row, "Name").trim();
        String email = cell(col, row, "Email").trim();
        String role = cell(col, row, "Role").trim().toUpperCase();
        String tabStr = cell(col, row, "TabBalance").trim();
        String status = cell(col, row, "Status").trim().toUpperCase();
        String newPassword = cell(col, row, "NewPassword (optional - blank keeps existing)");

        if (studentId.isEmpty()) throw new IllegalArgumentException("StudentID/Username is required");

        User existing = null;
        if (!idStr.isEmpty()) {
          try {
            existing = users.findById(Long.parseLong(idStr)).orElse(null);
          } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("ID must be numeric");
          }
        }
        if (existing == null) existing = users.findByStudentId(studentId).orElse(null);

        if (existing == null) {
          // ---- insert ----
          if (name.isEmpty()) throw new IllegalArgumentException("Name is required for new users");
          if (newPassword == null || newPassword.trim().length() < 4) {
            throw new IllegalArgumentException("NewPassword (>=4 chars) is required for new users");
          }
          if (!role.isEmpty() && !role.equals("STUDENT") && !role.equals("ADMIN")) {
            throw new IllegalArgumentException("Role must be STUDENT or ADMIN");
          }
          if (!status.isEmpty() && !status.equals("ACTIVE") && !status.equals("DISABLED")) {
            throw new IllegalArgumentException("Status must be ACTIVE or DISABLED");
          }
          double tab = 0;
          if (!tabStr.isEmpty()) {
            tab = parseTab(tabStr);
          }
          if (!email.isEmpty() && !email.contains("@")) {
            throw new IllegalArgumentException("Email looks invalid");
          }
          if (users.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("StudentID already exists");
          }
          User u = new User();
          u.setStudentId(studentId);
          u.setName(name);
          u.setEmail(email.isEmpty() ? null : email);
          u.setRole(role.isEmpty() ? "STUDENT" : role);
          u.setTabBalance(tab);
          u.setStatus(status.isEmpty() ? "ACTIVE" : status);
          u.setPasswordHash(encoder.encode(newPassword.trim()));
          users.save(u);
          created++;
          log.info("Imported new user {} (row {})", studentId, excelRow);
        } else {
          // ---- update (partial: blanks keep existing) ----
          if (!existing.getStudentId().equals(studentId)) {
            if (users.existsByStudentId(studentId)) {
              throw new IllegalArgumentException("StudentID already taken by another user");
            }
          }
          boolean changed = false;
          if (!studentId.equals(existing.getStudentId())) {
            existing.setStudentId(studentId); changed = true;
          }
          if (!name.isEmpty() && !name.equals(nz(existing.getName()))) {
            existing.setName(name); changed = true;
          }
          if (!email.isEmpty() ? !email.equals(nz(existing.getEmail())) : false) {
            if (!email.contains("@")) throw new IllegalArgumentException("Email looks invalid");
            existing.setEmail(email); changed = true;
          } else if (email.isEmpty() && existing.getEmail() != null && false) {
            // blank keeps existing (no clear via bulk import by design)
          }
          if (!role.isEmpty()) {
            if (!role.equals("STUDENT") && !role.equals("ADMIN")) {
              throw new IllegalArgumentException("Role must be STUDENT or ADMIN");
            }
            if (!role.equals(existing.getRole())) {
              guardLastAdmin(existing, role, null);
              existing.setRole(role); changed = true;
            }
          }
          if (!tabStr.isEmpty()) {
            double tab = parseTab(tabStr);
            if (Double.compare(tab, existing.getTabBalance()) != 0) {
              existing.setTabBalance(tab); changed = true;
            }
          }
          if (!status.isEmpty()) {
            if (!status.equals("ACTIVE") && !status.equals("DISABLED")) {
              throw new IllegalArgumentException("Status must be ACTIVE or DISABLED");
            }
            if (!status.equalsIgnoreCase(existing.getStatus())) {
              guardLastAdmin(existing, null, status);
              existing.setStatus(status); changed = true;
            }
          }
          if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (newPassword.trim().length() < 4) {
              throw new IllegalArgumentException("NewPassword must be >= 4 chars");
            }
            existing.setPasswordHash(encoder.encode(newPassword.trim()));
            changed = true;
          }
          if (changed) {
            users.save(existing);
            updated++;
          } else {
            skipped++;
          }
        }
      } catch (IllegalArgumentException e) {
        errors.add("Row " + excelRow + ": " + e.getMessage());
        log.warn("User import row {} skipped: {}", excelRow, e.getMessage());
      }
    }
    log.info("User import done: created={} updated={} skipped={} errors={}", created, updated, skipped, errors.size());
    return new ImportResult(created, updated, skipped, errors);
  }

  @Transactional
  public ImportResult importMenu(MultipartFile file) throws IOException {
    Sheet sh = openSheet(file, "MenuItems");
    Map<String, Integer> col = headerIndex(sh, MENU_HEADERS, true);
    int created = 0, updated = 0, skipped = 0;
    List<String> errors = new ArrayList<>();

    for (int i = 1; i <= sh.getLastRowNum(); i++) {
      Row row = sh.getRow(i);
      if (row == null || isBlankRow(row, col.size())) continue;
      int excelRow = i + 1;
      try {
        String idStr = cell(col, row, "ID");
        String name = cell(col, row, "Name").trim();
        String desc = cell(col, row, "Description").trim();
        String priceStr = cell(col, row, "Price").trim();
        String category = cell(col, row, "Category").trim();
        String availStr = cell(col, row, "Available").trim();
        String imageUrl = cell(col, row, "ImageURL").trim();
        String dateStr = cell(col, row, "MenuDate (yyyy-MM-dd)").trim();

        MenuItem existing = null;
        if (!idStr.isEmpty()) {
          try {
            existing = menu.findById(Long.parseLong(idStr)).orElse(null);
          } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("ID must be numeric");
          }
        }

        LocalDate date = null;
        if (!dateStr.isEmpty()) {
          try {
            date = LocalDate.parse(dateStr);
          } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("MenuDate must be yyyy-MM-dd or blank");
          }
        }

        if (existing == null) {
          if (name.isEmpty()) throw new IllegalArgumentException("Name is required for new items");
          if (priceStr.isEmpty()) throw new IllegalArgumentException("Price is required for new items");
          double price = parsePrice(priceStr);
          MenuItem m = new MenuItem();
          m.setName(name);
          m.setDescription(desc.isEmpty() ? null : desc);
          m.setPrice(price);
          m.setCategory(category.isEmpty() ? "General" : category);
          m.setAvailable(availStr.isEmpty() ? true : parseBool(availStr));
          m.setImageUrl(imageUrl.isEmpty() ? null : imageUrl);
          m.setMenuDate(date);
          menu.save(m);
          created++;
        } else {
          boolean changed = false;
          if (!name.isEmpty() && !name.equals(nz(existing.getName()))) {
            existing.setName(name); changed = true;
          }
          if (!desc.isEmpty() ? !desc.equals(nz(existing.getDescription())) : false) {
            existing.setDescription(desc); changed = true;
          }
          if (!priceStr.isEmpty()) {
            double price = parsePrice(priceStr);
            if (Double.compare(price, existing.getPrice()) != 0) {
              existing.setPrice(price); changed = true;
            }
          }
          if (!category.isEmpty() && !category.equals(nz(existing.getCategory()))) {
            existing.setCategory(category); changed = true;
          }
          if (!availStr.isEmpty()) {
            boolean av = parseBool(availStr);
            if (av != existing.isAvailable()) { existing.setAvailable(av); changed = true; }
          }
          if (!imageUrl.isEmpty() ? !imageUrl.equals(nz(existing.getImageUrl())) : false) {
            existing.setImageUrl(imageUrl); changed = true;
          }
          if (!dateStr.isEmpty() && !Objects.equals(date, existing.getMenuDate())) {
            existing.setMenuDate(date); changed = true;
          }
          if (changed) {
            menu.save(existing);
            updated++;
          } else {
            skipped++;
          }
        }
      } catch (IllegalArgumentException e) {
        errors.add("Row " + excelRow + ": " + e.getMessage());
        log.warn("Menu import row {} skipped: {}", excelRow, e.getMessage());
      }
    }
    log.info("Menu import done: created={} updated={} skipped={} errors={}", created, updated, skipped, errors.size());
    return new ImportResult(created, updated, skipped, errors);
  }

  // ---------- helpers ----------

  private Sheet openSheet(MultipartFile file, String expectedSheet) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file uploaded");
    }
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    if (!(name.endsWith(".xlsx") || name.endsWith(".xlsm"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .xlsx files are accepted");
    }
    try {
      // Intentionally not closed: small admin files only; sheet is read synchronously.
      InputStream in = file.getInputStream();
      Workbook live = new XSSFWorkbook(in);
      Sheet sh = live.getSheet(expectedSheet);
      if (sh == null) {
        // fall back: named sheet missing -> use matching sheet or first sheet
        // (supports single-sheet exports and combined export/all files)
        for (int s = 0; s < live.getNumberOfSheets(); s++) {
          String n = live.getSheetName(s);
          if (n != null && n.equalsIgnoreCase(expectedSheet)) return live.getSheetAt(s);
        }
        if (live.getNumberOfSheets() == 0) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Excel file has no sheets. Expected sheet: " + expectedSheet);
        }
        sh = live.getSheetAt(0);
      }
      return sh;
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Excel parse failed: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Could not parse Excel file. Export a fresh file and edit it without renaming sheets/headers.");
    }
  }

  /** Map canonical header keys to column indexes; validates required headers exist. */
  private Map<String, Integer> headerIndex(Sheet sh, String[] expected, boolean strictNames) {
    Row header = sh.getRow(0);
    if (header == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Missing header row. Expected: " + String.join(" | ", expected));
    }
    Map<String, Integer> found = new HashMap<>();
    for (int c = 0; c < header.getLastCellNum(); c++) {
      String h = FMT.formatCellValue(header.getCell(c)).trim();
      found.put(normalize(h), c);
    }
    Map<String, Integer> out = new HashMap<>();
    List<String> missing = new ArrayList<>();
    for (String exp : expected) {
      // CreatedAt/UpdatedAt are informational: tolerate missing on hand-made files
      boolean optional = exp.equals("CreatedAt") || exp.equals("UpdatedAt");
      Integer idx = found.get(normalize(exp));
      if (idx == null && !optional) missing.add(exp);
      if (idx != null) out.put(canonicalKey(exp), idx);
    }
    if (!missing.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid Excel headers. Missing: " + String.join(", ", missing)
          + ". Expected: " + String.join(" | ", expected));
    }
    return out;
  }

  private String canonicalKey(String header) {
    String n = normalize(header);
    if (n.startsWith("studentid")) return "ID".equals(header) ? "ID" : "StudentID/Username";
    if (n.startsWith("newpassword")) return "NewPassword (optional - blank keeps existing)";
    if (n.startsWith("menudate")) return "MenuDate (yyyy-MM-dd)";
    if (n.startsWith("imageurl")) return "ImageURL";
    if (n.startsWith("tabbalance")) return "TabBalance";
    if (n.startsWith("createdat")) return "CreatedAt";
    if (n.startsWith("updatedat")) return "UpdatedAt";
    // single-word headers match directly (case-insensitive)
    for (String k : new String[]{"ID","Name","Email","Role","Status","Description","Price","Category","Available"}) {
      if (normalize(k).equals(n)) return k;
    }
    return header;
  }

  private String normalize(String s) {
    return s == null ? "" : s.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  private String cell(Map<String, Integer> col, Row row, String key) {
    Integer idx = col.get(key);
    if (idx == null) return "";
    return FMT.formatCellValue(row.getCell(idx)).trim();
  }

  private boolean isBlankRow(Row row, int cols) {
    for (int c = 0; c < Math.max(cols, row.getLastCellNum()); c++) {
      if (!FMT.formatCellValue(row.getCell(c)).trim().isEmpty()) return false;
    }
    return true;
  }

  private double parseTab(String s) {
    try {
      double v = Double.parseDouble(s.replace("₹", "").replace(",", "").trim());
      if (v < 0) throw new IllegalArgumentException("TabBalance cannot be negative");
      return Math.round(v * 100.0) / 100.0;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("TabBalance must be a number >= 0");
    }
  }

  private double parsePrice(String s) {
    try {
      double v = Double.parseDouble(s.replace("₹", "").replace(",", "").trim());
      if (v <= 0) throw new IllegalArgumentException("Price must be positive");
      return Math.round(v * 100.0) / 100.0;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Price must be a positive number");
    }
  }

  private boolean parseBool(String s) {
    String n = s.trim().toLowerCase();
    return switch (n) {
      case "true", "yes", "1", "available", "show", "shown", "y" -> true;
      case "false", "no", "0", "hidden", "hide", "n" -> false;
      default -> throw new IllegalArgumentException("Available must be TRUE/FALSE (or Yes/No)");
    };
  }

  private void guardLastAdmin(User target, String newRole, String newStatus) {
    boolean wouldRemoveAdmin = (newRole != null && !newRole.equals("ADMIN") && target.getRole().equals("ADMIN"))
        || (newStatus != null && newStatus.equals("DISABLED") && target.getRole().equals("ADMIN"));
    if (!wouldRemoveAdmin) return;
    long adminCount = users.findAll().stream().filter(u -> "ADMIN".equals(u.getRole())).count();
    long activeAdmins = users.findAll().stream()
        .filter(u -> "ADMIN".equals(u.getRole()) && !"DISABLED".equalsIgnoreCase(u.getStatus())).count();
    if (adminCount <= 1 || (newStatus != null && activeAdmins <= 1)) {
      throw new IllegalArgumentException("Cannot demote/disable the last ADMIN account");
    }
  }
}
