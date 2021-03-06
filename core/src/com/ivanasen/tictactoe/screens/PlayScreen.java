package com.ivanasen.tictactoe.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.ivanasen.tictactoe.Board;
import com.ivanasen.tictactoe.Constants;
import com.ivanasen.tictactoe.TicTacToeMain;
import com.ivanasen.tictactoe.sprites.Scoreboard;

import java.util.Stack;

public abstract class PlayScreen implements Screen, InputProcessor {

    private final TicTacToeMain game;
    private Scoreboard scoreboard;
    Image[][] gameCells;
    Table table;
    private Stage stage;
    private Vector2 gameGridPosition;
    private Image gameGrid;
    private Container<Table> tableContainer;
    private boolean tableNotAdded;
    private boolean shouldResize;
    private boolean animated;
    private Image undoBtn;
    Stack<Vector2> moves;
    boolean aiOnTurn;
    int turnCount;
    Board board;
    private Texture transparentTexture;
    private Image restartBtn;
    private Sprite backgroundSprite;
    Texture circleTurn;
    Texture crossTurn;
    private Image mainMenuBtn;
    Image playerTurnCell;
    boolean gameEnded;
    Table gridTable;
    private Viewport viewport;

    PlayScreen(TicTacToeMain game) {
        this.game = game;
    }

    @Override
    public void show() {
        viewport = new StretchViewport(Constants.V_WIDTH, Constants.V_HEIGHT);
        stage = new Stage(viewport);

        InputMultiplexer multiplexer = new InputMultiplexer(this, stage);
        Gdx.input.setInputProcessor(multiplexer);

        board = new Board();

        table = new Table();
        table.setFillParent(true);
        gridTable = new Table();
        tableContainer = new Container<>(gridTable);
        scoreboard = new Scoreboard(this);
        moves = new Stack<>();

        initGridVars();
        initGameTrackingVars();
        loadImages();

        createMainMenuButton();
        createUndoButton();
        createRestartButton();
        createHintForPlayerTurn();
        updateGameGridSize();
        createScoreBoard();
        createGameGridCells();

        stage.addActor(gameGrid);
        stage.addActor(table);
    }


    private void initGridVars() {
        animated = false;
        tableNotAdded = true;
        gameGridPosition = new Vector2();
        shouldResize = false;
    }

    abstract void createHintForPlayerTurn();

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        backgroundSprite.draw(game.batch);
        game.batch.end();

