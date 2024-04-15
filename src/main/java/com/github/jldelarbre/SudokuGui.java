package com.github.jldelarbre;

import com.google.common.collect.Lists;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static com.github.jldelarbre.SudokuBoard.USED_CONSTRAINT_LEVEL;
import static java.lang.Math.floor;

public final class SudokuGui extends Application {

    public static final int CELL_SIZE = 55;
    public static final int BOARD_LEFT_MARGIN = 50;
    public static final int BOARD_UPPER_MARGIN = 50;
    public static final int ROW_OFFSET_VAL = 45;
    public static final int COL_OFFSET_VAL = 13;
    public static final int ROW_OFFSET_HINT = 17;
    public static final int COL_OFFSET_HINT = 8;
    public static final int ROW_SPACE_HINT = 16;
    public static final int COL_SPACE_HINT = 15;

    private static SudokuGame sudokuGame;

    private static Font valuesFont = Font.font(48);
//    private static Font startingValuesFont = Font.font(valuesFont.getFamily(), FontWeight.BOLD, 48);
    private static Font hintsFont = Font.font(14);

    private static GraphicsContext gc;
    private TextField valueToSetField;
    private TextField rowToSetField;
    private TextField colToSetField;

    private List<Integer> numPossibilities = Lists.newArrayList();

    public static synchronized void start(SudokuGame sudokuGame, String[] args) {
        SudokuGui.sudokuGame = sudokuGame;
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Scene scene = buildScene();

        primaryStage.setTitle("Sudoku");
        primaryStage.setScene(scene);
        primaryStage.setX(2500);
        primaryStage.setY(300);
        primaryStage.show();
    }

    private Scene buildScene() {
        BorderPane root = new BorderPane();

        drawActionCommands(root);
        drawBoard(root);
        setBoardActions();

        Scene scene = new Scene(root, 900, 674);
        return scene;
    }

    private void drawBoard(BorderPane root) {
        SudokuBoard board = sudokuGame.getBoard();
        Canvas canvas = new Canvas(900, 650);
        gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        drawBoard(sudokuGame.getBoard());
    }

    private void drawBoard(SudokuBoard board) {
        drawEmptyBoard(board.size(), CELL_SIZE * board.regionSize());
        drawBoardValues(board);
        drawMissingValues(board);
        drawDuplicateValueErrors(board);
        drawImpossibleToFillValueErrors(board);
        drawUniquePossibleValuesHighlighting(board);
    }

    private void drawUniquePossibleValuesHighlighting(SudokuBoard board) {
        gc.setStroke(Color.GREEN);
        gc.setFill(Color.GREEN);
        for (int iRow = 1; iRow <= board.regionSize() ; ++iRow) {
            int rowBase = BOARD_UPPER_MARGIN + (iRow-1) * CELL_SIZE;
            int rowHint = BOARD_UPPER_MARGIN + ROW_OFFSET_HINT + (iRow-1) * CELL_SIZE;
            for (int iCol = 1; iCol <= board.regionSize() ; ++iCol) {
                int colBase = BOARD_LEFT_MARGIN + (iCol-1) * CELL_SIZE;
                int colHint = BOARD_LEFT_MARGIN + COL_OFFSET_HINT + (iCol-1) * CELL_SIZE;
                SudokuBoard.Cell cell = board.cell(iRow, iCol);
                SortedSet<Integer> possibleValuesNotTakingPlaceOfOnePossiblePositionValue =
                        cell.getRemainingCandidateValuesUniquePositionValueExtractedIfAny();
                SortedSet<Integer> candidateValues =
                    cell.getRemainingCandidateValuesUniquePositionValueExtractedIfAny(USED_CONSTRAINT_LEVEL);
                if (possibleValuesNotTakingPlaceOfOnePossiblePositionValue.size() == 1) {
                    gc.strokeRect(colBase, rowBase, CELL_SIZE, CELL_SIZE);

                    Integer possibleValue = possibleValuesNotTakingPlaceOfOnePossiblePositionValue.iterator().next();
                    int subRowInd = (possibleValue - 1) / board.size();
                    int subColInd = (possibleValue - 1) % board.size();
                    gc.fillText(possibleValue.toString(), colHint + subColInd * COL_SPACE_HINT, rowHint + subRowInd * ROW_SPACE_HINT);
                }
                if (possibleValuesNotTakingPlaceOfOnePossiblePositionValue.size() != candidateValues.size()) {
                    for (Integer nextToPairValue : candidateValues) {
                        int subRowInd = (nextToPairValue - 1) / board.size();
                        int subColInd = (nextToPairValue - 1) % board.size();
                        gc.setFill(Color.PURPLE);
                        gc.fillText(nextToPairValue.toString(), colHint + subColInd * COL_SPACE_HINT, rowHint + subRowInd * ROW_SPACE_HINT);
                        gc.setFill(Color.GREEN);
                    }
                }
            }
        }
        gc.setStroke(Color.BLACK);
        gc.setFill(Color.BLACK);
    }

