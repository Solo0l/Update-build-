package com.example.PentoMaster_v1;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main extends Application {

    private static final int BOARD_ROWS = 6;
    private static final int BOARD_COLS = 10;
    private static final int CELL_SIZE = 50;
    private static final int WINDOW_WIDTH = 950;
    private static final int WINDOW_HEIGHT = 800;
    private long lastPlaceTime = 0;
    private int comboCount = 0;
    private String playerName = "Guest";
    private Pane root = new Pane(); // الحاوية الرئيسية للعبة
    private Group boardGroup = new Group(); // الحاوية الخاصة بمربعات البورد
    private Map<String, Integer> playerScores = new HashMap<>(); // لتخزين نتائج جميع اللاعبين
    private static final int ROWS = 6;     // عدد الصفوف
    private static final int COLS = 10;    // عدد الأعمدة
    private static final int TILE_SIZE = 50; // حجم المربع الواحد
    private int currentLevelTime;
    private Stage mainStage;
    private Scene menuScene;
    private Scene gameScene;
    private StackPane rootStack; // الوعاء الرئيسي للعبة
    private Pane instructionsOverlay;

    private Pane piecePool;
    private GridPane gameBoardView;
    private Pane placedPiecesLayer;
    private static final String DATA_FILE = "player_data.txt";
    private Pane dragOverlay;
    private GameBoard boardLogic;
    private Pentomino selectedPiece = null;
    private int playerCoins = 0;
    private int targetCells = ROWS * COLS;
    private long lastTapTime = 0;
    private static final long DOUBLE_TAP_THRESHOLD = 300; // ms
    private Group hintOverlay = null;
    private Timeline hintTimer = null;
    private Timeline timeline;
    private int timeSeconds;
    private Label timeLabel;
    private boolean isPaused = false;
    private Button pauseBtn;
    private Button storeBtn; // إضافة هذا السطر في أعلى الكلاس
    private VBox endLevelOverlay;

    private double mouseAnchorX, mouseAnchorY;
    private double initialPieceX, initialPieceY;
    private boolean wasOnBoard = false;
    private int oldGridX, oldGridY;
    private void rewardLevelCompletion() {
        int timeBonus = timeSeconds * 2;
        int totalReward = 50 + timeBonus;

        playerCoins += totalReward;
        savePlayerProgress(); // ✅ أهم سطر

        showComboText("LEVEL CLEAR! +" + totalReward + " COINS",
                WINDOW_WIDTH / 2.0, WINDOW_HEIGHT / 2.0);
    }
     // الحاوية اللي بترسم فيها البورد
    // قائمة الألوان المتاحة للشراء (Skin Name : Color : Price)
    private final Map<String, Color> availableSkins = Map.of(
            "DEFAULT", Color.CYAN,
            "GOLDEN", Color.GOLD,
            "NEON PINK", Color.HOTPINK,
            "EMERALD", Color.LIMEGREEN
    );
    private String activeSkin = "DEFAULT";
    private java.util.Set<String> ownedSkins = new java.util.HashSet<>(java.util.List.of("DEFAULT"));

    private int bestTime = Integer.MAX_VALUE;
    private Label bestScoreLabel;
    private Button hintBtn;
    private VBox gameOverPane;
    private VBox winPane;
    private int currentLevel = 1;
    private int currentScore = 0;
    private Label scoreLabel;

    private boolean isMuted = false;
    private Timeline poolRefiller;
    private Map<Node, Animation> pieceTimers = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;

        // --- 1. تصميم الخلفية (Dark Blue Gradient) ---
        StackPane menuRoot = new StackPane();
        menuRoot.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #1a2a6c, #b21f1f, #fdbb2d);");
        RadialGradient bgGradient = new RadialGradient(
                0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0f2027")),
                new Stop(1, Color.web("#203a43"))
        );
        menuRoot.setBackground(new Background(new BackgroundFill(bgGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        // --- 2. محتوى القائمة الرئيسية ---
        VBox menuLayout = new VBox(25); // مسافة أكبر بين الأزرار
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setPickOnBounds(false);
        Pane backgroundPane = new Pane();
        createFallingBackground(backgroundPane);
        // داخل ميثود start
        if (!root.getChildren().contains(boardGroup)) root.getChildren().add(boardGroup);
        boardGroup.setLayoutX(100);
        boardGroup.setLayoutY(100);

        // العنوان
        Label titleLabel = new Label("PENTOMINO\nMASTERMINDS");
        titleLabel.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 60px; -fx-font-weight: bold; -fx-text-fill: white; -fx-text-alignment: center; -fx-effect: dropshadow(three-pass-box, cyan, 20, 0, 0, 0);");

        // الأزرار الفضية (مطابقة للصورة)
        Button btnNewGame = createSilverButton("NEW GAME");
        btnNewGame.setOnAction(e -> showDifficultySelector(menuLayout));

        Button btnHowToPlay = createSilverButton("HOW TO PLAY");
        btnHowToPlay.setOnAction(e -> showHelpDialog());

        Button btnExit = createSilverButton("EXIT");
        btnExit.setOnAction(e -> primaryStage.close());

        menuLayout.getChildren().addAll(titleLabel, new Region(), btnNewGame, btnHowToPlay, btnExit);
        ((Region)menuLayout.getChildren().get(1)).setPrefHeight(50); // مسافة تحت العنوان

        menuRoot.getChildren().add(menuLayout);

        menuScene = new Scene(menuRoot, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setTitle("Pentomino Masterminds");
        primaryStage.setScene(menuScene);
        primaryStage.setResizable(true);
        primaryStage.show();

        try {
            Image icon = new Image("file:icon.png");
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {}

        loadHighScore();
    }

    // --- دالة تصميم الزر الفضي (نفس ستايل الصورة) ---
    private Button createSilverButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(400); // زر عريض
        btn.setPrefHeight(70); // زر مرتفع

        // CSS للأزرار الفضية مع حدود سوداء وتدرج لوني
        String silverStyle =
                "-fx-background-color: linear-gradient(to bottom, #f0f0f0 0%, #d9d9d9 50%, #a6a6a6 100%);" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #222222;" +
                        "-fx-border-width: 3;" +
                        "-fx-border-radius: 10;" +
                        "-fx-text-fill: #111111;" +
                        "-fx-font-family: 'Arial Black';" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 24px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 5);";

        btn.setStyle(silverStyle);

        // تأثير عند المرور (إضاءة خفيفة)
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff 0%, #e6e6e6 50%, #bfbfbf 100%);" +
                "-fx-background-radius: 10; -fx-border-color: #222222; -fx-border-width: 3; -fx-border-radius: 10; " +
                "-fx-text-fill: #111111; -fx-font-family: 'Arial Black'; -fx-font-weight: bold; -fx-font-size: 24px; " +
                "-fx-effect: dropshadow(three-pass-box, cyan, 15, 0, 0, 0);")); // توهج أزرق عند اللمس

        btn.setOnMouseExited(e -> btn.setStyle(silverStyle));

        return btn;
    }

    private void showDifficultySelector(VBox layout) {
        layout.getChildren().clear();

        Label title = new Label("SELECT DIFFICULTY");
        title.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, cyan, 10, 0, 0, 0);");

        Button easy = createSilverButton("EASY (300s)");
        easy.setOnAction(e -> startGame(300));

        Button normal = createSilverButton("NORMAL (175s)");
        normal.setOnAction(e -> startGame(175));

        Button hard = createSilverButton("HARD (100s)");
        hard.setOnAction(e -> startGame(100));

        Button back = createSilverButton("BACK");
        back.setOnAction(e -> start(mainStage));

        layout.getChildren().addAll(title, new Region(), easy, normal, hard, back);
        ((Region)layout.getChildren().get(1)).setPrefHeight(30);
    }

    private void startGame(int timeLimit) {
        currentLevelTime = timeLimit;
        boardLogic = new GameBoard();

        rootStack = new StackPane(); // استخدام المتغير المعرف في الكلاس
        RadialGradient bgGradient = new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#0d1b2a")), new Stop(1, Color.web("#000000")));
        rootStack.setBackground(new Background(new BackgroundFill(bgGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: transparent;");

        dragOverlay = new Pane();
        dragOverlay.setPickOnBounds(false);
        dragOverlay.prefWidthProperty().bind(rootStack.widthProperty());
        dragOverlay.prefHeightProperty().bind(rootStack.heightProperty());

        // --- الحل الجذري هنا: إنشاء لوحة التحكم مرة واحدة فقط ---
        HBox controlPanel = createControlPanel();
        mainLayout.setTop(controlPanel);

        // RIGHT: Piece Pool
        piecePool = new Pane();
        piecePool.setStyle("-fx-background-color: rgba(40, 40, 40, 0.5); -fx-border-color: #444; -fx-border-width: 0 0 0 1;");
        piecePool.setPrefWidth(320);

        Label poolLabel = new Label("Pieces");
        poolLabel.setStyle("-fx-text-fill: #CCCCCC; -fx-font-weight: bold; -fx-font-size: 14px;");
        poolLabel.setLayoutX(130); poolLabel.setLayoutY(10);
        piecePool.getChildren().add(poolLabel);
        mainLayout.setRight(piecePool);

        // CENTER: Board Area
        gameBoardView = createGameBoard();
        placedPiecesLayer = new Pane();
        double boardWidth = BOARD_COLS * CELL_SIZE;
        double boardHeight = BOARD_ROWS * CELL_SIZE;
        placedPiecesLayer.setMaxSize(boardWidth, boardHeight);
        placedPiecesLayer.setMinSize(boardWidth, boardHeight);

        StackPane boardStack = new StackPane(gameBoardView, placedPiecesLayer);
        boardStack.setMaxSize(boardWidth, boardHeight);
        boardStack.setStyle("-fx-background-color: #202020; -fx-background-radius: 10; -fx-border-color: #555555; -fx-border-width: 4; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, cyan, 20, 0, 0, 0);");

        StackPane boardContainer = new StackPane(boardStack);
        boardContainer.setAlignment(Pos.CENTER);



        VBox centerArea = new VBox(20);
        centerArea.setAlignment(Pos.CENTER);
        centerArea.getChildren().addAll(boardContainer);
        mainLayout.setCenter(centerArea);

        gameOverPane = createGameOverPane();
        winPane = createWinPane();

        rootStack.getChildren().addAll(mainLayout, dragOverlay, gameOverPane, winPane);
        endLevelOverlay = createEndLevelOverlay();
        rootStack.getChildren().add(endLevelOverlay);
        instructionsOverlay = createInstructionsOverlay();
        rootStack.getChildren().add(instructionsOverlay);

// عطّل اللعب مؤقتًا
        placedPiecesLayer.setDisable(true);
        piecePool.setDisable(true);

        gameScene = new Scene(rootStack, WINDOW_WIDTH, WINDOW_HEIGHT);
        gameScene.setOnKeyPressed(this::handleKeyPress);
        mainStage.setScene(gameScene);

        // إعادة ضبط البيانات وبدء المحركات
        timeSeconds = currentLevelTime;
        currentScore = 0;
        scoreLabel.setText("SCORE: 0");
        timeLabel.setText("Time: " + timeSeconds + "s");
        isPaused = false;

        pieceTimers.clear();

        startPoolRefiller();
        spawnInitialPieces();
    }

    private void startPoolRefiller() {
        if (poolRefiller != null) poolRefiller.stop();
        poolRefiller = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!isPaused && piecePool.getChildren().size() < 6) {
                spawnNewPiece();
            }
        }));
        poolRefiller.setCycleCount(Animation.INDEFINITE);
        poolRefiller.play();
    }

    private VBox createWinPane() {
        VBox pane = new VBox(20);
        pane.setAlignment(Pos.CENTER);
        pane.setStyle("-fx-background-color: rgba(20, 20, 20, 0.95); -fx-border-color: #00E676; -fx-border-width: 5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 40;");
        pane.setMaxSize(400, 300);
        Label lblOver = new Label("YOU WIN!");
        lblOver.setStyle("-fx-font-size: 50px; -fx-font-weight: bold; -fx-text-fill: #00E676; -fx-effect: dropshadow(three-pass-box, #00E676, 15, 0, 0, 0);");
        Label lblMsg = new Label("Great Job!");
        lblMsg.setStyle("-fx-font-size: 25px; -fx-text-fill: white;");
        Button btnRetry = new Button("PLAY AGAIN");
        btnRetry.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 16px; -fx-pref-width: 150;");
        btnRetry.setOnAction(e -> restartGame());
        pane.getChildren().addAll(lblOver, lblMsg, btnRetry);
        pane.setVisible(false);
        return pane;
    }

    private void shakeWinPane() {
        winPane.setVisible(true);
        winPane.toFront();
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), winPane);
        shake.setFromX(0); shake.setByX(10);
        shake.setCycleCount(10); shake.setAutoReverse(true);
        shake.play();
    }

    private VBox createGameOverPane() {
        VBox pane = new VBox(20);
        pane.setAlignment(Pos.CENTER);
        pane.setStyle("-fx-background-color: rgba(20, 20, 20, 0.9); -fx-border-color: #D32F2F; -fx-border-width: 5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 40;");
        pane.setMaxSize(400, 300);
        Label lblOver = new Label("GAME OVER");
        lblOver.setStyle("-fx-font-size: 50px; -fx-font-weight: bold; -fx-text-fill: #FF5252;");
        Label lblMsg = new Label("Time's Up!");
        lblMsg.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");
        Button btnRetry = new Button("TRY AGAIN");
        btnRetry.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 16px; -fx-pref-width: 150;");
        btnRetry.setOnAction(e -> restartGame());
        pane.getChildren().addAll(lblOver, lblMsg, btnRetry);
        pane.setVisible(false);
        return pane;
    }

    private void shakeGameOver() {
        gameOverPane.setVisible(true);
        gameOverPane.toFront();
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), gameOverPane);
        shake.setFromX(0); shake.setByX(15);
        shake.setCycleCount(10); shake.setAutoReverse(true);
        shake.play();
    }

    private GridPane createGameBoard() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLS; col++) {
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);

                // --- إضافة التمييز البصري للفخاخ ---
                if (boardLogic.isCracked(col, row)) {
                    cell.setFill(Color.web("#3d2b1f")); // بني داكن يوحي بالهشاشة
                    cell.setStroke(Color.ORANGE);       // حدود برتقالية تحذيرية
                } else {
                    cell.setFill(Color.web("#2a2a2a"));
                    cell.setStroke(Color.web("#3a3a3a"));
                }
                // ---------------------------------

                cell.setStrokeType(StrokeType.INSIDE);
                grid.add(cell, col, row);
            }
        }
        return grid;
    }

    private void spawnInitialPieces() {
        for (int i = 0; i < 5; i++) {
            spawnNewPiece();
        }
    }

    private void spawnNewPiece() {
        spawnNewPiece(-1, -1);
    }

    private void spawnNewPiece(double preferredX, double preferredY) {
        // 1. إنشاء شكل القطعة العشوائي
        Pentomino tempPiece = ShapeData.createRandomPiece();
        List<int[]> randomCoords = tempPiece.getCoordinates();

        // 2. تحديد اللون بناءً على "السكن" المختار
        Color skinColor = availableSkins.get(activeSkin);

        // تعريف القطعة كـ effectively final لاستخدامها في الـ Lambda لاحقاً
        final Pentomino piece;

        // 3. ميكانيكية القطعة الذهبية
        if (Math.random() < 0.15) {
            piece = new Pentomino(randomCoords, Color.GOLD);
            piece.setEffect(new javafx.scene.effect.Glow(0.8));
        } else {
            piece = new Pentomino(randomCoords, skinColor);
        }

        // 4. نظام التموضع العشوائي وتجنب التداخل
        // 4. نظام التموضع العشوائي الموزون (لمنع التباعد الغريب)
        boolean overlap;
        int attempts = 0;
        double poolWidth = piecePool.getPrefWidth();

        do {
            overlap = false;
            // تقليل نطاق التوزيع ليكون داخل حدود المخزن بشكل متقارب
            double x = (preferredX != -1) ? preferredX : 5 + Math.random() * (poolWidth - 100);
            double y = (preferredY != -1) ? preferredY : 10 + Math.random() * 400;

            piece.setLayoutX(x);
            piece.setLayoutY(y);

            // فحص التداخل بمسافة أمان بسيطة (Bounds Padding)
            for (Node node : piecePool.getChildren()) {
                if (node instanceof Pentomino && node != piece) {
                    // استخدام مسافة أمان صغيرة جداً لتقريب الأشكال
                    if (piece.getBoundsInParent().intersects(node.getBoundsInParent())) {
                        overlap = true;
                        break;
                    }
                }
            }
            attempts++;
        } while (overlap && attempts < 30); // تقليل المحاولات لضمان عدم التعليق برمجياً

        // 5. تفعيل السحب والإضافة للمخزن (مرة واحدة فقط)
        enableDrag(piece);
        piecePool.getChildren().add(piece);

        // 6. نظام التلاشي (Fade Out) - لزيادة صعوبة اللعبة وتحفيز الربح
        double lifespan = 5 + Math.random() * 7;
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), piece);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(lifespan));

        fadeOut.setOnFinished(e -> {
            // التحقق مما إذا كانت القطعة لا تزال في المخزن ولم يسحبها اللاعب
            if (piecePool.getChildren().contains(piece)) {
                piecePool.getChildren().remove(piece);
                pieceTimers.remove(piece);
            }
        });

        pieceTimers.put(piece, fadeOut);
        fadeOut.play();
    }

    private void enableDrag(Pentomino piece) {
        piece.setOnMousePressed(e -> {
            clearHint();
            if (isPaused) return;
            piece.requestFocus();
            selectedPiece = piece;
            piece.toFront();

            if (pieceTimers.containsKey(piece)) {
                pieceTimers.get(piece).stop();
                piece.setOpacity(1.0);
                pieceTimers.remove(piece);
            }

            // تأثير نيون عند المسك
            piece.setEffect(new DropShadow(20, Color.CYAN));

            Point2D p = piece.localToScene(0, 0);
            if (piece.getParent() != null) {
                ((Pane) piece.getParent()).getChildren().remove(piece);
            }
            dragOverlay.getChildren().add(piece);

            Point2D localP = dragOverlay.sceneToLocal(p);
            piece.setLayoutX(localP.getX());
            piece.setLayoutY(localP.getY());

            mouseAnchorX = e.getSceneX();
            mouseAnchorY = e.getSceneY();
            initialPieceX = piece.getLayoutX();
            initialPieceY = piece.getLayoutY();

            if (piece.getUserData() != null) {
                int[] pos = (int[]) piece.getUserData();
                wasOnBoard = true;
                oldGridX = pos[0];
                oldGridY = pos[1];
                boardLogic.removePiece(piece, oldGridX, oldGridY);
            } else {
                wasOnBoard = false;
            }
        });
        piece.setOnMouseClicked(e -> {
            if (isPaused) return;

            // فقط دبل كليك
            if (e.getClickCount() == 2) {

                // إذا القطعة ليست على البورد، تجاهل
                if (piece.getUserData() == null) return;

                int[] pos = (int[]) piece.getUserData();
                int gx = pos[0];
                int gy = pos[1];

                // 1. حذفها من المنطق
                boardLogic.removePiece(piece, gx, gy);

                // 2. حذفها من طبقة البورد
                placedPiecesLayer.getChildren().remove(piece);

                // 3. إعادة ضبط البيانات
                piece.setUserData(null);
                piece.setEffect(null);
                piece.setScaleX(1.0);
                piece.setScaleY(1.0);

                // 4. إضافتها للـ Pool
                piecePool.getChildren().add(piece);
                reorderPool();

                SoundManager.playSound("click.wav");
                e.consume();
            }
        });

        piece.setOnMouseDragged(e -> {
            if (isPaused) return;
            piece.setLayoutX(initialPieceX + (e.getSceneX() - mouseAnchorX));
            piece.setLayoutY(initialPieceY + (e.getSceneY() - mouseAnchorY));
        });

        piece.setOnMouseReleased(e -> {
            clearHint();
            if (isPaused) return;

            piece.setEffect(null);
            piece.setScaleX(1.0);
            piece.setScaleY(1.0);

            // ✅ استخدم حدود القطعة الحقيقية بدل (0,0)
            var boundsScene = piece.localToScene(piece.getBoundsInLocal());
            Point2D topLeftInBoard = placedPiecesLayer.sceneToLocal(boundsScene.getMinX(), boundsScene.getMinY());

            // ✅ Snap لأقرب خانة (أفضل من floor و round)
            int gridX = (int) Math.floor((topLeftInBoard.getX() + TILE_SIZE / 2.0) / TILE_SIZE);
            int gridY = (int) Math.floor((topLeftInBoard.getY() + TILE_SIZE / 2.0) / TILE_SIZE);

            System.out.println("DEBUG: GridX=" + gridX + ", GridY=" + gridY);

            if (boardLogic.canPlacePiece(piece, gridX, gridY)) {
                boardLogic.placePiece(piece, gridX, gridY);

                dragOverlay.getChildren().remove(piece);
                if (!placedPiecesLayer.getChildren().contains(piece)) {
                    placedPiecesLayer.getChildren().add(piece);
                }

                piece.setLayoutX(gridX * TILE_SIZE);
                piece.setLayoutY(gridY * TILE_SIZE);
                piece.setTranslateX(0);
                piece.setTranslateY(0);

                piece.setUserData(new int[]{gridX, gridY});
                piece.toFront();

                SoundManager.playSound("click.wav");

                if (boardLogic.isLevelComplete()) handleLevelComplete();
                else if (!wasOnBoard) spawnNewPiece();

            } else {
                System.out.println("X فشل التثبيت!");

                applyShake(piece);
                SoundManager.playSound("Shake.wav");

                dragOverlay.getChildren().remove(piece);

                // ✅ إذا كانت القطعة أصلاً على البورد قبل السحب، رجعها لمكانها القديم
                if (wasOnBoard) {
                    boardLogic.placePiece(piece, oldGridX, oldGridY);

                    if (!placedPiecesLayer.getChildren().contains(piece)) {
                        placedPiecesLayer.getChildren().add(piece);
                    }
                    piece.setLayoutX(oldGridX * TILE_SIZE);
                    piece.setLayoutY(oldGridY * TILE_SIZE);
                    piece.setUserData(new int[]{oldGridX, oldGridY});
                } else {
                    // ✅ إذا كانت من المخزن رجعها للمخزن
                    if (!piecePool.getChildren().contains(piece)) {
                        piecePool.getChildren().add(piece);
                    }
                    piece.setUserData(null);
                    reorderPool();
                }

                wasOnBoard = false;
            }
        });

        piece.setOnScroll(ev -> {
            clearHint();
            if (isPaused) return;
            // صوت عند التدوير
            SoundManager.playSound("Rotate.wav");

            if (ev.getDeltaY() > 0) piece.rotate(); else piece.rotateLeft();

            // إذا كانت القطعة على البورد، نحدث منطق التصادم
            if (piece.getUserData() != null) {
                int[] pos = (int[]) piece.getUserData();
                boardLogic.removePiece(piece, pos[0], pos[1]);
                if (boardLogic.canPlacePiece(piece, pos[0], pos[1])) {
                    boardLogic.placePiece(piece, pos[0], pos[1]);
                } else {
                    // تدوير عكسي إذا المكان ما سمح بعد الدوران
                    if (ev.getDeltaY() > 0) piece.rotateLeft(); else piece.rotate();
                    boardLogic.placePiece(piece, pos[0], pos[1]);
                    applyShake(piece);
                }
            }
            ev.consume();
        });
    }

    private void handleKeyPress(KeyEvent event) {
        if (isPaused || selectedPiece == null) return;

        // --- نظام الغش للمطورين (W) ---
        if (event.getCode() == KeyCode.W) {
            handleDevWin();
            return;
        }

        // تحديد نوع الحركة
        boolean isRight = (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D);
        boolean isLeft = (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A);
        boolean isFlip = (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.SPACE);

        if (isRight || isLeft || isFlip) {
            // إذا كانت القطعة مثبتة على البورد (UserData ليس null)
            if (selectedPiece.getUserData() != null) {
                int[] pos = (int[]) selectedPiece.getUserData();
                int gx = pos[0];
                int gy = pos[1];

                // 1. حذف مؤقت من المنطق البرمجي للفحص
                boardLogic.removePiece(selectedPiece, gx, gy);

                // 2. تنفيذ الحركة
                if (isFlip) selectedPiece.flip();
                else if (isRight) selectedPiece.rotate();
                else selectedPiece.rotateLeft();

                // 3. التحقق: هل المكان لا يزال قانونياً بعد الدوران؟
                if (boardLogic.canPlacePiece(selectedPiece, gx, gy)) {
                    boardLogic.placePiece(selectedPiece, gx, gy);
                    // رفع القطعة للأمام لضمان عدم دخولها تحت البورد
                    selectedPiece.toFront();
                    if (boardLogic.isFull()) handleLevelComplete();
                } else {
                    // 4. تراجع عن الحركة إذا كان هناك اصطدام
                    if (isFlip) selectedPiece.flip();
                    else if (isRight) selectedPiece.rotateLeft();
                    else selectedPiece.rotate();

                    boardLogic.placePiece(selectedPiece, gx, gy);

                    // تنبيه: اهتزاز القطعة فقط (selectedPiece) وليس البورد (gameBoardView)
                    applyShake(selectedPiece);
                }
            } else {
                // إذا كانت القطعة في الـ Pool أو يتم سحبها حالياً
                if (isFlip) selectedPiece.flip();
                else if (isRight) selectedPiece.rotate();
                else selectedPiece.rotateLeft();
            }
        }
    }

    // دالة مساعدة لتنظيف كود الغش
    private void handleDevWin() {
        timeline.stop();
        if(poolRefiller != null) poolRefiller.stop();
        piecePool.setDisable(true);
        placedPiecesLayer.getChildren().clear();
        Color[] cheatColors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE, Color.YELLOW};
        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLS; col++) {
                Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
                rect.setFill(cheatColors[(row + col) % cheatColors.length]);
                rect.setStroke(Color.BLACK);
                rect.setX(col * CELL_SIZE); rect.setY(row * CELL_SIZE);
                placedPiecesLayer.getChildren().add(rect);
            }
        }
        timeLabel.setText("YOU WIN! (Dev)");
        checkAndSaveScore();
        celebrateWin();
        shakeWinPane();
    }

    private void handleWin() {
        timeline.stop();
        if(poolRefiller != null) poolRefiller.stop();
        SoundManager.playSound("Win.wav");
        explodeBoard(); // التأثير البصري

        currentLevel++; // رفع الليفل
        playerCoins += (currentScore / 10); // تحويل 10% من السكور لعملات
        savePlayerProgress();

        // إظهار رسالة ليفل جديد
        showComboText("LEVEL " + currentLevel + " REACHED!", WINDOW_WIDTH/2.0 - 100, WINDOW_HEIGHT/2.0);

        // انتظار ثانية واحدة ثم إعادة التشغيل بصعوبة أعلى
        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> {
            // تقليل الوقت المبدئي في كل ليفل لزيادة التحدي
            currentLevelTime = Math.max(40, currentLevelTime - 15);
            savePlayerProgress();
            restartGame();
        });
        pause.play();

        checkAndSaveScore();
        celebrateWin();
    }

    private HBox createControlPanel() {
        HBox panel = new HBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(10, 20, 10, 20));
        panel.setStyle("-fx-background-color: rgba(30, 30, 30, 0.9); -fx-border-color: #444; -fx-border-width: 0 0 2 0;");


        // 1. زر العودة للقائمة (QUIT) في البداية
        Button backToMenuBtn = new Button("QUIT");
        backToMenuBtn.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-font-weight: bold;");
        backToMenuBtn.setOnAction(e -> {
            if (timeline != null) timeline.stop();
            if (poolRefiller != null) poolRefiller.stop();
            mainStage.setScene(menuScene);
        });
        Button returnBtn = new Button("RETURN");
        returnBtn.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-font-weight: bold;");

        returnBtn.setOnAction(e -> {
            clearHint();

            if (selectedPiece == null) return;
            if (selectedPiece.getUserData() == null) return;

            int[] pos = (int[]) selectedPiece.getUserData();

            boardLogic.removePiece(selectedPiece, pos[0], pos[1]);
            placedPiecesLayer.getChildren().remove(selectedPiece);

            selectedPiece.setUserData(null);
            piecePool.getChildren().add(selectedPiece);
            reorderPool();

            SoundManager.playSound("click.wav");
        });

        panel.getChildren().add(returnBtn);

        // 2. المعلومات (Score, Best, Time)
        scoreLabel = new Label("SCORE: 0");
        scoreLabel.setStyle("-fx-text-fill: #00fbff; -fx-font-weight: bold; -fx-font-size: 18px; -fx-effect: dropshadow(gaussian, #00fbff, 10, 0.5, 0, 0);");

        bestScoreLabel = new Label("Best: --");
        bestScoreLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4DB8FF; -fx-font-weight: bold;");

        timeLabel = new Label("Time: " + currentLevelTime + "s");
        timeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        // 3. أزرار التحكم (Restart, Pause, Hint)
        Button restartBtn = new Button("Restart");
        restartBtn.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold;");
        restartBtn.setOnAction(e -> restartGame());

        pauseBtn = new Button("Pause");
        pauseBtn.setStyle("-fx-background-color: #F57C00; -fx-text-fill: white; -fx-font-weight: bold;");
        pauseBtn.setOnAction(e -> togglePause());

        hintBtn = new Button("Hint");
        hintBtn.setStyle("-fx-background-color: gold; -fx-text-fill: black; -fx-font-weight: bold;");
        hintBtn.setOnAction(e -> showSmartHint());

        // 4. زر المتجر الذهبي (SHOP) - تم وضعه في مكان مميز
        storeBtn = new Button("SHOP 🛒");
        storeBtn.setStyle("-fx-background-color: #FFD700; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        storeBtn.setOnAction(e -> showStore());

        // 5. زر المساعدة والضبط الصغير
        Button helpBtn = new Button("?");
        helpBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-shape: 'M 0 10 A 10 10 0 1 1 20 10 A 10 10 0 1 1 0 10';");
        helpBtn.setOnAction(e -> showHelpDialog());

        // إضافة الكل بالترتيب الصحيح
        panel.getChildren().addAll(backToMenuBtn, bestScoreLabel, scoreLabel, timeLabel, restartBtn, pauseBtn, hintBtn, storeBtn, helpBtn);

        return panel;
    }

    private void useHint() {
        // إثبات كفاءة: الخوارزمية ستحلل الوضع الحالي
        List<Pentomino> piecesInPool = new java.util.ArrayList<>();
        for (Node node : piecePool.getChildren()) {
            if (node instanceof Pentomino) piecesInPool.add((Pentomino) node);
        }

        if (boardLogic.solve(piecesInPool)) {
            // إذا وجد الـ AI حلاً، سيقوم بتنفيذ الخطوة الأولى منه
            // ملاحظة: ستحتاج لتخزين الخطوة الناجحة في متغير مؤقت داخل solve
            showComboText("AI ANALYZED: OPTIMAL MOVE FOUND", WINDOW_WIDTH/2.0, 100);
        } else {
            // إذا لم يجد حلاً، فهذا يعني أن اللاعب أخطأ في القطع السابقة
            applyShake(gameBoardView);
            showComboText("IMPOSSIBLE STATE: RESET RECOMMENDED", WINDOW_WIDTH/2.0, 100);
        }
    }

    private boolean tryToFitPieceLogicOnly(Pentomino piece) {
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int row = 0; row < BOARD_ROWS; row++) {
                for (int col = 0; col < BOARD_COLS; col++) {
                    if (boardLogic.canPlacePiece(piece, col, row)) {
                        lastFoundCol = col;
                        lastFoundRow = row;
                        return true;
                    }
                }
            }
            piece.rotate();
        }
        return false;
    }

    private int lastFoundCol = -1;
    private int lastFoundRow = -1;

    private boolean tryToFitPiece(Pentomino piece) {
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int row = 0; row < BOARD_ROWS; row++) {
                for (int col = 0; col < BOARD_COLS; col++) {
                    if (boardLogic.canPlacePiece(piece, col, row)) {
                        performAutoMove(piece, col, row);
                        return true;
                    }
                }
            }
            piece.rotate();
        }
        return false;
    }

    private void fillSmallestGap() {
        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLS; col++) {
                if (!boardLogic.isOccupied(col, row)) {
                    Pentomino filler = new Pentomino(java.util.Collections.singletonList(new int[]{0, 0}), Color.LIGHTGRAY);
                    filler.setOpacity(0.8);
                    boardLogic.placePiece(filler, col, row);
                    placedPiecesLayer.getChildren().add(filler);
                    filler.setLayoutX(col * CELL_SIZE);
                    filler.setLayoutY(row * CELL_SIZE);
                    if (boardLogic.isFull()) handleWin();
                    return;
                }
            }
        }
    }

    private void performAutoMove(Pentomino piece, int gridX, int gridY) {
        boardLogic.placePiece(piece, gridX, gridY);
        ((Pane) piece.getParent()).getChildren().remove(piece);
        placedPiecesLayer.getChildren().add(piece);
        piece.setLayoutX(gridX * CELL_SIZE);
        piece.setLayoutY(gridY * CELL_SIZE);
        piece.setUserData(new int[]{gridX, gridY});

        if(pieceTimers.containsKey(piece)) {
            pieceTimers.get(piece).stop();
            pieceTimers.remove(piece);
        }

        if (boardLogic.isFull()) {
            handleWin();
        } else {
            spawnNewPiece();
        }
    }

    private void celebrateWin() {
        Pane root = (Pane) gameBoardView.getScene().getRoot();
        Color[] colors = {Color.RED, Color.GOLD, Color.CYAN, Color.MAGENTA, Color.LIME};
        for (int i = 0; i < 100; i++) {
            Rectangle confetti = new Rectangle(10, 10);
            confetti.setFill(colors[(int) (Math.random() * colors.length)]);
            confetti.setTranslateX(Math.random() * root.getWidth());
            confetti.setTranslateY(-50);
            root.getChildren().add(confetti);
            TranslateTransition fall = new TranslateTransition(Duration.seconds(2 + Math.random() * 3), confetti);
            fall.setByY(root.getHeight() + 100);
            FadeTransition fade = new FadeTransition(Duration.seconds(3), confetti);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            ParallelTransition pt = new ParallelTransition(fall, fade);
            pt.play();
            pt.setOnFinished(e -> root.getChildren().remove(confetti));
        }
    }

    private void resetHighScore() {
        bestTime = Integer.MAX_VALUE;
        bestScoreLabel.setText("Best: --");
        File file = new File("highscore.txt");
        if (file.exists()) file.delete();
    }

    private void loadHighScore() {
        File file = new File("highscore.txt");
        if (file.exists()) {
            try (Scanner scanner = new Scanner(file)) {
                if (scanner.hasNextInt()) {
                    bestTime = scanner.nextInt();
                    if (bestScoreLabel != null) bestScoreLabel.setText("Best: " + bestTime + "s");
                }
            } catch (FileNotFoundException e) { e.printStackTrace(); }
        }
    }

    private void checkAndSaveScore() {
        int timeTaken = currentLevelTime - timeSeconds;
        if (timeTaken < bestTime) {
            bestTime = timeTaken;
            playerScores.put(playerName, bestTime); // تحديث القائمة

            applySmallBump(bestScoreLabel);
            bestScoreLabel.setText("Best (" + playerName + "): " + bestTime + "s");

            // حفظ جميع نتائج اللاعبين في الملف
            try (PrintWriter writer = new PrintWriter(new FileWriter("player_scores.txt"))) {
                for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
                    writer.println(entry.getKey() + ":" + entry.getValue());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void togglePause() {
        if (isPaused) {
            isPaused = false;
            timeline.play();
            if(poolRefiller != null) poolRefiller.play();
            piecePool.setDisable(false);
            placedPiecesLayer.setOpacity(1.0);
            pauseBtn.setText("Pause");
        } else {
            isPaused = true;
            timeline.pause();
            if(poolRefiller != null) poolRefiller.pause();
            piecePool.setDisable(true);
            placedPiecesLayer.setOpacity(0.3);
            pauseBtn.setText("Resume");
        }
    }

    private void showHelpDialog() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Instructions");
        alert.setHeaderText("How to Play");
        alert.setContentText("Drag pieces to the grid.\nUse Arrow keys to rotate.\nHint button is UNLIMITED!");
        alert.showAndWait();
    }

    private void restartGame() {
        clearHint();
        boardLogic.clear();
        piecePool.getChildren().clear();
        placedPiecesLayer.getChildren().clear();
        dragOverlay.getChildren().clear();
        pieceTimers.clear();

        isPaused = false;
        pauseBtn.setText("Pause");
        piecePool.setDisable(false);
        placedPiecesLayer.setOpacity(1.0);
        if(hintBtn != null) {
            hintBtn.setText("Hint");
            hintBtn.setDisable(false);
        }

        if(gameOverPane != null) gameOverPane.setVisible(false);
        if(winPane != null) winPane.setVisible(false);

        timeSeconds = currentLevelTime;
        startTimer();
        startPoolRefiller();
        spawnInitialPieces();
    }

    private void startTimer() {
        // داخل Timeline الخاص بالـ Timer
        if (timeSeconds <= 15) {
            timeLabel.setTextFill(Color.RED);
            // تأثير نبض (Pulse) للتايمر عندما يقل الوقت عن 15 ثانية
            ScaleTransition pulse = new ScaleTransition(Duration.millis(500), timeLabel);
            pulse.setToX(1.3); pulse.setToY(1.3);
            pulse.setCycleCount(2); pulse.setAutoReverse(true);
            pulse.play();
        }
        timeLabel.setText("Time: " + timeSeconds + "s");
        timeLabel.setTextFill(Color.WHITE);
        if (timeline != null) timeline.stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeSeconds--;
            timeLabel.setText("Time: " + timeSeconds + "s");
            if (timeSeconds <= 0) {
                timeline.stop();
                if(poolRefiller != null) poolRefiller.stop();
                timeLabel.setText("GAME OVER");
                timeLabel.setTextFill(Color.RED);
                piecePool.setDisable(true);
                shakeGameOver();
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
    // داخل كلاس Main.java
    private void applyShake(Node node) {
        SoundManager.playSound("Shake.wav");
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }
    private void applySmallBump(javafx.scene.Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.03);  st.setToY(1.03); // تكبير طفيف جداً
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }
    private void spawnParticles(double x, double y, Color color) {
        for (int i = 0; i < 15; i++) {
            final javafx.scene.shape.Circle p = new javafx.scene.shape.Circle(3, color);
            p.setTranslateX(x + (Math.random() * 50));
            p.setTranslateY(y + (Math.random() * 50));
            dragOverlay.getChildren().add(p);

            TranslateTransition move = new TranslateTransition(Duration.millis(500), p);
            move.setByX((Math.random() - 0.5) * 150);
            move.setByY((Math.random() - 0.5) * 150);

            FadeTransition fade = new FadeTransition(Duration.millis(500), p);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);

            ParallelTransition pt = new ParallelTransition(move, fade);
            pt.setOnFinished(e -> dragOverlay.getChildren().remove(p)); // حل خطأ Lambda
            pt.play();
        }
    }
    private void showComboText(String text, double x, double y) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: 'Arial Black'; -fx-font-size: 30px; -fx-text-fill: GOLD; -fx-effect: dropshadow(three-pass-box, black, 5, 0, 0, 0);");
        lbl.setLayoutX(x); lbl.setLayoutY(y - 50);
        dragOverlay.getChildren().add(lbl);

        TranslateTransition tt = new TranslateTransition(Duration.seconds(1), lbl);
        tt.setByY(-80);
        FadeTransition ft = new FadeTransition(Duration.seconds(1), lbl);
        ft.setFromValue(1.0); ft.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.setOnFinished(e -> dragOverlay.getChildren().remove(lbl));
        pt.play();
    }
    private void explodeBoard() {
        for (int i = 0; i < placedPiecesLayer.getChildren().size(); i++) {
            Node node = placedPiecesLayer.getChildren().get(i);

            // تأخير بسيط لكل قطعة لإعطاء تأثير متسلسل
            PauseTransition delay = new PauseTransition(Duration.millis(i * 50));
            delay.setOnFinished(e -> {
                spawnParticles(node.getLayoutX(), node.getLayoutY(), Color.WHITE);

                TranslateTransition tt = new TranslateTransition(Duration.seconds(0.8), node);
                tt.setByY(500); // سقوط سريع
                tt.setByX((Math.random() - 0.5) * 200);

                FadeTransition ft = new FadeTransition(Duration.seconds(0.8), node);
                ft.setToValue(0);

                new ParallelTransition(tt, ft).play();
            });
            delay.play();
        }
    }
    private void createFallingBackground(Pane root) {
        Timeline fallingLogic = new Timeline(new KeyFrame(Duration.seconds(0.8), e -> {
            // إنشاء قطعة عشوائية للعرض فقط
            Pentomino p = ShapeData.createRandomPiece();
            p.setOpacity(0.2); // شفافية منخفضة لكي لا تشوش على الأزرار
            p.setScaleX(0.8); p.setScaleY(0.8);

            double startX = Math.random() * WINDOW_WIDTH;
            p.setLayoutX(startX);
            p.setLayoutY(-100);
            root.getChildren().add(0, p); // إضافتها في الخلف (Index 0)

            // أنيميشن السقوط والدوران
            TranslateTransition fall = new TranslateTransition(Duration.seconds(5 + Math.random() * 5), p);
            fall.setToY(WINDOW_HEIGHT + 100);

            RotateTransition rot = new RotateTransition(Duration.seconds(5), p);
            rot.setByAngle(360);

            ParallelTransition pt = new ParallelTransition(fall, rot);
            pt.setOnFinished(evt -> root.getChildren().remove(p));
            pt.play();
        }));
        fallingLogic.setCycleCount(Animation.INDEFINITE);
        fallingLogic.play();
    }
    // دالة الـ Slow-motion Game Over في Main.java
    private void triggerProGameOver() {
        dragOverlay.setEffect(new javafx.scene.effect.GaussianBlur(15));
        VBox ui = new VBox(20);
        ui.setAlignment(Pos.CENTER);
        ui.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 50;");

        Label msg = new Label("SYSTEM CRASHED");
        msg.setStyle("-fx-text-fill: red; -fx-font-size: 40px; -fx-font-weight: bold;");

        Button restart = createSilverButton("REBOOT");
        restart.setOnAction(e -> restartGame());

        ui.getChildren().addAll(msg, restart);
        rootStack.getChildren().add(ui);
    }
    private void spawnSmokeParticles(double x, double y) {
        for (int i = 0; i < 8; i++) {
            final Rectangle smoke = new Rectangle(5, 5, Color.DARKSLATEGRAY);
            smoke.setLayoutX(x + 20); smoke.setLayoutY(y + 20);
            dragOverlay.getChildren().add(smoke);

            TranslateTransition move = new TranslateTransition(Duration.millis(800), smoke);
            move.setByY(-50 - Math.random() * 50);
            move.setByX((Math.random() - 0.5) * 40);

            FadeTransition fade = new FadeTransition(Duration.millis(800), smoke);
            fade.setToValue(0);

            ParallelTransition pt = new ParallelTransition(move, fade);
            pt.setOnFinished(e -> dragOverlay.getChildren().remove(smoke));
            pt.play();
        }

    }
    private void loadHighScoreForPlayer(String name) {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");
                if (parts.length >= 2 && parts[0].equals(name)) {
                    bestTime = Integer.parseInt(parts[1]);
                    if (parts.length == 3) {
                        this.playerCoins = Integer.parseInt(parts[2]); // تحميل العملات
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showLoginDialog() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("Player 1");
        dialog.setTitle("Login System");
        dialog.setHeaderText("Welcome to Pentomino Pro");
        dialog.setContentText("Please enter your name:");

        // الانتظار حتى يدخل اللاعب اسمه
        dialog.showAndWait().ifPresent(name -> {
            this.playerName = name;
            loadHighScoreForPlayer(name); // تحميل سكور هذا اللاعب تحديداً
        });
    }
    @Override
    public void stop() {
        // إيقاف جميع التايمرز عند إغلاق النافذة لمنع استهلاك المعالج في الخلفية
        if (timeline != null) timeline.stop();
        if (poolRefiller != null) poolRefiller.stop();
        System.out.println("Cleaning up resources... Game closed safely.");
    }
    private void showAdForBonus() {
        // محاكاة لطلب إعلان (Ad Request)
        System.out.println("Requesting Ad from AdMob/Unity Ads...");

        // بعد انتهاء الإعلان:
        timeSeconds += 30; // مكافأة اللاعب
        showComboText("AD REWARD: +30 SEC", WINDOW_WIDTH/2, WINDOW_HEIGHT/2);
    }
    private void savePlayerProgress() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("player_data.txt"))) {
            // حفظ اسم اللاعب والسكور والعملات
            writer.println(playerName + ":" + bestTime + ":" + playerCoins);
            System.out.println("Progress Saved!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleLevelComplete() {
        SoundManager.playSound("Win.wav");

        rewardLevelCompletion();   // ✅ هنا الحل

        showEndLevelOverlay();     // overlay فيه زر Next
    }
    private void startNextLevel() {
        currentLevel++;
        boardLogic.loadLevel(currentLevel);
        updateBoardUI();

        currentLevelTime = Math.max(30, currentLevelTime - 10);
        savePlayerProgress();
        restartGame();
    }
    // أضف هذه الدالة في نهاية كلاس Main قبل آخر قوس }
    private void showStore() {
        Stage storeStage = new Stage();
        VBox storeLayout = new VBox(20);
        storeLayout.setPadding(new Insets(30));
        storeLayout.setAlignment(Pos.CENTER);
        storeLayout.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #00fbff; -fx-border-width: 2;");

        Label title = new Label("SKIN SHOP");
        title.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-weight: bold;");

        Label coinsLabel = new Label("YOUR COINS: " + playerCoins);
        coinsLabel.setStyle("-fx-text-fill: gold; -fx-font-size: 18px;");

        VBox skinsList = new VBox(10);
        for (String skinName : availableSkins.keySet()) {
            HBox skinRow = new HBox(20);
            skinRow.setAlignment(Pos.CENTER_LEFT);

            Rectangle preview = new Rectangle(30, 30, availableSkins.get(skinName));
            Label name = new Label(skinName);
            name.setStyle("-fx-text-fill: white; -fx-pref-width: 120;");

            Button actionBtn = new Button();
            if (ownedSkins.contains(skinName)) {
                actionBtn.setText(activeSkin.equals(skinName) ? "EQUIPPED" : "EQUIP");
                actionBtn.setDisable(activeSkin.equals(skinName));
                actionBtn.setOnAction(e -> { activeSkin = skinName; storeStage.close(); });
            } else {
                int price = 500;
                actionBtn.setText("BUY (" + price + ")");
                actionBtn.setOnAction(e -> {
                    if (playerCoins >= price) {
                        playerCoins -= price;
                        ownedSkins.add(skinName);
                        savePlayerProgress();
                        storeStage.close(); // إغلاق لإعادة التحديث
                        showStore();
                    } else {
                        applyShake(actionBtn);
                    }
                });
            }
            skinRow.getChildren().addAll(preview, name, actionBtn);
            skinsList.getChildren().add(skinRow);
        }

        Button closeBtn = new Button("CLOSE");
        closeBtn.setOnAction(e -> storeStage.close());

        storeLayout.getChildren().addAll(title, coinsLabel, skinsList, closeBtn);
        storeStage.setScene(new Scene(storeLayout, 400, 500));
        storeStage.show();
    }
    private void executeRotation(boolean right) {
        if (selectedPiece == null) return;
        SoundManager.playSound("Rotate.wav");

        // إذا كانت القطعة على البورد، نحذفها مؤقتاً للفحص
        boolean isOnBoard = (selectedPiece.getUserData() != null);
        int[] pos = isOnBoard ? (int[]) selectedPiece.getUserData() : null;

        if (isOnBoard) boardLogic.removePiece(selectedPiece, pos[0], pos[1]);

        if (right) selectedPiece.rotate();
        else selectedPiece.rotateLeft();

        if (isOnBoard) {
            if (boardLogic.canPlacePiece(selectedPiece, pos[0], pos[1])) {
                boardLogic.placePiece(selectedPiece, pos[0], pos[1]);
            } else {
                // تراجع فوري إذا لم تتوفر مساحة
                if (right) selectedPiece.rotateLeft();
                else selectedPiece.rotate();
                boardLogic.placePiece(selectedPiece, pos[0], pos[1]);
                applyShake(selectedPiece);
            }
        }
    }

    private void executeFlip() {
        if (selectedPiece == null) return;

        boolean isOnBoard = (selectedPiece.getUserData() != null);
        int[] pos = isOnBoard ? (int[]) selectedPiece.getUserData() : null;

        if (isOnBoard) boardLogic.removePiece(selectedPiece, pos[0], pos[1]);

        selectedPiece.flip();

        if (isOnBoard) {
            if (boardLogic.canPlacePiece(selectedPiece, pos[0], pos[1])) {
                boardLogic.placePiece(selectedPiece, pos[0], pos[1]);
            } else {
                selectedPiece.flip(); // تراجع
                boardLogic.placePiece(selectedPiece, pos[0], pos[1]);
                applyShake(selectedPiece);
            }
        }
    }
    private void reorderPool() {
        int count = 0;
        for (Node node : piecePool.getChildren()) {
            if (node instanceof Pentomino) {
                int row = count / 2;
                int col = count % 2;

                double targetX = 20 + (col * 110);
                double targetY = 30 + (row * 130);

                // إضافة أنيميشن بسيط للانتقال (اختياري)
                node.setLayoutX(targetX);
                node.setLayoutY(targetY);
                count++;
            }
        }
    }
    // قائمة الألوان المتاحة للشراء (Skin Name : Color : Price)
    private void buyTimeFreeze() {
        if (playerCoins >= 150) {
            playerCoins -= 150;
            isPaused = true; // نوقف المنطق
            timeline.pause(); // نوقف العداد

            // نرجعه يشتغل بعد 10 ثواني تلقائياً
            PauseTransition delay = new PauseTransition(Duration.seconds(10));
            delay.setOnFinished(e -> {
                isPaused = false;
                timeline.play();
                showComboText("TIME RESUMED!", WINDOW_WIDTH/2, 100);
            });
            delay.play();
            showComboText("TIME FROZEN FOR 10s!", WINDOW_WIDTH/2, 100);
            savePlayerProgress();
        } else {
            applyShake(storeBtn); // هز الزر إذا ما عنده فلوس
        }
    }

    private void playGameSound(String soundFile) {
        try {
            java.net.URL url = getClass().getResource("/sounds/" + soundFile);
            if (url != null) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } else {
                System.out.println("الصوت غير موجود في المسار المحدد: " + soundFile);
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
    private void updateBoardUI() {
        // 1. نمسح كل المربعات القديمة من واجهة البورد (الـ Group أو الـ Pane المسؤول)
        boardGroup.getChildren().clear();

        // 2. نعيد رسم المربعات بناءً على الخريطة الجديدة لليفل
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);

                // إذا كان المربع في الخريطة قيمته 0 يعني "محظور"
                if (boardLogic.getCurrentMap()[row][col] == 0) {
                    rect.setFill(Color.BLACK); // لون المربع المحظور
                    rect.setStroke(Color.web("#333333")); // حدود خفيفة
                } else {
                    rect.setFill(Color.TRANSPARENT); // مربع فاضي جاهز للعب
                    rect.setStroke(Color.web("#444444")); // حدود النيون
                }

                rect.setTranslateX(col * TILE_SIZE);
                rect.setTranslateY(row * TILE_SIZE);
                boardGroup.getChildren().add(rect);
            }
        }
    }
    private Pane createInstructionsOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.75);");
        overlay.setPickOnBounds(true);

        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(600);

        Label title = new Label("HOW TO PLAY");
        title.setStyle("""
        -fx-font-size: 40px;
        -fx-font-weight: bold;
        -fx-text-fill: cyan;
        -fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);
    """);

        Label text = new Label("""
🧩 OBJECTIVE
Fill the entire grid using the given pieces.

 HOW TO PLAY
                
• Drag pieces to fill the grid before time runs out
• A / D / Arrows rotate, Space flips the selected piece
• ↩ Return to Pool sends the selected piece back
• P pause, R restart
""");

        text.setStyle("""
        -fx-font-size: 16px;
        -fx-text-fill: white;
    """);
        text.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button playBtn = createSilverButton("PLAY NOW");
        playBtn.setOnAction(e -> {
            overlay.setVisible(false);
            placedPiecesLayer.setDisable(false);
            piecePool.setDisable(false);
            startTimer(); // يبدأ العداد فعليًا هنا
        });

        box.getChildren().addAll(title, text, playBtn);
        overlay.getChildren().add(box);

        return overlay;
    }
    private void showSmartHint() {
        if (selectedPiece == null) return;

        clearHint();

        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLS; col++) {

                if (boardLogic.canPlacePiece(selectedPiece, col, row)) {

                    hintOverlay = new Group();

                    for (int[] p : selectedPiece.getCoordinates()) {
                        Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
                        cell.setFill(Color.color(0, 1, 0, 0.18));
                        cell.setStroke(Color.LIME);
                        cell.setStrokeWidth(2);

                        cell.setLayoutX((col + p[0]) * CELL_SIZE);
                        cell.setLayoutY((row + p[1]) * CELL_SIZE);

                        hintOverlay.getChildren().add(cell);
                    }

                    placedPiecesLayer.getChildren().add(hintOverlay);

                    hintTimer = new Timeline(
                            new KeyFrame(Duration.seconds(2), e -> clearHint())
                    );
                    hintTimer.play();

                    return;
                }
            }
        }

        applyShake(gameBoardView);
    }
    private void clearHint() {
        if (hintTimer != null) {
            hintTimer.stop();
            hintTimer = null;
        }
        if (hintOverlay != null) {
            placedPiecesLayer.getChildren().remove(hintOverlay);
            hintOverlay = null;
        }
    }
    private VBox createEndLevelOverlay() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
                "-fx-background-color: rgba(0,0,0,0.85);" +
                        "-fx-border-color: #00E676;" +
                        "-fx-border-width: 4;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;" +
                        "-fx-padding: 30;"
        );
        box.setMaxSize(380, 320);

        Label title = new Label("LEVEL COMPLETE ✔");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #00E676;");

        Label timeLbl = new Label();
        timeLbl.setId("endTime");
        timeLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

        Label scoreLbl = new Label();
        scoreLbl.setId("endScore");
        scoreLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

        Label coinsLbl = new Label();
        coinsLbl.setId("endCoins");
        coinsLbl.setStyle("-fx-text-fill: gold; -fx-font-size: 18px; -fx-font-weight: bold;");

        Button nextBtn = new Button("NEXT LEVEL");
        nextBtn.setStyle("-fx-background-color: #00E676; -fx-text-fill: black; -fx-font-weight: bold;");
        nextBtn.setOnAction(e -> {
            endLevelOverlay.setVisible(false); // ✅ أخفِ الشاشة أولاً
            startNextLevel();                  // ✅ ثم ابدأ الليفل الجديد
        });

        Button retryBtn = new Button("RETRY");
        retryBtn.setStyle("-fx-background-color: #BDBDBD; -fx-text-fill: black; -fx-font-weight: bold;");
        retryBtn.setOnAction(e -> {
            clearEndOverlay();
            restartGame();
        });

        box.getChildren().addAll(title, timeLbl, scoreLbl, coinsLbl, nextBtn, retryBtn);
        box.setVisible(false);

        return box;
    }
    private void showEndLevelOverlay() {
        clearHint();

        Label timeLbl = (Label) endLevelOverlay.lookup("#endTime");
        Label scoreLbl = (Label) endLevelOverlay.lookup("#endScore");
        Label coinsLbl = (Label) endLevelOverlay.lookup("#endCoins");

        int timeUsed = currentLevelTime - timeSeconds;
        int coinsEarned = (timeSeconds * 2) + 50;

        timeLbl.setText("Time: " + timeUsed + "s");
        scoreLbl.setText("Score: " + currentScore);
        coinsLbl.setText("Coins: +" + coinsEarned);

        endLevelOverlay.setScaleX(0.8);
        endLevelOverlay.setScaleY(0.8);
        endLevelOverlay.setOpacity(0);
        endLevelOverlay.setVisible(true);
        endLevelOverlay.toFront();

        FadeTransition fade = new FadeTransition(Duration.millis(300), endLevelOverlay);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), endLevelOverlay);
        scale.setToX(1);
        scale.setToY(1);

        new ParallelTransition(fade, scale).play();
    }
    private void clearEndOverlay() {
        if (endLevelOverlay != null) {
            endLevelOverlay.setVisible(false);
        }
    }
    private void returnPieceToPool(Pentomino piece) {
        if (piece.getUserData() != null) {
            int[] pos = (int[]) piece.getUserData();
            boardLogic.removePiece(piece, pos[0], pos[1]);
            piece.setUserData(null);
        }
        placedPiecesLayer.getChildren().remove(piece);
        piecePool.getChildren().add(piece);
        reorderPool();
    }





    public static void main(String[] args) {
        launch(args);
    }
}