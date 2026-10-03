package com.canteen.dto;

import java.util.List;

/** Summary of an Excel import: counts + per-row error messages. */
public record ImportResult(int created, int updated, int skipped, List<String> errors) {
}
