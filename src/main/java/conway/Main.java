package conway;

import java.io.*;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.*;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import com.jfoenix.controls.*;
import com.jfoenix.validation.IntegerValidator;

import processing.core.*;
import processing.event.MouseEvent;

public class Main extends PApplet {

    private final int WIDTH = 900, HEIGHT = 600;
    private final int CELL_SIZE = 5;
    private final int ROWS = HEIGHT / CELL_SIZE, COLUMNS = WIDTH / CELL_SIZE;

    private SimpleStringProperty numberOfAliveCells = new SimpleStringProperty();
    private SimpleFloatProperty scaleRate = new SimpleFloatProperty(1);
    private Cell[][] cells = new Cell[ROWS][COLUMNS];

    public static void main(String[] args) {
        PApplet.main("conway.Main");
    }

    @Override
    protected PSurface initSurface() {
        PSurface surface = super.initSurface();

        Canvas canvas = (Canvas) surface.getNative();
        canvas.widthProperty().unbind();
        canvas.heightProperty().unbind();

        Stage stage = (Stage) canvas.getScene().getWindow();
        stage.setResizable(false);

        BorderPane root = new BorderPane();

        GridPane gridPane = createGridPane();
        gridPane.getStyleClass().add("grid-pane");

        HBox hbox = new HBox();
        hbox.getStyleClass().add("hbox");

        HBox hbox2 = new HBox();
        hbox2.getStyleClass().add("hbox");

        JFXTextField textField = new JFXTextField();
        textField.getValidators().add(new IntegerValidator());

        JFXButton button = new JFXButton("Revive");
        button.setOnAction(e -> {
            textField.validate();
            String text = textField.getText().trim();
            if (isValidNumber(text)) {
                randomlyReviveCells(Integer.parseInt(text));
            }
        });

        JFXButton button2 = new JFXButton("Reset");
        button2.setOnAction(e -> {
            Arrays.stream(cells)
                .forEach(rowOfCells -> {
                    Arrays.stream(rowOfCells)
                        .forEach(cell -> cell.setAlive(false));
                });
        });
        hbox.getChildren().addAll(textField, button, button2);

        Label label1 = new Label();
        label1.textProperty().bind(numberOfAliveCells);

        Label label2 = new Label("Scale:");

        Label label3 = new Label();
        label3.textProperty().bindBidirectional(scaleRate, new DecimalFormat("###%"));
        hbox2.getChildren().addAll(label2, label3);

        gridPane.addColumn(0, hbox);
        gridPane.addColumn(1, label1);
        gridPane.addColumn(2, hbox2);

        root.setCenter(canvas);
        root.setBottom(gridPane);

        Platform.runLater(() -> {
            Image icon = new Image("icon.png", 32, 32, true, true);

            JFXDecorator decorator = new JFXDecorator(stage, root, false, false, true);
            decorator.setGraphic(new ImageView(icon));

            Scene scene = new Scene(decorator);
            scene.getStylesheets().add("style.css");

            stage.setWidth(WIDTH);
            stage.setScene(scene);
        });

        return surface;
    }

