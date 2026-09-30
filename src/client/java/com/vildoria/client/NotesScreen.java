package com.vildoria.client;

import com.vildoria.Vildoria;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public class NotesScreen extends Screen {
	private static final int COLUMN_COUNT = 8;
	private static final int ROW_COUNT = 12;
	private static final int INDEX_WIDTH = 34;
	private static final int FILE_LIST_ROW_HEIGHT = 25;
	private static final int FILE_LIST_LIMIT = 10;
	private static final int NEW_BUTTON_WIDTH = 72;
	private static final int OPEN_BUTTON_WIDTH = 72;
	private static final int SAVE_BUTTON_WIDTH = 88;
	private static final int BUTTON_GAP = 6;
	private final String[][] cells = new String[ROW_COUNT][COLUMN_COUNT];
	private final List<Path> noteFiles = new ArrayList<>();
	private int selectedRow;
	private int selectedColumn;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;
	private int gridLeft;
	private int gridTop;
	private int rowHeight;
	private int cellWidth;
	private String documentName = "";
	private EditBox documentNameField;
	private EditBox formulaField;
	private boolean openDialog;
	private Component status;

	public NotesScreen() {
		super(Component.translatable("screen.vildoria.notes"));
		clearCells();
		status = Component.translatable("screen.vildoria.notes.ready");
	}

	@Override
	protected void init() {
		panelWidth = NotesSettings.effectiveWidth(width);
		panelHeight = NotesSettings.effectiveHeight(height);
		panelLeft = (width - panelWidth) / 2;
		panelTop = (height - panelHeight) / 2;
		gridLeft = panelLeft + 15;
		gridTop = panelTop + 128;
		int densityBias = NotesSettings.density();
		int preferredRowHeight = switch (densityBias) {
			case 0 -> 18;
			case 2 -> 26;
			default -> 22;
		};
		int preferredCellWidth = switch (densityBias) {
			case 0 -> 48;
			case 2 -> 60;
			default -> 56;
		};
		rowHeight = Math.max(8, Math.min(preferredRowHeight, (panelHeight - 176) / (ROW_COUNT + 1)));
		cellWidth = Math.max(12, Math.min(preferredCellWidth, (panelWidth - 30 - INDEX_WIDTH) / COLUMN_COUNT));

		documentNameField = new EditBox(font, panelLeft + 14, panelTop + 43, panelWidth - 28, 22,
				NotesFont.apply(Component.translatable("screen.vildoria.notes.document_name")));
		documentNameField.setMaxLength(48);
		documentNameField.setValue(documentName);
		documentNameField.setHint(NotesFont.apply(Component.translatable("screen.vildoria.notes.document_name")));
		documentNameField.addFormatter(NotesFont::format);
		documentNameField.setResponder(value -> documentName = value);
		addRenderableWidget(documentNameField);

		formulaField = new EditBox(font, panelLeft + 48, panelTop + 99, panelWidth - 62, 22,
			NotesFont.apply(Component.translatable("screen.vildoria.notes.formula")));
		formulaField.setMaxLength(256);
		formulaField.setValue(cells[selectedRow][selectedColumn]);
		formulaField.addFormatter(NotesFont::format);
		formulaField.setResponder(value -> cells[selectedRow][selectedColumn] = value);
		addRenderableWidget(formulaField);
		setInitialFocus(formulaField);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		NotesSettings.Palette palette = NotesSettings.palette();
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, palette.background());
		graphics.outline(panelLeft, panelTop, panelWidth, panelHeight, palette.border());
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 36, palette.header());
		graphics.text(font, NotesFont.apply(title), panelLeft + 15, panelTop + 13, 0xffffffff);

		drawButton(graphics, actionButtonX(0), panelTop + 70, NEW_BUTTON_WIDTH, "screen.vildoria.notes.new", palette);
		drawButton(graphics, actionButtonX(1), panelTop + 70, OPEN_BUTTON_WIDTH, "screen.vildoria.notes.open", palette);
		drawButton(graphics, actionButtonX(2), panelTop + 70, SAVE_BUTTON_WIDTH, "screen.vildoria.notes.save", palette);
		int mainTextColor = NotesSettings.textColorValue();
		graphics.text(font, NotesFont.literal(selectedCellName()), panelLeft + 15, panelTop + 107, mainTextColor);
		drawGrid(graphics, palette);
		graphics.text(font, NotesFont.apply(status), panelLeft + 14, panelTop + panelHeight - 18, mainTextColor);

		super.extractRenderState(graphics, mouseX, mouseY, delta);
		if (openDialog) {
			drawOpenDialog(graphics, palette);
		}
	}

	private void drawGrid(GuiGraphicsExtractor graphics, NotesSettings.Palette palette) {
		if (NotesSettings.gridLines()) {
			graphics.fill(gridLeft, gridTop, gridLeft + INDEX_WIDTH, gridTop + rowHeight, palette.gridHeader());
			for (int column = 0; column < COLUMN_COUNT; column++) {
				int x = gridLeft + INDEX_WIDTH + column * cellWidth;
				graphics.fill(x, gridTop, x + cellWidth, gridTop + rowHeight, palette.gridHeader());
				graphics.centeredText(font, NotesFont.literal(Character.toString((char) ('A' + column))), x + cellWidth / 2,
						gridTop + Math.max(1, (rowHeight - 8) / 2), palette.text());
			}
		}

		for (int row = 0; row < ROW_COUNT; row++) {
			int y = gridTop + rowHeight * (row + 1);
			if (NotesSettings.gridLines()) {
				graphics.fill(gridLeft, y, gridLeft + INDEX_WIDTH, y + rowHeight, palette.gridHeader());
				graphics.centeredText(font, NotesFont.literal(Integer.toString(row + 1)), gridLeft + INDEX_WIDTH / 2,
						y + Math.max(1, (rowHeight - 8) / 2), palette.text());
			}
			for (int column = 0; column < COLUMN_COUNT; column++) {
				int x = gridLeft + INDEX_WIDTH + column * cellWidth;
				boolean selected = row == selectedRow && column == selectedColumn;
				graphics.fill(x, y, x + cellWidth, y + rowHeight, selected ? palette.selected() : palette.cell());
				if (NotesSettings.gridLines()) {
					graphics.outline(x, y, cellWidth, rowHeight, selected ? palette.accent() : palette.border());
				}
				String value = FormulaEvaluator.display(cells, row, column);
				graphics.text(font, NotesFont.literal(truncate(value, cellWidth - 8)), x + 4,
						y + Math.max(1, (rowHeight - 8) / 2), palette.text());
			}
		}
	}

	private void drawOpenDialog(GuiGraphicsExtractor graphics, NotesSettings.Palette palette) {
		int dialogWidth = Math.min(460, panelWidth - 24);
		int dialogHeight = Math.min(326, panelHeight - 24);
		int left = panelLeft + (panelWidth - dialogWidth) / 2;
		int top = panelTop + (panelHeight - dialogHeight) / 2;
		graphics.fill(left, top, left + dialogWidth, top + dialogHeight, palette.cell());
		graphics.outline(left, top, dialogWidth, dialogHeight, palette.border());
		graphics.fill(left, top, left + dialogWidth, top + 30, palette.gridHeader());
		graphics.text(font, NotesFont.apply(Component.translatable("screen.vildoria.notes.open_title")), left + 12, top + 10, NotesSettings.textColorValue());

		int visibleFiles = Math.max(0, Math.min(FILE_LIST_LIMIT, (dialogHeight - 68) / FILE_LIST_ROW_HEIGHT));
		if (noteFiles.isEmpty()) {
			graphics.text(font, NotesFont.apply(Component.translatable("screen.vildoria.notes.no_files")), left + 14, top + 48, palette.muted());
		} else {
			for (int index = 0; index < Math.min(noteFiles.size(), visibleFiles); index++) {
				int y = top + 36 + index * FILE_LIST_ROW_HEIGHT;
				graphics.fill(left + 8, y, left + dialogWidth - 8, y + FILE_LIST_ROW_HEIGHT - 2, palette.panel());
				graphics.text(font, NotesFont.literal(stripExtension(noteFiles.get(index).getFileName().toString())),
						left + 16, y + 7, NotesSettings.textColorValue());
			}
		}
		drawButton(graphics, left + dialogWidth - 84, top + dialogHeight - 28, 72,
				"screen.vildoria.notes.close", palette);
	}

	private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int buttonWidth, String label, NotesSettings.Palette palette) {
		graphics.fill(x, y, x + buttonWidth, y + 22, palette.button());
		graphics.outline(x, y, buttonWidth, 22, palette.border());
		graphics.centeredText(font, NotesFont.apply(Component.translatable(label)), x + buttonWidth / 2, y + 7, palette.text());
	}

	private int actionButtonX(int index) {
		int totalWidth = NEW_BUTTON_WIDTH + OPEN_BUTTON_WIDTH + SAVE_BUTTON_WIDTH + BUTTON_GAP * 2;
		int left = panelLeft + (panelWidth - totalWidth) / 2;
		return switch (index) {
			case 0 -> left;
			case 1 -> left + NEW_BUTTON_WIDTH + BUTTON_GAP;
			default -> left + NEW_BUTTON_WIDTH + OPEN_BUTTON_WIDTH + BUTTON_GAP * 2;
		};
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (openDialog) {
			return handleOpenDialogClick(mouseX, mouseY);
		}
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		if (inside(mouseX, mouseY, actionButtonX(0), panelTop + 70, NEW_BUTTON_WIDTH, 22)) {
			createDocument();
			return true;
		}
		if (inside(mouseX, mouseY, actionButtonX(1), panelTop + 70, OPEN_BUTTON_WIDTH, 22)) {
			refreshFileList();
			openDialog = true;
			return true;
		}
		if (inside(mouseX, mouseY, actionButtonX(2), panelTop + 70, SAVE_BUTTON_WIDTH, 22)) {
			saveDocument();
			return true;
		}

		int row = (int) ((mouseY - gridTop) / rowHeight) - 1;
		int column = (int) ((mouseX - gridLeft - INDEX_WIDTH) / cellWidth);
		if (row >= 0 && row < ROW_COUNT && column >= 0 && column < COLUMN_COUNT
				&& mouseX >= gridLeft + INDEX_WIDTH) {
			selectedRow = row;
			selectedColumn = column;
			formulaField.setValue(cells[row][column]);
			formulaField.setFocused(true);
			return true;
		}
		return false;
	}

	private boolean handleOpenDialogClick(double mouseX, double mouseY) {
		int dialogWidth = Math.min(460, panelWidth - 24);
		int dialogHeight = Math.min(326, panelHeight - 24);
		int left = panelLeft + (panelWidth - dialogWidth) / 2;
		int top = panelTop + (panelHeight - dialogHeight) / 2;
		if (inside(mouseX, mouseY, left + dialogWidth - 84, top + dialogHeight - 28, 72, 22)
				|| !inside(mouseX, mouseY, left, top, dialogWidth, dialogHeight)) {
			openDialog = false;
			return true;
		}
		int index = (int) ((mouseY - (top + 36)) / FILE_LIST_ROW_HEIGHT);
		int visibleFiles = Math.max(0, Math.min(FILE_LIST_LIMIT, (dialogHeight - 68) / FILE_LIST_ROW_HEIGHT));
		if (index >= 0 && index < Math.min(noteFiles.size(), visibleFiles)) {
			loadDocument(noteFiles.get(index));
			openDialog = false;
		}
		return true;
	}

	@Override
	public void onClose() {
		if (!documentName.isBlank()) {
			saveDocument();
		}
		super.onClose();
	}

	private void createDocument() {
		if (!documentName.isBlank()) {
			saveDocument();
		}
		clearCells();
		selectedRow = 0;
		selectedColumn = 0;
		documentName = "";
		documentNameField.setValue("");
		formulaField.setValue("");
		status = Component.translatable("screen.vildoria.notes.new_document");
	}

	private void saveDocument() {
		String safeName = documentName.trim().replaceAll("[^\\p{L}\\p{N} _-]", "_");
		if (safeName.isBlank()) {
			status = Component.translatable("screen.vildoria.notes.name_required");
			return;
		}
		documentName = safeName;
		documentNameField.setValue(safeName);
		Properties properties = new Properties();
		properties.setProperty("document", safeName);
		for (int row = 0; row < ROW_COUNT; row++) {
			for (int column = 0; column < COLUMN_COUNT; column++) {
				properties.setProperty(row + "." + column, cells[row][column]);
			}
		}
		try {
			Path path = notesFolder().resolve(safeName + ".vnotes");
			Files.createDirectories(path.getParent());
			try (var output = Files.newOutputStream(path)) {
				properties.store(output, "Vildoria notes");
			}
			status = Component.translatable("screen.vildoria.notes.saved", safeName);
		} catch (IOException exception) {
			status = Component.translatable("screen.vildoria.notes.save_failed");
			Vildoria.LOGGER.error("Could not save notes", exception);
		}
	}

	private void refreshFileList() {
		noteFiles.clear();
		Path folder = notesFolder();
		if (!Files.isDirectory(folder)) {
			return;
		}
		try (Stream<Path> files = Files.list(folder)) {
			files.filter(path -> path.getFileName().toString().endsWith(".vnotes"))
					.sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
					.forEach(noteFiles::add);
		} catch (IOException exception) {
			Vildoria.LOGGER.error("Could not list notes", exception);
		}
	}

	private void loadDocument(Path path) {
		Properties properties = new Properties();
		try (var input = Files.newInputStream(path)) {
			properties.load(input);
			clearCells();
			for (int row = 0; row < ROW_COUNT; row++) {
				for (int column = 0; column < COLUMN_COUNT; column++) {
					cells[row][column] = properties.getProperty(row + "." + column, "");
				}
			}
			documentName = properties.getProperty("document", stripExtension(path.getFileName().toString()));
			documentNameField.setValue(documentName);
			selectedRow = 0;
			selectedColumn = 0;
			formulaField.setValue(cells[0][0]);
			status = Component.translatable("screen.vildoria.notes.opened", documentName);
		} catch (IOException exception) {
			status = Component.translatable("screen.vildoria.notes.open_failed");
			Vildoria.LOGGER.error("Could not open notes", exception);
		}
	}

	private Path notesFolder() {
		return minecraft.gameDirectory.toPath().resolve("config").resolve("vildoria").resolve("notes");
	}

	private void clearCells() {
		for (int row = 0; row < ROW_COUNT; row++) {
			for (int column = 0; column < COLUMN_COUNT; column++) {
				cells[row][column] = "";
			}
		}
	}

	private boolean inside(double mouseX, double mouseY, int x, int y, int buttonWidth, int buttonHeight) {
		return mouseX >= x && mouseX < x + buttonWidth && mouseY >= y && mouseY < y + buttonHeight;
	}

	private String selectedCellName() {
		return Character.toString((char) ('A' + selectedColumn)) + (selectedRow + 1);
	}

	private String stripExtension(String fileName) {
		return fileName.endsWith(".vnotes") ? fileName.substring(0, fileName.length() - 7) : fileName;
	}

	private String truncate(String value, int maxWidth) {
		if (font.width(NotesFont.literal(value)) <= maxWidth) {
			return value;
		}
		String shortened = value;
		while (!shortened.isEmpty() && font.width(NotesFont.literal(shortened + "…")) > maxWidth) {
			shortened = shortened.substring(0, shortened.length() - 1);
		}
		return shortened + "…";
	}
}