package life1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;

public class cell extends JPanel implements
        ActionListener,
        MouseListener,
        MouseMotionListener,
        MouseWheelListener,
        KeyListener {

    // =========================================================
    // PARTICLE
    // =========================================================

    static class Particle {

        double x;
        double y;

        double vx;
        double vy;

        // 0 = Red
        // 1 = Blue
        // 2 = Yellow
        // 3 = Green
        int type;

        Particle(double x, double y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }


    // =========================================================
    // PARTICLES
    // =========================================================

    ArrayList<Particle> cells = new ArrayList<>();

    Random random = new Random();


    // =========================================================
    // CAMERA
    // =========================================================

    double cameraX = 0;
    double cameraY = 0;

    double zoom = 1.0;

    int lastMouseX;
    int lastMouseY;

    boolean rightDragging = false;

    double cameraMoveSpeed = 10.0;


    // =========================================================
    // PHYSICS SETTINGS
    // =========================================================

    double minimumCellDistance = 6.0;

    double collisionRepulsion = 2;

    double minimumInteractionRange = 10.0;

    double maxSpeed = 5.0;


    // =========================================================
    // WALL SETTINGS
    // =========================================================

    // true = particles are inside a wall
    // false = particles can leave the world
    boolean wallsEnabled = true;

    // Current wall size
    // 200, 500, or 1000
    double currentWorldSize = 1000.0;


    // =========================================================
    // PHYSICS BOX
    // =========================================================

    static final double DEFAULT_PHYSICS_BOX_SIZE = 1000.0;


    // These are calculated dynamically from currentWorldSize
    double getPhysicsBoxSize() {
        return currentWorldSize;
    }

    double getPhysicsMin() {
        return -currentWorldSize / 2.0;
    }

    double getPhysicsMax() {
        return currentWorldSize / 2.0;
    }


    // =========================================================
    // ATTRACTION RULES
    // =========================================================

    double[][] attractionRules =
            new double[4][4];

    double[][] attractionRangeRules =
            new double[4][4];

    double currentSpeed = 0;

    double currentRange = 200;

    boolean loadingPair = false;


    // =========================================================
    // BUTTONS / MENUS
    // =========================================================

    JButton startButton;
    JButton clearButton;
    JButton addButton;

    JComboBox<String> cellColorMenu;

    JComboBox<String> attractionFromMenu;

    JComboBox<String> attractionToMenu;

    JComboBox<String> attractionSpeedMenu;

    JComboBox<String> attractionRangeMenu;

    JComboBox<String> spawnAmountMenu;

    JButton randomButton;

    JComboBox<String> maxSpeedMenu;

    // Process speed menu
    JComboBox<String> processSpeedMenu;

    // Wall menu
    JComboBox<String> wallMenu;

    // 5-character code box
    JTextField codeField;

    JLabel codeCountLabel;

    boolean running = false;


    // =========================================================
    // PROCESS SPEED
    // =========================================================

    int processSpeed = 60;


    // =========================================================
    // KEYBOARD
    // =========================================================

    boolean keyW = false;
    boolean keyA = false;
    boolean keyS = false;
    boolean keyD = false;


    // =========================================================
    // ATTRACTION GRID
    // =========================================================

    static final int ATTRACTION_GRID_CELL_SIZE = 50;

    static final int ATTRACTION_GRID_WIDTH =
            20;

    static final int ATTRACTION_GRID_HEIGHT =
            20;

    int[] attractionGridHead =
            new int[
                    ATTRACTION_GRID_WIDTH *
                            ATTRACTION_GRID_HEIGHT
                    ];

    int[] attractionNext =
            new int[1000];


    // =========================================================
    // COLLISION GRID
    // =========================================================

    static final int COLLISION_GRID_CELL_SIZE = 12;

    static final int COLLISION_GRID_WIDTH =
            84;

    static final int COLLISION_GRID_HEIGHT =
            84;

    int[] collisionGridHead =
            new int[
                    COLLISION_GRID_WIDTH *
                            COLLISION_GRID_HEIGHT
                    ];

    int[] collisionNext =
            new int[1000];


    // =========================================================
    // MAX RELEVANT RANGE
    // =========================================================

    double[] maxRelevantRange =
            new double[4];


    // =========================================================
    // TIMER
    // =========================================================

    Timer timer =
            new Timer(
                    16,
                    this
            );


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public cell() {

        setBackground(Color.BLACK);

        setFocusable(true);

        addMouseListener(this);

        addMouseMotionListener(this);

        addMouseWheelListener(this);

        addKeyListener(this);

        createDefaultAttractions();

        clearSpatialGrids();
    }


    // =========================================================
    // DEFAULT ATTRACTIONS
    // =========================================================

    public void createDefaultAttractions() {

        for (int i = 0; i < 4; i++) {

            for (int j = 0; j < 4; j++) {

                attractionRules[i][j] = 0.0;

                attractionRangeRules[i][j] = 200.0;
            }
        }

        rebuildMaximumRanges();
    }


    // =========================================================
    // RANDOMIZE ALL ATTRACTION RULES
    // =========================================================

    public void randomizeAttractions() {

        double[] possibleForces = {

                -0.5,
                -0.25,
                -0.1,
                -0.05,
                -0.025,
                0.0,
                0.025,
                0.05,
                0.1,
                0.25,
                0.5
        };


        double[] possibleRanges = {

                20,
                50,
                100,
                150,
                200
        };


        for (int from = 0;
             from < 4;
             from++) {

            for (int to = 0;
                 to < 4;
                 to++) {

                int forceIndex =
                        random.nextInt(
                                possibleForces.length
                        );

                attractionRules[from][to] =
                        possibleForces[forceIndex];


                int rangeIndex =
                        random.nextInt(
                                possibleRanges.length
                        );

                attractionRangeRules[from][to] =
                        possibleRanges[rangeIndex];
            }
        }


        rebuildMaximumRanges();

        loadSelectedPair();

        repaint();
    }


    // =========================================================
    // RANDOMIZE FROM 5-CHARACTER CODE
    // =========================================================

    public void randomizeFromCode(String code) {

        if (code == null)
            return;

        code = code.trim();

        if (code.length() == 0)
            return;


        long seed = 0;

        for (int i = 0; i < code.length(); i++) {

            seed =
                    seed * 131
                            + code.charAt(i);
        }

        Random codeRandom =
                new Random(seed);


        double[] possibleForces = {

                -0.5,
                -0.25,
                -0.1,
                -0.05,
                -0.025,
                0.0,
                0.025,
                0.05,
                0.1,
                0.25,
                0.5
        };


        double[] possibleRanges = {

                20,
                50,
                100,
                150,
                200
        };


        for (int from = 0;
             from < 4;
             from++) {

            for (int to = 0;
                 to < 4;
                 to++) {

                int forceIndex =
                        codeRandom.nextInt(
                                possibleForces.length
                        );

                attractionRules[from][to] =
                        possibleForces[forceIndex];


                int rangeIndex =
                        codeRandom.nextInt(
                                possibleRanges.length
                        );

                attractionRangeRules[from][to] =
                        possibleRanges[rangeIndex];
            }
        }


        rebuildMaximumRanges();

        loadSelectedPair();

        repaint();
    }


    // =========================================================
    // REBUILD MAXIMUM ACTIVE RANGES
    // =========================================================

    public void rebuildMaximumRanges() {

        Arrays.fill(
                maxRelevantRange,
                0.0
        );


        for (int from = 0;
             from < 4;
             from++) {

            for (int to = 0;
                 to < 4;
                 to++) {

                double attraction =
                        attractionRules[from][to];

                if (attraction == 0.0)
                    continue;


                double range =
                        attractionRangeRules[from][to];

                if (range <= 0)
                    continue;


                if (range > maxRelevantRange[from])
                    maxRelevantRange[from] = range;


                if (range > maxRelevantRange[to])
                    maxRelevantRange[to] = range;
            }
        }
    }


    // =========================================================
    // COLOR -> TYPE
    // =========================================================

    public int colorToType(String color) {

        if (color.equals("Red"))
            return 0;

        if (color.equals("Blue"))
            return 1;

        if (color.equals("Yellow"))
            return 2;

        if (color.equals("Green"))
            return 3;

        return 0;
    }


    // =========================================================
    // TYPE -> COLOR
    // =========================================================

    public Color typeToColor(int type) {

        if (type == 0)
            return Color.RED;

        if (type == 1)
            return Color.BLUE;

        if (type == 2)
            return Color.YELLOW;

        if (type == 3)
            return Color.GREEN;

        return Color.WHITE;
    }


    // =========================================================
    // SELECTED TYPE
    // =========================================================

    public int getSelectedType() {

        return colorToType(
                (String)
                        cellColorMenu.getSelectedItem()
        );
    }


    // =========================================================
    // CURRENT FROM TYPE
    // =========================================================

    public int getCurrentPairA() {

        return colorToType(
                (String)
                        attractionFromMenu.getSelectedItem()
        );
    }


    // =========================================================
    // CURRENT TO TYPE
    // =========================================================

    public int getCurrentPairB() {

        return colorToType(
                (String)
                        attractionToMenu.getSelectedItem()
        );
    }


    // =========================================================
    // GET ATTRACTION
    // =========================================================

    public double getAttraction(
            int from,
            int to
    ) {

        return attractionRules[from][to];
    }


    // =========================================================
    // GET RANGE
    // =========================================================

    public double getAttractionRange(
            int from,
            int to
    ) {

        return attractionRangeRules[from][to];
    }


    // =========================================================
    // SAVE ATTRACTION
    // =========================================================

    public void setAttractionRule() {

        if (loadingPair)
            return;


        int from =
                getCurrentPairA();

        int to =
                getCurrentPairB();


        attractionRules[from][to] =
                currentSpeed;


        rebuildMaximumRanges();
    }


    // =========================================================
    // SAVE RANGE
    // =========================================================

    public void setAttractionRangeRule() {

        if (loadingPair)
            return;


        int from =
                getCurrentPairA();

        int to =
                getCurrentPairB();


        attractionRangeRules[from][to] =
                currentRange;


        rebuildMaximumRanges();
    }


    // =========================================================
    // LOAD SELECTED PAIR
    // =========================================================

    public void loadSelectedPair() {

        if (attractionFromMenu == null ||
                attractionToMenu == null ||
                attractionSpeedMenu == null ||
                attractionRangeMenu == null) {

            return;
        }


        int from =
                getCurrentPairA();

        int to =
                getCurrentPairB();


        loadingPair = true;


        currentSpeed =
                attractionRules[from][to];


        attractionSpeedMenu.setSelectedItem(
                speedToText(currentSpeed)
        );


        currentRange =
                attractionRangeRules[from][to];


        attractionRangeMenu.setSelectedItem(
                rangeToText(currentRange)
        );


        loadingPair = false;
    }


    // =========================================================
    // SPEED -> TEXT
    // =========================================================

    public String speedToText(double speed) {

        if (speed == -0.5)
            return "-10x";

        if (speed == -0.25)
            return "-5x";

        if (speed == -0.1)
            return "-2x";

        if (speed == -0.05)
            return "-1x";

        if (speed == -0.025)
            return "-0.5x";

        if (speed == 0)
            return "0x";

        if (speed == 0.025)
            return "0.5x";

        if (speed == 0.05)
            return "1x";

        if (speed == 0.1)
            return "2x";

        if (speed == 0.25)
            return "5x";

        if (speed == 0.5)
            return "10x";

        return "0x";
    }


    // =========================================================
    // RANGE -> TEXT
    // =========================================================

    public String rangeToText(double range) {

        return Integer.toString(
                (int) range
        );
    }


    // =========================================================
    // WORLD -> SCREEN
    // =========================================================

    int screenX(double x) {

        return (int) (
                (x - cameraX)
                        * zoom
                        + getWidth() / 2
        );
    }


    int screenY(double y) {

        return (int) (
                (y - cameraY)
                        * zoom
                        + getHeight() / 2
        );
    }


    // =========================================================
    // DRAW
    // =========================================================

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);


        int size =
                (int) (8 * zoom);


        if (size < 2)
            size = 2;


        int width =
                getWidth();

        int height =
                getHeight();


        double worldLeft =
                cameraX
                        - width / 2.0 / zoom
                        - 10;


        double worldRight =
                cameraX
                        + width / 2.0 / zoom
                        + 10;


        double worldTop =
                cameraY
                        - height / 2.0 / zoom
                        - 10;


        double worldBottom =
                cameraY
                        + height / 2.0 / zoom
                        + 10;


        // =====================================================
        // DRAW PHYSICS BOX
        // =====================================================

        if (wallsEnabled) {

            g.setColor(Color.DARK_GRAY);


            int boxLeft =
                    screenX(getPhysicsMin());

            int boxTop =
                    screenY(getPhysicsMin());

            int boxRight =
                    screenX(getPhysicsMax());

            int boxBottom =
                    screenY(getPhysicsMax());


            g.drawRect(
                    boxLeft,
                    boxTop,
                    boxRight - boxLeft,
                    boxBottom - boxTop
            );
        }


        // =====================================================
        // DRAW PARTICLES
        // =====================================================

        for (Particle p : cells) {

            if (p.x < worldLeft ||
                    p.x > worldRight ||
                    p.y < worldTop ||
                    p.y > worldBottom) {

                continue;
            }


            g.setColor(
                    typeToColor(p.type)
            );


            g.fillOval(

                    screenX(p.x)
                            - size / 2,

                    screenY(p.y)
                            - size / 2,

                    size,

                    size
            );
        }
    }


    // =========================================================
    // ADD RANDOM CELLS
    // =========================================================

    public void addRandomCell() {

        int amount =
                Integer.parseInt(
                        (String)
                                spawnAmountMenu
                                        .getSelectedItem()
                );


        int type =
                getSelectedType();


        for (int i = 0;
             i < amount;
             i++) {

            double x;

            double y;


            if (wallsEnabled) {

                x =
                        getPhysicsMin()
                                + random.nextDouble()
                                * getPhysicsBoxSize();


                y =
                        getPhysicsMin()
                                + random.nextDouble()
                                * getPhysicsBoxSize();

            }
            else {

                // No wall starts particles around the camera
                double spawnSize = 500;

                x =
                        cameraX
                                - spawnSize / 2
                                + random.nextDouble()
                                * spawnSize;

                y =
                        cameraY
                                - spawnSize / 2
                                + random.nextDouble()
                                * spawnSize;
            }


            cells.add(
                    new Particle(
                            x,
                            y,
                            type
                    )
            );
        }


        ensureGridParticleCapacity();

        repaint();
    }


    // =========================================================
    // ENSURE ARRAY SIZE
    // =========================================================

    public void ensureGridParticleCapacity() {

        if (attractionNext.length >= cells.size())
            return;


        int newSize =
                Math.max(
                        cells.size() * 2,
                        1000
                );


        attractionNext =
                new int[newSize];


        collisionNext =
                new int[newSize];
    }


    // =========================================================
    // CLEAR GRIDS
    // =========================================================

    public void clearSpatialGrids() {

        Arrays.fill(
                attractionGridHead,
                -1
        );


        Arrays.fill(
                collisionGridHead,
                -1
        );
    }


    // =========================================================
    // ATTRACTION GRID X
    // =========================================================

    int attractionGridX(double x) {

        double min =
                getPhysicsMin();

        double max =
                getPhysicsMax();


        if (x < min ||
                x > max)
            return -1;


        if (x == max)
        {
            return ATTRACTION_GRID_WIDTH - 1;
        }


        return (int) (
                (x - min)
                        / ATTRACTION_GRID_CELL_SIZE
        );
    }


    // =========================================================
    // ATTRACTION GRID Y
    // =========================================================

    int attractionGridY(double y) {

        double min =
                getPhysicsMin();

        double max =
                getPhysicsMax();


        if (y < min ||
                y > max)
            return -1;


        if (y == max)
        {
            return ATTRACTION_GRID_HEIGHT - 1;
        }


        return (int) (
                (y - min)
                        / ATTRACTION_GRID_CELL_SIZE
        );
    }


    // =========================================================
    // COLLISION GRID X
    // =========================================================

    int collisionGridX(double x) {

        double min =
                getPhysicsMin();

        double max =
                getPhysicsMax();


        if (x < min ||
                x > max)
            return -1;


        int result =
                (int) (
                        (x - min)
                                / COLLISION_GRID_CELL_SIZE
                );


        if (result >= COLLISION_GRID_WIDTH)
            result =
                    COLLISION_GRID_WIDTH - 1;


        return result;
    }


    // =========================================================
    // COLLISION GRID Y
    // =========================================================

    int collisionGridY(double y) {

        double min =
                getPhysicsMin();

        double max =
                getPhysicsMax();


        if (y < min ||
                y > max)
            return -1;


        int result =
                (int) (
                        (y - min)
                                / COLLISION_GRID_CELL_SIZE
                );


        if (result >= COLLISION_GRID_HEIGHT)
            result =
                    COLLISION_GRID_HEIGHT - 1;


        return result;
    }


    // =========================================================
    // BUILD SPATIAL GRIDS
    // =========================================================

    public void buildSpatialGrids() {

        ensureGridParticleCapacity();

        clearSpatialGrids();


        /*
         * IMPORTANT:
         *
         * When there is NO WALL, particles can leave the
         * normal physics box.
         *
         * Therefore we do NOT try to put them into the
         * finite spatial grids.
         *
         * The No Wall physics functions below use pairwise
         * calculations instead.
         */


        if (!wallsEnabled)
            return;


        for (int i = 0;
             i < cells.size();
             i++) {

            Particle p =
                    cells.get(i);


            if (p.x < getPhysicsMin())
                p.x = getPhysicsMin();

            if (p.x > getPhysicsMax())
                p.x = getPhysicsMax();

            if (p.y < getPhysicsMin())
                p.y = getPhysicsMin();

            if (p.y > getPhysicsMax())
                p.y = getPhysicsMax();


            // =================================================
            // ATTRACTION GRID
            // =================================================

            int attractionGX =
                    attractionGridX(p.x);

            int attractionGY =
                    attractionGridY(p.y);


            if (attractionGX >= 0 &&
                    attractionGX < ATTRACTION_GRID_WIDTH &&
                    attractionGY >= 0 &&
                    attractionGY < ATTRACTION_GRID_HEIGHT) {

                int index =
                        attractionGY *
                                ATTRACTION_GRID_WIDTH
                                + attractionGX;


                attractionNext[i] =
                        attractionGridHead[index];


                attractionGridHead[index] =
                        i;
            }
            else {

                attractionNext[i] =
                        -1;
            }


            // =================================================
            // COLLISION GRID
            // =================================================

            int collisionGX =
                    collisionGridX(p.x);

            int collisionGY =
                    collisionGridY(p.y);


            if (collisionGX >= 0 &&
                    collisionGX < COLLISION_GRID_WIDTH &&
                    collisionGY >= 0 &&
                    collisionGY < COLLISION_GRID_HEIGHT) {

                int index =
                        collisionGY *
                                COLLISION_GRID_WIDTH
                                + collisionGX;


                collisionNext[i] =
                        collisionGridHead[index];


                collisionGridHead[index] =
                        i;
            }
            else {

                collisionNext[i] =
                        -1;
            }
        }
    }


    // =========================================================
    // COLLISION PHYSICS
    // =========================================================

    public void updateCollisions() {

        if (!wallsEnabled) {

            updateCollisionsNoWall();

            return;
        }


        double minDistance =
                minimumCellDistance;


        double minDistanceSquared =
                minDistance *
                        minDistance;


        int neighborRadius = 1;


        for (int i = 0;
             i < cells.size();
             i++) {

            Particle a =
                    cells.get(i);


            int gridX =
                    collisionGridX(a.x);

            int gridY =
                    collisionGridY(a.y);


            if (gridX < 0 ||
                    gridX >= COLLISION_GRID_WIDTH ||
                    gridY < 0 ||
                    gridY >= COLLISION_GRID_HEIGHT) {

                continue;
            }


            for (int offsetY = -neighborRadius;
                 offsetY <= neighborRadius;
                 offsetY++) {

                int neighborGridY =
                        gridY + offsetY;


                if (neighborGridY < 0 ||
                        neighborGridY >= COLLISION_GRID_HEIGHT)
                    continue;


                for (int offsetX = -neighborRadius;
                     offsetX <= neighborRadius;
                     offsetX++) {

                    int neighborGridX =
                            gridX + offsetX;


                    if (neighborGridX < 0 ||
                            neighborGridX >= COLLISION_GRID_WIDTH)
                        continue;


                    int gridIndex =
                            neighborGridY *
                                    COLLISION_GRID_WIDTH
                                    + neighborGridX;


                    int j =
                            collisionGridHead[
                                    gridIndex
                                    ];


                    while (j != -1) {

                        if (j > i) {

                            Particle b =
                                    cells.get(j);


                            applyCollision(
                                    a,
                                    b,
                                    minDistance,
                                    minDistanceSquared
                            );
                        }


                        j =
                                collisionNext[j];
                    }
                }
            }
        }
    }


    // =========================================================
    // NO WALL COLLISION
    // =========================================================

    public void updateCollisionsNoWall() {

        double minDistance =
                minimumCellDistance;


        double minDistanceSquared =
                minDistance *
                        minDistance;


        /*
         * No Wall cannot use the normal finite grid because
         * particles can move outside the 200/500/1000 area.
         *
         * Therefore every pair is checked.
         *
         * This makes sure particles outside the old box
         * still collide correctly.
         */

        for (int i = 0;
             i < cells.size();
             i++) {

            Particle a =
                    cells.get(i);


            for (int j = i + 1;
                 j < cells.size();
                 j++) {

                Particle b =
                        cells.get(j);


                applyCollision(
                        a,
                        b,
                        minDistance,
                        minDistanceSquared
                );
            }
        }
    }


    // =========================================================
    // APPLY COLLISION
    // =========================================================

    public void applyCollision(
            Particle a,
            Particle b,
            double minDistance,
            double minDistanceSquared
    ) {

        double dx =
                b.x - a.x;

        double dy =
                b.y - a.y;


        double distanceSquared =
                dx * dx
                        + dy * dy;


        if (distanceSquared <
                minDistanceSquared) {

            double distance;


            if (distanceSquared == 0) {

                dx = 0.01;
                dy = 0.01;

                distance =
                        Math.sqrt(
                                dx * dx
                                        + dy * dy
                        );
            }
            else {

                distance =
                        Math.sqrt(
                                distanceSquared
                        );
            }


            double directionX =
                    dx / distance;

            double directionY =
                    dy / distance;


            double overlap =
                    minDistance
                            - distance;


            double push =
                    overlap
                            * collisionRepulsion;


            a.vx -=
                    directionX * push;

            a.vy -=
                    directionY * push;


            b.vx +=
                    directionX * push;

            b.vy +=
                    directionY * push;


            double correction =
                    overlap / 2.0;


            a.x -=
                    directionX * correction;

            a.y -=
                    directionY * correction;


            b.x +=
                    directionX * correction;

            b.y +=
                    directionY * correction;
        }
    }


    // =========================================================
    // ATTRACTION PHYSICS
    // =========================================================

    public void updateAttraction() {

        if (!wallsEnabled) {

            updateAttractionNoWall();

            return;
        }


        int particleCount =
                cells.size();


        for (int i = 0;
             i < particleCount;
             i++) {

            Particle a =
                    cells.get(i);


            double searchRange =
                    maxRelevantRange[a.type];


            if (searchRange <= 0)
                continue;


            if (searchRange > getPhysicsBoxSize())
                searchRange =
                        getPhysicsBoxSize();


            int gridX =
                    attractionGridX(a.x);

            int gridY =
                    attractionGridY(a.y);


            if (gridX < 0 ||
                    gridX >= ATTRACTION_GRID_WIDTH ||
                    gridY < 0 ||
                    gridY >= ATTRACTION_GRID_HEIGHT) {

                continue;
            }


            int neighborRadius =
                    (int) Math.ceil(
                            searchRange /
                                    ATTRACTION_GRID_CELL_SIZE
                    );


            for (int offsetY = -neighborRadius;
                 offsetY <= neighborRadius;
                 offsetY++) {

                int neighborGridY =
                        gridY + offsetY;


                if (neighborGridY < 0 ||
                        neighborGridY >= ATTRACTION_GRID_HEIGHT)
                    continue;


                for (int offsetX = -neighborRadius;
                     offsetX <= neighborRadius;
                     offsetX++) {

                    int neighborGridX =
                            gridX + offsetX;


                    if (neighborGridX < 0 ||
                            neighborGridX >= ATTRACTION_GRID_WIDTH)
                        continue;


                    int gridIndex =
                            neighborGridY *
                                    ATTRACTION_GRID_WIDTH
                                    + neighborGridX;


                    int j =
                            attractionGridHead[
                                    gridIndex
                                    ];


                    while (j != -1) {

                        if (j > i) {

                            Particle b =
                                    cells.get(j);


                            applyAttraction(
                                    a,
                                    b
                            );
                        }


                        j =
                                attractionNext[j];
                    }
                }
            }
        }
    }


    // =========================================================
    // NO WALL ATTRACTION
    // =========================================================

    public void updateAttractionNoWall() {

        /*
         * Important:
         *
         * No Wall particles can move outside the normal
         * physics box.
         *
         * We therefore cannot use the fixed 1000x1000
         * spatial grid.
         *
         * Every pair is checked instead.
         *
         * The attraction RANGE is still respected, so
         * particles only affect each other when they are
         * actually inside their selected attraction range.
         */

        for (int i = 0;
             i < cells.size();
             i++) {

            Particle a =
                    cells.get(i);


            for (int j = i + 1;
                 j < cells.size();
                 j++) {

                Particle b =
                        cells.get(j);


                applyAttraction(
                        a,
                        b
                );
            }
        }
    }


    // =========================================================
    // APPLY ATTRACTION
    // =========================================================

    public void applyAttraction(
            Particle a,
            Particle b
    ) {

        double forceAB =
                attractionRules[
                        a.type
                ][
                        b.type
                ];


        double forceBA =
                attractionRules[
                        b.type
                ][
                        a.type
                ];


        if (forceAB == 0.0 &&
                forceBA == 0.0) {

            return;
        }


        double rangeAB = 0;

        double rangeBA = 0;


        if (forceAB != 0.0) {

            rangeAB =
                    attractionRangeRules[
                            a.type
                    ][
                            b.type
                    ];
        }


        if (forceBA != 0.0) {

            rangeBA =
                    attractionRangeRules[
                            b.type
                    ][
                            a.type
                    ];
        }


        if (rangeAB <= 0 &&
                rangeBA <= 0) {

            return;
        }


        double effectiveRange =
                Math.max(
                        rangeAB,
                        rangeBA
                );


        if (effectiveRange <=
                minimumInteractionRange) {

            return;
        }


        double dx =
                b.x - a.x;

        double dy =
                b.y - a.y;


        double distanceSquared =
                dx * dx
                        + dy * dy;


        double effectiveRangeSquared =
                effectiveRange *
                        effectiveRange;


        if (distanceSquared >
                effectiveRangeSquared) {

            return;
        }


        if (distanceSquared == 0) {

            return;
        }


        double distance =
                Math.sqrt(
                        distanceSquared
                );


        double inverseDistance =
                1.0 / distance;


        double directionX =
                dx *
                        inverseDistance;


        double directionY =
                dy *
                        inverseDistance;


        if (forceAB != 0.0 &&
                distance >
                        minimumInteractionRange &&
                distance <
                        rangeAB) {

            double strength =
                    (rangeAB - distance)
                            / rangeAB;


            double force =
                    strength *
                            forceAB;


            a.vx +=
                    directionX * force;

            a.vy +=
                    directionY * force;
        }


        if (forceBA != 0.0 &&
                distance >
                        minimumInteractionRange &&
                distance <
                        rangeBA) {

            double strength =
                    (rangeBA - distance)
                            / rangeBA;


            double force =
                    strength *
                            forceBA;


            b.vx -=
                    directionX * force;

            b.vy -=
                    directionY * force;
        }
    }


    // =========================================================
    // MOVE PARTICLES
    // =========================================================

    public void moveParticles() {

        double maxSpeedSquared =
                maxSpeed *
                        maxSpeed;


        for (Particle p : cells) {

            double speedSquared =
                    p.vx * p.vx
                            + p.vy * p.vy;


            if (speedSquared >
                    maxSpeedSquared) {

                double speed =
                        Math.sqrt(
                                speedSquared
                        );


                p.vx =
                        (p.vx / speed)
                                * maxSpeed;


                p.vy =
                        (p.vy / speed)
                                * maxSpeed;
            }


            p.x += p.vx;

            p.y += p.vy;


            // =================================================
            // WALL REFLECTION
            // =================================================

            if (wallsEnabled) {

                if (p.x > getPhysicsMax()) {

                    p.x =
                            getPhysicsMax();

                    p.vx =
                            -Math.abs(p.vx);
                }
                else if (p.x < getPhysicsMin()) {

                    p.x =
                            getPhysicsMin();

                    p.vx =
                            Math.abs(p.vx);
                }


                if (p.y > getPhysicsMax()) {

                    p.y =
                            getPhysicsMax();

                    p.vy =
                            -Math.abs(p.vy);
                }
                else if (p.y < getPhysicsMin()) {

                    p.y =
                            getPhysicsMin();

                    p.vy =
                            Math.abs(p.vy);
                }
            }


            // =================================================
            // FRICTION
            // =================================================

            p.vx *= 0.98;

            p.vy *= 0.98;
        }
    }


    // =========================================================
    // WASD CAMERA
    // =========================================================

    public void updateCamera() {

        boolean changed = false;

        double movement =
                cameraMoveSpeed / zoom;


        if (keyW) {

            cameraY -= movement;

            changed = true;
        }


        if (keyS) {

            cameraY += movement;

            changed = true;
        }


        if (keyA) {

            cameraX -= movement;

            changed = true;
        }


        if (keyD) {

            cameraX += movement;

            changed = true;
        }


        if (changed)
            repaint();
    }


    // =========================================================
    // PHYSICS UPDATE
    // =========================================================

    public void updatePhysics() {

        buildSpatialGrids();

        updateCollisions();

        updateAttraction();

        moveParticles();
    }


    // =========================================================
    // GAME LOOP
    // =========================================================

    @Override
    public void actionPerformed(
            ActionEvent e
    ) {

        if (e.getSource() == timer) {

            updateCamera();


            if (running) {

                /*
                 * Process speed is controlled separately.
                 *
                 * Timer runs frequently and accumulator
                 * decides when the next physics calculation
                 * happens.
                 */

                long now =
                        System.nanoTime();

                processPhysicsIfNeeded(now);
            }


            repaint();
        }
    }


    // =========================================================
    // PROCESS SPEED SYSTEM
    // =========================================================

    long lastPhysicsTime =
            System.nanoTime();

    double physicsAccumulator = 0;


    public void processPhysicsIfNeeded(
            long currentTime
    ) {

        double secondsPassed =
                (currentTime - lastPhysicsTime)
                        / 1_000_000_000.0;


        lastPhysicsTime =
                currentTime;


        if (secondsPassed > 0.1)
            secondsPassed = 0.1;


        physicsAccumulator +=
                secondsPassed;


        double physicsStep =
                1.0 / processSpeed;


        int safetyCounter = 0;


        while (physicsAccumulator >= physicsStep &&
                safetyCounter < 20) {

            updatePhysics();

            physicsAccumulator -=
                    physicsStep;

            safetyCounter++;
        }
    }


    // =========================================================
    // RIGHT CLICK CAMERA
    // =========================================================

    @Override
    public void mousePressed(
            MouseEvent e
    ) {

        requestFocusInWindow();


        if (SwingUtilities.isRightMouseButton(e)) {

            rightDragging = true;

            lastMouseX =
                    e.getX();

            lastMouseY =
                    e.getY();
        }
    }


    // =========================================================
    // CAMERA DRAG
    // =========================================================

    @Override
    public void mouseDragged(
            MouseEvent e
    ) {

        if (rightDragging) {

            int dx =
                    e.getX()
                            - lastMouseX;


            int dy =
                    e.getY()
                            - lastMouseY;


            cameraX -=
                    dx / zoom;


            cameraY -=
                    dy / zoom;


            lastMouseX =
                    e.getX();


            lastMouseY =
                    e.getY();


            repaint();
        }
    }


    // =========================================================
    // STOP CAMERA
    // =========================================================

    @Override
    public void mouseReleased(
            MouseEvent e
    ) {

        if (SwingUtilities.isRightMouseButton(e)) {

            rightDragging = false;
        }
    }


    // =========================================================
    // ZOOM
    // =========================================================

    @Override
    public void mouseWheelMoved(
            MouseWheelEvent e
    ) {

        double oldZoom =
                zoom;


        if (e.getWheelRotation() < 0) {

            zoom *= 1.15;

        }
        else {

            zoom /= 1.15;
        }


        if (zoom > 10)
            zoom = 10;


        if (zoom < 0.05)
            zoom = 0.05;


        double mouseWorldX =
                cameraX
                        +
                        (e.getX()
                                - getWidth() / 2.0)
                                / oldZoom;


        double mouseWorldY =
                cameraY
                        +
                        (e.getY()
                                - getHeight() / 2.0)
                                / oldZoom;


        cameraX =
                mouseWorldX
                        -
                        (e.getX()
                                - getWidth() / 2.0)
                                / zoom;


        cameraY =
                mouseWorldY
                        -
                        (e.getY()
                                - getHeight() / 2.0)
                                / zoom;


        repaint();
    }


    // =========================================================
    // KEY PRESSED
    // =========================================================

    @Override
    public void keyPressed(KeyEvent e) {

        int key =
                e.getKeyCode();


        if (key == KeyEvent.VK_W)
            keyW = true;

        if (key == KeyEvent.VK_A)
            keyA = true;

        if (key == KeyEvent.VK_S)
            keyS = true;

        if (key == KeyEvent.VK_D)
            keyD = true;
    }


    // =========================================================
    // KEY RELEASED
    // =========================================================

    @Override
    public void keyReleased(KeyEvent e) {

        int key =
                e.getKeyCode();


        if (key == KeyEvent.VK_W)
            keyW = false;

        if (key == KeyEvent.VK_A)
            keyA = false;

        if (key == KeyEvent.VK_S)
            keyS = false;

        if (key == KeyEvent.VK_D)
            keyD = false;
    }


    // =========================================================
    // KEY TYPED
    // =========================================================

    @Override
    public void keyTyped(KeyEvent e) {}


    // =========================================================
    // UNUSED MOUSE EVENTS
    // =========================================================

    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}


    // =========================================================
    // UPDATE CODE COUNTER
    // =========================================================

    public void updateCodeCounter() {

        if (codeField == null ||
                codeCountLabel == null)
            return;


        int length =
                codeField.getText().length();


        codeCountLabel.setText(
                length + "/5"
        );
    }


    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args
    ) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame =
                    new JFrame(
                            "Cell Attraction Simulation"
                    );


            cell panel =
                    new cell();


            JPanel buttons =
                    new JPanel();


            // =================================================
            // BASIC BUTTONS
            // =================================================

            panel.startButton =
                    new JButton("Start");

            panel.clearButton =
                    new JButton("Clear");

            panel.addButton =
                    new JButton("Add Cell");

            panel.randomButton =
                    new JButton("Random");


            // =================================================
            // SPAWN AMOUNT
            // =================================================

            panel.spawnAmountMenu =
                    new JComboBox<>();


            panel.spawnAmountMenu.addItem("1");
            panel.spawnAmountMenu.addItem("10");
            panel.spawnAmountMenu.addItem("100");
            panel.spawnAmountMenu.addItem("1000");


            panel.spawnAmountMenu.setSelectedItem(
                    "1"
            );


            // =================================================
            // CELL COLOR
            // =================================================

            panel.cellColorMenu =
                    new JComboBox<>();


            String[] colors =
                    {
                            "Red",
                            "Blue",
                            "Yellow",
                            "Green"
                    };


            for (String c : colors) {

                panel.cellColorMenu.addItem(c);
            }


            // =================================================
            // FROM
            // =================================================

            panel.attractionFromMenu =
                    new JComboBox<>();


            // =================================================
            // TO
            // =================================================

            panel.attractionToMenu =
                    new JComboBox<>();


            for (String c : colors) {

                panel.attractionFromMenu
                        .addItem(c);

                panel.attractionToMenu
                        .addItem(c);
            }


            // =================================================
            // ATTRACTION MENU
            // =================================================

            panel.attractionSpeedMenu =
                    new JComboBox<>();


            panel.attractionSpeedMenu.addItem("-10x");
            panel.attractionSpeedMenu.addItem("-5x");
            panel.attractionSpeedMenu.addItem("-2x");
            panel.attractionSpeedMenu.addItem("-1x");
            panel.attractionSpeedMenu.addItem("-0.5x");
            panel.attractionSpeedMenu.addItem("0x");
            panel.attractionSpeedMenu.addItem("0.5x");
            panel.attractionSpeedMenu.addItem("1x");
            panel.attractionSpeedMenu.addItem("2x");
            panel.attractionSpeedMenu.addItem("5x");
            panel.attractionSpeedMenu.addItem("10x");


            panel.attractionSpeedMenu
                    .setSelectedItem("0x");


            // =================================================
            // RANGE MENU
            // =================================================

            panel.attractionRangeMenu =
                    new JComboBox<>();


            panel.attractionRangeMenu.addItem("20");
            panel.attractionRangeMenu.addItem("50");
            panel.attractionRangeMenu.addItem("100");
            panel.attractionRangeMenu.addItem("150");
            panel.attractionRangeMenu.addItem("200");


            panel.attractionRangeMenu
                    .setSelectedItem("200");


            // =================================================
            // MAX SPEED MENU
            // =================================================

            panel.maxSpeedMenu =
                    new JComboBox<>();


            panel.maxSpeedMenu.addItem("1");
            panel.maxSpeedMenu.addItem("2");
            panel.maxSpeedMenu.addItem("5");
            panel.maxSpeedMenu.addItem("10");
            panel.maxSpeedMenu.addItem("20");
            panel.maxSpeedMenu.addItem("30");
            panel.maxSpeedMenu.addItem("50");
            panel.maxSpeedMenu.addItem("100");


            panel.maxSpeedMenu.setSelectedItem("5");


            // =================================================
            // MAX SPEED CHANGED
            // =================================================

            panel.maxSpeedMenu
                    .addActionListener(e -> {

                        String value =
                                (String)
                                        panel.maxSpeedMenu
                                                .getSelectedItem();


                        panel.maxSpeed =
                                Double.parseDouble(value);
                    });


            // =================================================
            // PROCESS SPEED MENU
            // =================================================

            panel.processSpeedMenu =
                    new JComboBox<>();


            panel.processSpeedMenu.addItem("30/s");
            panel.processSpeedMenu.addItem("60/s");
            panel.processSpeedMenu.addItem("120/s");
            panel.processSpeedMenu.addItem("240/s");
            panel.processSpeedMenu.addItem("500/s");


            panel.processSpeedMenu
                    .setSelectedItem("60/s");


            // =================================================
            // PROCESS SPEED CHANGED
            // =================================================

            panel.processSpeedMenu
                    .addActionListener(e -> {

                        String value =
                                (String)
                                        panel.processSpeedMenu
                                                .getSelectedItem();


                        if (value.equals("30/s"))
                            panel.processSpeed = 30;

                        else if (value.equals("60/s"))
                            panel.processSpeed = 60;

                        else if (value.equals("120/s"))
                            panel.processSpeed = 120;

                        else if (value.equals("240/s"))
                            panel.processSpeed = 240;

                        else if (value.equals("500/s"))
                            panel.processSpeed = 500;


                        panel.lastPhysicsTime =
                                System.nanoTime();


                        panel.physicsAccumulator = 0;
                    });


            // =================================================
            // WALL MENU
            // =================================================

            panel.wallMenu =
                    new JComboBox<>();


            panel.wallMenu.addItem("No Wall");
            panel.wallMenu.addItem("200x200");
            panel.wallMenu.addItem("500x500");
            panel.wallMenu.addItem("1000x1000");


            panel.wallMenu
                    .setSelectedItem("1000x1000");


            // =================================================
            // WALL CHANGED
            // =================================================

            panel.wallMenu
                    .addActionListener(e -> {

                        String value =
                                (String)
                                        panel.wallMenu
                                                .getSelectedItem();


                        if (value.equals("No Wall")) {

                            panel.wallsEnabled =
                                    false;

                        }
                        else {

                            panel.wallsEnabled =
                                    true;


                            if (value.equals("200x200")) {

                                panel.currentWorldSize =
                                        200;

                            }
                            else if (value.equals("500x500")) {

                                panel.currentWorldSize =
                                        500;

                            }
                            else if (value.equals("1000x1000")) {

                                panel.currentWorldSize =
                                        1000;
                            }


                            /*
                             * Move particles that are outside
                             * the newly selected wall back
                             * inside the wall.
                             *
                             * This prevents particles from
                             * remaining outside after switching
                             * from No Wall to a finite wall.
                             */

                            for (Particle p : panel.cells) {

                                if (p.x <
                                        panel.getPhysicsMin()) {

                                    p.x =
                                            panel.getPhysicsMin();

                                }

                                if (p.x >
                                        panel.getPhysicsMax()) {

                                    p.x =
                                            panel.getPhysicsMax();

                                }

                                if (p.y <
                                        panel.getPhysicsMin()) {

                                    p.y =
                                            panel.getPhysicsMin();

                                }

                                if (p.y >
                                        panel.getPhysicsMax()) {

                                    p.y =
                                            panel.getPhysicsMax();
                                }
                            }
                        }


                        /*
                         * Clear the old grid because the world
                         * size has changed.
                         */

                        panel.clearSpatialGrids();

                        panel.requestFocusInWindow();

                        panel.repaint();
                    });


            // =================================================
            // CODE FIELD
            // =================================================

            panel.codeField =
                    new JTextField(5);


            panel.codeField.setColumns(5);


            // Only allow letters and numbers.
            panel.codeField.setDocument(
                    new javax.swing.text.PlainDocument() {

                        @Override
                        public void insertString(
                                int offs,
                                String str,
                                javax.swing.text.AttributeSet a
                        )
                                throws javax.swing.text.BadLocationException {

                            if (str == null)
                                return;


                            String current =
                                    getText(
                                            0,
                                            getLength()
                                    );


                            StringBuilder filtered =
                                    new StringBuilder();


                            for (char c : str.toCharArray()) {

                                if (Character.isLetterOrDigit(c)) {

                                    if (current.length()
                                            + filtered.length()
                                            < 5) {

                                        filtered.append(c);
                                    }
                                }
                            }


                            super.insertString(
                                    offs,
                                    filtered.toString(),
                                    a
                            );


                            SwingUtilities.invokeLater(
                                    panel::updateCodeCounter
                            );
                        }


                        @Override
                        public void remove(
                                int offs,
                                int len
                        )
                                throws javax.swing.text.BadLocationException {

                            super.remove(
                                    offs,
                                    len
                            );


                            SwingUtilities.invokeLater(
                                    panel::updateCodeCounter
                            );
                        }
                    }
            );


            panel.codeCountLabel =
                    new JLabel("0/5");


            // =================================================
            // ENTER CODE
            // =================================================

            panel.codeField.addActionListener(e -> {

                String code =
                        panel.codeField
                                .getText()
                                .trim();


                if (code.length() > 0) {

                    panel.randomizeFromCode(code);
                }


                panel.requestFocusInWindow();
            });


            // =================================================
            // START
            // =================================================

            panel.startButton
                    .addActionListener(e -> {

                        panel.running =
                                !panel.running;


                        if (panel.running) {

                            panel.lastPhysicsTime =
                                    System.nanoTime();

                            panel.physicsAccumulator =
                                    0;


                            panel.timer.start();

                            panel.startButton
                                    .setText("Pause");

                        }
                        else {

                            panel.timer.stop();

                            panel.startButton
                                    .setText("Start");
                        }


                        panel.requestFocusInWindow();
                    });


            // =================================================
            // CLEAR
            // =================================================

            panel.clearButton
                    .addActionListener(e -> {

                        panel.cells.clear();

                        panel.clearSpatialGrids();

                        panel.repaint();

                        panel.requestFocusInWindow();
                    });


            // =================================================
            // ADD
            // =================================================

            panel.addButton
                    .addActionListener(e -> {

                        panel.addRandomCell();

                        panel.requestFocusInWindow();
                    });


            // =================================================
            // ATTRACTION CHANGED
            // =================================================

            panel.attractionSpeedMenu
                    .addActionListener(e -> {

                        if (panel.loadingPair)
                            return;


                        String value =
                                (String)
                                        panel.attractionSpeedMenu
                                                .getSelectedItem();


                        if (value.equals("-10x"))
                            panel.currentSpeed = -0.5;

                        else if (value.equals("-5x"))
                            panel.currentSpeed = -0.25;

                        else if (value.equals("-2x"))
                            panel.currentSpeed = -0.1;

                        else if (value.equals("-1x"))
                            panel.currentSpeed = -0.05;

                        else if (value.equals("-0.5x"))
                            panel.currentSpeed = -0.025;

                        else if (value.equals("0x"))
                            panel.currentSpeed = 0;

                        else if (value.equals("0.5x"))
                            panel.currentSpeed = 0.025;

                        else if (value.equals("1x"))
                            panel.currentSpeed = 0.05;

                        else if (value.equals("2x"))
                            panel.currentSpeed = 0.1;

                        else if (value.equals("5x"))
                            panel.currentSpeed = 0.25;

                        else if (value.equals("10x"))
                            panel.currentSpeed = 0.5;


                        panel.setAttractionRule();

                        panel.requestFocusInWindow();
                    });


            // =================================================
            // RANGE CHANGED
            // =================================================

            panel.attractionRangeMenu
                    .addActionListener(e -> {

                        if (panel.loadingPair)
                            return;


                        String value =
                                (String)
                                        panel.attractionRangeMenu
                                                .getSelectedItem();


                        panel.currentRange =
                                Double.parseDouble(value);


                        panel.setAttractionRangeRule();

                        panel.requestFocusInWindow();
                    });


            // =================================================
            // RANDOM ATTRACTIONS
            // =================================================

            panel.randomButton
                    .addActionListener(e -> {

                        panel.randomizeAttractions();

                        panel.requestFocusInWindow();
                    });


            // =================================================
            // FROM CHANGED
            // =================================================

            panel.attractionFromMenu
                    .addActionListener(e -> {

                        panel.loadSelectedPair();

                        panel.requestFocusInWindow();
                    });


            // =================================================
            // TO CHANGED
            // =================================================

            panel.attractionToMenu
                    .addActionListener(e -> {

                        panel.loadSelectedPair();

                        panel.requestFocusInWindow();
                    });


            // =================================================
            // BUTTONS
            // =================================================

            buttons.add(
                    panel.startButton
            );

            buttons.add(
                    panel.clearButton
            );

            buttons.add(
                    panel.addButton
            );

            buttons.add(
                    panel.spawnAmountMenu
            );


            buttons.add(
                    new JLabel("Cell Color")
            );

            buttons.add(
                    panel.cellColorMenu
            );


            buttons.add(
                    new JLabel("From")
            );

            buttons.add(
                    panel.attractionFromMenu
            );


            buttons.add(
                    new JLabel("To")
            );

            buttons.add(
                    panel.attractionToMenu
            );


            buttons.add(
                    new JLabel("Attraction")
            );

            buttons.add(
                    panel.attractionSpeedMenu
            );


            buttons.add(
                    new JLabel("Range")
            );

            buttons.add(
                    panel.attractionRangeMenu
            );


            buttons.add(
                    panel.randomButton
            );


            // =================================================
            // CODE SECTION
            // =================================================

            buttons.add(
                    new JLabel("Code")
            );

            buttons.add(
                    panel.codeField
            );

            buttons.add(
                    panel.codeCountLabel
            );


            // =================================================
            // MAX SPEED
            // =================================================

            buttons.add(
                    new JLabel("Max Speed")
            );

            buttons.add(
                    panel.maxSpeedMenu
            );


            // =================================================
            // PROCESS SPEED
            // =================================================

            buttons.add(
                    new JLabel("Process Speed")
            );

            buttons.add(
                    panel.processSpeedMenu
            );


            // =================================================
            // WALL
            // =================================================

            buttons.add(
                    new JLabel("Wall")
            );

            buttons.add(
                    panel.wallMenu
            );


            // =================================================
            // FRAME
            // =================================================

            frame.setLayout(
                    new BorderLayout()
            );


            frame.add(
                    panel,
                    BorderLayout.CENTER
            );


            frame.add(
                    buttons,
                    BorderLayout.SOUTH
            );


            frame.setSize(
                    1100,
                    700
            );


            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE
            );


            frame.setLocationRelativeTo(null);


            frame.setVisible(true);


            panel.requestFocusInWindow();

            panel.timer.start();
        });
    }
}