    private GridPane createGridPane() {
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.SOMETIMES);
        col1.setFillWidth(false);
        col1.setHalignment(HPos.LEFT);
        col1.setPercentWidth(35);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.SOMETIMES);
        col2.setFillWidth(false);
        col2.setHalignment(HPos.CENTER);
        col2.setPercentWidth(30);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setHgrow(Priority.SOMETIMES);
        col3.setFillWidth(false);
        col3.setHalignment(HPos.RIGHT);
        col3.setPercentWidth(35);

        GridPane gridPane = new GridPane();
        gridPane.getColumnConstraints().addAll(col1, col2, col3);

        return gridPane;
    }

    private boolean isValidNumber(String text) {
        if (text.matches("^\\d+$")) {
            BigInteger max = new BigInteger(Integer.toString(Integer.MAX_VALUE));
            BigInteger input = new BigInteger(text);
            BigInteger totalCells = new BigInteger(Integer.toString(ROWS * COLUMNS));

            return (max.compareTo(input) >= 0) && (totalCells.compareTo(input) >= 0);
        }
        
        return false;
    }

    @Override
    public void settings() {
        size(WIDTH, HEIGHT, FX2D);
    }

    @Override
    public void setup() {
        surface.setTitle("Conway's Game of Life");
        surface.setIcon(loadImage("icon.png"));

        traverseNestedLoop(ROWS, COLUMNS, (i, j) -> {
            cells[i][j] = new Cell(j, i);
        });
    }

    @Override
    public void draw() {
        background(255);

        if (frameCount % 30 == 0) {
            updateCells();
            updateCellNumber();
        }

        pushMatrix();
        translate(mouseX, mouseY);
        scale(scaleRate.getValue());
        translate(-mouseX, -mouseY);
        drawGrids();
        drawCells();
        popMatrix();
    }

    private void randomlyReviveCells(int numbers) {
        getRandomCells(numbers).forEach(cell -> {
            cell.setAlive(true);
        });
    }

    private List<Cell> getRandomCells(int numbers) {
        Random random = new Random();

        return random.ints(0, ROWS)
            .mapToObj(i -> {
                int j = random.nextInt(COLUMNS);
                return cells[i][j];
            })
            .distinct()
            .limit(numbers)
            .collect(Collectors.toList());
    }

    private void updateCells() {
        getChangeCells().stream()
            .forEach(cell -> {
                cell.setAlive(cell.willAlive());
            });
    }

    private List<Cell> getChangeCells() {
        List<Cell> change = new ArrayList<>();

        traverseNestedLoop(ROWS, COLUMNS, (i, j) -> {
            Cell cell = cells[i][j];
            List<Cell> around = getAroundCells(cell);
            int count = getAliveCellsCount(around);

            if (cell.isAlive()) {
                if (count < 2 || count > 3) {
                    cell.setWillAlive(false);
                    change.add(cell);
                }
            } else {
                if (count == 3) {
                    cell.setWillAlive(true);
                    change.add(cell);
                }
            }
        });

        return change;
    }

    private List<Cell> getAroundCells(Cell cell) {
        List<Cell> rlt = new ArrayList<>(8);
        int row = cell.getY(), col = cell.getX();
        boolean isTop = (row == 0), isLeft = (col == 0),
            isButtom = (row == ROWS - 1), isRight = (col == COLUMNS - 1);

        if (!isTop && !isLeft) {
            rlt.add(cells[row - 1][col - 1]);
        }

        if (!isTop) {
            rlt.add(cells[row - 1][col]);
        }

        if (!isTop && !isRight) {
            rlt.add(cells[row - 1][col + 1]);
        }

        if (!isLeft) {
            rlt.add(cells[row][col - 1]);
        }

        if (!isRight) {
            rlt.add(cells[row][col + 1]);
        }

        if (!isButtom && !isLeft) {
            rlt.add(cells[row + 1][col - 1]);
        }

        if (!isButtom) {
            rlt.add(cells[row + 1][col]);
        }

        if (!isButtom && !isRight) {
            rlt.add(cells[row + 1][col + 1]);
        }

        return rlt;
    }

    private int getAliveCellsCount(List<Cell> around) {
        return (int) around.stream()
            .filter(Cell::isAlive)
            .count();
    }

    private void updateCellNumber() {
        numberOfAliveCells.setValue("Number of Alive Cells: " + getAliveCellNumber());
    }

    private int getAliveCellNumber() {
        return Arrays.stream(cells)
            .mapToInt(rowOfCells -> {
                return (int) Arrays.stream(rowOfCells)
                    .filter(Cell::isAlive)
                    .count();
            })
            .sum();
    }

    private void drawGrids() {
        pushStyle();
        stroke(127);

        for (int i = 0; i <= WIDTH; i += CELL_SIZE) {
            line(i, 0, i, HEIGHT);
        }

        for (int i = 0; i <= HEIGHT; i += CELL_SIZE) {
            line(0, i, WIDTH, i);
        }
        popStyle();
    }

    private void drawCells() {
        pushStyle();
        noStroke();
        fill(0);

        traverseNestedLoop(ROWS, COLUMNS, (i, j) -> {
            if (cells[i][j].isAlive()) {
                rect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        });

        popStyle();
    }

    private void traverseNestedLoop(int outerBound, int innerBound, BiConsumer<Integer, Integer> biConsumer) {
        for (int i = 0; i < outerBound; i++) {
            for (int j = 0; j < innerBound; j++) {
                biConsumer.accept(i, j);
            }
        }
    }

    @Override
    public void mousePressed() {
        reviveCell();
    }

    @Override
    public void mouseDragged() {
        reviveCell();
    }

    private void reviveCell() {
        int row = constrain(mouseY, 0, HEIGHT - 1) / CELL_SIZE;
        int col = constrain(mouseX, 0, WIDTH - 1) / CELL_SIZE;
        cells[row][col].setAlive(true);
    }

    @Override
    public void mouseWheel(MouseEvent e) {
        float scale = scaleRate.getValue() - e.getCount() / 10f;
        scale = constrain(scale, 1, Float.MAX_VALUE);
        scaleRate.setValue(scale);
    }
}
