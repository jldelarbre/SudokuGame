package com.github.jldelarbre;

import com.google.common.collect.*;

import java.util.*;
import java.util.function.Supplier;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newTreeSet;
import static java.util.Collections.unmodifiableSortedSet;
import static java.util.function.Predicate.not;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.generate;

public class SudokuBoard {

    private final int size;
    private final int regionSize;
    private final int maxValue;
    private final List<Optional<Integer>> values;

    private final Cell[][] cells;
    private final List<Row> rows;
    private final List<Column> columns;
    private final Box[][] boxes;

    private final int NUM_CONSTRAINT_LEVEL = 5;
    private final List<Map<Cell, SortedSet<Integer>>> remainingCandidateValuesUniquenessExtracted =
            generate((Supplier<Map<Cell, SortedSet<Integer>>>) Maps::newHashMap)
                    .limit(NUM_CONSTRAINT_LEVEL)
                    .collect(toImmutableList());

    private final List<Map<Cell, SortedSet<Integer>>> remainingCandidateValues =
            generate((Supplier<Map<Cell, SortedSet<Integer>>>) Maps::newHashMap)
                    .limit(NUM_CONSTRAINT_LEVEL)
                    .collect(toImmutableList());

    public static final int USED_CONSTRAINT_LEVEL = 4;

    private SudokuBoard(int size, int regionSize, int maxValue, List<Optional<Integer>> values) {
        this.size = size;
        this.regionSize = regionSize;
        this.maxValue = maxValue;
        this.values = values;

        cells = rangeClosed(1, regionSize)
                .mapToObj(row -> rangeClosed(1, regionSize)
                        .mapToObj(column -> new Cell(this, row, column))
                        .toArray(Cell[]::new)
                )
                .toArray(Cell[][]::new);
        rows = rangeClosed(1, regionSize)
                .mapToObj(row -> new Row(this, row))
                .collect(toImmutableList());
        columns = rangeClosed(1, regionSize)
                .mapToObj(column -> new Column(this, column))
                .collect(toImmutableList());
        boxes = rangeClosed(1, size)
                .mapToObj(row -> rangeClosed(1, size)
                                    .mapToObj(column -> new Box(this, row, column))
                                    .toArray(Box[]::new)
                )
                .toArray(Box[][]::new);
    }

    public static SudokuBoard create(int size) {
        int squaredSize = size * size;
        List<Optional<Integer>> empties = generate((Supplier<Optional<Integer>>) Optional::empty)
                .limit((long) squaredSize * squaredSize)
                .collect(toImmutableList());
        return new SudokuBoard(size, squaredSize, squaredSize, empties);
    }

    public int size() {
        return size;
    }

    public int regionSize() {
        return regionSize;
    }

    public int maxValue() {
        return maxValue;
    }

    public Cell cell(int row, int column) {
        checkIndexes(row, column);
        return cells[row - 1][column - 1];
    }

    public Collection<Cell> cells() {
        return Arrays.stream(cells).flatMap(Arrays::stream).collect(toImmutableList());
    }

    public SudokuBoard set(int value, int row, int column) {
        checkIndexes(row, column);
        checkValue(value);
        int index = listValuesIndex(row, column);
        List<Optional<Integer>> temp = newArrayList(values);
        temp.set(index, Optional.of(value));
        List<Optional<Integer>> updatedList = ImmutableList.copyOf(temp);
        return new SudokuBoard(size, regionSize, maxValue, updatedList);
    }

    public SudokuBoard clear(int row, int column) {
        checkIndexes(row, column);
        int index = listValuesIndex(row, column);
        List<Optional<Integer>> temp = newArrayList(values);
        temp.set(index, Optional.empty());
        List<Optional<Integer>> updatedList = ImmutableList.copyOf(temp);
        return new SudokuBoard(size, regionSize, maxValue, updatedList);
    }

    public SudokuBoard.Row row(int row) {
        if (row < 1 || row > regionSize) {
            throw new IndexOutOfBoundsException("Row out of bound: (" + row + ") - regionSize = " + regionSize);
        }
        return rows.get(row - 1);
    }

    public SudokuBoard.Column column(int column) {
        if (column < 1 || column > regionSize) {
            throw new IndexOutOfBoundsException("Column out of bound: (" + column + ") - regionSize = " + regionSize);
        }
        return columns.get(column - 1);
    }

    public Box box(int row, int column) {
        if (row < 1 || row > size || column < 1 || column > size) {
            throw new IndexOutOfBoundsException("Box row, column out of bound: (" + row + ", " + column + ") - size = " + size);
        }
        return boxes[row - 1][column - 1];
    }

