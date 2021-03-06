/*
 * Copyright 2017 riddles.io (developers@riddles.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     For the full copyright and license information, please view the LICENSE
 *     file that was distributed with this source code.
 */

package ufrn.alvarofpp.field;

import java.awt.*;
import java.util.ArrayList;

import ufrn.alvarofpp.field.grid.Grid;
import ufrn.alvarofpp.field.grid.cell.Cell;
import ufrn.alvarofpp.move.MoveType;
import ufrn.alvarofpp.move.pathfinding.DistributeInfluence;
import ufrn.alvarofpp.move.pathfinding.InfluenceType;
import ufrn.alvarofpp.move.pathfinding.MapInfluence;

/**
 * field.Field
 * <p>
 * Stores all information about the playing field and
 * contains methods to perform calculations about the field
 *
 * @author Jim van Eeden - jim@riddles.io
 */
public class Field {

    private String myId;
    private String opponentId;
    private int width;
    private int height;

    private Grid grid;
    private Point myPosition;
    private Point opponentPosition;
    private ArrayList<Point> enemyPositions;
    private ArrayList<Point> snippetPositions;
    private ArrayList<Point> bombPositions;
    private ArrayList<Point> tickingBombPositions;

    private DistributeInfluence distributeInfluence;
    private ArrayList<BugType> enemyTypes;

    public Field() {
        this.enemyPositions = new ArrayList<>();
        this.snippetPositions = new ArrayList<>();
        this.bombPositions = new ArrayList<>();
        this.tickingBombPositions = new ArrayList<>();

        this.enemyTypes = new ArrayList<>();
    }

    /**
     * Initializes field
     *
     * @throws Exception: exception
     */
    public void initField() throws Exception {
        try {
            if (this.grid == null) {
                this.grid = new Grid(this.width, this.height);
            }
        } catch (Exception e) {
            throw new Exception("Error: trying to initialize field while field "
                    + "settings have not been parsed yet.");
        }
        clearField();
    }

    /**
     * Clears the field
     */
    private void clearField() {
        this.myPosition = null;
        this.opponentPosition = null;
        this.enemyPositions.clear();
        this.snippetPositions.clear();
        this.bombPositions.clear();
        this.tickingBombPositions.clear();

        this.enemyTypes.clear();
    }

    /**
     * Parses input string from the engine and stores it in
     * this.field. Also stores several interesting points.
     *
     * @param input String input from the engine
     */
    public void parseFromString(String input) {
        clearField();

        String[] cells = input.split(",");

        // Define a malha caso ela ainda não tenha sido definida
        if (!this.grid.isDefined()) {
            this.grid.define(cells);
        } else {
            this.grid.clearDangerLaser();
            this.grid.clearSpawn();
        }

        int x = 0;
        int y = 0;

        for (String cell : cells) {
            Cell cellGrid = this.grid.getCell(x, y);
            // Quando é uma celula que pode ser atualizada os valores
            if (cellGrid.isPassable()) {
                for (String cellPart : cell.split(";")) {
                    switch (cellPart.charAt(0)) {
                        case 'P':
                            parsePlayerCell(cellPart.charAt(1), x, y);
                            break;
                        case 'e':
                            // TODO: store spawn points
                            break;
                        case 'E':
                            parseEnemyCell(cellPart.charAt(1), x, y);
                            break;
                        case 'B':
                            parseBombCell(cellPart, x, y);
                            break;
                        case 'C':
                            parseSnippetCell(x, y);
                            break;
                        case 'S':
                            parseSpawnCell(cellPart, x, y);
                            break;
                    }
                }
            }

            // Controle dos indices da matriz
            if (++x == this.width) {
                x = 0;
                y++;
            }
        }

        this.distributeInfluence();
    }

    /**
     * Distribui as influencias nas celulas da malha
     */
    private void distributeInfluence() {
        // Caso a classe não tenha ainda sido instanciada
        if (this.distributeInfluence == null) {
            // Atribuir nova instancia
            distributeInfluence = new DistributeInfluence(this.grid, this.myPosition);
        } else {
            // Atualiza posição do jogador
            distributeInfluence.setPlayerPosition(this.myPosition);
        }
        // Realizar a distribuição de influências
        distributeInfluence.distribute(getSnippetPositions(), getEnemyPositions(),
                getTickingBombPositions(), getEnemyTypes());
    }

    /**
     * Stores the position of one of the players, given by the id
     *
     * @param id Player ID
     * @param x  X-position
     * @param y  Y-position
     */
    private void parsePlayerCell(char id, int x, int y) {
        if (id == this.myId.charAt(0)) {
            this.myPosition = new Point(x, y);
        } else if (id == this.opponentId.charAt(0)) {
            this.opponentPosition = new Point(x, y);
        }
    }

    /**
     * Stores the position of an enemy. The type of enemy AI
     * is also given, but not stored in the starterbot.
     *
     * @param type Type of enemy AI
     * @param x    X-position
     * @param y    Y-position
     */
    private void parseEnemyCell(char type, int x, int y) {
        this.enemyPositions.add(new Point(x, y));
        this.enemyTypes.add(BugType.declare(Integer.parseInt(String.valueOf(type))));
    }

    /**
     * Stores the position of a bomb that can be collected or is
     * about to explode. The amount of ticks is not stored
     * in this starterbot.
     *
     * @param cell The string that represents a bomb, if only 1 letter it
     *             can be collected, otherwise it will contain a number
     *             2 - 5, that means it's ticking to explode in that amount
     *             of rounds.
     * @param x    X-position
     * @param y    Y-position
     */
    private void parseBombCell(String cell, int x, int y) {
        if (cell.length() <= 1) {
            this.bombPositions.add(new Point(x, y));
        } else {
            this.tickingBombPositions.add(new Point(x, y));
            this.grid.getCell(x, y).setDangerLaser(Integer.parseInt(String.valueOf(cell.charAt(1))));
        }
    }

    /**
     * Stores the position of a snippet
     *
     * @param x X-position
     * @param y Y-position
     */
    private void parseSnippetCell(int x, int y) {
        this.snippetPositions.add(new Point(x, y));
    }

    /**
     * Salva a quantidade de turnos que falta para um bug spawnar na celula
     *
     * @param cell Celula spawn
     * @param x    Posição X
     * @param y    Posição Y
     */
    private void parseSpawnCell(String cell, int x, int y) {
        if (cell.length() > 1) {
            this.grid.getCell(x, y).setRoundSpawn(Integer.parseInt(String.valueOf(cell.charAt(1))));
        }
    }

    //Pick the best move type out of getValidMoveTypes
    public MoveType getBestMoveTypes() {
        Cell myCell = this.grid.getCell(this.myPosition.x, this.myPosition.y);

        return myCell.getBestValidMove();
    }

    public void setMyId(int id) {
        this.myId = id + "";
    }

    public void setOpponentId(int id) {
        this.opponentId = id + "";
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Point getMyPosition() {
        return this.myPosition;
    }

    public Point getOpponentPosition() {
        return this.opponentPosition;
    }

    public ArrayList<Point> getEnemyPositions() {
        return this.enemyPositions;
    }

    public ArrayList<Point> getSnippetPositions() {
        return this.snippetPositions;
    }

    public ArrayList<Point> getBombPositions() {
        return this.bombPositions;
    }

    public ArrayList<Point> getTickingBombPositions() {
        return this.tickingBombPositions;
    }

    public ArrayList<BugType> getEnemyTypes() {
        return enemyTypes;
    }
}
