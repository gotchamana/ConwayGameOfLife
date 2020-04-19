package conway;

import java.util.Objects;

public class Cell {

  private int x, y;
  private boolean isAlive = false, willAlive = false;

  public Cell(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public boolean isAlive() {
    return isAlive;
  }

  public void setAlive(boolean isAlive) {
    this.isAlive = isAlive;
  }

  public boolean willAlive() {
    return willAlive;
  }

  public void setWillAlive(boolean willAlive) {
    this.willAlive = willAlive;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y, isAlive, willAlive);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Cell) {
      Cell cell = (Cell) o;

      if (this == cell) {
        return true;
      }

      return (x == cell.getX()) && (y == cell.getY()) &&
        (isAlive == cell.isAlive()) && (willAlive == cell.willAlive());
    }

    return false;
  }
}