        stage.draw();
    }

    protected void update(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.BACK)) {
            game.setScreen(new MainMenu(game));
        }

        if (!animated) {
            playEnterAnimation();
            animated = true;
        }

        backgroundSprite.setSize(viewport.getScreenWidth(), viewport.getScreenHeight());
        stage.act(delta);
    }

    private void playEnterAnimation() {
        scoreboard.getColor().a = 0;
        gameGrid.getColor().a = 0;
        restartBtn.getColor().a = 0;
        mainMenuBtn.getColor().a = 0;
        playerTurnCell.getColor().a = 0;
        undoBtn.getColor().a = 0;

        gameGrid.addAction(Actions.fadeIn(Constants.Animations.BASE_ANIMATION_DURATION));
        restartBtn.addAction(Actions.fadeIn(Constants.Animations.BASE_ANIMATION_DURATION));
        mainMenuBtn.addAction(Actions.fadeIn(Constants.Animations.BASE_ANIMATION_DURATION));
        playerTurnCell.addAction(Actions.fadeIn(Constants.Animations.BASE_ANIMATION_DURATION));
        undoBtn.addAction(Actions.fadeIn(Constants.Animations.BASE_ANIMATION_DURATION));
        scoreboard.addAction(Actions.parallel(Actions.moveTo(0, 0,
                Constants.Animations.BASE_ANIMATION_DURATION, Interpolation.exp10Out),
                Actions.fadeIn(Constants.Animations.BASE_ANIMATION_DURATION)));
    }

    @Override
    public void resize(int width, int height) {
        if (shouldResize) {
            stage.getCamera().update();
            updateGameGridSize();
        } else {
            shouldResize = true;
        }
    }

    private void declareGameEnded(Board.CellState cellState) {
        if (cellState == Board.CellState.CIRCLE) {
            scoreboard.setCircleWon();
        } else if (cellState == Board.CellState.CROSS) {
            scoreboard.setCrossWon();
        }

        game.actionResolver.checkIfShouldShowAd();
        playRestartAnimation();
    }


    private void playRestartAnimation() {
        for (int i = 0; i < gameCells.length; i++) {
            Image[] gameCell = gameCells[i];
            for (int j = 0; j < gameCell.length; j++) {
                if (j == gameCell.length - 1 && i == gameCell.length - 1) {
                    gameCell[j].addAction(Actions.sequence(
                            Actions.fadeOut(Constants.Animations.RESTART_ANIM_DURATION),
                            Actions.run(new Runnable() {
                                @Override
                                public void run() {
                                    restartGame();
                                }
                            })));
                } else {
                    gameCell[j].addAction(Actions.fadeOut(Constants.Animations.FADE_ANIM_DURATION));
                }
            }
        }
    }

    private void updateGameGridSize() {
        float viewportWidth = stage.getCamera().viewportWidth;
        float viewportHeight = stage.getCamera().viewportHeight;
        float gameGridSize = Constants.PlayscreenEntry.getGameGridSize(viewportWidth);
        gameGridPosition.x = (viewportWidth - gameGridSize) / 2f
                - Constants.PlayscreenEntry.GAME_GRID_PADDING_RIGHT;
        gameGridPosition.y = (viewportHeight - gameGridSize) / 2f
                + Constants.PlayscreenEntry.GAME_GRID_IMG_PADDING;

        gameGrid.setSize(gameGridSize, gameGridSize);

        if (tableNotAdded) {
            table.row();
            table.add(tableContainer).width(gameGridSize).height(gameGridSize)
                    .colspan(Constants.PlayscreenEntry.GAME_COLS).expand().center();
            tableNotAdded = false;
        } else {
            table.getCell(tableContainer).width(gameGridSize).height(gameGridSize)
                    .expand().center();
        }

        gameGrid.setPosition(gameGridPosition.x, gameGridPosition.y);
    }

    private void createUndoButton() {
        Texture btnTexture = new Texture(Constants.FileDirectories.UNDO_BTN_IMG);
        btnTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        undoBtn = new Image(btnTexture);
        undoBtn.setTouchable(Touchable.enabled);

        undoBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                undoBtn.getColor().a = Constants.PlayscreenEntry.BTN_COLOR_A_WHEN_PRESSED;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                undoBtn.getColor().a = 1;
                undoLastMove();
            }
        });

        undoBtn.setScale(Constants.PlayscreenEntry.BTN_SCALE);
        table.add(undoBtn).padTop(Constants.PlayscreenEntry.UNDO_BUTTON_PADDING)
                .padLeft(Constants.PlayscreenEntry.UNDO_BUTTON_PADDING).top().right().expandX();
    }

    void clearCell(int i, int j) {
        board.clearCell(i, j);
        gameCells[i][j].setDrawable(new SpriteDrawable(new Sprite(transparentTexture)));
        gameCells[i][j].setTouchable(Touchable.enabled);
    }


    private void createScoreBoard() {
        table.row();
        table.add(scoreboard).colspan(Constants.PlayscreenEntry.GAME_COLS).bottom().expandX();
    }

    private void createRestartButton() {
        Texture btnTexture = new Texture(Constants.FileDirectories.RESTART_BTN_IMG);
        btnTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        restartBtn = new Image(btnTexture);
        restartBtn.setTouchable(Touchable.enabled);

        restartBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                restartBtn.getColor().a = Constants.PlayscreenEntry.BTN_COLOR_A_WHEN_PRESSED;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                restartBtn.getColor().a = 1;
                playRestartAnimation();
                scoreboard.restart();
            }
        });

        table.add(restartBtn).pad(Constants.PlayscreenEntry.BUTTON_PADDING).top().right().expandX();
        table.row();
    }

    private void loadImages() {
        backgroundSprite = new Sprite(new Texture(Constants.FileDirectories.BACKGROUND_IMG));

        Texture grid = new Texture(Constants.FileDirectories.GAME_GRID_IMG);
        grid.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        gameGrid = new Image(grid);
        gameCells = new Image[Constants.PlayscreenEntry.GRID_COUNT][Constants.PlayscreenEntry.GRID_COUNT];
        transparentTexture = new Texture(Constants.FileDirectories.TRANSPARENT_IMG);

        circleTurn = new Texture(Constants.FileDirectories.CIRCLE_TURN_IMG);
        crossTurn = new Texture(Constants.FileDirectories.CROSS_TURN_IMG);
        circleTurn.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        crossTurn.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private void createMainMenuButton() {
        Texture btnTexture = new Texture(Constants.FileDirectories.MAIN_MENU_BTN_IMG);
        btnTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        mainMenuBtn = new Image(btnTexture);
        mainMenuBtn.setTouchable(Touchable.enabled);

        mainMenuBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                mainMenuBtn.getColor().a = Constants.PlayscreenEntry.BTN_COLOR_A_WHEN_PRESSED;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                super.touchUp(event, x, y, pointer, button);
                playExitAnimation();
            }
        });

        table.add(mainMenuBtn).padLeft(Constants.PlayscreenEntry.BUTTON_PADDING).top().left().expandX();
    }

    private void playExitAnimation() {
        Action backToMainMenu = Actions.run(new Runnable() {
            @Override
            public void run() {
                game.setScreen(new MainMenu(game));
                dispose();
            }
        });

        for (Image[] gameCell : gameCells) {
            for (Image image : gameCell) {
                image.addAction(Actions.fadeOut(Constants.Animations.FADE_ANIM_DURATION));
            }
        }
        mainMenuBtn.addAction(Actions.sequence(
                Actions.fadeOut(Constants.Animations.FADE_ANIM_DURATION), backToMainMenu));
        gameGrid.addAction(Actions.fadeOut(Constants.Animations.FADE_ANIM_DURATION));
        restartBtn.addAction(Actions.fadeOut(Constants.Animations.FADE_ANIM_DURATION));
        playerTurnCell.addAction(Actions.fadeOut(Constants.Animations.FADE_ANIM_DURATION));
        undoBtn.addAction(Actions.fadeOut(Constants.Animations.FADE_ANIM_DURATION));

        scoreboard.addAction(Actions.parallel(
                Actions.moveTo(0,
                        -scoreboard.getHeight(),
                        Constants.Animations.BASE_ANIMATION_DURATION,
                        Interpolation.linear),
                Actions.fadeOut(Constants.Animations.FADE_ANIM_DURATION)));
    }

    public void checkIfGameHasEnded() {
        int boardSize = board.getSize();
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Board.CellState currentCell = board.getCell(row, col);

                if (currentCell == Board.CellState.BLANK || currentCell == null)
                    continue;

                for (int i = 0; i < boardSize; i++) {
                    if (board.getCell(row, i) != currentCell)
                        break;
                    if (i == boardSize - 1) {
                        gameEnded = true;
                        declareGameEnded(currentCell);
                        return;
                    }
                }

                for (int i = 0; i < boardSize; i++) {
                    if (board.getCell(i, col) != currentCell)
                        break;
                    if (i == boardSize - 1) {
                        gameEnded = true;
                        declareGameEnded(currentCell);
                        return;
                    }
                }

                if (row == col) {
                    for (int i = 0; i < boardSize; i++) {
                        if (board.getCell(i, i) != currentCell)
                            break;
                        if (i == boardSize - 1) {
                            gameEnded = true;
                            declareGameEnded(currentCell);
                            return;
                        }
                    }
                }

                for (int i = 0; i < boardSize; i++) {
                    if (board.getCell(i, board.getSize() - i - 1) != currentCell)
                        break;
                    if (i == boardSize - 1) {
                        gameEnded = true;
                        declareGameEnded(currentCell);
                        return;
                    }
                }
            }
        }

        if (turnCount >= 9) {
            declareGameEnded(Board.CellState.BLANK);
        }
    }

    protected void initGameTrackingVars() {
        board.init();
        turnCount = 0;
        gameEnded = false;
        moves.clear();
    }

    private void createGameGridCells() {
        board.init();
        for (int i = 0; i < gameCells.length; i++) {
            gridTable.row();
            for (int j = 0; j < gameCells[0].length; j++) {
                gameCells[i][j] = new Image(transparentTexture);
                gridTable.add(gameCells[i][j]).fill().expand();

                final int finalI = i;
                final int finalJ = j;
                gameCells[i][j].addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        super.clicked(event, x, y);
                        processPlayerTurn(finalI, finalJ);
                    }
                });
            }
        }
    }

    public void restartGame() {
        initGameTrackingVars();
        gridTable.clear();
        createHintForPlayerTurn();
        createGameGridCells();
    }

    protected abstract void processPlayerTurn(int finalI, int finalJ);

    abstract void drawCurrentPlayerSymbol(Image image);

    abstract void undoLastMove();

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        transparentTexture.dispose();
        scoreboard.dispose();
        circleTurn.dispose();
        crossTurn.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.BACK) {
            dispose();
            game.setScreen(new MainMenu(game));
        }

        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

}
