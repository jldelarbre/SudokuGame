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
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(size, startBoard.size());
        assertEquals(size*size, startBoard.regionSize());
        assertEquals(size*size, startBoard.maxValue());

        SudokuBoard.Cell randomCell = startBoard.cell(shuffledRows.getFirst(), shuffledColumns.getFirst());
        assertEquals(Optional.empty(), randomCell.value());
        assertEquals(allPossibleValues(), randomCell.getPossibleValuesThatDoNotProduceDuplicate());
    }

    @Test
    void shouldSetCell() {
        SudokuBoard board = startBoard.set(shuffledValues.getFirst(),
                                           shuffledRows.getFirst(),
                                           shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.cell(shuffledRows.getFirst(), shuffledColumns.getFirst());
        assertEquals(shuffledRows.getFirst(), randomCell.rowIndex());
        assertEquals(shuffledColumns.getFirst(), randomCell.columnIndex());
        assertEquals(Optional.of(shuffledValues.getFirst()), randomCell.value());
        assertEquals(ImmutableSortedSet.of(), randomCell.getPossibleValuesThatDoNotProduceDuplicate());
    }

    @Test
    void shouldResetCell() {
        SudokuBoard board = startBoard.set(shuffledValues.getFirst(),
                                           shuffledRows.getFirst(),
                                           shuffledColumns.getFirst());

        SudokuBoard newBoard = board.clear(shuffledRows.getFirst(), shuffledColumns.getFirst());

        SudokuBoard.Cell clearedCell = newBoard.cell(shuffledRows.getFirst(), shuffledColumns.getFirst());
        assertEquals(Optional.empty(), clearedCell.value());
    }

    @Test
    void possibleValuesForUnsetCellOnRowWithOneCellSet() {
        SudokuBoard board = startBoard.set(shuffledValues.getFirst(),
                                           shuffledRows.getFirst(),
                                           shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.cell(shuffledRows.getFirst(), shuffledColumns.get(1));
        assertEquals(Optional.empty(), randomCell.value());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst())),
                     randomCell.getPossibleValuesThatDoNotProduceDuplicate());
    }

    @Test
    void possibleValuesForUnsetCellOnColumnWithOneCellSet() {
        SudokuBoard board = startBoard.set(shuffledValues.getFirst(),
                                           shuffledRows.getFirst(),
                                           shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.cell(shuffledRows.get(1), shuffledColumns.getFirst());
        assertEquals(Optional.empty(), randomCell.value());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst())),
                     randomCell.getPossibleValuesThatDoNotProduceDuplicate());
    }

    @Test
    void possibleValuesForUnsetCellOnRowWithTwoCellsSet() {
        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.getFirst())
                .set(shuffledValues.get(1), shuffledRows.getFirst(), shuffledColumns.get(1));

        SudokuBoard.Cell randomCell = board.cell(shuffledRows.getFirst(), shuffledColumns.get(2));
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(), shuffledValues.get(1))),
                     randomCell.getPossibleValuesThatDoNotProduceDuplicate());
    }

    @Test
    void possibleValuesForUnsetCellOnColumnWithTwoCellsSet() {
        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.getFirst())
                .set(shuffledValues.get(1), shuffledRows.get(1), shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.cell(shuffledRows.get(2), shuffledColumns.getFirst());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(), shuffledValues.get(1))),
                     randomCell.getPossibleValuesThatDoNotProduceDuplicate());
    }

    @Test
    void possibleValuesForUnsetCellWithOtherCellsSetOnRowAndColumn() {
        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.get(1))
                .set(shuffledValues.get(1), shuffledRows.get(1), shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.cell(shuffledRows.getFirst(), shuffledColumns.getFirst());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(), shuffledValues.get(1))),
                     randomCell.getPossibleValuesThatDoNotProduceDuplicate());
    }

    @Test
    void checkBoxExtractionFromCell() {
        SudokuBoard board = startBoard.set(shuffledValues.getFirst(),
                                           shuffledRows.getFirst(),
                                           shuffledColumns.getFirst());

        SudokuBoard.Cell randomCell = board.cell(shuffledRows.getFirst(), shuffledColumns.getFirst());
        SudokuBoard.Box box = randomCell.box();
        int boxRow = ((shuffledRows.getFirst() - 1) / board.size()) + 1;
        int boxColumn = ((shuffledColumns.getFirst() - 1) / board.size()) + 1;

        assertEquals(boxRow, box.boxRowIndex());
        assertEquals(boxColumn, box.boxColumnIndex());
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
        SudokuBoard.Cell cell = board.cell(boardRow, boardColumn);

        SudokuBoard.Box box = cell.box();

        assertEquals(Optional.of(shuffledValues.getFirst()), cell.value());
        assertEquals(shuffledBoxRows.getFirst(), box.boxRowIndex());
        assertEquals(shuffledBoxColumns.getFirst(), box.boxColumnIndex());
        assertEquals(shuffledRowsInBox.getFirst(), cell.rowIndexInBox());
        assertEquals(shuffledColumnsInBox.getFirst(), cell.columnIndexInBox());
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

        SudokuBoard.Cell randomCell = board.cell(referenceRow, referenceColumn);
        assertEquals(Optional.empty(), randomCell.value());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(),
                                                              shuffledValues.get(1),
                                                              shuffledValues.get(2))),
                     randomCell.getPossibleValuesThatDoNotProduceDuplicate());
    }

    @Test
    void checkRowMissingValues() {
        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.getFirst())
                .set(shuffledValues.get(1), shuffledRows.getFirst(), shuffledColumns.get(1));

        SudokuBoard.Row row = board.row(shuffledRows.getFirst());

        assertEquals(shuffledRows.getFirst(), row.index());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(),
                                                              shuffledValues.get(1))),
                     row.missingValues());
    }

    @Test
    void checkColumnMissingValues() {
        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.getFirst())
                .set(shuffledValues.get(1), shuffledRows.get(1), shuffledColumns.getFirst());

        SudokuBoard.Column column = board.column(shuffledColumns.getFirst());

        assertEquals(shuffledColumns.getFirst(), column.index());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(),
                                                              shuffledValues.get(1))),
                     column.missingValues());
    }

    @Test
    void checkBoxMissingValues() {
        List<Integer> shuffledBoxRows = shuffledBoxRows();
        List<Integer> shuffledBoxColumns = shuffledBoxColumns();
        List<Integer> shuffledRowsInBox = shuffledRowsInBox();
        List<Integer> shuffledColumnsInBox = shuffledColumnsInBox();

        int boardRow = (shuffledBoxRows.getFirst() - 1) * size + shuffledRowsInBox.getFirst();
        int boardColumn = (shuffledBoxColumns.getFirst() - 1) * size + shuffledColumnsInBox.getFirst();
        int boardRow2 = (shuffledBoxRows.getFirst() - 1) * size + shuffledRowsInBox.get(1);
        int boardColumn2 = (shuffledBoxColumns.getFirst() - 1) * size + shuffledColumnsInBox.get(1);

        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), boardRow, boardColumn)
                .set(shuffledValues.get(1), boardRow2, boardColumn2);

        SudokuBoard.Box box = board.box(shuffledBoxRows.getFirst(), shuffledBoxColumns.getFirst());

        assertEquals(shuffledBoxRows.getFirst(), box.boxRowIndex());
        assertEquals(shuffledBoxColumns.getFirst(), box.boxColumnIndex());
        assertEquals(allPossibleValuesExcept(ImmutableList.of(shuffledValues.getFirst(),
                                                              shuffledValues.get(1))),
                     box.missingValues());
    }

    @Test
    void checkNoErrors() {
        List<Integer> shuffledBoxRows = shuffledBoxRows();
        List<Integer> shuffledBoxColumns = shuffledBoxColumns();
        List<Integer> shuffledRowsInBox = shuffledRowsInBox();
        List<Integer> shuffledColumnsInBox = shuffledColumnsInBox();

        int boardRow = (shuffledBoxRows.getFirst() - 1) * size + shuffledRowsInBox.getFirst();
        int boardColumn = (shuffledBoxColumns.getFirst() - 1) * size + shuffledColumnsInBox.getFirst();
        int boardRow2 = (shuffledBoxRows.getFirst() - 1) * size + shuffledRowsInBox.get(1);
        int boardColumn2 = (shuffledBoxColumns.getFirst() - 1) * size + shuffledColumnsInBox.get(1);

        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), boardRow, boardColumn)
                .set(shuffledValues.get(1), boardRow2, boardColumn2)
                .set(shuffledValues.get(2), boardRow, boardColumn2);

        SudokuBoard.Row row = board.row(boardRow);
        SudokuBoard.Column column = board.column(boardColumn2);
        SudokuBoard.Box box = board.box(shuffledBoxRows.getFirst(), shuffledBoxColumns.getFirst());

        assertTrue(row.getDuplicateValuesErrors().isEmpty());
        assertTrue(column.getDuplicateValuesErrors().isEmpty());
        assertTrue(box.getDuplicateValuesErrors().isEmpty());
    }

    @Test
    void checkDuplicateValuesInRowErrors() {
        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.getFirst())
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.get(1));

        SudokuBoard.Row row = board.row(shuffledRows.getFirst());

        Set<SudokuBoard.Cell> erroneousCells = row.getDuplicateValuesErrors();

        assertEquals(2, erroneousCells.size());
        Iterator<SudokuBoard.Cell> iterator = erroneousCells.iterator();
        SudokuBoard.Cell cell1 = iterator.next();
        SudokuBoard.Cell cell2 = iterator.next();

        assertEquals(shuffledValues.getFirst(), cell1.value().get());
        assertEquals(shuffledValues.getFirst(), cell2.value().get());
        assertEquals(shuffledRows.getFirst(), cell1.rowIndex());
        assertEquals(shuffledRows.getFirst(), cell2.rowIndex());
    }

    @Test
    void checkDuplicateValuesInColumnErrors() {
        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), shuffledRows.getFirst(), shuffledColumns.getFirst())
                .set(shuffledValues.getFirst(), shuffledRows.get(1), shuffledColumns.getFirst());

        SudokuBoard.Column column = board.column(shuffledColumns.getFirst());

        Set<SudokuBoard.Cell> erroneousCells = column.getDuplicateValuesErrors();

        assertEquals(2, erroneousCells.size());
        Iterator<SudokuBoard.Cell> iterator = erroneousCells.iterator();
        SudokuBoard.Cell cell1 = iterator.next();
        SudokuBoard.Cell cell2 = iterator.next();

        assertEquals(shuffledValues.getFirst(), cell1.value().get());
        assertEquals(shuffledValues.getFirst(), cell2.value().get());
        assertEquals(shuffledColumns.getFirst(), cell1.columnIndex());
        assertEquals(shuffledColumns.getFirst(), cell2.columnIndex());
    }

    @Test
    void checkDuplicateValuesInBoxErrors() {
        List<Integer> shuffledBoxRows = shuffledBoxRows();
        List<Integer> shuffledBoxColumns = shuffledBoxColumns();
        List<Integer> shuffledRowsInBox = shuffledRowsInBox();
        List<Integer> shuffledColumnsInBox = shuffledColumnsInBox();

        int boardRow = (shuffledBoxRows.getFirst() - 1) * size + shuffledRowsInBox.getFirst();
        int boardColumn = (shuffledBoxColumns.getFirst() - 1) * size + shuffledColumnsInBox.getFirst();
        int boardRow2 = (shuffledBoxRows.getFirst() - 1) * size + shuffledRowsInBox.get(1);
        int boardColumn2 = (shuffledBoxColumns.getFirst() - 1) * size + shuffledColumnsInBox.get(1);

        SudokuBoard board = startBoard
                .set(shuffledValues.getFirst(), boardRow, boardColumn)
                .set(shuffledValues.getFirst(), boardRow2, boardColumn2);

        SudokuBoard.Box box = board.box(shuffledBoxRows.getFirst(), shuffledBoxColumns.getFirst());

        Set<SudokuBoard.Cell> erroneousCells = box.getDuplicateValuesErrors();

        assertEquals(2, erroneousCells.size());
        Iterator<SudokuBoard.Cell> iterator = erroneousCells.iterator();
        SudokuBoard.Cell cell1 = iterator.next();
        SudokuBoard.Cell cell2 = iterator.next();

        assertEquals(shuffledValues.getFirst(), cell1.value().get());
        assertEquals(shuffledValues.getFirst(), cell2.value().get());
        assertEquals(shuffledBoxRows.getFirst(), cell1.box().boxRowIndex());
        assertEquals(shuffledBoxColumns.getFirst(), cell1.box().boxColumnIndex());
        assertEquals(shuffledBoxRows.getFirst(), cell2.box().boxRowIndex());
        assertEquals(shuffledBoxColumns.getFirst(), cell2.box().boxColumnIndex());
    }

    @Test
    void checkImpossibleToFillValueInRowErrors() {
        SudokuBoard board = startBoard
                .set(2, 1, 1)
                .set(3, 1, 4)
                .set(1, 2, 1)
                .set(1, 3, 9)
                .set(1, 4, 5)
                .set(1, 7, 6);

        SudokuBoard.Row row = board.row(1);

        SortedSet<Integer> impossibleToFillValueErrors = row.getImpossibleToFillValueErrors();

        assertEquals(1, impossibleToFillValueErrors.size());
        assertTrue(impossibleToFillValueErrors.contains(1));
    }

    @Test
    void checkImpossibleToFillValueInColumnErrors() {
        SudokuBoard board = startBoard
                .set(2, 1, 1)
                .set(3, 5, 1)
                .set(1, 1, 2)
                .set(1, 9, 3)
                .set(1, 4, 4)
                .set(1, 6, 7);

        SudokuBoard.Column col = board.column(1);

        SortedSet<Integer> impossibleToFillValueErrors = col.getImpossibleToFillValueErrors();

        assertEquals(1, impossibleToFillValueErrors.size());
        assertTrue(impossibleToFillValueErrors.contains(1));
    }

    @Test
    void checkImpossibleToFillValueInBoxErrors() {
        SudokuBoard board = startBoard
                .set(2, 1, 1)
                .set(3, 5, 1)
                .set(1, 1, 2)
                .set(1, 9, 3)
                .set(1, 4, 4)
                .set(1, 6, 7);

        SudokuBoard.Box box = board.box(2, 1);

        SortedSet<Integer> impossibleToFillValueErrors = box.getImpossibleToFillValueErrors();

        assertEquals(1, impossibleToFillValueErrors.size());
        assertTrue(impossibleToFillValueErrors.contains(1));
    }

    @Test
    void checkUnfillableErroneousCellErrors() {
        SudokuBoard board = startBoard
                .set(7, 1, 7)
                .set(8, 1, 8)
                .set(9, 1, 9)
                .set(1, 3, 4)
                .set(2, 3, 5)
                .set(3, 3, 6)
                .set(4, 4, 7)
                .set(5, 5, 7)
                .set(6, 6, 7);

        Set<SudokuBoard.Cell> unfillableErroneousCells = board.unfillableErroneousCells();

        assertEquals(1, unfillableErroneousCells.size());
        SudokuBoard.Cell erroneousCell = unfillableErroneousCells.iterator().next();
        assertEquals(3, erroneousCell.rowIndex());
        assertEquals(7, erroneousCell.columnIndex());
    }

    @Test
    void checkGetRemainingCandidateValuesUniquePositionValueExtractedIfAny_AllValuesPossible() {
        SudokuBoard board = startBoard
                .set(1, 1, 1)
                .set(2, 1, 2)
                .set(3, 1, 4)
                .set(4, 4, 3);

        SudokuBoard.Cell cell = board.cell(1, 3);

        SortedSet<Integer> possibleValuesThatDoNotProduceDuplicate = cell.getPossibleValuesThatDoNotProduceDuplicate();
        assertEquals(5, possibleValuesThatDoNotProduceDuplicate.size());
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(5));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(6));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(7));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(8));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(9));

        SortedSet<Integer> remainingCandidateValuesUniquePositionExtractedValueIfAny =
                cell.getRemainingCandidateValuesUniquePositionValueExtractedIfAny();

        assertEquals(5, remainingCandidateValuesUniquePositionExtractedValueIfAny.size());
        assertTrue(remainingCandidateValuesUniquePositionExtractedValueIfAny.contains(5));
        assertTrue(remainingCandidateValuesUniquePositionExtractedValueIfAny.contains(6));
        assertTrue(remainingCandidateValuesUniquePositionExtractedValueIfAny.contains(7));
        assertTrue(remainingCandidateValuesUniquePositionExtractedValueIfAny.contains(8));
        assertTrue(remainingCandidateValuesUniquePositionExtractedValueIfAny.contains(9));
    }

    @Test
    void checkGetRemainingCandidateValuesUniquePositionValueExtractedIfAny_RowCase() {
        SudokuBoard board = startBoard
                .set(1, 3, 1)
                .set(2, 3, 2)
                .set(3, 3, 5)
                .set(7, 2, 7)
                .set(7, 4, 4)
                .set(7, 7, 6);

        SudokuBoard.Cell cell = board.cell(3, 3);

        SortedSet<Integer> possibleValuesThatDoNotProduceDuplicate = cell.getPossibleValuesThatDoNotProduceDuplicate();
        assertEquals(6, possibleValuesThatDoNotProduceDuplicate.size());
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(4));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(5));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(6));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(7));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(8));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(9));

        SortedSet<Integer> remainingCandidateValuesUniquePositionValueExtractedIfAny =
                cell.getRemainingCandidateValuesUniquePositionValueExtractedIfAny();

        assertEquals(1, remainingCandidateValuesUniquePositionValueExtractedIfAny.size());
        assertTrue(remainingCandidateValuesUniquePositionValueExtractedIfAny.contains(7));
    }

    @Test
    void checkGetRemainingCandidateValuesUniquePositionValueExtractedIfAny_ColumnCase() {
        SudokuBoard board = startBoard
                .set(1, 1, 3)
                .set(2, 2, 3)
                .set(3, 5, 3)
                .set(7, 7, 2)
                .set(7, 4, 4)
                .set(7, 6, 7);

        SudokuBoard.Cell cell = board.cell(3, 3);

        SortedSet<Integer> possibleValuesThatDoNotProduceDuplicate = cell.getPossibleValuesThatDoNotProduceDuplicate();
        assertEquals(6, possibleValuesThatDoNotProduceDuplicate.size());
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(4));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(5));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(6));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(7));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(8));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(9));

        SortedSet<Integer> remainingCandidateValuesUniquePositionValueExtractedIfAny =
                cell.getRemainingCandidateValuesUniquePositionValueExtractedIfAny();

        assertEquals(1, remainingCandidateValuesUniquePositionValueExtractedIfAny.size());
        assertTrue(remainingCandidateValuesUniquePositionValueExtractedIfAny.contains(7));
    }

    @Test
    void checkGetRemainingCandidateValuesUniquePositionValueExtractedIfAny_BoxCase() {
        SudokuBoard board = startBoard
                .set(1, 1, 1)
                .set(2, 1, 2)
                .set(3, 2, 1)
                .set(4, 2, 2)
                .set(6, 2, 3)
                .set(7, 3, 2)
                .set(5, 1, 4)
                .set(5, 4, 1);

        SudokuBoard.Cell cell = board.cell(3, 3);

        SortedSet<Integer> possibleValuesThatDoNotProduceDuplicate = cell.getPossibleValuesThatDoNotProduceDuplicate();
        assertEquals(3, possibleValuesThatDoNotProduceDuplicate.size());
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(5));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(8));
        assertTrue(possibleValuesThatDoNotProduceDuplicate.contains(9));

        SortedSet<Integer> remainingCandidateValuesUniquePositionValueExtractedIfAny =
                cell.getRemainingCandidateValuesUniquePositionValueExtractedIfAny();

        assertEquals(1, remainingCandidateValuesUniquePositionValueExtractedIfAny.size());
        assertTrue(remainingCandidateValuesUniquePositionValueExtractedIfAny.contains(5));
    }

    @Test
    void checkGetPossibleValuesNotTakingPlaceOfPair_RowCase() {
        SudokuBoard board = startBoard
                .set(1, 1, 1)
                .set(2, 1, 2)
                .set(3, 1, 3)
                .set(7, 2, 1)
                .set(8, 2, 2)
                .set(9, 2, 3)
                .set(4, 3, 1)
                .set(7, 3, 4)
                .set(8, 3, 5)
                .set(9, 3, 6)
                .set(1, 3, 8)
                .set(5, 6, 7);

        SudokuBoard.Cell cell = board.cell(3, 2);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair =
                cell.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair.contains(5));
        assertTrue(possibleValuesNotTakingPlaceOfPair.contains(6));

        SudokuBoard.Cell cell2 = board.cell(3, 3);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair2 =
                cell2.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair2.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair2.contains(5));
        assertTrue(possibleValuesNotTakingPlaceOfPair2.contains(6));

        SudokuBoard.Cell cell3 = board.cell(3, 7);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair3 =
                cell3.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair3.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair3.contains(2));
        assertTrue(possibleValuesNotTakingPlaceOfPair3.contains(3));

        SudokuBoard.Cell cell4 = board.cell(3, 9);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair4 =
                cell4.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair4.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair4.contains(2));
        assertTrue(possibleValuesNotTakingPlaceOfPair4.contains(3));
    }

    @Test
    void checkGetPossibleValuesNotTakingPlaceOfPair_ColumnCase() {
        SudokuBoard board = startBoard
                .set(1, 1, 1)
                .set(2, 2, 1)
                .set(3, 3, 1)
                .set(7, 1, 2)
                .set(8, 2, 2)
                .set(9, 3, 2)
                .set(4, 1, 3)
                .set(7, 4, 3)
                .set(8, 5, 3)
                .set(9, 6, 3)
                .set(1, 8, 3);

        SudokuBoard.Cell cell = board.cell(2, 3);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair =
                cell.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair.contains(5));
        assertTrue(possibleValuesNotTakingPlaceOfPair.contains(6));

        SudokuBoard.Cell cell2 = board.cell(3, 3);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair2 =
                cell2.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair2.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair2.contains(5));
        assertTrue(possibleValuesNotTakingPlaceOfPair2.contains(6));

        SudokuBoard.Cell cell3 = board.cell(7, 3);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair3 =
                cell3.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair3.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair3.contains(2));
        assertTrue(possibleValuesNotTakingPlaceOfPair3.contains(3));

        SudokuBoard.Cell cell4 = board.cell(9, 3);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair4 =
                cell4.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair4.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair4.contains(2));
        assertTrue(possibleValuesNotTakingPlaceOfPair4.contains(3));
    }

    @Test
    void checkGetPossibleValuesNotTakingPlaceOfPair_BoxCase() {
        SudokuBoard board = startBoard
                .set(1, 8, 4)
                .set(2, 8, 5)
                .set(4, 7, 7)
                .set(3, 8, 9)
                .set(7, 9, 7)
                .set(8, 9, 8)
                .set(9, 9, 9);

        SudokuBoard.Cell cell = board.cell(8, 7);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair =
                cell.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair.contains(5));
        assertTrue(possibleValuesNotTakingPlaceOfPair.contains(6));

        SudokuBoard.Cell cell2 = board.cell(8, 8);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair2 =
                cell2.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair2.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair2.contains(5));
        assertTrue(possibleValuesNotTakingPlaceOfPair2.contains(6));

        SudokuBoard.Cell cell3 = board.cell(7, 8);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair3 =
                cell3.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair3.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair3.contains(1));
        assertTrue(possibleValuesNotTakingPlaceOfPair3.contains(2));

        SudokuBoard.Cell cell4 = board.cell(7, 9);
        SortedSet<Integer> possibleValuesNotTakingPlaceOfPair4 =
                cell4.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(2);
        assertEquals(2, possibleValuesNotTakingPlaceOfPair4.size());
        assertTrue(possibleValuesNotTakingPlaceOfPair4.contains(1));
        assertTrue(possibleValuesNotTakingPlaceOfPair4.contains(2));
    }

    @Test
    void checkBoundChecking() {
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.cell(0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.cell(regionSize + 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.cell(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.cell(1, regionSize + 1));

        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.set(1, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.set(1, regionSize + 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.set(1, 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> startBoard.set(1, 1, regionSize + 1));

        assertThrows(IllegalArgumentException.class, () -> startBoard.set(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> startBoard.set(maxValue + 1, 1, 1));
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
        return rangeClosed(1, startBoard.maxValue()).boxed().collect(toImmutableSortedSet(Integer::compareTo));
    }

    private SortedSet<Integer> allPossibleValuesExcept(List<Integer> discardedValues) {
        return rangeClosed(1, startBoard.maxValue()).boxed()
                .filter(not(discardedValues::contains))
                .collect(toImmutableSortedSet(Integer::compareTo));
    }
}
