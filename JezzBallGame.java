import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class JezzBallGame {
    public static void main(String[] args) {
        // Set up the main JFrame
        JFrame frame = new JFrame("JezzBall");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);  // Set window size
        frame.setResizable(false);

        // Create the game panel and add it to the window
        GamePanel gamePanel = new GamePanel();
        frame.add(gamePanel);
        frame.setVisible(true);
    }
}

class GamePanel extends JPanel {
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    private static final int BALL_RADIUS = 10;
    private static final int BALL_SPEED = 2;
    private static final int SECTION_SIZE = 100;  // Size of the sections that will be filled

    private List<Ball> balls;
    private List<Wall> walls;
    private boolean drawingLine = false;
    private int startX, startY, currentX, currentY;
    private boolean isHorizontalLine = true;  // Flag to switch between horizontal and vertical lines
    private Color[][] sectionColors;  // Array to store the color of each section
    private boolean[][] visited;  // Track which sections can be reached

    private Line redLine;  // Horizontal line (red)
    private Line blueLine; // Vertical line (blue)

    public GamePanel() {
        balls = new ArrayList<>();
        walls = new ArrayList<>();
        sectionColors = new Color[GAME_WIDTH / SECTION_SIZE][GAME_HEIGHT / SECTION_SIZE];
        visited = new boolean[GAME_WIDTH / SECTION_SIZE][GAME_HEIGHT / SECTION_SIZE];
        setBackground(Color.BLACK);

        // Add initial balls
        balls.add(new Ball(100, 100, BALL_SPEED, BALL_SPEED));
        balls.add(new Ball(200, 150, -BALL_SPEED, BALL_SPEED));

        // Initialize section colors
        for (int i = 0; i < sectionColors.length; i++) {
            for (int j = 0; j < sectionColors[i].length; j++) {
                sectionColors[i][j] = Color.BLACK;  // Start with sections being empty (black)
            }
        }

        // Mouse listener for wall drawing
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    drawingLine = true;
                    startX = e.getX();
                    startY = e.getY();
                    currentX = startX;
                    currentY = startY;
                    // Start drawing the lines (red and blue)
                    redLine = new Line(startX, startY, Color.RED, true);  // Red line (horizontal)
                    blueLine = new Line(startX, startY, Color.BLUE, false);  // Blue line (vertical)
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (drawingLine && SwingUtilities.isLeftMouseButton(e)) {
                    drawingLine = false;
                    // Stop drawing the lines when mouse is released
                }
            }
        });

        // Mouse motion listener for drawing line preview
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (drawingLine) {
                    currentX = e.getX();
                    currentY = e.getY();
                    repaint();  // Redraw the preview as the mouse moves
                }
            }
        });

        // Timer for game loop
        Timer timer = new Timer(10, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGame();
            }
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the sections
        for (int i = 0; i < sectionColors.length; i++) {
            for (int j = 0; j < sectionColors[i].length; j++) {
                g.setColor(sectionColors[i][j]);
                g.fillRect(i * SECTION_SIZE, j * SECTION_SIZE, SECTION_SIZE, SECTION_SIZE);
            }
        }

        // Draw the walls
        g.setColor(Color.GREEN);
        for (Wall wall : walls) {
            g.fillRect(wall.x, wall.y, wall.width, wall.height);
        }

        // Draw the balls
        g.setColor(Color.RED);
        for (Ball ball : balls) {
            g.fillOval(ball.x - BALL_RADIUS, ball.y - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2);
        }

        // Draw the red line (horizontal)
        if (redLine != null) {
            g.setColor(redLine.color);
            g.drawLine(redLine.x1, redLine.y1, redLine.x2, redLine.y2);
        }

        // Draw the blue line (vertical)
        if (blueLine != null) {
            g.setColor(blueLine.color);
            g.drawLine(blueLine.x1, blueLine.y1, blueLine.x2, blueLine.y2);
        }
    }

    private void updateGame() {
        // Move the balls
        for (Ball ball : balls) {
            ball.x += ball.dx;
            ball.y += ball.dy;

            // Check if the ball collides with any line
            if (redLine != null && ball.collidesWith(redLine)) {
                redLine = null;  // Red line disappears if a ball hits it
            }
            if (blueLine != null && ball.collidesWith(blueLine)) {
                blueLine = null;  // Blue line disappears if a ball hits it
            }

            // Check for collisions with walls
            for (Wall wall : walls) {
                if (ball.collidesWith(wall)) {
                    ball.dx = -ball.dx;
                    ball.dy = -ball.dy;
                }
            }

            // Check if ball hits the window boundaries
            if (ball.x - BALL_RADIUS <= 0 || ball.x + BALL_RADIUS >= GAME_WIDTH) {
                ball.dx = -ball.dx;
            }
            if (ball.y - BALL_RADIUS <= 0 || ball.y + BALL_RADIUS >= GAME_HEIGHT) {
                ball.dy = -ball.dy;
            }
        }

        // Extend the lines until they hit a wall, ball, or edge
        if (redLine != null) {
            extendLine(redLine, true);  // Red line extends horizontally
        }
        if (blueLine != null) {
            extendLine(blueLine, false);  // Blue line extends vertically
        }

        // Redraw the game panel
        repaint();
    }

    private void extendLine(Line line, boolean isHorizontal) {
        if (isHorizontal) {
            // Extend the red horizontal line to the right
            while (line.x2 < GAME_WIDTH && !checkBallHitLine(line)) {
                line.x2 += 5;  // Extend by 5 pixels at a time
            }
        } else {
            // Extend the blue vertical line downward
            while (line.y2 < GAME_HEIGHT && !checkBallHitLine(line)) {
                line.y2 += 5;  // Extend by 5 pixels at a time
            }
        }
    }

    private boolean checkBallHitLine(Line line) {
        // Check if any ball intersects with the line while it is extending
        for (Ball ball : balls) {
            if (line.isIntersecting(ball)) {
                return true;
            }
        }
        return false;
    }

    // Ball class
    class Ball {
        int x, y, dx, dy;

        Ball(int x, int y, int dx, int dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }

        boolean collidesWith(Wall wall) {
            Rectangle ballBounds = new Rectangle(x - BALL_RADIUS, y - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2);
            Rectangle wallBounds = new Rectangle(wall.x, wall.y, wall.width, wall.height);
            return ballBounds.intersects(wallBounds);
        }

        boolean collidesWith(Line line) {
            Rectangle ballBounds = new Rectangle(x - BALL_RADIUS, y - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2);
            return line.getBounds().intersects(ballBounds);
        }
    }

    // Line class
    class Line {
        int x1, y1, x2, y2;
        Color color;
        boolean isHorizontal;

        Line(int x1, int y1, Color color, boolean isHorizontal) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x1;
            this.y2 = y1;
            this.color = color;
            this.isHorizontal = isHorizontal;
        }

        boolean isIntersecting(Ball ball) {
            // Check if ball intersects with the line (simplified for horizontal or vertical)
            if (isHorizontal) {
                return ball.y >= y1 && ball.y <= y2 && ball.x > x1 && ball.x < x2;
            } else {
                return ball.x >= x1 && ball.x <= x2 && ball.y > y1 && ball.y < y2;
            }
        }

        Rectangle getBounds() {
            return new Rectangle(x1, y1, x2 - x1, y2 - y1);
        }
    }

    // Wall class
    class Wall {
        int x, y, width, height;

        Wall(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