    public Set<Cell> unfillableErroneousCells() {
        return cells().stream()
                .filter(cell -> cell.value().isEmpty())
                .filter(cell -> cell.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(USED_CONSTRAINT_LEVEL).isEmpty())
                .collect(toImmutableSet());
    }

    private void checkValue(int value) {
        if (value < 1 || value > maxValue) {
            throw new IllegalArgumentException("Value = " + value + " shall be in [1 " + maxValue + "]");
        }
    }

    private void checkIndexes(int row, int column) {
        if (row < 1 || row > regionSize || column < 1 || column > regionSize) {
            throw new IndexOutOfBoundsException("Row, column out of bound: (" + row + ", " + column + ") - regionSize = " + regionSize);
        }
    }

    private int listValuesIndex(int row, int column) {
        return (row - 1) * regionSize + (column - 1);
    }

    private SortedSet<Integer> allPossibleValues() {
        return rangeClosed(1, maxValue).boxed().collect(toImmutableSortedSet(Integer::compareTo));
    }

    public static class Cell {
        private final SudokuBoard board;
        private final int row;
        private final int column;

        private Cell(SudokuBoard board, int row, int column) {
            this.board = board;
            this.row = row;
            this.column = column;
        }

        public Optional<Integer> value() {
            int index = board.listValuesIndex(row, column);
            return board.values.get(index);
        }

        public SortedSet<Integer> getPossibleValuesThatDoNotProduceDuplicate() {
            return getRemainingCandidateValuesAfterEliminationFromNeighboringCells(0);
        }

        /**
         * For a given cell, we start with all candidate values remaining after the elimination of a previous call
         * to {@link Cell#getRemainingCandidateValuesAfterEliminationFromNeighboringCells(int)} with
         * {@code (neighborhoodConstraintLevel - 1)}.<br>
         * When {@code neighborhoodConstraintLevel == 0}, we begin with all possible values.<br>
         * The 3 regions (row, column, box) a cell belongs to are called its neighborhood. In a cell neighborhood, we
         * look in other cells if values must be (or are) set in those cells. So we eliminated those values from the
         * current cell.<br>
         * In the neighboring cells, the candidate remaining value(s) form a nuplet. This same nuplet may be found in
         * many other cells.<br>
         * The value(s) of a nuplet found in exactly n location(s) in the neighboring cells could be eliminated from the
         * current cell.
         *
         * @param neighborhoodConstraintLevel constraint level on neighboring cells
         * @return remaining candidate values in current cell after elimination of values (if any) by neighborhoodConstraintLevel
         * constraints.<br>
         * The higher the neighborhoodConstraintLevel is, the greater the elimination of candidate values is.
         */
        public SortedSet<Integer>
        getRemainingCandidateValuesAfterEliminationFromNeighboringCells(int neighborhoodConstraintLevel) {
            if (value().isPresent()) {
                return ImmutableSortedSet.of();
            }
            SortedSet<Integer> cachedResult = board.remainingCandidateValues.get(neighborhoodConstraintLevel).get(this);
            if (cachedResult != null) {
                return cachedResult;
            }

            SortedSet<Integer> remainingCandidateValues = computeRemainingCandidateValues(neighborhoodConstraintLevel);
            board.remainingCandidateValues.get(neighborhoodConstraintLevel).put(this, remainingCandidateValues);
            return remainingCandidateValues;
        }

        private ImmutableSortedSet<Integer> computeRemainingCandidateValues(int neighborhoodConstraintLevel) {
            SortedSet<Integer> remainingCandidateValuesOfPreviousConstraintLevel =
                    getRemainingCandidateValuesUniquePositionValueExtractedIfAny(neighborhoodConstraintLevel - 1);
            SortedSet<Integer> constrainedValuesOfEliminationBySurroundingCells =
                    computeConstrainedValuesOfEliminationBySurroundingCells(neighborhoodConstraintLevel);

            ImmutableSortedSet<Integer> remainingCandidateValues = remainingCandidateValuesOfPreviousConstraintLevel.stream()
                    .filter(not(constrainedValuesOfEliminationBySurroundingCells::contains))
                    .collect(toImmutableSortedSet(Integer::compareTo));
            return remainingCandidateValues;
        }

        private SortedSet<Integer> computeConstrainedValuesOfEliminationBySurroundingCells(int neighborhoodConstraintLevel) {
            final SortedSet<Integer> constrainedValuesOfEliminationBySurroundingCells;
            if (neighborhoodConstraintLevel == 0) {
                SortedSet<Integer> usedValuesInNeighborhood = neighborhood().stream()
                        .map(Region::usedValues)
                        .flatMap(Collection::stream)
                        .collect(toImmutableSortedSet(Integer::compareTo));
                constrainedValuesOfEliminationBySurroundingCells = usedValuesInNeighborhood;
            } else {
                SortedSet<Integer> constrainedValuesOfElimination =
                        computeConstrainedValuesOfEliminationByNeighboringNuplet(neighborhoodConstraintLevel);
                constrainedValuesOfEliminationBySurroundingCells =
                        Collections.unmodifiableSortedSet(constrainedValuesOfElimination);
            }
            return constrainedValuesOfEliminationBySurroundingCells;
        }

