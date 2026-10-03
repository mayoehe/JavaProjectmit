package com.canteen.service;

import com.canteen.entity.CanteenOrder;
import com.canteen.entity.OrderItem;
import com.canteen.repository.OrderRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Day-by-day sales report for Admin.
 * Cancelled orders are counted separately and excluded from revenue / items-sold.
 * Dates use the server's default time zone (see README).
 */
@Service
public class SalesReportService {

  private static final Logger log = LoggerFactory.getLogger(SalesReportService.class);

  static final String[] DAILY_HEADERS = {
      "Date", "Orders", "Items Sold", "Total Revenue (Rs)",
      "Collected (Rs)", "Tab/UNCOLLECTED (Rs)", "Open Pipeline (Rs)",
      "Pending", "Ready", "Collected", "Uncollected", "Cancelled",
      "Unique Students", "Avg Order (Rs)"
  };
  static final String[] ITEM_HEADERS = {
      "Date", "Item", "Category", "Qty Sold", "Revenue (Rs)"
  };

  private final OrderRepository orders;

  public SalesReportService(OrderRepository orders) {
    this.orders = orders;
  }

  @Transactional(readOnly = true)
  public byte[] exportDailySales(LocalDate from, LocalDate to) throws IOException {
    ZoneId zone = ZoneId.systemDefault();
    Map<LocalDate, List<CanteenOrder>> byDay = new TreeMap<>();
    for (CanteenOrder o : orders.findAllByOrderByCreatedAtDesc()) {
      if (o.getCreatedAt() == null) continue;
      LocalDate d = o.getCreatedAt().atZone(zone).toLocalDate();
      if (from != null && d.isBefore(from)) continue;
      if (to != null && d.isAfter(to)) continue;
      byDay.computeIfAbsent(d, k -> new ArrayList<>()).add(o);
    }

    try (Workbook wb = new XSSFWorkbook()) {
      fillDailySheet(wb.createSheet("DailySales"), byDay);
      fillItemSheet(wb.createSheet("ItemBreakdown"), byDay);
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        wb.write(out);
        return out.toByteArray();
      }
    }
  }

  private void fillDailySheet(Sheet sh, Map<LocalDate, List<CanteenOrder>> byDay) {
    header(sh, DAILY_HEADERS);
    int r = 1;
    // totals accumulator: orders, items, totalRev, collected, tab, open, pending, ready,
    // collectedCt, uncollectedCt, cancelledCt (unique students + avg computed at end)
    double[] t = new double[11];
    java.util.Set<String> allStudents = new java.util.HashSet<>();
    for (Map.Entry<LocalDate, List<CanteenOrder>> e : byDay.entrySet()) {
      DayAgg a = aggregate(e.getValue());
      Row row = sh.createRow(r++);
      int c = 0;
      row.createCell(c++).setCellValue(e.getKey().toString());
      row.createCell(c++).setCellValue(a.orders);
      row.createCell(c++).setCellValue(a.itemsSold);
      row.createCell(c++).setCellValue(a.totalRevenue);
      row.createCell(c++).setCellValue(a.collectedRevenue);
      row.createCell(c++).setCellValue(a.tabRevenue);
      row.createCell(c++).setCellValue(a.openRevenue);
      row.createCell(c++).setCellValue(a.pending);
      row.createCell(c++).setCellValue(a.ready);
      row.createCell(c++).setCellValue(a.collected);
      row.createCell(c++).setCellValue(a.uncollected);
      row.createCell(c++).setCellValue(a.cancelled);
      row.createCell(c++).setCellValue(a.uniqueStudents);
      row.createCell(c++).setCellValue(a.avgOrder);
      t[0] += a.orders; t[1] += a.itemsSold; t[2] += a.totalRevenue;
      t[3] += a.collectedRevenue; t[4] += a.tabRevenue; t[5] += a.openRevenue;
      t[6] += a.pending; t[7] += a.ready; t[8] += a.collected;
      t[9] += a.uncollected; t[10] += a.cancelled;
      allStudents.addAll(a.studentIds);
    }
    // TOTAL row (bold) — avg = totalRevenue / non-cancelled orders
    Row total = sh.createRow(r);
    CellStyle bold = bold(sh.getWorkbook());
    Cell c0 = total.createCell(0);
    c0.setCellValue(byDay.isEmpty() ? "TOTAL (no orders)" : "TOTAL");
    c0.setCellStyle(bold);
    double nonCancelled = t[0] - t[10];
    double avgAll = nonCancelled > 0 ? round(t[2] / nonCancelled) : 0;
    double[] vals = {t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7], t[8], t[9], t[10],
        allStudents.size(), avgAll};
    for (int i = 0; i < vals.length; i++) {
      Cell cell = total.createCell(i + 1);
      cell.setCellValue(vals[i]);
      cell.setCellStyle(bold);
    }
    autosize(sh, DAILY_HEADERS.length);
    sh.createFreezePane(0, 1);
    log.info("Sales report: {} day(s)", byDay.size());
  }

  private void fillItemSheet(Sheet sh, Map<LocalDate, List<CanteenOrder>> byDay) {
    header(sh, ITEM_HEADERS);
    int r = 1;
    for (Map.Entry<LocalDate, List<CanteenOrder>> e : byDay.entrySet()) {
      Map<String, ItemAgg> items = new LinkedHashMap<>();
      for (CanteenOrder o : e.getValue()) {
        if (CanteenOrder.CANCELLED.equals(o.getStatus())) continue;
        for (OrderItem oi : o.getItems()) {
          String name = oi.getMenuItem() == null ? "(removed item)" : oi.getMenuItem().getName();
          String cat = oi.getMenuItem() == null || oi.getMenuItem().getCategory() == null
              ? "" : oi.getMenuItem().getCategory();
          ItemAgg a = items.computeIfAbsent(name + "\u0000" + cat, k -> new ItemAgg(name, cat));
          a.qty += oi.getQuantity();
          a.revenue += oi.getPrice() * oi.getQuantity();
        }
      }
      for (ItemAgg a : items.values()) {
        Row row = sh.createRow(r++);
        row.createCell(0).setCellValue(e.getKey().toString());
        row.createCell(1).setCellValue(a.name);
        row.createCell(2).setCellValue(a.category);
        row.createCell(3).setCellValue(a.qty);
        row.createCell(4).setCellValue(round(a.revenue));
      }
    }
    if (r == 1) {
      Row row = sh.createRow(1);
      row.createCell(0).setCellValue("No item sales in range");
    }
    autosize(sh, ITEM_HEADERS.length);
    sh.createFreezePane(0, 1);
  }

  private DayAgg aggregate(List<CanteenOrder> list) {
    DayAgg a = new DayAgg();
    a.orders = list.size();
    for (CanteenOrder o : list) {
      String s = o.getStatus();
      if (CanteenOrder.PENDING.equals(s)) a.pending++;
      else if (CanteenOrder.READY.equals(s)) a.ready++;
      else if (CanteenOrder.COLLECTED.equals(s)) a.collected++;
      else if (CanteenOrder.UNCOLLECTED.equals(s)) a.uncollected++;
      else if (CanteenOrder.CANCELLED.equals(s)) a.cancelled++;
      if (o.getStudent() != null && o.getStudent().getStudentId() != null) {
        a.studentIds.add(o.getStudent().getStudentId());
      }
      if (CanteenOrder.CANCELLED.equals(s)) continue;
      a.totalRevenue += o.getTotal();
      if (CanteenOrder.COLLECTED.equals(s)) a.collectedRevenue += o.getTotal();
      else if (CanteenOrder.UNCOLLECTED.equals(s)) a.tabRevenue += o.getTotal();
      else a.openRevenue += o.getTotal(); // PENDING + READY pipeline
      for (OrderItem oi : o.getItems()) a.itemsSold += oi.getQuantity();
    }
    a.totalRevenue = round(a.totalRevenue);
    a.collectedRevenue = round(a.collectedRevenue);
    a.tabRevenue = round(a.tabRevenue);
    a.openRevenue = round(a.openRevenue);
    a.uniqueStudents = a.studentIds.size();
    long billable = a.orders - a.cancelled;
    a.avgOrder = billable > 0 ? round(a.totalRevenue / billable) : 0;
    return a;
  }

  private void header(Sheet sh, String[] cols) {
    CellStyle st = sh.getWorkbook().createCellStyle();
    Font f = sh.getWorkbook().createFont();
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

  private CellStyle bold(Workbook wb) {
    CellStyle st = wb.createCellStyle();
    Font f = wb.createFont();
    f.setBold(true);
    st.setFont(f);
    return st;
  }

  private void autosize(Sheet sh, int cols) {
    for (int i = 0; i < cols; i++) {
      sh.autoSizeColumn(i);
      sh.setColumnWidth(i, Math.min(sh.getColumnWidth(i) + 800, 12000));
    }
  }

  private static double round(double v) { return Math.round(v * 100.0) / 100.0; }

  private static final class DayAgg {
    long orders, itemsSold, pending, ready, collected, uncollected, cancelled, uniqueStudents;
    double totalRevenue, collectedRevenue, tabRevenue, openRevenue, avgOrder;
    final java.util.Set<String> studentIds = new java.util.HashSet<>();
  }

  private static final class ItemAgg {
    final String name;
    final String category;
    long qty;
    double revenue;
    ItemAgg(String name, String category) { this.name = name; this.category = category; }
  }
}