    private void drawImpossibleToFillValueErrors(SudokuBoard board) {
        gc.setFill(Color.RED);
        gc.setStroke(Color.RED);
        drawImpossibleToFillValueInRowErrors(board);
        drawImpossibleToFillValueInColumnErrors(board);
        drawImpossibleToFillValueInBoxErrors(board);
        drawImpossibleToFillCellErrors(board);
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.BLACK);
    }

    private void drawImpossibleToFillCellErrors(SudokuBoard board) {
        Set<SudokuBoard.Cell> unfillableErroneousCells = board.unfillableErroneousCells();
        for (SudokuBoard.Cell unfillableErroneousCell : unfillableErroneousCells) {
            int row = unfillableErroneousCell.rowIndex();
            int column = unfillableErroneousCell.columnIndex();
            int rowBase = BOARD_UPPER_MARGIN + (row-1) * CELL_SIZE;
            int colBase = BOARD_LEFT_MARGIN + (column-1) * CELL_SIZE;
            gc.strokeRect(colBase, rowBase, CELL_SIZE, CELL_SIZE);
        }
    }

    private void drawImpossibleToFillValueInBoxErrors(SudokuBoard board) {
        int hintLeftMargin = BOARD_LEFT_MARGIN + CELL_SIZE * board.regionSize() + 2*CELL_SIZE;
        for (int iBoxRow = 1; iBoxRow <= board.size() ; ++iBoxRow) {
            int rowHint = BOARD_UPPER_MARGIN + ROW_OFFSET_HINT + (iBoxRow-1) * CELL_SIZE;
            for (int iBoxCol = 1; iBoxCol <= board.size() ; ++iBoxCol) {
                int colHint = hintLeftMargin + COL_OFFSET_HINT + (iBoxCol-1) * CELL_SIZE;
                SortedSet<Integer> impossibleToFillValueErrors = board.box(iBoxRow, iBoxCol).getImpossibleToFillValueErrors();
                for (Integer impossibleValueToFill : impossibleToFillValueErrors) {
                    int subRowInd = (impossibleValueToFill - 1) / board.size();
                    int subColInd = (impossibleValueToFill - 1) % board.size();
                    gc.fillText(impossibleValueToFill.toString(), colHint + subColInd * COL_SPACE_HINT, rowHint + subRowInd * ROW_SPACE_HINT);
                    gc.strokeRect(colHint - COL_OFFSET_HINT, rowHint - ROW_OFFSET_HINT, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    private void drawImpossibleToFillValueInColumnErrors(SudokuBoard board) {
        int rowHint = BOARD_UPPER_MARGIN + ROW_OFFSET_HINT + board.regionSize() * CELL_SIZE;
        for (int iCol = 1; iCol <= board.regionSize() ; ++iCol) {
            int colHint = BOARD_LEFT_MARGIN + COL_OFFSET_HINT + (iCol-1) * CELL_SIZE;
            SortedSet<Integer> impossibleToFillValueErrors = board.column(iCol).getImpossibleToFillValueErrors();
            for (Integer impossibleValueToFill : impossibleToFillValueErrors) {
                int subRowInd = (impossibleValueToFill - 1) / board.size();
                int subColInd = (impossibleValueToFill - 1) % board.size();
                gc.fillText(impossibleValueToFill.toString(), colHint + subColInd * COL_SPACE_HINT, rowHint + subRowInd * ROW_SPACE_HINT);
                gc.strokeRect(colHint - COL_OFFSET_HINT, rowHint - ROW_OFFSET_HINT, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    private void drawImpossibleToFillValueInRowErrors(SudokuBoard board) {
        int colHint = BOARD_LEFT_MARGIN + COL_OFFSET_HINT + board.regionSize() * CELL_SIZE;
        for (int iRow = 1; iRow <= board.regionSize() ; ++iRow) {
            int rowHint = BOARD_UPPER_MARGIN + ROW_OFFSET_HINT + (iRow-1) * CELL_SIZE;
            SortedSet<Integer> impossibleToFillValueErrors = board.row(iRow).getImpossibleToFillValueErrors();
            for (Integer impossibleValueToFill : impossibleToFillValueErrors) {
                int subRowInd = (impossibleValueToFill - 1) / board.size();
                int subColInd = (impossibleValueToFill - 1) % board.size();
                gc.fillText(impossibleValueToFill.toString(), colHint + subColInd * COL_SPACE_HINT, rowHint + subRowInd * ROW_SPACE_HINT);
                gc.strokeRect(colHint - COL_OFFSET_HINT, rowHint - ROW_OFFSET_HINT, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    private void drawDuplicateValueErrors(SudokuBoard board) {
        gc.setStroke(Color.RED);
        drawDuplicateValueInRowErrors(board);
        drawDuplicateValueInColumnErrors(board);
        drawDuplicateValueInBoxErrors(board);
        gc.setStroke(Color.BLACK);
    }

    private void drawDuplicateValueInBoxErrors(SudokuBoard board) {
        for (int iBoxRow = 1; iBoxRow <= board.size() ; ++iBoxRow) {
            for (int iBoxCol = 1; iBoxCol <= board.size() ; ++iBoxCol) {
                SudokuBoard.Box box = board.box(iBoxRow, iBoxCol);
                Set<SudokuBoard.Cell> erroneousCells = box.getDuplicateValuesErrors();
                for (SudokuBoard.Cell erroneousCell : erroneousCells) {
                    int iRow = erroneousCell.rowIndex();
                    int iCol = erroneousCell.columnIndex();
                    int rowBase = BOARD_UPPER_MARGIN + (iRow-1) * CELL_SIZE;
                    int colBase = BOARD_LEFT_MARGIN + (iCol-1) * CELL_SIZE;
                    gc.strokeLine(colBase, rowBase, colBase + CELL_SIZE, rowBase + CELL_SIZE);
                    gc.strokeLine(colBase, rowBase + CELL_SIZE, colBase + CELL_SIZE, rowBase);
                }
            }
        }
    }

    private void drawDuplicateValueInColumnErrors(SudokuBoard board) {
        for (int iCol = 1; iCol <= board.regionSize() ; ++iCol) {
            SudokuBoard.Column col = board.column(iCol);
            Set<SudokuBoard.Cell> erroneousCells = col.getDuplicateValuesErrors();
            int colBase = BOARD_LEFT_MARGIN + (iCol-1) * CELL_SIZE;
            for (SudokuBoard.Cell erroneousCell : erroneousCells) {
                int iRow = erroneousCell.rowIndex();
                int rowBase = BOARD_UPPER_MARGIN + (iRow-1) * CELL_SIZE;
                gc.strokeLine(colBase, rowBase, colBase + CELL_SIZE, rowBase + CELL_SIZE);
                gc.strokeLine(colBase, rowBase + CELL_SIZE, colBase + CELL_SIZE, rowBase);
            }
        }
    }

    private void drawDuplicateValueInRowErrors(SudokuBoard board) {
        for (int iRow = 1; iRow <= board.regionSize() ; ++iRow) {
            SudokuBoard.Row row = board.row(iRow);
            Set<SudokuBoard.Cell> erroneousCells = row.getDuplicateValuesErrors();
            int rowBase = BOARD_UPPER_MARGIN + (iRow-1) * CELL_SIZE;
            for (SudokuBoard.Cell erroneousCell : erroneousCells) {
                int iCol = erroneousCell.columnIndex();
                int colBase = BOARD_LEFT_MARGIN + (iCol-1) * CELL_SIZE;
                gc.strokeLine(colBase, rowBase, colBase + CELL_SIZE, rowBase + CELL_SIZE);
                gc.strokeLine(colBase, rowBase + CELL_SIZE, colBase + CELL_SIZE, rowBase);
            }
        }
    }

    private void drawMissingValues(SudokuBoard board) {
        gc.setFont(hintsFont);
        gc.setFill(Color.GRAY);
        drawRowMissingValues(board);
        drawColumnMissingValues(board);
        drawBoxMissingValues(board);
    }

    private void drawBoxMissingValues(SudokuBoard board) {
        drawEmptyBoxHints(board.size(), CELL_SIZE * board.regionSize());
        int hintLeftMargin = BOARD_LEFT_MARGIN + CELL_SIZE * board.regionSize() + 2*CELL_SIZE;
        for (int iBoxRow = 1; iBoxRow <= board.size() ; ++iBoxRow) {
            int rowHint = BOARD_UPPER_MARGIN + ROW_OFFSET_HINT + (iBoxRow-1) * CELL_SIZE;
            for (int iBoxCol = 1; iBoxCol <= board.size() ; ++iBoxCol) {
                int colHint = hintLeftMargin + COL_OFFSET_HINT + (iBoxCol-1) * CELL_SIZE;
                SudokuBoard.Box box = board.box(iBoxRow, iBoxCol);
                SortedSet<Integer> missingValues = box.missingValues();
                for (Integer missingValue : missingValues) {
                    int subRowInd = (missingValue - 1) / board.size();
                    int subColInd = (missingValue - 1) % board.size();
                    gc.fillText(missingValue.toString(), colHint + subColInd * COL_SPACE_HINT, rowHint + subRowInd * ROW_SPACE_HINT);
                }
            }
        }
    }

    private void drawEmptyBoxHints(int boardSize, int boardWholeSize) {
        gc.setLineWidth(4);
        int miniBoardSize = boardSize * CELL_SIZE;
        int hintLeftMargin = BOARD_LEFT_MARGIN + boardWholeSize + 2*CELL_SIZE;
        gc.strokeRect(hintLeftMargin, BOARD_UPPER_MARGIN, miniBoardSize, miniBoardSize);
        for (int i = 1; i < boardSize; ++i) {
            int x = hintLeftMargin + i * CELL_SIZE;
            gc.strokeLine(x, BOARD_UPPER_MARGIN, x, BOARD_UPPER_MARGIN + miniBoardSize);
        }
        for (int i = 1; i < boardSize; ++i) {
            int y = BOARD_UPPER_MARGIN + i * CELL_SIZE;
            gc.strokeLine(hintLeftMargin, y, hintLeftMargin + miniBoardSize, y);
        }
    }

    private void drawColumnMissingValues(SudokuBoard board) {
        int rowHint = BOARD_UPPER_MARGIN + ROW_OFFSET_HINT + board.regionSize() * CELL_SIZE;
        for (int iCol = 1; iCol <= board.regionSize() ; ++iCol) {
            SudokuBoard.Column col = board.column(iCol);
            SortedSet<Integer> missingValues = col.missingValues();
            int colHint = BOARD_LEFT_MARGIN + COL_OFFSET_HINT + (iCol-1) * CELL_SIZE;
            for (Integer missingValue : missingValues) {
                int subRowInd = (missingValue - 1) / board.size();
                int subColInd = (missingValue - 1) % board.size();
                gc.fillText(missingValue.toString(), colHint + subColInd * COL_SPACE_HINT, rowHint + subRowInd * ROW_SPACE_HINT);
            }
        }
    }

    private void drawRowMissingValues(SudokuBoard board) {
        int colHint = BOARD_LEFT_MARGIN + COL_OFFSET_HINT + board.regionSize() * CELL_SIZE;
        for (int iRow = 1; iRow <= board.regionSize() ; ++iRow) {
            SudokuBoard.Row row = board.row(iRow);
            SortedSet<Integer> missingValues = row.missingValues();
            int rowHint = BOARD_UPPER_MARGIN + ROW_OFFSET_HINT + (iRow-1) * CELL_SIZE;
            for (Integer missingValue : missingValues) {
                int subRowInd = (missingValue - 1) / board.size();
                int subColInd = (missingValue - 1) % board.size();
                gc.fillText(missingValue.toString(), colHint + subColInd * COL_SPACE_HINT, rowHint + subRowInd * ROW_SPACE_HINT);
            }
        }
    }

    private void drawBoardValues(SudokuBoard board) {
        for (int iRow = 1; iRow <= board.regionSize() ; ++iRow) {
            int rowBase = BOARD_UPPER_MARGIN + (iRow-1) * CELL_SIZE;
            int rowHint = BOARD_UPPER_MARGIN + ROW_OFFSET_HINT + (iRow-1) * CELL_SIZE;
            for (int iCol = 1; iCol <= board.regionSize() ; ++iCol) {
                int colBase = BOARD_LEFT_MARGIN + (iCol-1) * CELL_SIZE;
                int colHint = BOARD_LEFT_MARGIN + COL_OFFSET_HINT + (iCol-1) * CELL_SIZE;
                SudokuBoard.Cell cell = board.cell(iRow, iCol);
                if (cell.value().isPresent()) {
                    gc.setFont(valuesFont);
                    gc.setFill(Color.BLACK);
                    gc.fillText(cell.value().get().toString(), colBase + COL_OFFSET_VAL, rowBase + ROW_OFFSET_VAL);
//                    gc.setFont(startingValuesFont);
                } else {
                    gc.setFont(hintsFont);
                    gc.setFill(Color.GRAY);
                    SortedSet<Integer> possibleValues = cell.getPossibleValuesThatDoNotProduceDuplicate();
                    for (Integer possibleValue : possibleValues) {
                        int subRowInd = (possibleValue - 1) / board.size();
                        int subColInd = (possibleValue - 1) % board.size();
                        gc.fillText(possibleValue.toString(), colHint + subColInd * COL_SPACE_HINT, rowHint + subRowInd * ROW_SPACE_HINT);
                    }
                }
            }
        }
    }

    private void drawEmptyBoard(int boardSize, int boardWholeSize) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        gc.setFill(Color.BLACK);
        gc.setLineWidth(4);
        gc.strokeRect(BOARD_LEFT_MARGIN, BOARD_UPPER_MARGIN, boardWholeSize, boardWholeSize);
        for (int i = 1; i < boardSize; ++i) {
            int x = BOARD_LEFT_MARGIN + i * (CELL_SIZE * boardSize);
            gc.strokeLine(x, BOARD_UPPER_MARGIN, x, BOARD_UPPER_MARGIN + boardWholeSize);
        }
        for (int i = 1; i < boardSize; ++i) {
            int y = BOARD_UPPER_MARGIN + i * (CELL_SIZE * boardSize);
            gc.strokeLine(BOARD_LEFT_MARGIN, y, BOARD_LEFT_MARGIN + boardWholeSize, y);
        }
        gc.setLineWidth(1);
        for (int i = 0; i < boardSize; ++i) {
            int xBase = BOARD_LEFT_MARGIN + i * (CELL_SIZE * boardSize);
            for (int j = 1; j < boardSize; ++j) {
                int x = xBase + j * CELL_SIZE;
                gc.strokeLine(x, BOARD_UPPER_MARGIN, x, BOARD_UPPER_MARGIN + boardWholeSize);
            }
        }
        for (int i = 0; i < boardSize; ++i) {
            int yBase = BOARD_UPPER_MARGIN + i * (CELL_SIZE * boardSize);
            for (int j = 1; j < boardSize; ++j) {
                int y = yBase + j * CELL_SIZE;
                gc.strokeLine(BOARD_LEFT_MARGIN, y, BOARD_LEFT_MARGIN + boardWholeSize, y);
            }
        }
    }

    private void drawActionCommands(BorderPane root) {
        FlowPane flow = new FlowPane();

        valueToSetField = new TextField();
        valueToSetField.setPrefWidth(30);
        rowToSetField = new TextField();
        rowToSetField.setPrefWidth(30);
        colToSetField = new TextField();
        colToSetField.setPrefWidth(30);

        valueToSetField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                setValueAction();
            }
        });

        Button btnSet = new Button();
        btnSet.setText("Set");
        btnSet.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setValueAction();
            }
        });

        Button btnClear = new Button();
        btnClear.setText("Clear");
        btnClear.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                SudokuBoard sudokuBoard = sudokuGame.startNewGame(3);
                numPossibilities.clear();
                drawBoard(sudokuBoard);
            }
        });

        Button btnUndo = new Button();
        btnUndo.setText("undo");
        btnUndo.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                SudokuBoard sudokuBoard = sudokuGame.undo();
                if (!numPossibilities.isEmpty()) {
                    numPossibilities.removeLast();
                }
                drawBoard(sudokuBoard);
                displayPossibilities();
            }
        });

        ObservableList<Node> flowChildren = flow.getChildren();
        flowChildren.add(btnSet);
        flowChildren.add(new Label("Val"));
        flowChildren.add(valueToSetField);
        flowChildren.add(new Label("Row"));
        flowChildren.add(rowToSetField);
        flowChildren.add(new Label("Col"));
        flowChildren.add(colToSetField);
        flowChildren.add(btnClear);
        flowChildren.add(btnUndo);
        root.setBottom(flow);
    }

    private void setValueAction() {
        int rowToSet = Integer.parseInt(rowToSetField.getText());
        int colToSet = Integer.parseInt(colToSetField.getText());

        String valueToSetStr = valueToSetField.getText();
        final SudokuBoard sudokuBoard;
        if (valueToSetStr.isBlank()) {
            sudokuBoard = sudokuGame.clearCell(rowToSet, colToSet);
        } else {
            SudokuBoard board = sudokuGame.getBoard();
            int valueToSet = Integer.parseInt(valueToSetStr);
            if (valueToSet > board.maxValue()) {
                return;
            }
            SudokuBoard.Cell cell = board.cell(rowToSet, colToSet);
            int numPossibleValues =
                cell.getRemainingCandidateValuesUniquePositionValueExtractedIfAny(USED_CONSTRAINT_LEVEL).size();
            numPossibilities.add(numPossibleValues);
            sudokuBoard = sudokuGame.setCell(valueToSet, rowToSet, colToSet);
            displayPossibilities();
        }
        drawBoard(sudokuBoard);
    }

    private void displayPossibilities() {
        System.out.println(numPossibilities);
        BigInteger totalPossibilities = numPossibilities.stream().map(BigInteger::valueOf).reduce(BigInteger.ONE, BigInteger::multiply);
        System.out.println(totalPossibilities);
    }

    private void setBoardActions() {
        gc.getCanvas().setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                double xPos = event.getX();
                double yPos = event.getY();
                int iCol = (int) floor((xPos - BOARD_LEFT_MARGIN) / CELL_SIZE) + 1;
                int iRow = (int) floor((yPos - BOARD_UPPER_MARGIN) / CELL_SIZE) + 1;
                if (iRow >= 1 && iRow <= sudokuGame.getBoard().regionSize()) {
                    if (iCol >= 1 && iCol <= sudokuGame.getBoard().regionSize()) {
                        rowToSetField.setText(Integer.toString(iRow));
                        colToSetField.setText(Integer.toString(iCol));
                        valueToSetField.requestFocus();
                        valueToSetField.clear();
                    }
                }
            }
        });
    }
}
