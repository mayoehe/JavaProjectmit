package com.canteen.controller;

import com.canteen.dto.ImportResult;
import com.canteen.service.AdminExcelService;
import com.canteen.service.SalesReportService;
import java.io.IOException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin-only Excel export/import.
 * All endpoints require ROLE_ADMIN (401 without token, 403 for students).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin
public class AdminExcelController {

  private static final Logger log = LoggerFactory.getLogger(AdminExcelController.class);
  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final AdminExcelService excel;
  private final SalesReportService sales;

  public AdminExcelController(AdminExcelService excel, SalesReportService sales) {
    this.excel = excel;
    this.sales = sales;
  }

  @GetMapping("/export/users")
  public ResponseEntity<byte[]> exportUsers() throws IOException {
    log.info("Admin export users");
    return xlsx(excel.exportUsers(), "canteen-users.xlsx");
  }

  @GetMapping("/export/menu")
  public ResponseEntity<byte[]> exportMenu() throws IOException {
    log.info("Admin export menu");
    return xlsx(excel.exportMenu(), "canteen-menu.xlsx");
  }

  /** Preferred: one file, two sheets (Users + MenuItems). */
  @GetMapping("/export/all")
  public ResponseEntity<byte[]> exportAll() throws IOException {
    log.info("Admin export all");
    return xlsx(excel.exportAll(), "canteen-export.xlsx");
  }

  @PostMapping(value = "/import/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImportResult importUsers(@RequestParam("file") MultipartFile file) throws IOException {
    log.info("Admin import users: {}", file.getOriginalFilename());
    return excel.importUsers(file);
  }

  @PostMapping(value = "/import/menu", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImportResult importMenu(@RequestParam("file") MultipartFile file) throws IOException {
    log.info("Admin import menu: {}", file.getOriginalFilename());
    return excel.importMenu(file);
  }

  /**
   * Day-by-day sales report. Optional ?from=yyyy-MM-dd&to=yyyy-MM-dd (inclusive).
   * Cancelled orders are counted but excluded from revenue / items-sold.
   */
  @GetMapping("/export/sales-daily")
  public ResponseEntity<byte[]> exportSalesDaily(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
      throws IOException {
    if (from != null && to != null && from.isAfter(to)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be on or before to (yyyy-MM-dd)");
    }
    log.info("Admin export sales-daily from={} to={}", from, to);
    String filename = "canteen-sales-daily"
        + (from != null || to != null ? "-" + (from == null ? "all" : from) + "_to_" + (to == null ? "all" : to) : "")
        + ".xlsx";
    return xlsx(sales.exportDailySales(from, to), filename);
  }

  private ResponseEntity<byte[]> xlsx(byte[] body, String filename) {
    return ResponseEntity.status(HttpStatus.OK)
        .contentType(XLSX)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString())
        .contentLength(body.length)
        .body(body);
  }
}