        private SortedSet<Integer> computeConstrainedValuesOfEliminationByNeighboringNuplet(int neighborhoodConstraintLevel) {
            int nupletSize = neighborhoodConstraintLevel;
            SortedSet<Integer> constrainedValuesOfElimination = newTreeSet();
            for (Region region : neighborhood()) {
                List<SortedSet<Integer>> nupletsInRegion = Lists.newArrayList();
                for (Cell neighborCell : region.cells()) {
                    if (neighborCell.equals(this) || neighborCell.value().isPresent()) {
                        continue;
                    }
                    SortedSet<Integer> neighboringCellsRemainingCandidateValues =
                        neighborCell.getRemainingCandidateValuesUniquePositionValueExtractedIfAny(neighborhoodConstraintLevel - 1);
                    if (neighboringCellsRemainingCandidateValues.size() == nupletSize) {
                        nupletsInRegion.add(neighboringCellsRemainingCandidateValues);
                    }
                }
                Map<SortedSet<Integer>, Integer> nupletsCount = Maps.newHashMap();
                for (SortedSet<Integer> nuplet : nupletsInRegion) {
                    if (!nupletsCount.containsKey(nuplet)) {
                        nupletsCount.put(nuplet, 0);
                    }
                    int count = nupletsCount.get(nuplet) + 1;
                    nupletsCount.put(nuplet, count);
                }
                for (SortedSet<Integer> nuplet : nupletsCount.keySet()) {
                    if (nupletsCount.get(nuplet) == nupletSize) {
                        constrainedValuesOfElimination.addAll(nuplet);
                    }
                }
            }
            return constrainedValuesOfElimination;
        }

        private Collection<Region> neighborhood() {
            return ImmutableList.of(board.row(row), board.column(column), box());
        }

        public SortedSet<Integer> getRemainingCandidateValuesUniquePositionValueExtractedIfAny() {
            return getRemainingCandidateValuesUniquePositionValueExtractedIfAny(0);
        }

        public SortedSet<Integer> getRemainingCandidateValuesUniquePositionValueExtractedIfAny(int constraintLevel) {
            if (constraintLevel == -1) {
                return board.allPossibleValues();
            }
            SortedSet<Integer> cachedResult = board.remainingCandidateValuesUniquenessExtracted.get(constraintLevel).get(this);
            if (cachedResult != null) {
                return cachedResult;
            }

            SortedSet<Integer> remainingCandidateValues =
                    getRemainingCandidateValuesAfterEliminationFromNeighboringCells(constraintLevel);
            if (remainingCandidateValues.size() == 1) {
                board.remainingCandidateValuesUniquenessExtracted.get(constraintLevel).put(this, remainingCandidateValues);
                return remainingCandidateValues;
            }
            Optional<Integer> onlyPossibleValue = Optional.empty();
            possibleValuesLoop:
            for (Integer refPossibleValueTDNPD : remainingCandidateValues) {
                for (Region region : neighborhood()) {
                    if (region.missingValues().contains(refPossibleValueTDNPD)) {
                        boolean otherPossibleValueInRegion = false;
                        for (Cell neighborCell : region.cells()) {
                            if (neighborCell.equals(this) || neighborCell.value().isPresent()) {
                                continue;
                            }
                            SortedSet<Integer> remainingCandidateValuesOfNeighborCell =
                                    neighborCell.getRemainingCandidateValuesAfterEliminationFromNeighboringCells(constraintLevel);
                            if (remainingCandidateValuesOfNeighborCell.contains(refPossibleValueTDNPD)) {
                                otherPossibleValueInRegion = true;
                                break;
                            }
                        }
                        if (!otherPossibleValueInRegion) {
                            onlyPossibleValue = Optional.of(refPossibleValueTDNPD);
                            break possibleValuesLoop;
                        }
                    }
                }
            }
            final SortedSet<Integer> result;
            if (onlyPossibleValue.isPresent()) {
                result = ImmutableSortedSet.of(onlyPossibleValue.get());
            } else {
                result = remainingCandidateValues;
            }
            board.remainingCandidateValuesUniquenessExtracted.get(constraintLevel).put(this, result);
            return result;
        }

        public SudokuBoard.Box box() {
            return board.boxes[((row - 1) / board.size)][((column - 1) / board.size)];
        }

        public int rowIndex() {
            return row;
        }

