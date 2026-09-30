package com.vildoria.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class FormulaEvaluator {
	private FormulaEvaluator() {
	}

	static String display(String[][] cells, int row, int column) {
		String value = cells[row][column];
		if (!value.startsWith("=")) {
			return value;
		}
		try {
			double result = evaluateCell(cells, row, column, new boolean[cells.length][cells[0].length]);
			if (!Double.isFinite(result)) {
				return "#VALUE!";
			}
			return BigDecimal.valueOf(result).stripTrailingZeros().toPlainString();
		} catch (FormulaException exception) {
			return exception.code;
		}
	}

	private static double evaluateCell(String[][] cells, int row, int column, boolean[][] active) {
		if (active[row][column]) {
			throw new FormulaException("#CIRC!");
		}
		String value = cells[row][column].trim();
		if (value.isEmpty()) {
			return 0;
		}
		if (!value.startsWith("=")) {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException exception) {
				throw new FormulaException("#VALUE!");
			}
		}
		active[row][column] = true;
		try {
			Parser parser = new Parser(value.substring(1), cells, active);
			double result = parser.parseExpression();
			parser.skipSpaces();
			if (!parser.atEnd()) {
				throw new FormulaException("#ERROR!");
			}
			return result;
		} finally {
			active[row][column] = false;
		}
	}

	private record CellReference(int row, int column) {
	}

	private static final class Parser {
		private final String source;
		private final String[][] cells;
		private final boolean[][] active;
		private int index;

		private Parser(String source, String[][] cells, boolean[][] active) {
			this.source = source;
			this.cells = cells;
			this.active = active;
		}

		private double parseExpression() {
			double value = parseTerm();
			while (true) {
				if (eat('+')) {
					value += parseTerm();
				} else if (eat('-')) {
					value -= parseTerm();
				} else {
					return value;
				}
			}
		}

		private double parseTerm() {
			double value = parseFactor();
			while (true) {
				if (eat('*')) {
					value *= parseFactor();
				} else if (eat('/')) {
					double divisor = parseFactor();
					if (divisor == 0) {
						throw new FormulaException("#DIV/0!");
					}
					value /= divisor;
				} else {
					return value;
				}
			}
		}

		private double parseFactor() {
			if (eat('+')) {
				return parseFactor();
			}
			if (eat('-')) {
				return -parseFactor();
			}
			double value = parsePrimary();
			if (eat('^')) {
				value = Math.pow(value, parseFactor());
			}
			while (eat('%')) {
				value /= 100;
			}
			return value;
		}

		private double parsePrimary() {
			skipSpaces();
			if (eat('(')) {
				double value = parseExpression();
				if (!eat(')')) {
					throw new FormulaException("#ERROR!");
				}
				return value;
			}
			if (index < source.length() && (Character.isDigit(source.charAt(index)) || source.charAt(index) == '.')) {
				return parseNumber();
			}
			if (index < source.length() && Character.isLetter(source.charAt(index))) {
				return parseNameOrReference();
			}
			throw new FormulaException("#ERROR!");
		}

		private double parseNameOrReference() {
			int start = index;
			while (index < source.length() && Character.isLetter(source.charAt(index))) {
				index++;
			}
			String name = source.substring(start, index).toUpperCase();
			if (index < source.length() && Character.isDigit(source.charAt(index))) {
				index = start;
				return readCellValue();
			}
			if (!eat('(')) {
				throw new FormulaException("#NAME?");
			}
			return parseFunction(name);
		}

		private double parseFunction(String name) {
			List<Double> values = new ArrayList<>();
			if (!eat(')')) {
				do {
					int savedIndex = index;
					CellReference first = readCellReference();
					if (first != null && eat(':')) {
						CellReference last = readCellReference();
						if (last == null) {
							throw new FormulaException("#REF!");
						}
						appendRange(values, first, last);
					} else {
						index = savedIndex;
						values.add(parseExpression());
					}
				} while (eat(',') || eat(';'));
				if (!eat(')')) {
					throw new FormulaException("#ERROR!");
				}
			}
			return switch (name) {
				case "SUM", "SOMME" -> values.stream().mapToDouble(Double::doubleValue).sum();
				case "AVERAGE", "MOYENNE" -> values.isEmpty() ? 0 : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
				case "MIN" -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
				case "MAX" -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
				case "COUNT", "NB" -> values.size();
				default -> throw new FormulaException("#NAME?");
			};
		}

		private void appendRange(List<Double> values, CellReference first, CellReference last) {
			for (int row = Math.min(first.row(), last.row()); row <= Math.max(first.row(), last.row()); row++) {
				for (int column = Math.min(first.column(), last.column()); column <= Math.max(first.column(), last.column()); column++) {
					values.add(evaluateCell(cells, row, column, active));
				}
			}
		}

		private double readCellValue() {
			CellReference reference = readCellReference();
			if (reference == null) {
				throw new FormulaException("#REF!");
			}
			return evaluateCell(cells, reference.row(), reference.column(), active);
		}

		private CellReference readCellReference() {
			skipSpaces();
			int start = index;
			int column = 0;
			while (index < source.length() && Character.isLetter(source.charAt(index))) {
				column = column * 26 + Character.toUpperCase(source.charAt(index)) - 'A' + 1;
				index++;
			}
			int rowStart = index;
			while (index < source.length() && Character.isDigit(source.charAt(index))) {
				index++;
			}
			if (rowStart == index) {
				index = start;
				return null;
			}
			int row = Integer.parseInt(source.substring(rowStart, index)) - 1;
			column--;
			if (row < 0 || row >= cells.length || column < 0 || column >= cells[0].length) {
				throw new FormulaException("#REF!");
			}
			return new CellReference(row, column);
		}

		private double parseNumber() {
			skipSpaces();
			int start = index;
			while (index < source.length() && (Character.isDigit(source.charAt(index)) || source.charAt(index) == '.')) {
				index++;
			}
			try {
				return Double.parseDouble(source.substring(start, index));
			} catch (NumberFormatException exception) {
				throw new FormulaException("#VALUE!");
			}
		}

		private boolean eat(char expected) {
			skipSpaces();
			if (index < source.length() && source.charAt(index) == expected) {
				index++;
				return true;
			}
			return false;
		}

		private void skipSpaces() {
			while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
				index++;
			}
		}

		private boolean atEnd() {
			return index == source.length();
		}
	}

	private static final class FormulaException extends RuntimeException {
		private final String code;

		private FormulaException(String code) {
			this.code = code;
		}
	}
}