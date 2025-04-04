import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.util.List;


class KomiGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(KomiGame::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Komi Game");
        frame.setLayout(new BorderLayout());
        KomiBoard board = new KomiBoard(9, frame);
        frame.add(board, BorderLayout.CENTER);
        frame.setSize(600, 675);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}

@SuppressWarnings("serial")
class KomiBoard extends JPanel {
    private final Cell[][] board;
    private boolean whiteToMove;
    private final Set<Point> restrictedSpots = Collections.synchronizedSet(new HashSet<>());
    private int blackCaptures = 0, whiteCaptures = 0;
    private final JLabel scoreBoard;
    private final int size;
    private final JFrame parentFrame;

    KomiBoard(int size, JFrame frame) {
        this.size = size;
        this.parentFrame = frame;
        board = new Cell[size][size];
        whiteToMove = true;
        setLayout(new GridLayout(size, size));
        scoreBoard = new JLabel("Black: 0 | White: 0", SwingConstants.CENTER);
        scoreBoard.setFont(new Font("Arial", Font.BOLD, 16));
        initBoard();
        addTopPanel();
    }

    private void initBoard() {
        removeAll();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                board[row][col] = new Cell(row, col);
                add(board[row][col]);
            }
        }
        revalidate();
        repaint();
    }

    private void addTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetBoard());
        topPanel.add(scoreBoard, BorderLayout.CENTER);
        topPanel.add(resetButton, BorderLayout.EAST);
        parentFrame.add(topPanel, BorderLayout.NORTH);
    }

    private void resetBoard() {
        blackCaptures = 0;
        whiteCaptures = 0;
        whiteToMove = true;
        restrictedSpots.clear();
        initBoard();
        updateScoreBoard();
    }

    private void updateScoreBoard() {
        SwingUtilities.invokeLater(() ->
                scoreBoard.setText("Black: " + blackCaptures + " | White: " + whiteCaptures)
        );
    }

    private class Cell extends JPanel {
        private Stone stone;
        private final int row, col;

        Cell(int r, int c) {
            stone = Stone.NONE;
            row = r;
            col = c;
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (stone != Stone.NONE || restrictedSpots.contains(new Point(row, col))) return;
                    new Thread(() -> placeStone()).start();
                }
            });
        }

        private synchronized void placeStone() {
            stone = whiteToMove ? Stone.WHITE : Stone.BLACK;
            repaint();
            checkCapture();
            whiteToMove = !whiteToMove;
            checkWinCondition();
        }

        private void checkCapture() {
            List<Cell> capturedStones = new ArrayList<>();
            Set<Cell> visited = new HashSet<>();

            for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (isInsideBoard(newRow, newCol)) {
                    Cell adjacent = board[newRow][newCol];
                    if (adjacent.stone != Stone.NONE && adjacent.stone != stone) {
                        Set<Cell> group = new HashSet<>();
                        if (isSurrounded(adjacent, group, visited)) {
                            capturedStones.addAll(group);
                        }
                    }
                }
            }

            if (!capturedStones.isEmpty()) {
                for (Cell captured : capturedStones) {
                    captured.stone = Stone.NONE;
                    restrictedSpots.add(new Point(captured.row, captured.col));
                    captured.repaint();
                }
                if (!whiteToMove) {
                    blackCaptures += capturedStones.size();
                } else {
                    whiteCaptures += capturedStones.size();
                }
                updateScoreBoard();
            }
        }

        private boolean isSurrounded(Cell cell, Set<Cell> group, Set<Cell> visited) {
            if (visited.contains(cell)) return true;
            visited.add(cell);
            group.add(cell);

            for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int newRow = cell.row + dir[0];
                int newCol = cell.col + dir[1];
                if (isInsideBoard(newRow, newCol)) {
                    Cell adjacent = board[newRow][newCol];
                    if (adjacent.stone == Stone.NONE) return false;
                    if (adjacent.stone == cell.stone && !group.contains(adjacent)) {
                        if (!isSurrounded(adjacent, group, visited)) return false;
                    }
                }
            }
            return true;
        }

        private boolean isInsideBoard(int r, int c) {
            return r >= 0 && r < size && c >= 0 && c < size;
        }

        private void checkWinCondition() {
            SwingUtilities.invokeLater(() -> {
                if (blackCaptures >= 10 || whiteCaptures >= 10) {
                    String winner = (blackCaptures >= 10) ? "Black" : "White";
                    int option = JOptionPane.showConfirmDialog(null, winner + " wins! Play again?", "Game Over", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        resetBoard();
                    } else {
                        System.exit(0);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0xB78600));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.BLACK);
            g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            g.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
            stone.paint(g, getWidth());
        }
    }
}

enum Stone {
    BLACK(Color.BLACK), WHITE(Color.WHITE), NONE(null);
    final Color color;
    Stone(Color c) { color = c; }
    void paint(Graphics g, int size) {
        if (this == NONE) return;
        g.setColor(color);
        g.fillOval(5, 5, size - 10, size - 10);
    }
}
