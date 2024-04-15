package com.github.jldelarbre;

public class SudokuMain {
    public static void main(String[] args) {
        SudokuGame sudokuGame = SudokuGame.build();
        SudokuGui.start(sudokuGame, args);
    }
}
