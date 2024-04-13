package com.github.jldelarbre;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Supplier;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.function.Predicate.not;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.generate;

public class SudokuBoard {

    private final int size;
    private final int regionSize;
    private final int maxValue;
    private final List<Optional<Integer>> values;

    private SudokuBoard(int size, int regionSize, int maxValue, List<Optional<Integer>> values) {
        this.size = size;
        this.regionSize = regionSize;
        this.maxValue = maxValue;
        this.values = values;
    }

    public static SudokuBoard create(int size) {
        int squaredSize = size * size;
        List<Optional<Integer>> empties = generate((Supplier<Optional<Integer>>) Optional::empty)
                .limit((long) squaredSize * squaredSize)
                .collect(toImmutableList());
        return new SudokuBoard(size, squaredSize, squaredSize, empties);
    }

    public int getSize() {
        return size;
    }

    public int getRegionSize() {
        return regionSize;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public Cell getCell(int row, int column) {
        checkIndexes(row, column);
        return new Cell(this, row, column);
    }

    public SudokuBoard set(int value, int row, int column) {
        checkIndexes(row, column);
        checkValue(value);
        int index = index(row, column);
        List<Optional<Integer>> temp = newArrayList(values);
        temp.set(index, Optional.of(value));
        List<Optional<Integer>> updatedList = ImmutableList.copyOf(temp);
        return new SudokuBoard(size, regionSize, maxValue, updatedList);
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

    private int index(int row, int column) {
        return (row - 1) * regionSize + (column - 1);
    }

    private ImmutableSortedSet<Integer> allPossibleValues() {
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

        public Optional<Integer> getValue() {
            int index = board.index(row, column);
            return board.values.get(index);
        }

        public SortedSet<Integer> getPossibleValues() {
            Optional<Integer> val = board.values.get(board.index(row, column));
            if (val.isPresent()) {
                return ImmutableSortedSet.of();
            }

            SortedSet<Integer> valuesOnCommonsRegions = ImmutableSortedSet.<Integer>naturalOrder()
                    .addAll(usedValuesOnRow())
                    .addAll(usedValuesOnColumn())
                    .addAll(usedValuesInBox())
                    .build();
            return board.allPossibleValues().stream()
                    .filter(not(valuesOnCommonsRegions::contains))
                    .collect(toImmutableSortedSet(Integer::compareTo));
        }

        public SudokuBoard.Box getBox() {
            return new Box(board,
                           ((row - 1) / board.size) + 1,
                           ((column - 1) / board.size) + 1);
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        public int getRowInBox() {
            return ((row - 1) % board.size) + 1;
        }

        public int getColumnInBox() {
            return ((column - 1) % board.size) + 1;
        }

        private SortedSet<Integer> usedValuesOnRow() {
            List<Optional<Integer>> rowCells = board.values.subList(board.index(row, 1),
                                                                    board.index(row, board.regionSize) + 1);
            return rowCells.stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toImmutableSortedSet(Integer::compareTo));
        }

        private SortedSet<Integer> usedValuesOnColumn() {
            List<Optional<Integer>> columnCells = newArrayList();
            for (int i = 1 ; i <= board.regionSize ; ++i) {
                columnCells.add(board.values.get(board.index(i, column)));
            }
            return columnCells.stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toImmutableSortedSet(Integer::compareTo));
        }

        private SortedSet<Integer> usedValuesInBox() {
            List<Optional<Integer>> boxCells = newArrayList();
            Box box = getBox();
            for (int i = 1 ; i <= board.size ; ++i) {
                int boardRow = (box.getRow() - 1) * board.size + i;
                for (int j = 1 ; j <= board.size ; ++j) {
                    int boardColumn = (box.getColumn() - 1) * board.size + j;
                    boxCells.add(board.values.get(board.index(boardRow, boardColumn)));
                }
            }
            return boxCells.stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toImmutableSortedSet(Integer::compareTo));
        }
    }

    public static class Box {
        private final SudokuBoard board;
        private final int row;
        private final int column;

        private Box(SudokuBoard board, int row, int column) {
            this.board = board;
            this.row = row;
            this.column = column;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }
    }
}
