package com.github.jldelarbre;

import java.util.Stack;

public class SudokuGame {
    public static final int DEFAULT_GAME_SIZE = 3;

    private SudokuBoard sudokuBoard;
    private Stack<SudokuBoard> previousBoards = new Stack<>();

    private SudokuGame(SudokuBoard sudokuBoard) {
        this.sudokuBoard = sudokuBoard;
    }

    public static SudokuGame build() {
        return new SudokuGame(SudokuBoard.create(DEFAULT_GAME_SIZE));
    }

    public SudokuBoard getBoard() {
        return sudokuBoard;
    }

    SudokuBoard startNewGame(int size) {
        sudokuBoard = SudokuBoard.create(size);
        previousBoards.clear();
        return sudokuBoard;
    }

    SudokuBoard setCell(int value, int row, int column) {
        previousBoards.push(sudokuBoard);
        sudokuBoard = sudokuBoard.set(value, row, column);
        return sudokuBoard;
    }

    SudokuBoard clearCell(int row, int column) {
        previousBoards.push(sudokuBoard);
        sudokuBoard = sudokuBoard.clear(row, column);
        return sudokuBoard;
    }

    SudokuBoard undo() {
        if (!previousBoards.empty()) {
            sudokuBoard = previousBoards.pop();
        }
        return sudokuBoard;
    }
}
