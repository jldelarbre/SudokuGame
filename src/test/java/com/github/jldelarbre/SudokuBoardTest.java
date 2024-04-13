package com.github.jldelarbre;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static java.util.Collections.shuffle;
import static java.util.Collections.unmodifiableList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SudokuBoardTest {
    private final Random rand = new Random();
    private final int size = 3;
    private final int regionSize = size * size;
    private final int maxValue = size * size;

    private final SudokuBoard startBoard = SudokuBoard.create(size);
    List<Integer> shuffledRows = shuffledRows();
    List<Integer> shuffledColumns = shuffledColumns();
    List<Integer> shuffledValues = shuffledValues();

    @Test
    void checkEmptyBoard() {
        assertEquals(size, startBoard.getSize());
        assertEquals(size*size, startBoard.getRegionSize());
        assertEquals(size*size, startBoard.getMaxValue());

        SudokuBoard.Cell randomCell = startBoard.getCell(shuffledRows.getFirst(), shuffledColumns.getFirst());
        assertEquals(Optional.empty(), randomCell.getValue());
        assertEquals(allPossibleValues(), randomCell.getPossibleValues());
    }

    @Test
    void shouldSetCell() {
        SudokuBoard board = startBoard.set(shuffledValues.getFirst(),
                                           shuffledRows.getFirst(),
                                           shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.getCell(shuffledRows.getFirst(), shuffledColumns.getFirst());
        assertEquals(shuffledRows.getFirst(), randomCell.getRow());
        assertEquals(shuffledColumns.getFirst(), randomCell.getColumn());
        assertEquals(Optional.of(shuffledValues.getFirst()), randomCell.getValue());
        assertEquals(ImmutableSortedSet.of(), randomCell.getPossibleValues());
    }

    @Test
    void possibleValuesForUnsetCellOnRowWithOneCellSet() {
        SudokuBoard board = startBoard.set(shuffledValues.getFirst(),
                                           shuffledRows.getFirst(),
                                           shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.getCell(shuffledRows.getFirst(), shuffledColumns.get(1));
        assertEquals(Optional.empty(), randomCell.getValue());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst())),
                     randomCell.getPossibleValues());
    }

    @Test
    void possibleValuesForUnsetCellOnColumnWithOneCellSet() {
        SudokuBoard board = startBoard.set(shuffledValues.getFirst(),
                                           shuffledRows.getFirst(),
                                           shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.getCell(shuffledRows.get(1), shuffledColumns.getFirst());
        assertEquals(Optional.empty(), randomCell.getValue());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst())),
                     randomCell.getPossibleValues());
    }

    @Test
    void possibleValuesForUnsetCellOnRowWithTwoCellsSet() {
        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.getFirst())
                .set(shuffledValues.get(1), shuffledRows.getFirst(), shuffledColumns.get(1));

        SudokuBoard.Cell randomCell = board.getCell(shuffledRows.getFirst(), shuffledColumns.get(2));
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(), shuffledValues.get(1))),
                     randomCell.getPossibleValues());
    }

    @Test
    void possibleValuesForUnsetCellOnColumnWithTwoCellsSet() {
        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.getFirst())
                .set(shuffledValues.get(1), shuffledRows.get(1), shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.getCell(shuffledRows.get(2), shuffledColumns.getFirst());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(), shuffledValues.get(1))),
                     randomCell.getPossibleValues());
    }

    @Test
    void possibleValuesForUnsetCellWithOtherCellsSetOnRowAndColumn() {
        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.get(1))
                .set(shuffledValues.get(1), shuffledRows.get(1), shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.getCell(shuffledRows.getFirst(), shuffledColumns.getFirst());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(), shuffledValues.get(1))),
                     randomCell.getPossibleValues());
    }

    @Test
    void checkBoxExtractionFromCell() {
        SudokuBoard board = startBoard.set(shuffledValues.getFirst(),
                                           shuffledRows.getFirst(),
                                           shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.getCell(shuffledRows.getFirst(), shuffledColumns.getFirst());
        SudokuBoard.Box box = randomCell.getBox();
        int boxRow = ((shuffledRows.getFirst() - 1) / board.getSize()) + 1;
        int boxColumn = ((shuffledColumns.getFirst() - 1) / board.getSize()) + 1;

        assertEquals(boxRow, box.getRow());
        assertEquals(boxColumn, box.getColumn());
    }

    @Test
    void shallGetCellFromBox() {
        List<Integer> shuffledBoxRows = shuffledBoxRows();
        List<Integer> shuffledBoxColumns = shuffledBoxColumns();
        List<Integer> shuffledRowsInBox = shuffledRowsInBox();
        List<Integer> shuffledColumnsInBox = shuffledColumnsInBox();

        int boardRow = (shuffledBoxRows.getFirst() - 1) * size + shuffledRowsInBox.getFirst();
        int boardColumn = (shuffledBoxColumns.getFirst() - 1) * size + shuffledColumnsInBox.getFirst();

        SudokuBoard board = startBoard.set(shuffledValues.getFirst(), boardRow, boardColumn);
        SudokuBoard.Cell cell = board.getCell(boardRow, boardColumn);

        SudokuBoard.Box box = cell.getBox();

        assertEquals(Optional.of(shuffledValues.getFirst()), cell.getValue());
        assertEquals(shuffledBoxRows.getFirst(), box.getRow());
        assertEquals(shuffledBoxColumns.getFirst(), box.getColumn());
        assertEquals(shuffledRowsInBox.getFirst(), cell.getRowInBox());
        assertEquals(shuffledColumnsInBox.getFirst(), cell.getColumnInBox());
    }

    @Test
    void possibleValuesForUnsetCellWithOtherCellsSetOnRowColumnAndBox() {
        List<Integer> shuffledBoxRows = shuffledBoxRows();
        List<Integer> shuffledBoxColumns = shuffledBoxColumns();
        List<Integer> shuffledRowsInBox = shuffledRowsInBox();
        List<Integer> shuffledColumnsInBox = shuffledColumnsInBox();

        int referenceRow = (shuffledBoxRows.getFirst() - 1) * size + shuffledRowsInBox.getFirst();
        int referenceColumn = (shuffledBoxColumns.getFirst() - 1) * size + shuffledColumnsInBox.getFirst();
        int rowOutOfReferenceBox = (shuffledBoxRows.get(1) - 1) * size + shuffledRowsInBox.get(1);
        int columnOutOfReferenceBox = (shuffledBoxColumns.get(1) - 1) * size + shuffledColumnsInBox.get(1);
        int otherRowInReferenceBox = (shuffledBoxRows.getFirst() - 1) * size + shuffledRowsInBox.get(1);
        int otherColumnInReference = (shuffledBoxColumns.getFirst() - 1) * size + shuffledColumnsInBox.get(1);

        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), referenceRow, columnOutOfReferenceBox)
                .set(shuffledValues.get(1), rowOutOfReferenceBox, referenceColumn)
                .set(shuffledValues.get(2), otherRowInReferenceBox, otherColumnInReference);

        SudokuBoard.Cell randomCell = board.getCell(referenceRow, referenceColumn);
        assertEquals(Optional.empty(), randomCell.getValue());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(),
                                                              shuffledValues.get(1),
                                                              shuffledValues.get(2))),
                     randomCell.getPossibleValues());
    }

    @Test
    void checkBoundChecking() {
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.getCell(0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.getCell(regionSize + 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.getCell(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.getCell(1, regionSize + 1));

        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.set(1, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.set(1, regionSize + 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.set(1, 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.set(1, 1, regionSize + 1));

        assertThrows(IllegalArgumentException.class, () -> startBoard.set(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> startBoard.set(maxValue + 1, 1, 1));
    }

    private int offsetOf(int randomColumn) {
        return ((randomColumn - 1) + rand.nextInt(regionSize)) % regionSize;
    }

    private List<Integer> shuffledBoxRows() {
        List<Integer> shuffledIndexes = rangeClosed(1, size).boxed().collect(toList());
        shuffle(shuffledIndexes);
        return unmodifiableList(shuffledIndexes);
    }

    private List<Integer> shuffledBoxColumns() {
        List<Integer> shuffledIndexes = rangeClosed(1, size).boxed().collect(toList());
        shuffle(shuffledIndexes);
        return unmodifiableList(shuffledIndexes);
    }

    private List<Integer> shuffledRowsInBox() {
        List<Integer> shuffledIndexes = rangeClosed(1, size).boxed().collect(toList());
        shuffle(shuffledIndexes);
        return unmodifiableList(shuffledIndexes);
    }

    private List<Integer> shuffledColumnsInBox() {
        List<Integer> shuffledIndexes = rangeClosed(1, size).boxed().collect(toList());
        shuffle(shuffledIndexes);
        return unmodifiableList(shuffledIndexes);
    }

    private List<Integer> shuffledRows() {
        List<Integer> shuffledIndexes = rangeClosed(1, regionSize).boxed().collect(toList());
        shuffle(shuffledIndexes);
        return unmodifiableList(shuffledIndexes);
    }

    private List<Integer> shuffledColumns() {
        List<Integer> shuffledIndexes = rangeClosed(1, regionSize).boxed().collect(toList());
        shuffle(shuffledIndexes);
        return unmodifiableList(shuffledIndexes);
    }

    private List<Integer> shuffledValues() {
        List<Integer> shuffledValues = rangeClosed(1, maxValue).boxed().collect(toList());
        shuffle(shuffledValues);
        return unmodifiableList(shuffledValues);
    }

    private SortedSet<Integer> allPossibleValues() {
        return rangeClosed(1, startBoard.getMaxValue()).boxed().collect(toImmutableSortedSet(Integer::compareTo));
    }

    private SortedSet<Integer> allPossibleValuesExcept(List<Integer> discardedValues) {
        return rangeClosed(1, startBoard.getMaxValue()).boxed()
                .filter(not(discardedValues::contains))
                .collect(toImmutableSortedSet(Integer::compareTo));
    }
}