        public int columnIndex() {
            return column;
        }

        public int rowIndexInBox() {
            return ((row - 1) % board.size) + 1;
        }

        public int columnIndexInBox() {
            return ((column - 1) % board.size) + 1;
        }

        public int boxRowIndex() {
            return ((row - 1) / board.size) + 1;
        }

        public int boxColumnIndex() {
            return ((column - 1) / board.size) + 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cell cell = (Cell) o;
            return row == cell.row && column == cell.column && Objects.equals(board, cell.board);
        }

        @Override
        public int hashCode() {
            return Objects.hash(board, row, column);
        }
    }

    public interface Region {
        SudokuBoard board();
        Collection<Cell> cells();

        default SortedSet<Integer> usedValues() {
            return cells().stream()
                    .map(Cell::value)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toImmutableSortedSet(Integer::compareTo));
        }

        default SortedSet<Integer> missingValues() {
            SortedSet<Integer> values = newTreeSet(board().allPossibleValues());
            values.removeAll(usedValues());
            return unmodifiableSortedSet(values);
        }

        default Set<Cell> getDuplicateValuesErrors() {
            Map<Integer, Set<Cell>> errMap = Maps.newHashMap();
            for (Cell cell : cells()) {
                Optional<Integer> value = cell.value();
                if (value.isPresent()) {
                    final Set<Cell> cells;
                    if (!errMap.containsKey(value.get())) {
                        cells = Sets.newHashSet();
                        errMap.put(value.get(), cells);
                    } else {
                        cells = errMap.get(value.get());
                    }
                    cells.add(cell);
                }
            }
            Set<Cell> erroneousCells = Sets.newHashSet();
            for (Set<Cell> cells : errMap.values()) {
                if (cells.size() > 1) {
                    erroneousCells.addAll(cells);
                }
            }
            return Collections.unmodifiableSet(erroneousCells);
        }

        default SortedSet<Integer> getImpossibleToFillValueErrors() {
            SortedSet<Integer> impossibleToFillValues = newTreeSet();
            SortedSet<Integer> missingValues = missingValues();
            for (int missingValue : missingValues) {
                boolean possibleLocationFound = false;
                for (Cell cell : cells()) {
                    if (cell.value().isEmpty()) {
                        if (cell
                            .getRemainingCandidateValuesAfterEliminationFromNeighboringCells(USED_CONSTRAINT_LEVEL)
                            .contains(missingValue)) {
                            possibleLocationFound = true;
                            break;
                        }
                    }
                }
                if (!possibleLocationFound) {
                    impossibleToFillValues.add(missingValue);
                }
            }
            return unmodifiableSortedSet(impossibleToFillValues);
        }
    }

    public static class Box implements Region {
        private final SudokuBoard board;
        private final int boxRow;
        private final int boxColumn;

        private Box(SudokuBoard board, int boxRow, int boxColumn) {
            this.board = board;
            this.boxRow = boxRow;
            this.boxColumn = boxColumn;
        }

        public int boxRowIndex() {
            return boxRow;
        }

        public int boxColumnIndex() {
            return boxColumn;
        }

        @Override
        public SudokuBoard board() {
            return board;
        }

        @Override
        public Collection<Cell> cells() {
            ImmutableList.Builder<Cell> boxBuilder = ImmutableList.builder();
            for (int iRow = 1 ; iRow <= board.size ; ++iRow) {
                for (int iCol = 1 ; iCol <= board.size ; ++iCol) {
                    boxBuilder.add(board.cells[(boxRow-1) * board.size + iRow - 1]
                                              [(boxColumn-1) * board.size + iCol - 1]);
                }
            }
            return boxBuilder.build();
        }
    }

    public class Row implements Region {
        private final SudokuBoard board;
        private final int row;

        private Row(SudokuBoard board, int row) {
            this.board = board;
            this.row = row;
        }

        public int index() {
            return row;
        }

        @Override
        public SudokuBoard board() {
            return board;
        }

        @Override
        public Collection<Cell> cells() {
            return Collections.unmodifiableList(Arrays.asList(board.cells[row-1]));
        }
    }

    public class Column implements Region {
        private final SudokuBoard board;
        private final int column;

        private Column(SudokuBoard board, int column) {
            this.board = board;
            this.column = column;
        }

        public int index() {
            return column;
        }

        @Override
        public SudokuBoard board() {
            return board;
        }

        @Override
        public Collection<Cell> cells() {
            ImmutableList.Builder<Cell> columnBuilder = ImmutableList.builder();
            for (int iRow = 1 ; iRow <= board.regionSize ; ++iRow) {
                columnBuilder.add(board.cells[iRow - 1][column - 1]);
            }
            return columnBuilder.build();
        }
    }
}
